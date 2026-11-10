# Development setup

## Tools

These must be on your `PATH`. The [Brewfile](../../Brewfile) is an example for macOS (`brew bundle`).
Manage them however suits your OS.

- JDK (recommended `Amazon Corretto` via https://sdkman.io)
- Apache Maven
- aws-cli
- [dx.sh](../../dx.sh)
- [Makefile](../../Makefile)

```
sdk list java | grep -i 'amzn\|amazon\|corretto'
sdk install java 21.0.12-amzn
sdk install maven
```

Make sure the toolchain points to `sdkman`:

```
which java    # ~/.sdkman/candidates/java/current/bin/java
which javac   # ~/.sdkman/candidates/java/current/bin/javac
which mvn     # ~/.sdkman/candidates/maven/current/bin/mvn
```

```
java -version
openjdk version "21.0.12" 2026-07-21 LTS
OpenJDK Runtime Environment Corretto-21.0.12.8.1 (build 21.0.12+8-LTS)
OpenJDK 64-Bit Server VM Corretto-21.0.12.8.1 (build 21.0.12+8-LTS, mixed mode, sharing)

javac -version
javac 21.0.12

mvn -version
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
Maven home: ~/.sdkman/candidates/maven/current
```

## AWS session

We develop against the remote OrcaNexus dev environment, so you need an authenticated AWS
session. Use your usual AWS CLI setup and manage profiles however you like. Any profile or
command shown in the docs is only an example; it signals that you must be authenticated at
that point.

```
export AWS_PROFILE=unimelb-warehouse-prod-poweruser
aws sso login
```

The project uses the [AWS SDK for Java](https://aws.amazon.com/sdk-for-java/), which loads
credentials through the default
[credential chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html).

## Tunnel and credentials

Source the dx script. It exports shell functions (`nexus_*` shortcuts) for the remote dev
environment.

```
source dx.sh
```

Make the key and tunnel:

```
nexus_rule
nexus_key
nexus_tunnel
nexus_status
nexus_nc
```

Make the credentials:

```
nexus_env
nexus_cred
```

## Build and run

Maven multi-module project built on Spring Boot and jOOQ.

```
mvn clean
mvn compile
mvn package -DskipTests
java -jar orcanexus-application/target/orcanexus-application-1.0-SNAPSHOT.jar
```

`SERVER_PORT` overrides the HTTP port. Swagger UI is at
http://localhost:8080/swagger-ui/index.html.

## Makefile

At minimum, run:

```
make setup
make check
```

## Lint

_Consult with AI on how to set this up for your IDE and local toolchain._

* [Spotless](https://github.com/diffplug/spotless) formats with
  [google-java-format](https://github.com/google/google-java-format)
* [Checkstyle](https://checkstyle.sourceforge.io) enforces style rules
  ([IDEA plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea))
* [SpotBugs](https://github.com/spotbugs/spotbugs) finds likely defects
* [Maven Enforcer](https://maven.apache.org/enforcer/) checks Java and Maven versions

```
mvn spotless:check
mvn spotless:apply
```

```
mvn checkstyle:check
mvn checkstyle:checkstyle    # report in target/checkstyle*
```

```
mvn compile spotbugs:check
mvn compile spotbugs:spotbugs    # report in target/spotbugs*
```

```
mvn enforcer:enforce
```

## IDE

_Recommendation only. Use whichever IDE makes you productive._

- https://www.jetbrains.com/idea/
- https://spring.io/tools
- https://netbeans.apache.org/
- VSCode, Zed, Sublime, Vim, Emacs

## Skills

Expected intermediate to advanced knowledge. Read the project documentation at
https://github.com/umccr/orcahouse-doc first.

Dev:

- SQL
- Java
- OOP (Object-oriented programming)
- jOOQ (Java Object-Oriented Querying)
- Spring Boot
- Maven
- PostgreSQL
- API development skill for REST and GraphQL
- Domain-driven Design (DDD)
- Software Design Pattern

Infra:

- AWS (RDS Aurora, Redshift, Athena, Glue, ECS, Lambda, EC2, EventBridge)
- Container (Docker, OrbStack, ...)
- Data lake (S3)
- Terraform
- Git and GitHub
- Database Administration (query perf, tuning, backup, snapshot, proxy, tunnel, etc.)

## Further reading

- [Architecture overview](../architecture/overview.md) for concepts (database-first, remote-dev)
- [Adding a resource](adding-a-resource.md)
