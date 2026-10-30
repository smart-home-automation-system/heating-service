# heating-service

Controls the house heating. Temperature readings arrive over RabbitMQ from `amx-service`;
for every reading the service stores the measurement, decides per room whether the heater
should run and drives the matching Shelly relay over HTTP - and since HAS-352 it decides every room
again every minute and right after the switch of the house, without waiting for a reading. The aggregate state ("is heating
required right now") is polled by `boiler-service`, which fires the furnace from it.

Part of the smart-home-automation-system organization — org-wide conventions, the
repository map and working rules come from the workspace-level context
(`organization-repository/claude/organization.md`). The user writes the code in this
repository themselves; Claude's default role here is analysis, code review and security
review - the owner had Claude implement the window hold (HAS-232) and the decision of every room (HAS-352).

## Role in the system

- Consumes: `temperature.prod.heating` on the `/temperature` virtual host
  (`TemperatureMessage` from `smart-home-sdk`), published by `amx-service`; and since HAS-232
  `device.prod.heating` on `/device` - the events of every device, of which only the window
  sensors are read.
- Publishes: plain-text notifications about silent temperature and window sensors to the headers exchange
  `notification` on the `/notification` virtual host (HAS-94), consumed by
  `notification-service` and posted on Discord.
- Is called by: `boiler-service` (`GET /home/heating/status/active`) and, through
  `api-gateway-service`, by whoever switches the heating on and off (`/home/heating`).
- Calls: the Shelly relays on the LAN over HTTP, PostgreSQL, `database-service` (the device
  registry: opening sensors and BTHome gateways) and the BTHome gateways (`Shelly.GetComponents`
  through `ShellyRpcClient` of `shelly-client`).
- Owns its own database, `home-automation-heating` — it got one in HAS-123 because the
  legacy database carried a `V1` migration that no longer matched; never `flyway repair`.
- Uses libraries: `cholewa-commons`, `smart-home-sdk`, `shelly-client`.

## Build & run

- Build + tests: `mvn verify` - needs a running Docker (one test starts a PostgreSQL in it)
- Local run: `home,local` Spring profiles, port `6002` (Actuator `8002`); in-cluster port
  `6200`, Actuator `8200`. Needs PostgreSQL and a RabbitMQ broker; the `local` profile uses
  the `temperature.dev.heating` and `device.dev.heating` queues, `database-service` on
  `localhost:6005` and a dummy for the three passwords of the broker, so it starts without
  `rabbitmq-password`, `notification-rabbitmq-password` and `device-rabbitmq-password` - under
  `home` alone the start is refused without them (below).
- Spring Boot **4.1.2** (HAS-303; 4.1.1 since HAS-151, 2026-10-03) with logbook 4.2.0; `cholewa-commons` 1.12.0,
  `smart-home-sdk` 1.15.0, `shelly-client` 2.3.0 since HAS-352; own libraries on the
  latest releases, as the org rule asks of every service task.

## Specifics

- **Every scheduled task is bounded and runs on a pool** (HAS-303, `cholewa-commons` 1.11.0). Spring
  runs a reactive `@Scheduled(fixedDelay)` method by blocking a scheduler thread until its `Mono`
  completes; with Boot's one thread a hung pass stopped every task of blinds-service on 2026-10-22.
  The library defaults `spring.task.scheduling.pool.size` to 10 (`HeatingServiceApplicationTest`
  pins it), and each of the three tasks wraps its pass in `ScheduledPass.bounded` - the pass built
  anew for every run, cancelled at its `LIMIT` with a WARN, always completing:
  `SensorMonitorScheduler` (hourly cron, 15 min), `DecisionScheduler` (every minute, 50 s - the
  `WindowHoldScheduler` until HAS-352) and
  `WindowSensorsRefresh` (every five minutes, 4 min). Their own `onErrorResume` with the ERROR stays
  inside the bound. A cut window hold leaves a heater the next minute switches off; a cut refresh
  leaves the last sensors and checks the silent ones with the next read. What became concurrent
  was checked: the state the tasks share - `WindowSensors` (`AtomicReference`,
  `ConcurrentHashMap`), the volatile fields of `Room` / `HeaterActor` - was already read and written
  by the two listeners at the same time, and the database passes (hourly check, the alerts of the
  window sensors) take one connection per statement, so with the listener they still fit the pool
  of 2 (a third waits `max-acquire-time`, as before).

- **Logbook logs from the reactive chain, through `cholewa-commons` 1.10.0** (HAS-298): the
  server through `LogbookWebFilter` (the library defaults `logbook.reactive.server-mode` to
  `web-filter`), every `WebClient` of the injected builder - the relays, `database-service`, the
  BTHome gateways of `shelly-client` - through `LogbookExchangeFilterFunction`. That is why the
  request and response lines carry `traceId` and `spanId` since then, and why **no
  `LogbookClientHandler` is added to an `HttpClient` here any more** (`AppConfig`,
  `RegistryWebClientConfig`, the former `shellyCallsInTheLog` bean): a handler logs outside the
  chain, without the trace, and next to the filter it logged every call twice. A `WebClient`
  built with `WebClient.create()` would not be logged - every one here comes from the builder.
  Since 1.10.1 the library also keeps the actuator out of the server side of the log
  (`logbook.server.predicate.exclude` defaulted to `/actuator/**`): the probes of the kubelet
  and the scrape of Prometheus leave no line. Do not add a `logbook.server.predicate.exclude`
  of our own without `/actuator/**` in it - a list of ours replaces the default whole.
- **The notification connection is deliberately not a bean** (`NotificationRabbitConfig`).
  The exchange is on another virtual host, so it takes a second connection — but a
  `ConnectionFactory` or `RabbitOperations` bean makes `RabbitAutoConfiguration` back off, and
  the temperature listener would lose the auto-configured connection with its manual ack,
  prefetch and observation. Since HAS-206 the factory and the template are built by
  `NotificationConnection` of `cholewa-commons` (1.8.0), which keeps both to itself: the beans
  here are that connection and the `NotificationPublisher` it gives, **never what
  `rabbitOperations()` returns**. Observation is switched on by the library, because
  `spring.rabbitmq.template.*` never reaches this template. The connection is opened on the
  first publish, so a wrong password shows up in the hourly check, not at startup. **A missing
  or empty one fails the start**, for the three passwords of the broker -
  `spring.rabbitmq.password`, `notification.password` and `device.password`: since HAS-232 by
  `cholewa.required-properties` of `cholewa-commons` 1.9.0, before the first bean (the own
  `BrokerPasswordCheck` of HAS-217 is gone). None has a default in a document a pod runs with,
  and that alone did not do it: the binder leaves a placeholder it cannot resolve as its literal
  text, so the password would bind as the string `${...}` and the pod turn Ready with every
  publish refused. `RequiredPropertiesTest` ties the list to the placeholders; the `local`
  document gives all three a dummy and the `test` document a value.
  The connection is closed with the context (`close()`, found by Spring as the destroy method),
  and closed is final: a publish during the shutdown fails instead of opening a connection
  again. `HeatingServiceApplicationTest` and `NotificationRabbitConfigTest` pin all of it — one
  `ConnectionFactory` bean, the listener on it with manual ack, prefetch 3 and observation —
  and both fail when the template is exposed as a bean (tried).
- **The publisher and the connection name are the library's; the `notification.*` group is
  not** (HAS-206). `NotificationConnectionProperties` (virtual host, user, password, exchange,
  env) stays here, because the library has no properties for a second connection; its own
  group, `cholewa.notification.*`, **stays unset** — `cholewa.notification.enabled` would
  create a publisher over the connection of the listener, on `/temperature`, where there is
  no notification exchange. The deleted copy of the publisher differed from the library's in
  three things: a refused, returned or unconfirmed message is a `NotificationPublishException`
  (it was a `HeatingException`, and Reactor's `TimeoutException` for the timeout) — nothing
  here matches on the type, `SensorMonitorService` resumes on any error of a sensor; what
  follows a publish, the write of the alert row, runs on `boundedElastic` and no longer on the
  thread of the broker connection; and the shutdown above. The tests of the confirm, return
  and timeout paths live in the library now.
- **A publish completes only on a broker confirm without a return** (the library's
  `NotificationPublisher`, correlated confirms + mandatory). A `send` that merely returns
  proves nothing: an unroutable message is confirmed too, and the alert row would be written
  for a notification that reached no queue.
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
- **The last reading is one indexed query per room** (`room_temperature (room, date DESC)`, `V3`),
  run one room at a time so a pass holds a single pooled connection. A `GROUP BY` over the
  whole table would read every row.
- **The temperature history of a room is averaged by the database** (HAS-199:
  `GET /rooms/{name}/temperature/history?from=&to=`, `TemperatureHistoryReply`). The JSON is
  the contract of the charts of the web dashboard, pinned whole and strict in
  `TemperatureHistoryControllerTest`. What to keep:
  - **The rules of the range are those of the reports of `presence-service`** (`HistoryRange`):
    local date-times without an offset - an offset is a 400, by the pattern on the controller -
    `from` before `to`, start included and end not, at most 31 days. A range in the future is
    valid and simply has no points.
  - **The service chooses the width of the bucket** from the length of the range: 20 min up to
    2 days, 1 h up to 8, 3 h up to 31 - at most 144, 192 and 248 points, one more when the range
    starts inside a bucket. Sized by the sensors:
    the median gap between two readings of a room is 42 s to 16 min (production, 2026-10-09 - a
    median, not a rate: a sensor reports on a change and pauses for long, the busiest room
    stored 15 254 rows in a month), and a bucket narrower than the slowest sensor leaves a hole
    in its line. **Every width divides a day**, which is what makes
    a bucket start on the clock of the house; `HistoryRangeTest` pins that for a width added
    later.
  - **A bucket is aligned to the clock of the house, not to `from`**: a range that starts
    inside a bucket gets a first point whose `at` is before `from`, averaged from the readings
    within the range only - and so is its last one. On that clock the hour repeated when the
    summer time ends is averaged into the same buckets twice, and the hour skipped when it
    begins has no bucket: once a year the 20 min and 1 h histories show a gap of an hour that
    no sensor caused.
  - **The years are bounded** (2000 to 9999): `LocalDateTime` holds years the timestamp of the
    database does not, and a short range out there passed every other rule and failed in the
    query, as a 500 (found in review). That is the upper bound; the lower one is no limit of the
    database, only of sense - the house has no reading from before 2026.
  - **A bucket without a reading has no point** - no nulls, no zeros. The reader breaks the line
    where two points are further apart than `bucketSeconds`, which is why the width is in the
    answer.
  - **The readings are asked for by the name of the configuration** (`RoomService.queryRoomName`),
    never by the text of the path: they are stored as `living room`, and asked for as
    `Living Room` the query would find nothing and answer an empty history with a 200.
  - **The query is PostgreSQL's own** (`TemperatureRepository.findHistory`), so its test runs
    it against a PostgreSQL 16 in Docker: `TemperatureRepositoryTest`, the one test here that
    executes SQL - the repository is a mock in every other (Testcontainers, added at the
    owner's word, 2026-10-10). **`mvn verify` therefore needs a running Docker**; without one
    that test fails, on purpose - it is not skipped. It is a `@DataR2dbcTest` slice: the
    connection comes from `spring.r2dbc.*`, not from the pooled factory of `cholewa-commons`,
    so the container needs no SSL, and the tables are created by the migrations of the service,
    run by hand in the test with the defaults of Flyway (the `test` profile switches Flyway
    off) - a `spring.flyway.*` setting added later has to be repeated there. The JVM of that
    test is put on the zone of the house, as the image is, so a conversion of a date-time on its
    way to the database would show on the two nights of the clock change it asks about. What it does not cover is
    the way from the request to the query - for that the jar was started with `home,local` on
    such a database and the endpoint called; there the container has to speak SSL (`-c ssl=on`
    with the snakeoil certificate of the image), and with no broker on the configured port the
    service starts all the same and drives nothing.
  - It is one statement on one pooled connection, through the index on `(room, date)`; a month
    of the busiest room is some 15 000 rows read and 248 answered, in 170 ms on the production
    database (2026-10-09). It shares the pool of 2 with the listener: a client that asked for
    many rooms at once, over and over, would make the readings wait - the dashboard asks for
    one room at a time. Nothing limits it here.
  - **The refusals of the range carry no code** (a `ResponseStatusException`, as in
    `presence-service`): the dashboard never sends a range the rules refuse, so nothing
    branches on them. A client that has to tell them apart needs a `HeatingErrorId` first.
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
  - After a failed **status read** the timestamp stays stale and the next pass of the
    room asks again - within the minute since HAS-352. After a failed **switch command** the status was just refreshed, so the
    relay is not re-read for five minutes — if it did switch and only answered too late, the
    service is wrong about it for that long.
  - The floor pump is still fail-fast: a failed pump status read ends the message before
    `setFloorPump`, so the pump waits for the next pass - the decision of every minute since HAS-352.
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
  not blank the dashboard. No pass is started by it - **but since HAS-352 the decision of every
  minute acts on a seeded value**: the task asks for every room with a known temperature, and the
  seeded one is the last stored reading (a sensor silent for days is the alert of the sensor monitor,
  and the room keeps deciding by its last reading, as it always kept its last state). It never fails the startup, runs after the heating switch is loaded
  (`@Order` on the two runners) and does not replace a reading the listener delivered in the
  meantime. The heaters are not seeded, and
  `inSchedule` / `heatingEnabled` are left out of a reply until a pass has decided them: the
  setters `HeaterActor.setInSchedule` and `Room.setRoomHeatingEnabled` are written by hand and
  mark "decided" - set those fields through them only. The builders write the fields directly
  and leave "decided" false, which is what the tests of the control loop use them for; a
  builder is no way to hand a decision to `RoomMapper`.
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
- **The RabbitMQ connection is named after the pod** (HAS-106; since HAS-206 by
  `RabbitConnectionNameAutoConfiguration` of `cholewa-commons`, bean
  `cholewaPodConnectionNameStrategy`): the broker shows
  `heating-service-<pod id>`, which tells the old pod from the new one during a rollout.
  `HOSTNAME` is taken only when it starts with `spring.application.name` — outside the cluster
  it is missing, empty, a workstation or a container id, and the name is then
  `heating-service-local`. The same convention holds in every service that talks to the broker.
  The second connection, the one publishing notifications, takes the name from the same
  strategy and adds `/notification` (`NotificationConnection`). **A `ConnectionNameStrategy`
  bean declared here switches the library's off** — there is none any more, and
  `NotificationRabbitConfigTest` pins the names of both connections.
- **Open windows hold the heating of a room** (HAS-232, package `window`). What to keep:
  - **`WindowSensors` is a copy of the one of `blinds-service` (HAS-290)**, judged per room
    instead of per blind - two copies stay (the commons rule). It reads every `opening sensor`
    and the switched-on `bthome gateway` devices of the registry (`DeviceRegistryClient`, a copy
    too) and each gateway with `ShellyRpcClient.dynamicComponents` at the start and every five
    minutes (`WindowSensorsRefresh`); between reads the `bthomesensor` status topics of
    `device.prod.heating`, picked by the routing key before a body is read
    (`WindowEventRouter`). A BLU sensor has no heartbeat: a state stands while its gateway
    confirms it within `heating.window.gateway-silent-after` (PT30M).
  - **Per room**: every opening sensor whose registry `room` is the room, doors included; a
    switched-off one is left out; a room without one heats as before. `open` beats `unknown`
    beats `closed`. **An unknown sensor never holds** - a dead battery must not leave a room
    cold (owner, 2026-10-16).
  - **The hold**: open for `heating.window.hold-after` (**PT3M**, owner 2026-10-17; 1 min - 1 h,
    pinned by `WindowPropertiesTest`), counted from `last_updated_ts` of the gateway (never
    later than now), by the window open longest. `HeatingService` judges it once per pass and
    treats a held room as out of schedule - off, never on; `inSchedule` / `targetTemperature`
    stay the decisions of the schedule. The decision of every minute (HAS-352,
    `HomeService.decideRooms`) applies the hold within the minute and lets the room heat again within
    the minute after the window closes - the pass of the held rooms alone (`stopHeldRooms`) is gone.
  - **The silent sensors** (`OpeningSensorMonitor`, after every refresh): a switched-on sensor
    of a room of `Home` without a known state for gateway-silent-after - since the registry first
    listed it switched on, or since a gateway last stood for a state - is an alert, a reminder
    every `heating.sensor-monitor.reminder-interval`, an `info` when a gateway stands for it
    again; `opening_sensor_alert` (V4) holds the rows, the message first and the row second (the
    writes deferred). Unknown but not yet silent - the half hour after a restart - leaves a row
    as it is: closing it there announced a false recovery after every rollout (found in review).
    A row of a sensor removed, switched off or moved to an unheated room goes without a message.
    During a rollout both pods run the pass - a duplicate alert is possible, as for the
    temperature sensors.
  - **The third connection** (`DeviceRabbitConfig`): `DeviceConnection` builds the factory to
    `/device` (user `device`, `device-rabbitmq-password`) by hand and keeps it - a copy of the
    setup of `NotificationConnection` without confirms - and the listener has its own container
    factory `deviceListenerContainerFactory` (manual ack, prefetch 10, observation), because
    `spring.rabbitmq.listener.simple.*` reaches only the auto-configured one.
    `HeatingServiceApplicationTest` pins both listeners on their connections.
  - `GET /rooms` answers `window` (`state` / `openSince` / `hold`), worked out when asked;
    the values of `state` are those of `blinds-service` - the dashboard copies them.
  - The Gen1 Door/Window 2 of the sauna entrance is out (owner, 2026-10-17, as in HAS-290): no
    `/events/gen1` here. The static `OpeningSensor` / `OpeningType` of `HomeConfig` are gone.
- **Every room is decided every minute and on the switch** (HAS-352, `HomeService.decideRooms`). A room
  was decided only when its reading came (about every 10 minutes): on 2026-10-30 two rooms started nine
  minutes after the switch, a switched-off house kept its relays on, and a schedule boundary waited for the
  next reading. What to keep:
  - **The path of a reading without its temperature**: `ScheduleService.processSchedule`, then
    `HeatingService.processHeatingRequest`, for every room whose temperature has a time (a seeded one
    too), one after the other; then "any heater active" and the floor pump once. A room without a
    temperature is skipped, a room that fails is logged and the others go on. `HomeDecisionTest` pins it
    on the real services with only the relays faked (the switch on and off, the 05:00 example, a skipped
    and a failing room, the pass subscribed again with the clock moved on).
  - **Every minute** by `DecisionScheduler` (`ScheduledPass.bounded`, 50 s), and **after the switch** by
    `HeatingStatusService`, detached from the reply (bounded 50 s, a failure logged): a caller waits for
    the switch only - the dashboard times a call out after 10 s, and a pass with slow relays takes longer.
  - **No extra device traffic**: a relay is commanded only when its state differs, its status read only
    when older than five minutes - so every relay is now re-read every five minutes, not with its reading.
  - **The logs stay quiet**: `ScheduleService` logs a schedule decision only when it changes, `HomeService`
    "any heater active" only on a change; a relay change keeps its INFO.
  - **Concurrency**: the minute pass, a pass after the switch and the temperature listener may decide one
    room at once - as the held-room pass did with the listener. The fields of `Room` / `HeaterActor` are
    volatile; two passes deciding one relay can both command it (an idempotent switch) and the later
    response wins - nothing is written before a device answered.
- **Surefire activates the `test` profile for every class** (`spring.profiles.active` in the
  pom), so a test without `@ActiveProfiles` does not fall through to the shared document and
  switch the log output to JSON for the classes that follow.
- **The `test` document points everything that leads out of the JVM at a dead address**
  (HAS-206, the convention of `boiler-service` since HAS-181): the broker and the database at
  `localhost:1`, the four Shelly hosts at `localhost` with port 1, the queue
  `temperature.test.heating` and `notification.env: test`, which no binding of the exchange
  matches. A `@SpringBootTest` context starts the listener and the two runners that read the
  database; until then it ran with the broker host, the queue and the `env` of production and
  was harmless only because `rabbitmq-headless` does not resolve outside the cluster.
  `database.host` is a literal there, so a `database-host` in the environment of the machine
  does not reach a test. `ApplicationYamlTest` reads the file per profile without starting
  anything and pins what the test document now hides from the context test: the values of
  production (`home`) and of `local`. **A new outward address gets a line in the `test`
  document and in that test** - `database-service` and the device queue have theirs since
  HAS-232. Scheduling is off under `test` since then (`SchedulingConfig`, `@Profile("!test")`):
  the read of the window sensors starts at once.
- **Tests still use the legacy `com.squareup.okhttp3:mockwebserver`**, which brings JUnit 4
  onto the classpath — a JUnit 4 test compiles and never runs. `mockwebserver3` is the
  replacement (see `presence-service`); not migrated yet.
