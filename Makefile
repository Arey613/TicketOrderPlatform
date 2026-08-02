.PHONY: help build package package-api package-web package-migrations test test-api test-web verify run run-api run-web clean check-maven check-npm check-docker install-web validate-contracts generate generate-api-contracts generate-web-contracts migrate-transactional migrate-analytical docker-build docker-build-api docker-build-web compose-build compose-up compose-down compose-logs

MAVEN ?= $(shell test -x ./mvnw && printf './mvnw' || printf 'mvn')
NPM ?= npm
DOCKER ?= docker
CONTRACT_MODULE := contracts/openapi/ticket-order-api
DB_MIGRATIONS_MODULE := database/java/migrations/ticket-order-db-migrations
DB_TRANSACTIONAL_MIGRATIONS_MODULE := $(DB_MIGRATIONS_MODULE)/ticket-order-transactional-migrations
DB_ANALYTICAL_MIGRATIONS_MODULE := $(DB_MIGRATIONS_MODULE)/ticket-order-analytical-migrations
API_MODULE := services/java/ticket-order-api
WEB_MODULE := apps/web/ticket-order-web
SPRING_PROFILES ?= local
API_IMAGE ?= ticket-order-api
WEB_IMAGE ?= ticket-order-web

help:
	@printf 'TicketOrderPlatform\n\n'
	@printf 'Targets:\n'
	@printf '  make build        Compile, test, and package modules\n'
	@printf '  make package      Build module artifacts\n'
	@printf '  make package-migrations Package database migration resources\n'
	@printf '  make test         Run all module tests\n'
	@printf '  make verify       Run Maven verify and web build\n'
	@printf '  make run          Start the Java API locally\n'
	@printf '  make run-api      Start the Java API locally\n'
	@printf '  make run-web      Start the React/Vite web app locally\n'
	@printf '  make install-web  Install web dependencies\n'
	@printf '  make validate-contracts Validate OpenAPI contracts\n'
	@printf '  make generate     Generate API code from OpenAPI contracts\n'
	@printf '  make migrate-transactional Run transactional Flyway migrations\n'
	@printf '  make migrate-analytical Run analytical Flyway migrations\n'
	@printf '  make docker-build Build API and web Docker images\n'
	@printf '  make compose-up   Build and start the Docker Compose stack\n'
	@printf '  make compose-down Stop the Docker Compose stack\n'
	@printf '  make compose-logs Follow Docker Compose logs\n'
	@printf '  make clean        Remove build output\n'

build: test package

package: package-api package-web package-migrations

package-api: check-maven
	$(MAVEN) -pl $(API_MODULE) -am package

package-web: check-npm
	$(NPM) --prefix $(WEB_MODULE) run build

package-migrations: check-maven
	$(MAVEN) -pl $(DB_TRANSACTIONAL_MIGRATIONS_MODULE),$(DB_ANALYTICAL_MIGRATIONS_MODULE) package

test: test-api test-web

test-api: check-maven
	$(MAVEN) -pl $(API_MODULE) -am test

test-web: check-npm
	$(NPM) --prefix $(WEB_MODULE) run test

verify: check-maven check-npm
	$(MAVEN) verify
	$(NPM) --prefix $(WEB_MODULE) run build

run: run-api

run-api: check-maven
	$(MAVEN) -pl $(API_MODULE) spring-boot:run -Dspring-boot.run.profiles=$(SPRING_PROFILES)

run-web: check-npm
	$(NPM) --prefix $(WEB_MODULE) run dev

install-web: check-npm
	$(NPM) --prefix $(WEB_MODULE) install

clean: check-maven
	$(MAVEN) clean
	rm -rf $(WEB_MODULE)/dist
	rm -rf $(WEB_MODULE)/src/generated

validate-contracts: check-npm
	$(NPM) --prefix $(WEB_MODULE) run validate:api

generate: generate-api-contracts generate-web-contracts

generate-api-contracts: check-maven
	$(MAVEN) -pl $(API_MODULE) generate-sources

generate-web-contracts: check-npm
	$(NPM) --prefix $(WEB_MODULE) run generate:api

migrate-transactional: check-maven
	$(MAVEN) -pl $(DB_TRANSACTIONAL_MIGRATIONS_MODULE) process-resources flyway:migrate

migrate-analytical: check-maven
	$(MAVEN) -pl $(DB_ANALYTICAL_MIGRATIONS_MODULE) process-resources flyway:migrate

docker-build: docker-build-api docker-build-web

docker-build-api: check-docker
	$(DOCKER) build -f $(API_MODULE)/Dockerfile -t $(API_IMAGE) .

docker-build-web: check-docker
	$(DOCKER) build -f $(WEB_MODULE)/Dockerfile -t $(WEB_IMAGE) .

compose-build: check-docker
	$(DOCKER) compose build

compose-up: check-docker
	$(DOCKER) compose up --build

compose-down: check-docker
	$(DOCKER) compose down

compose-logs: check-docker
	$(DOCKER) compose logs -f

check-maven:
	@command -v $(MAVEN) >/dev/null 2>&1 || { \
		printf 'Maven is required. Install mvn or add a Maven wrapper at ./mvnw.\n' >&2; \
		exit 1; \
	}

check-npm:
	@command -v $(NPM) >/dev/null 2>&1 || { \
		printf 'Node.js and npm are required for the web module.\n' >&2; \
		exit 1; \
	}

check-docker:
	@command -v $(DOCKER) >/dev/null 2>&1 || { \
		printf 'Docker is required for image and compose targets.\n' >&2; \
		exit 1; \
	}
