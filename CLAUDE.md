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
  (`TemperatureMessage` from `smart-home-sdk`), published by `amx-service`. It does not
  publish anything.
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

- **`database.pool.max-size` is 4, and `prefetch` is 3 because of it.** The listener returns
  a `Mono`, so every in-flight message may hold a connection; the prefetch has to stay below
  the pool size or a backlog is pulled into the service and waits on the pool instead of in
  the broker. Change one, check the other. The pool was 8 (prefetch 5) until 1.3.2; in the
  metrics of 2026-10-02/03 the service held 1-2 connections, and the managed database has 22
  for everyone (heating 4 / database 6 / water 4 / presence 2).
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
- **Surefire activates the `test` profile for every class** (`spring.profiles.active` in the
  pom), so a test without `@ActiveProfiles` does not fall through to the shared document and
  switch the log output to JSON for the classes that follow.
- **Tests still use the legacy `com.squareup.okhttp3:mockwebserver`**, which brings JUnit 4
  onto the classpath — a JUnit 4 test compiles and never runs. `mockwebserver3` is the
  replacement (see `presence-service`); not migrated yet.
