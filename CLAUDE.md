# heating-service

Controls the house heating. Temperature readings arrive over RabbitMQ from `amx-service`;
for every reading the service stores the measurement, decides per room whether the heater
should run and drives the matching Shelly relay over HTTP. The aggregate state ("is heating
required right now") is polled by `boiler-service`, which fires the furnace from it.

Part of the smart-home-automation-system organization — org-wide conventions, the
repository map and working rules come from the workspace-level context
(`organization-repository/claude/organization.md`). The user writes the code in this
repository themselves; Claude's default role here is analysis, code review and security
review.

## Role in the system

- Consumes: `temperature.prod.heating` on the `/temperature` virtual host
  (`TemperatureMessage` from `smart-home-sdk`), published by `amx-service`.
- Publishes: plain-text notifications about silent temperature sensors to the headers exchange
  `notification` on the `/notification` virtual host (HAS-94), consumed by
  `notification-service` and posted on Discord.
- Is called by: `boiler-service` (`GET /home/heating/status/active`) and, through
  `api-gateway-service`, by whoever switches the heating on and off (`/home/heating`).
- Calls: the Shelly relays on the LAN over HTTP, and PostgreSQL.
- Owns its own database, `home-automation-heating` — it got one in HAS-123 because the
  legacy database carried a `V1` migration that no longer matched; never `flyway repair`.
- Uses libraries: `cholewa-commons`, `smart-home-sdk`, `shelly-client`.

## Build & run

- Build + tests: `mvn verify`
- Local run: `home,local` Spring profiles, port `6002` (Actuator `8002`); in-cluster port
  `6200`, Actuator `8200`. Needs PostgreSQL and a RabbitMQ broker; the `local` profile uses
  the `temperature.dev.heating` queue.
- Spring Boot **4.1.1** with logbook 4.2.0 since HAS-151 (2026-10-03); own libraries on the
  latest releases, as the org rule asks of every service task.

## Specifics

- **The notification connection is deliberately not a bean** (`NotificationRabbitConfig`).
  The exchange is on another virtual host, so it takes a second connection — but a
  `ConnectionFactory` or `RabbitOperations` bean makes `RabbitAutoConfiguration` back off, and
  the temperature listener would lose the auto-configured connection with its manual ack,
  prefetch and observation. The factory and the template are built by hand inside the
  `NotificationPublisher` bean method; observation is switched on there explicitly, because
  `spring.rabbitmq.template.*` never reaches this template. The connection is opened on the
  first publish, so a wrong password shows up in the hourly check, not at startup — a missing
  one does fail the startup, `notification.password` has no default outside the `test`
  document.
- **A publish completes only on a broker confirm without a return** (`NotificationPublisher`,
  correlated confirms + mandatory). A `send` that merely returns proves nothing: an unroutable
  message is confirmed too, and the alert row would be written for a notification that reached
  no queue.
- **The `level` header (`error` / `warn` / `info`) is for the reader, not for the broker**: the
  exchange routes by `category` and `env` only, and `notification-service` colors the Discord
  message by `level`. The first alert is an `error`, a reminder a `warn` — both on the `alert`
  queue — and a recovery an `info`.
- **Notifications are plain text, not JSON.** `notification-service` reads the message as a
  `String` with the default converter; through `JacksonJsonMessageConverter` the text would
  arrive quoted and typed `application/json`.
- **The sensor check publishes first and writes the alert row second.** A failed write repeats
  the message on the next pass; the other order would lose it. `temperature_sensor_alert`
  holds one row per sensor that is silent right now — it changes on a state change or a
  reminder, never on a plain check. "Now" is truncated to the minute there, so two passes a
  reminder interval apart do not miss each other by scheduler jitter.
- **`heating.sensor-monitor.muted-rooms`** takes a sensor out of the alerts (a retired one would
  otherwise remind every day forever). The values bind to `RoomName` by constant name
  (`living-room`, not `living room`); an unknown name fails the startup. A muted room is still
  listed by the endpoint, with `muted: true`, and an alert row it still had is deleted without
  a message.
- **The check reads the rooms with `queryReadableSensors`**: a room whose last reading cannot
  be read is logged and skipped, so one failing query does not end the pass for the rooms
  after it. The endpoint uses the strict `querySensors` and fails as a whole.
- **"Now" is read inside the chain** in `SensorMonitorService` and `TemperatureSensorService`:
  Spring calls the reactive `@Scheduled` method once and re-subscribes to the same `Mono`.
  The test subscribes twice with the clock moved on.
- **The last reading is one indexed query per room** (`room_temperature (room, date DESC)`),
  run one room at a time so a pass holds a single pooled connection. A `GROUP BY` over the
  whole table would read every row.
- **`database.pool.max-size` is 2, and `prefetch` stays 3 — above the pool, on purpose.** The
  pool was 8 (prefetch 5) until 1.3.2 and 4 up to and including 1.6.0; the managed database
  has 22 connections for everyone (heating 2 / database 4 / water 2 / presence 2 = 10). The
  size is what keeps a rollout inside that budget (HAS-169): the Deployment rolls, so the old
  and the new pod each hold a pool and Flyway adds one JDBC connection — with this split even
  the three rolling services at once stay at 21.
  The old rule "prefetch below the pool size" assumed that an in-flight message holds a
  connection. It does not: the message takes one for the `save` of its reading, hands it
  back, and spends the rest of its time in the Shelly calls. So three messages in flight
  compete for two connections only for the length of a write, while a prefetch of 1 would
  make the whole chain serial — one slow relay holding up the readings of every room.
  What the small pool costs: with both connections in use — a write, a query of the hourly
  sensor pass, an endpoint — the next statement waits and fails after `max-acquire-time`
  (10 s). In the listener that failure is retried as transient (`Retry.backoff(2, …)`), so a
  starved write can hold its slot for about 30 s before the reading is logged and dropped;
  `POST /home/heating` answers it as its usual 400 "Heater problem". It works because nothing
  here holds two connections at once — no transaction, no parallel queries; code that does
  needs the size re-thought first. Watch `r2dbc_pool_pending_connections`:
  `HeatingServiceApplicationTest` pins the size, not the load.
- **Every Shelly call has a connect and a response timeout** — `shelly.actor.connect-timeout`
  (10 s) and `shelly.actor.response-timeout` (5 s), the defaults of `ShellyTimeoutProperties`
  (HAS-169; the values of `boiler-service`), set on the `HttpClient` in `AppConfig`. A bare
  number is seconds and anything outside 1–60 s is refused at startup — zero would switch
  netty's timeout off, and "5000" meant as milliseconds would be 83 minutes. Until then a relay that accepted the connection and never answered
  held its message for good, and three such messages stalled the consumer. The response
  timeout is netty's: the longest silence while the response is read, not a limit on the
  whole call — a device trickling bytes is not cut off, and waiting for a pooled HTTP
  connection (45 s by default) is not covered either.
  A timeout ends the call the way every device error does: `ShellyClient` logs it at ERROR
  and wraps it in a `BoilerException` that carries the cause. **What that aborts** was the
  point of the review. `HeatingService.handleHeaterActor` skips an actor whose device call
  failed (WARN) — the other actors of the room are still driven and the pass goes on to
  `anyHeaterActive` and the floor pump. Before, one actor cancelled the rest and the pass ended
  there; harmless while a slow relay merely answered late, not once slow became an error.
  Only a `BoilerException` is skipped: anything else — a room missing from `ShellyConfig` or
  the relay map — still ends the pass, and `HomeService` logs why. One room is in that state
  today: SANCTUM has a radiator of its own, but it is driven outside these services — for now
  by a scene in the Shelly cloud, from the temperature sensor of that room (owner,
  2026-10-06). `HomeConfig` models the radiator as an actor while `ShellyConfig` and the
  relay map have no entry for it, deliberately: this service must not switch it. A SANCTUM
  reading reaching the listener would therefore end its pass with "Unknown configuration for
  room heater" — do not "fix" that by adding a relay entry.
  What to know about the skip:
  - State is written only from a device response, never from the intent, so a skipped actor
    keeps what it last reported. **That value can be old, and it still feeds
    `roomHeatingEnabled`, `anyHeaterActive` and the floor pump**: a relay that went away
    while "on" keeps the furnace requested for as long as it is away. That was so before
    the skip too (every other room's pass recomputed the flag from the same state); nothing
    here ages the state out.
  - After a failed **status read** the timestamp stays stale and the next reading of the
    room asks again. After a failed **switch command** the status was just refreshed, so the
    relay is not re-read for five minutes — if it did switch and only answered too late, the
    service is wrong about it for that long.
  - The floor pump is still fail-fast: a failed pump status read ends the message before
    `setFloorPump`, so the pump waits for the next reading of any room.
  - A new step in that chain gets its own error handling; do not write state before the
    device answered.
  `ShellyTimeoutPropertiesTest` pins the values and the bounds, `AppConfigTest` that they
  reach the client and that a silent device ends as a timeout, `HeatingServiceTest` the skip.
- **The rooms are served from memory, as replies of their own** (HAS-197: `GET /rooms`,
  `/rooms/{name}`, `/floor-pump`; `RoomReply`, `FloorPumpReply`, mapped by hand in
  `RoomMapper`). The internal `Room` is mutable and starts with defaults — a temperature of
  0.0, a heater that is "not working" — so the mapper leaves a value out unless the timestamp
  next to it says it was measured or reported: **never serialize `Room`, `HeaterActor` or
  `FloorPump` themselves**, and a new field of the state gets the same question ("how does a
  reader tell it was never set?"). The JSON is the contract of the web dashboard, pinned whole
  and strict in `RoomControllerTest`. `scheduledTemperature` is the one
  field worked out when asked (`RoomMapper`, by the `Clock` bean) instead of copied from the
  state: the highest of the schedules that are on now, by `Schedule.covers` - the rule the
  control loop decides by as well, so change it there and nowhere else. Two things a reader of it has to know: `inSchedule` /
  `targetTemperature` are the control loop's decision at the last reading (a schedule is on
  **and** the room is colder than it asks), not "a schedule is on"; and the state is read from
  an HTTP thread while the listener writes it — the fields are `volatile`, so a reader sees what
  was written, but a reply is not an atomic snapshot. Two pairs are made consistent: a
  temperature and its time are written, seeded and read as one, through the synchronized
  `update` / `updateIfAbsent` / `snapshot` of `Temperature` (`HomeService`,
  `RoomTemperatureSeeder`, `RoomMapper`), and `RoomMapper` makes
  `inSchedule` and `targetTemperature` agree with each other. At a start `RoomTemperatureSeeder` gives every room its
  last stored temperature with the time of the measurement (owner, 2026-10-08), so a rollout does
  not blank the dashboard. It changes what a reader sees and nothing else: no pass is started,
  and a pass never acts on a seeded value, because it begins by writing the reading that
  triggered it - keep it that way. It never fails the startup, runs after the heating switch is loaded
  (`@Order` on the two runners) and does not replace a reading the listener delivered in the
  meantime. The heaters are not seeded, and
  `inSchedule` / `heatingEnabled` are left out of a reply until a pass has decided them: the
  setters `HeaterActor.setInSchedule` and `Room.setRoomHeatingEnabled` are written by hand and
  mark "decided" - set those fields through them only.
  An unknown room is a 404 with the code `NOT_FOUND_ROOM`; the names of `HeatingErrorId` are
  wire contract, pinned by `HeatingErrorIdTest`, and the WARN of the processor by
  `RoomControllerTest`. `HeaterType` goes out as `radiator` / `floor` (`@JsonValue`); `mode`,
  the schedule `type` and `days` as constant names.
- **The listener acknowledges manually** (`acknowledge-mode: manual`): with the default the
  container acks before the reactive pipeline runs, so the prefetch would throttle nothing.
- **`.contextCapture()` is the last operator of the listener chain** in
  `RabbitTemperatureConsumer` and has to stay there — Spring AMQP does not put the listener
  observation into the reactor context, so without it everything a message triggers is
  logged without a `traceId`.
- **The pooled `ConnectionFactory` comes from `cholewa-commons`** via the `database.*` group;
  there is no `DbConfig` here and no `@EnableR2dbcRepositories`. Since `cholewa-commons` 1.5
  the pool validates every connection on acquire (`SELECT 1`, 2 s) — before that this
  service kept a broken connection for as long as it was in use.
- **The RabbitMQ connection is named after the pod** (`RabbitConfig`, HAS-106): the broker shows
  `heating-service-<pod id>`, which tells the old pod from the new one during a rollout.
  `HOSTNAME` is taken only when it starts with `spring.application.name` — outside the cluster
  it is missing, empty, a workstation or a container id, and the name is then
  `heating-service-local`. The same convention holds in every service that talks to the broker.
  The second connection, the one publishing notifications, takes the name from the same
  strategy and adds `/notification`.
- **Surefire activates the `test` profile for every class** (`spring.profiles.active` in the
  pom), so a test without `@ActiveProfiles` does not fall through to the shared document and
  switch the log output to JSON for the classes that follow.
- **Tests still use the legacy `com.squareup.okhttp3:mockwebserver`**, which brings JUnit 4
  onto the classpath — a JUnit 4 test compiles and never runs. `mockwebserver3` is the
  replacement (see `presence-service`); not migrated yet.
