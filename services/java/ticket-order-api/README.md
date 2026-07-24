# ticket-order-api

Spring Boot HTTP API for TicketOrderPlatform.

## Stack

- Java 25
- Spring Boot 4.1
- Spring Web
- Spring Security
- Maven

## Architecture

The module follows a hexagonal architecture package layout:

- `domain/model` - framework-free domain objects
- `application/port/in` - inbound use case contracts
- `application/service` - use case implementations
- `adapter/in/web` - HTTP controllers that call application ports
- `infrastructure/config` - Spring, CORS, and security configuration

## Endpoints

```text
GET /hello
```

Returns:

```text
Hello, World!
```

## Commands

From the repository root:

```sh
make test-api
make package-api
make run-api
```

From this module directory:

```sh
mvn test
mvn package
mvn spring-boot:run
```

## Docker

Build from the repository root:

```sh
docker build -f services/java/ticket-order-api/Dockerfile -t ticket-order-api .
```

Run:

```sh
docker run --rm -p 8080:8080 ticket-order-api
```

## Configuration

CORS defaults are configured in `src/main/resources/application.properties`.

```properties
ticket-order-platform.cors.allowed-origins=http://localhost:5173
ticket-order-platform.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
ticket-order-platform.cors.allowed-headers=*
ticket-order-platform.cors.allow-credentials=false
ticket-order-platform.cors.max-age=3600
```
