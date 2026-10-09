# Development

We expect the following dev tools to be installed and available in your system PATH. We provide [Brewfile](Brewfile) as an example on macOS and you can run `brew bundle` to install them. You can manage these dev tools in any other way as see fit for your local dev setup and suits to your OS.

Tools:
- JDK _(recommended `Amazon Corretto JDK` via https://sdkman.io)_
- Apache Maven
- aws-cli
- [dx.sh](dx.sh)
- [Makefile](Makefile)

Example:

From the project root directory, do like so.
```
sdk list java | grep -i 'amzn\|amazon\|corretto'
```

```
sdk install java 21.0.12-amzn
sdk install maven
```

Make sure the toolchain binary is pointing to the `sdkman`, like so.
```
which java
~/.sdkman/candidates/java/current/bin/java

which javac
~/.sdkman/candidates/java/current/bin/javac

which mvn
~/.sdkman/candidates/maven/current/bin/mvn
```

```
java -version
openjdk version "21.0.12" 2026-07-21 LTS
OpenJDK Runtime Environment Corretto-21.0.12.8.1 (build 21.0.12+8-LTS)
OpenJDK 64-Bit Server VM Corretto-21.0.12.8.1 (build 21.0.12+8-LTS, mixed mode, sharing)
```

```
javac -version
javac 21.0.12
```

```
mvn -version
Apache Maven 3.10.0 (c43a36b8d67be7e0805a411bc0898af1a51f5472)
Maven home: ~/.sdkman/candidates/maven/current
```

## AWS Session

Use your usual way of AWS CLI setup to authenticate. Manage and switch authenticated session and AWS profile however you like. Be consistent with your setup in your way. You do not need to change that. Any README.md guideline "step" around AWS CLI authenticated session is just "an example" only. The step only _signals_ that you need to be authenticated at the point. How is – up to you.

The project uses AWS SDK for Java - https://aws.amazon.com/sdk-for-java/. The SDK should do the default loading [AWS credential chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html).

> The default credentials provider chain in the AWS SDK for Java 2.x automatically searches for AWS credentials in a predefined sequence of locations, allowing applications to authenticate with AWS services without explicitly specifying credential sources.

## IDE

_note; recommendation only. leverage any other IDE combo as see fit for your productivity._

- https://www.jetbrains.com/idea/
- https://spring.io/tools
- https://netbeans.apache.org/
- VSCode
- Zed
- Sublime
- Vim
- Emacs

## Lint

_consult with AI on how to set up with your local dev and IDE toolchain scenario_

* [Spotless](https://github.com/diffplug/spotless) for formatting with [google-java-format](https://github.com/google/google-java-format)
* [Checkstyle](https://checkstyle.sourceforge.io) for style rules ([IDEA plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea))
* [SpotBugs](https://github.com/spotbugs/spotbugs) for likely defects
* [Maven Enforcer](https://maven.apache.org/enforcer/) for Java and Maven version requirements

```
mvn spotless:check
mvn spotless:apply
```

```
mvn checkstyle:check
mvn checkstyle:checkstyle
```
See the report in `target/checkstyle*`

```
mvn compile spotbugs:check
mvn compile spotbugs:spotbugs
```
See the report in `target/spotbugs*`

```
mvn enforcer:enforce
```

## Makefile

There are a couple of [Makefile](Makefile) targets that is prepared for some dev routine.

At minimal, you should run the following targets:
```
make setup
make check
```

## Concepts

* **Database-first** approach with jOOQ.
  * No ORM.
  * The jOOQ introspects the database schema and generates the Java model.
  * Modeling happens in the database and drives the Java backend application. 
* **Remote-dev**
  * Development is remote-only (no local database).
  * SSM tunnel to a small EC2 instance in a private subnet. (AWS VPN may follow at ~10 developers).
  * IAM database auth with 15-minute tokens and a read-only user.
  * No static passwords.

## Skills

_expected intermediate to advanced knowledge on the following skills_

Please do read all the documentation at https://github.com/umccr/orcahouse-doc

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
- Database Administration — DBA (query pref, tuning, backup, snapshot, proxy, tunnel, etc.)
