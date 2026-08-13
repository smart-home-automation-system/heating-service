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
![SpringBoot](https://img.shields.io/badge/SpringBoot-4.1.0-blue?style=plastic)
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
with the `/temperature` virtual host. Connection details come from the `database.*`
properties and `spring.rabbitmq.*`; in the cluster they are injected from Kubernetes
secrets. The `local` profile points RabbitMQ at `localhost` and uses the
`temperature.dev.heating` queue.

The `ConnectionFactory` itself is built by `cholewa-commons`, not by this service. Only the
pool size is pinned here — `database.pool.max-size: 8` — because the managed database allows
22 backend connections in total and this service holds the largest share of them; the
remaining pool settings come from the library defaults.

## API

All paths are served under the `/home/heating` base path (`spring.webflux.base-path`), which
is also the path the Kubernetes ingress routes to this service.

| Method | Path | Description |
|---|---|---|
| `GET` | `/home/heating` | Current state of the heating system switch, with the timestamp of the last change |
| `POST` | `/home/heating?turn=on\|off` | Enables or disables the heating system and persists the change |
| `GET` | `/home/heating/status/active` | Whether the system is enabled and any heater is currently active — polled by `boiler-service` |

Actuator endpoints, including the `readiness` and `liveness` health groups used by the
Kubernetes probes, live on the management port, not on the application one.

## Messaging

| Direction | Queue | Virtual host | Payload |
|---|---|---|---|
| consumes | `temperature.prod.heating` (`temperature.dev.heating` in the `local` profile) | `/temperature` | `cloud.cholewa.home.model.TemperatureMessage` (`smart-home-sdk`) |

Messages are produced by `amx-service`; both sides use `JacksonJsonMessageConverter`. The
listener acknowledges manually — it returns a `Mono`, so the acknowledgement has to wait for
the reactive pipeline to finish — and the prefetch is kept below the database connection
pool size, so a backlog is held by the broker instead of the service. The service does not
publish to RabbitMQ; outgoing traffic goes to the Shelly devices over HTTP and to
PostgreSQL.
