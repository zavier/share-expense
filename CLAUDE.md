# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an expense sharing application (记账分摊软件) built with Java 21 and Spring Boot 3.2.0, using the COLA (Clean Object-Oriented and Layered Architecture) framework. It provides functionality for recording expenses among friends, calculating settlement amounts, and sharing costs.

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
- Database initialization script: `share-expense-infrastructure/src/main/resources/expense.sql`

## Project Configuration

### Database Configuration
- Database name: `share_expense`
- Default username: `root`
- Default password: `mysqlroot` (for development)
- Connection URL: `jdbc:mysql://localhost:3306/share_expense`
- MyBatis configuration: `classpath:mybatis/mybatis-config.xml`

### WeChat Mini-Program Configuration
- App ID and App Secret: Configured in `start/src/main/resources/application.properties`
- Note: Keep these credentials secure and never expose in documentation or commits

### Server Configuration
- Default port: 8081
- Project name: start
- Main class: `com.github.zavier.Application`

## Database Schema

### Core Tables

#### User Table (user)
- **Purpose**: Stores user account information
- **Key fields**: id, user_name, email, password_hash, open_id
- **Features**: Supports both email and WeChat login

#### Expense Project Table (expense_project)
- **Purpose**: Stores expense sharing projects
- **Key fields**: id, name, description, create_user_id, locked, version
- **Features**: Project locking and version control

#### Expense Project Member Table (expense_project_member)
- **Purpose**: Manages project members
- **Key fields**: id, project_id, name
- **Relationship**: Links to expense_project

#### Expense Record Table (expense_record)
- **Purpose**: Stores individual expense records
- **Key fields**: id, project_id, pay_member, amount, pay_date, expense_type, remark
- **Features**: Categorized expenses with date tracking

#### Expense Record Consumer Table (expense_record_consumer)
- **Purpose**: Links expense records to consumers
- **Key fields**: id, project_id, record_id, member
- **Relationship**: Many-to-many between expenses and consumers

### Database Initialization
```bash
# Create database
mysql -u root -p
CREATE DATABASE share_expense;

# Import schema
mysql -u root -p share_expense < share-expense-infrastructure/src/main/resources/expense.sql
```

## Environment Requirements

### Prerequisites
- **Java**: JDK 21 or higher
- **Maven**: 3.6.0 or higher
- **MySQL**: 8.0 or higher
- **IDE**: IntelliJ IDEA or Eclipse (recommended)

### Java Configuration
- Source version: 21
- Target version: 21
- Encoding: UTF-8

### Database Requirements
- MySQL 8.0+ recommended
- InnoDB engine required
- Default charset: utf8mb4

## Development Environment Setup

### IDE Configuration
- **Recommended IDE**: IntelliJ IDEA
- **Required plugins**: Lombok, MyBatis, Spring Boot
- **Code style**: Follow Google Java Style Guide
- **Encoding**: UTF-8 for all files

### Development Database Setup
```bash
# Create development database
mysql -u root -p
CREATE DATABASE share_expense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Import schema
mysql -u root -p share_expense < share-expense-infrastructure/src/main/resources/expense.sql
```

### Test Configuration
- Test database: Same as development database
- Test profiles: Configure in `start/src/test/resources/test.properties`
- Integration tests: Use `*IT.java` naming convention
- Unit tests: Use `*Test.java` naming convention

### Debug Configuration
- Remote debugging port: 5005 (if needed)
- JVM options for development: `-Xmx1024m -Xms512m`
- Spring Boot dev tools: Enabled by default

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

## Deployment

### Packaging
```bash
# Clean and package entire project
mvn clean package

# Skip tests during packaging
mvn clean package -DskipTests

# Package specific module
mvn clean package -pl start
```

### Production Deployment
```bash
# Build for production
mvn clean package -Pprod

# Run JAR file
java -jar start/target/start-1.0.0-SNAPSHOT.jar

# Run with JVM options
java -Xmx2048m -Xms1024m -jar start/target/start-1.0.0-SNAPSHOT.jar
```

### Environment Variables
- `SPRING_PROFILES_ACTIVE`: Set to `prod` for production
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `WX_APP_ID`: WeChat mini-program App ID
- `WX_APP_SECRET`: WeChat mini-program App Secret

### Docker Deployment (Optional)
```dockerfile
FROM openjdk:21-jre-slim
COPY target/start-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## Application Entry Point

Main class: `com.github.zavier.Application` in the `start` module
Default port: 8081

## API Documentation

### REST API Design
- **Base URL**: `http://localhost:8081/api`
- **Content-Type**: `application/json`
- **Character Encoding**: UTF-8

### Authentication
- **Method**: JWT (JSON Web Token)
- **Header**: `Authorization: Bearer <token>`
- **Login Endpoint**: `/api/auth/login`
- **Token Expiry**: 24 hours (configurable)

### Error Handling
- **Success Response**: `{"success": true, "data": {...}}`
- **Error Response**: `{"success": false, "error": {"code": "ERROR_CODE", "message": "Error description"}}`
- **HTTP Status Codes**:
  - 200: Success
  - 400: Bad Request
  - 401: Unauthorized
  - 403: Forbidden
  - 404: Not Found
  - 500: Internal Server Error

### Common Error Codes
- `INVALID_CREDENTIALS`: Invalid username or password
- `TOKEN_EXPIRED`: JWT token has expired
- `USER_NOT_FOUND`: User does not exist
- `PROJECT_NOT_FOUND`: Expense project does not exist
- `VALIDATION_ERROR`: Input validation failed

### API Endpoints Categories
- **Authentication**: Login, logout, token refresh
- **User Management**: User registration, profile management
- **Expense Projects**: CRUD operations for projects
- **Expense Records**: Add, update, delete expense records
- **Settlement**: Calculate and view settlement details
- **Export**: Export data to Excel format

## WeChat Mini-Program Integration

### WeChat Login Flow
1. **Frontend**: Get wx.login() code from WeChat API
2. **Backend**: Exchange code for OpenID using WeChat API
3. **Database**: Create or update user with OpenID
4. **Authentication**: Generate JWT token for subsequent requests

### WeChat API Configuration
- **App ID and App Secret**: Configured in `application.properties`
- **API Endpoints**:
  - Token exchange: `https://api.weixin.qq.com/sns/jscode2session`
  - Access token: `https://api.weixin.qq.com/cgi-bin/token`
- **Security**: Never expose App Secret in frontend code or documentation

### Required WeChat Permissions
- **Scope**: `scope.userInfo` for user profile information
- **APIs Used**:
  - `wx.login()`: Get login code
  - `wx.getUserProfile()`: Get user profile
  - `wx.getUserInfo()`: Get user information (deprecated)

### WeChat User Binding
- **OpenID Storage**: Stored in `user.open_id` field
- **User Creation**: Auto-create user on first WeChat login
- **Account Linking**: Link WeChat account to existing email accounts
- **Password Default**: Uses username as default password for WeChat users

### Testing WeChat Integration
- **Sandbox Mode**: Use WeChat Developer Tools for testing
- **Test Accounts**: Apply for test accounts through WeChat Developer Console
- **Local Testing**: Configure local backend URL in mini-program development settings

### Security Considerations
- **App Secret**: Never expose in frontend code
- **OpenID**: Treat as sensitive user identifier
- **Session Management**: Use JWT tokens instead of WeChat sessions
- **HTTPS Required**: All WeChat API calls must use HTTPS in production