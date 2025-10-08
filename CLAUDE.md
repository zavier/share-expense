# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a expense sharing application (记账分摊软件) built with Java 21 and Spring Boot 3.2.0, using the COLA (Clean Object-Oriented and Layered Architecture) framework. It provides functionality for recording expenses among friends, calculating settlement amounts, and sharing costs.

The application consists of multiple modules:
- Web frontend built with amis
- WeChat mini-program
- Java backend using COLA architecture

## Architecture

The project follows COLA architecture with these modules:

- **share-expense-adapter**: Web layer with REST controllers and WeChat mini-program adapters
- **share-expense-app**: Application service layer with business logic execution
- **share-expense-domain**: Domain models and domain services
- **share-expense-infrastructure**: Data access and external service integrations
- **share-expense-client**: DTOs and client interfaces
- **start**: Spring Boot application starter module

Key components:
- Controllers in `share-expense-adapter` handle HTTP requests
- Command/Query Executors in `share-expense-app` execute business logic
- Domain models in `share-expense-domain` contain core business rules
- Gateway implementations in `share-expense-infrastructure` handle data persistence

## Development Commands

### Build and Run
```bash
# Build entire project
mvn clean compile

# Run application (from start module)
cd start && mvn spring-boot:run

# Package for deployment
mvn clean package
```

### Testing
```bash
# Run all tests
mvn test

# Run integration tests
mvn failsafe:integration-test

# Run tests for specific module
mvn test -pl share-expense-app
```

### Database
- MySQL database required
- Default connection: `jdbc:mysql://localhost:3306/share_expense`
- Configuration in `start/src/main/resources/application.properties`

## Key Technologies

- Java 21
- Spring Boot 3.2.0
- MyBatis for data access
- COLA framework 4.3.2
- JWT for authentication
- WeChat integration for mini-program
- EasyExcel for data export

## File Naming Conventions

- Command classes: `*AddCmd`, `*UpdateCmd`, `*DeleteCmd`
- Query classes: `*ListQry`, `*DetailQry`
- Executor classes: `*CmdExe`, `*QryExe`
- Integration tests: `*IT.java`
- Unit tests: `*Test.java`

## Application Entry Point

Main class: `com.github.zavier.Application` in the `start` module
Default port: 8081