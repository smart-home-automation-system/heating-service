# heating-service

---

[![CI](https://github.com/smart-home-automation-system/heating-service/actions/workflows/CI.yml/badge.svg)](https://github.com/smart-home-automation-system/heating-service/actions/workflows/CI.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_heating-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_heating-service)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_heating-service&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_heating-service)

![GitHub Release Date - Published_At](https://img.shields.io/github/release-date/smart-home-automation-system/heating-service?style=plastic)
![GitHub Release](https://img.shields.io/github/v/release/smart-home-automation-system/heating-service?style=plastic)

---

![GitHub top language](https://img.shields.io/github/languages/top/smart-home-automation-system/heating-service?style=plastic)
![Java](https://img.shields.io/badge/java-21-yellow?style=plastic)
![SpringBoot](https://img.shields.io/badge/SpringBoot-4.1.1-blue?style=plastic)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_heating-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_heating-service)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_heating-service&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_heating-service)


![GitHub issues](https://img.shields.io/github/issues/smart-home-automation-system/heating-service?style=plastic)
![GitHub contributors](https://img.shields.io/github/contributors/smart-home-automation-system/heating-service?style=plastic)
![GitHub pull requests](https://img.shields.io/github/issues-pr-raw/smart-home-automation-system/heating-service?style=plastic)

![GitHub last commit](https://img.shields.io/github/last-commit/smart-home-automation-system/heating-service?style=plastic)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/smart-home-automation-system/heating-service?style=plastic)

---

# Description

Controls the house heating. Temperature readings arrive over RabbitMQ — they are published
by `amx-service` from the sensors of the AMX control system, and are expected at least once
per hour. For every reading the service stores the measurement, then decides per room
whether the heater should run: it compares the reading with the target temperature of the
room's active schedule and drives the matching Shelly Pro 4 relay over HTTP. Relay state is
cached and re-read from the device only when it is older than five minutes, so a burst of
readings does not turn into a burst of device calls. Radiator and floor heaters are handled
separately — when any floor heater is on, the floor pump is switched on as well.

The aggregate state ("is heating required right now") is exposed over REST and polled by
`boiler-service`, which uses it to decide whether to fire the furnace. The heating system
can also be switched on and off through the API; that switch is persisted in Postgres and
restored on startup.

The service also watches the sensors themselves. Once an hour it compares the last stored
reading of every room with the clock: a sensor silent for more than 24 hours is reported
through `notification-service` (which posts it on Discord), the report is repeated every 24
hours for as long as the sensor stays silent, and one more message says that it is reporting
again. Which sensors are currently reported is kept in Postgres, so a restart neither repeats
an alert nor loses the "back again" message. A room that has never stored a reading is not
watched — the service cannot tell a broken sensor from a room without one. A sensor taken
out of service is silenced by listing its room in `heating.sensor-monitor.muted-rooms`.

## Run locally

```bash
mvn verify
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

| | Application | Actuator |
|---|---|---|
| `local` profile | 6002 | 8002 |
| in the cluster (`home` profile) | 6200 | 8200 |

Needs a PostgreSQL database (R2DBC at runtime, Flyway for migrations) and a RabbitMQ broker
with the `/temperature` and `/notification` virtual hosts. Connection details come from the `database.*`
properties and `spring.rabbitmq.*`; in the cluster they are injected from Kubernetes
secrets. The `local` profile points RabbitMQ at `localhost` and uses the
`temperature.dev.heating` queue, and publishes its notifications with `env=dev`.

The `ConnectionFactory` itself is built by `cholewa-commons`, not by this service. Only the
pool size is pinned here — `database.pool.max-size: 2` — because the managed database allows
22 backend connections in total, shared by four services, and a rollout holds the pool twice; the remaining pool settings come
from the library defaults, which since `cholewa-commons` 1.5 include validating every
connection on acquire.

## API

All paths are served under the `/home/heating` base path (`spring.webflux.base-path`), which
is also the path the Kubernetes ingress routes to this service.

| Method | Path | Description |
|---|---|---|
| `GET` | `/home/heating` | Current state of the heating system switch, with the timestamp of the last change |
| `POST` | `/home/heating?turn=on\|off` | Enables or disables the heating system and persists the change |
| `GET` | `/home/heating/temperature/sensors` | Last reading of every temperature sensor: `room`, `lastReadingAt`, `stale` (silent for longer than `heating.sensor-monitor.stale-after`) and `muted` (excluded from the alerts); rooms that never reported are left out |
| `GET` | `/home/heating/status/active` | Whether the system is enabled and any heater is currently active — polled by `boiler-service` |

Actuator endpoints, including the `readiness` and `liveness` health groups used by the
Kubernetes probes, live on the management port, not on the application one.

## Messaging

| Direction | Queue / exchange | Virtual host | Payload |
|---|---|---|---|
| consumes | queue `temperature.prod.heating` (`temperature.dev.heating` in the `local` profile) | `/temperature` | `cloud.cholewa.home.model.TemperatureMessage` (`smart-home-sdk`) |
| publishes | exchange `notification` (headers), `category=alert\|info`, `env=prod` (`dev` in the `local` profile), `level=error\|warn\|info` | `/notification` | plain text (`text/plain`, UTF-8) |

Messages are produced by `amx-service`; both sides use `JacksonJsonMessageConverter`. The
listener acknowledges manually — it returns a `Mono`, so the acknowledgement has to wait for
the reactive pipeline to finish — and the prefetch (1) is kept below the database connection
pool size (2), so a backlog is held by the broker instead of the service.

Notifications about silent sensors go to the `notification` exchange, which lives on another
virtual host, so the service holds a second connection for it (user `notification`, password
from `notification-rabbitmq-password` — it has no default, the service does not start without
it). The exchange is a headers exchange: the routing key is
ignored and the queue — `notification.<env>.<category>` — is chosen by the `category` and
`env` headers. A sensor that went silent or is still silent is an `alert`, a sensor that came
back an `info`. A third header, `level`, is not routed by: `notification-service` picks the
color on Discord from it — `error` for the first alert, `warn` for the reminders, `info` for
the recovery. A notification counts as sent only when the broker has confirmed it and has
not returned it as unroutable; until then the sensor state is not updated and the next check
tries again. The check is configured under `heating.sensor-monitor` (`cron`, `stale-after`,
`reminder-interval` — a bare number is hours, the minimum is one hour — and `muted-rooms`,
a list of room names such as `[sauna, living-room]`).
