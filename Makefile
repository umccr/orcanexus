MVN ?= mvn

.PHONY: build check format format-check clean

setup:
	@pre-commit install
	@pre-commit autoupdate

check:
	@pre-commit run --all-files

scan:
	@trufflehog --debug --only-verified git file://./ --since-commit main --branch HEAD --fail

deep: scan
	@ggshield secret scan repo .

baseline:
	@detect-secrets scan --exclude-files '^(.idea/|.local/|.terraform/|terraform.tfstate.d/|target/|logs/)|package-lock.yml' > .secrets.baseline

# ---

format:
	$(MVN) -B spotless:apply

format-debug:
	$(MVN) -e spotless:apply

format-trace:
	$(MVN) -X spotless:apply

format-check:
	$(MVN) -B spotless:check

format-check-debug:
	$(MVN) -e spotless:check

format-check-trace:
	$(MVN) -X spotless:check

# ---

deps:
	@$(MVN) dependency:tree

awssdk:
	@$(MVN) -pl orcanexus-application -am dependency:tree -Dincludes=software.amazon.awssdk

test:
	@$(MVN) "test"

verify:
	@$(MVN) verify

purge:
	@rm -rf ~/.m2/repository/dev/ccgcm

clean:
	@$(MVN) clean

compile:
	@$(MVN) compile

package:
	@$(MVN) package -DskipTests

install: clean
	@$(MVN) install -DskipTests

run:
	@java -jar orcanexus-application/target/orcanexus-application-1.0-SNAPSHOT.jar

compile2:
	@$(MVN) -pl orcanexus-application -am compile

install2: clean compile2
	@$(MVN) -pl orcanexus-application -am install -DskipTests

run2: install2
	@$(MVN) -pl orcanexus-application spring-boot:run
