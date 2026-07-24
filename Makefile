.PHONY: help build package package-api package-web test test-api test-web verify run run-api run-web clean check-maven check-npm install-web

MAVEN ?= $(shell test -x ./mvnw && printf './mvnw' || printf 'mvn')
NPM ?= npm
API_MODULE := services/java/ticket-order-api
WEB_MODULE := apps/web/ticket-order-web
SPRING_PROFILES ?= local

help:
	@printf 'TicketOrderPlatform\n\n'
	@printf 'Targets:\n'
	@printf '  make build        Compile, test, and package modules\n'
	@printf '  make package      Build module artifacts\n'
	@printf '  make test         Run all module tests\n'
	@printf '  make verify       Run Maven verify and web build\n'
	@printf '  make run          Start the Java API locally\n'
	@printf '  make run-api      Start the Java API locally\n'
	@printf '  make run-web      Start the React/Vite web app locally\n'
	@printf '  make install-web  Install web dependencies\n'
	@printf '  make clean        Remove build output\n'

build: test package

package: package-api package-web

package-api: check-maven
	$(MAVEN) -pl $(API_MODULE) -am package

package-web: check-npm
	$(NPM) --prefix $(WEB_MODULE) run build

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
