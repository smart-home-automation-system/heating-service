# heating-service

---

[![CI](https://github.com/smart-home-automation-system/heating-service/actions/workflows/CI.yml/badge.svg)](https://github.com/smart-home-automation-system/heating-service/actions/workflows/CI.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_heating-service&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_heating-service)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_heating-service&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_heating-service)

![GitHub Release Date - Published_At](https://img.shields.io/github/release-date/smart-home-automation-system/heating-service?style=plastic)
![GitHub Release](https://img.shields.io/github/v/release/smart-home-automation-system/heating-service?style=plastic)

---

![GitHub top language](https://img.shields.io/github/languages/top/smart-home-automation-system/heating-service?style=plastic)
![Java](https://img.shields.io/badge/java-17-yellow?style=plastic)
![SpringBoot](https://img.shields.io/badge/SpringBoot-4.0.1-blue?style=plastic)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_heating-service&metric=coverage)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_heating-service)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=smart-home-automation-system_heating-service&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=smart-home-automation-system_heating-service)


![GitHub issues](https://img.shields.io/github/issues/smart-home-automation-system/heating-service?style=plastic)
![GitHub contributors](https://img.shields.io/github/contributors/smart-home-automation-system/heating-service?style=plastic)
![GitHub pull requests](https://img.shields.io/github/issues-pr-raw/smart-home-automation-system/heating-service?style=plastic)

![GitHub last commit](https://img.shields.io/github/last-commit/smart-home-automation-system/heating-service?style=plastic)
![GitHub commit activity](https://img.shields.io/github/commit-activity/m/smart-home-automation-system/heating-service?style=plastic)

---

# Description

The general purpose of this service is to control the heating system based on the temperature readings from the sensors.
Temperature values are available via RabbitMQ. It is expected that values are min once per hour.
There is additional logic that checks if the temperature was updated within the last 1 hour, and if not, it triggers an alert.

This service communicates with the boiler-service via REST API to inform if heating is required or not.
