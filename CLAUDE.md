# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an expense sharing application (记账分摊软件) built with Java 21 and Spring Boot 3.2.0, using the COLA (Clean Object-Oriented and Layered Architecture) framework. It provides functionality for recording expenses among friends, calculating settlement amounts, and sharing costs.

The application consists of multiple modules:
- Web frontend built with amis
- WeChat mini-program
- Java backend using COLA architecture（DDD Architecture）

## Architecture

The project follows COLA architecture with these modules:

- **share-expense-client**: Defines external contracts with service interfaces and DTOs (CQRS pattern with Command/Query objects)
- **share-expense-adapter**: Web layer with REST controllers and WeChat mini-program adapters
- **share-expense-app**: Application service layer with Command/Query Executors that orchestrate business logic
- **share-expense-domain**: Domain models, domain services, and gateway interfaces (core business rules)
- **share-expense-infrastructure**: Data access implementations (Spring Data JPA repositories) and external service integrations
- **start**: Spring Boot application starter module

**Key architectural patterns:**
- **CQRS**: Separate Command and Query objects in client layer (e.g., `*AddCmd`, `*ListQry`)
- **Gateway Pattern**: Domain layer defines gateway interfaces, infrastructure layer provides implementations
- **Executor Pattern**: App layer contains executors (e.g., `*CmdExe`, `*QryExe`) that handle business operations
- **Hexagonal Architecture**: Adapters handle external interfaces, domain contains core logic, infrastructure provides technical implementations

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

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName
```

### Database
- MySQL database required
- Default connection: `jdbc:mysql://localhost:3306/share_expense`
- Configuration in `start/src/main/resources/application.properties`
- Database initialization script: `share-expense-infrastructure/src/main/resources/expense.sql`
- Default password placeholder: `${MYSQL_PWD}` (set as environment variable)

## Project Configuration

### Database Configuration
- Database name: `share_expense`
- Default username: `root`
- Default password: `mysqlroot` (for development)
- Connection URL: `jdbc:mysql://localhost:3306/share_expense`
- **ORM**: Spring Data JPA (migrated from MyBatis)
- JPA Configuration: `JpaConfig.java` in infrastructure module

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
- Lombok plugin required for IDE

### Database Requirements
- MySQL 8.0+ recommended
- InnoDB engine required
- Default charset: utf8mb4

### Environment Variables
```bash
# Set MySQL password for development
export MYSQL_PWD=mysqlroot

# Or pass directly to Maven
mvn spring-boot:run -DMYSQL_PWD=${pwd}
```

## Development Environment Setup

### IDE Configuration
- **Recommended IDE**: IntelliJ IDEA
- **Required plugins**: Lombok, Spring Boot
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
- **Spring Data JPA** for data access (migrated from MyBatis)
- COLA framework 4.3.2
- JWT (io.jsonwebtoken:jjwt-api 0.12.5) for authentication
- WeChat integration for mini-program
- EasyExcel 3.3.4 for data export
- BCrypt for password hashing
- Spring AI 1.0.0-M4 for AI Assistant features

## File Naming Conventions

- Command classes: `*AddCmd`, `*UpdateCmd`, `*DeleteCmd` (in share-expense-client)
- Query classes: `*ListQry`, `*DetailQry` (in share-expense-client)
- Executor classes: `*CmdExe`, `*QryExe` (in share-expense-app)
- Domain entities: `*` (in share-expense-domain)
- Data objects: `*DO` (in share-expense-infrastructure, JPA entities)
- Repositories: `*Repository` (in share-expense-infrastructure, Spring Data JPA)
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
- **Method**: JWT (JSON Web Token) using `io.jsonwebtoken:jjwt-api 0.12.5`
- **Storage**: HTTP-only cookies (current implementation)
- **Login Endpoint**: `/api/user/login` (web), `/expense/wx/user/login` (WeChat mini-program)
- **Token Expiry**: 30 days (configurable in `TokenHelper`)
- **Password Hashing**: BCrypt algorithm with salt

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
- **Authentication**: Login, logout (`/api/user/login`, `/api/user/logout`)
- **User Management**: User registration, profile management (`/api/user/*`)
- **Expense Projects**: CRUD operations for projects (`/api/project/*`)
- **Expense Records**: Add, update, delete expense records (`/api/expenseRecord/*`)
- **Statistics**: Settlement calculation and fee details (`/api/statistics/*`)
- **WeChat Mini-Program**: Specialized endpoints for WeChat integration (`/expense/wx/*`)

## COLA Architecture Details

### Module Dependencies
```
start → share-expense-adapter → share-expense-app → share-expense-domain
                                    ↓              ↓
share-expense-infrastructure → share-expense-client (interfaces defined here)
```

### Request Flow
1. **Adapter Layer** (`share-expense-adapter`): REST controller receives HTTP request
2. **App Layer** (`share-expense-app`): Executor validates and orchestrates business logic
3. **Domain Layer** (`share-expense-domain`): Domain models and services execute business rules
4. **Infrastructure Layer** (`share-expense-infrastructure`): Gateway implementations handle data persistence

### Example: Adding an Expense Record
- Controller: `ExpenseController.addExpenseRecord()` in adapter
- Executor: `ExpenseRecordAddCmdExe.execute()` in app
- Domain: `ExpenseProject.addRecord()` in domain
- Gateway: `ExpenseGatewayImpl.save()` in infrastructure

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

## AI Assistant Integration

### Feature Overview

AI Assistant allows users to interact with the expense sharing system using natural language. Users can:
- Create expense sharing projects
- Add project members
- Record expense transactions
- Query settlement status

### Architecture

The AI Assistant is implemented in the **share-expense-ai** module using:
- **Spring AI 1.0.0-M4**: Framework for AI integration
- **OpenAI GPT-4o-mini**: Language model for natural language processing
- **Function Calling**: AI invokes backend functions through Spring AI's `@Tool` annotation mechanism
- **Multi-Session Management**: Users can create and manage multiple conversation sessions

### Module Structure

```
share-expense-ai/
├── src/main/java/com/github/zavier/ai/
│   ├── AiChatController.java          # Chat REST endpoints
│   ├── AiChatService.java             # Chat service interface
│   ├── impl/AiChatServiceImpl.java    # Chat service implementation
│   ├── AiSessionController.java       # Session management endpoints
│   ├── AiSessionService.java          # Session service interface
│   ├── impl/AiSessionServiceImpl.java # Session service implementation
│   ├── config/
│   │   └── AiConfig.java              # Spring AI configuration
│   ├── dto/
│   │   ├── AiChatRequest.java
│   │   ├── AiChatResponse.java
│   │   ├── ChatMessage.java
│   │   ├── SessionDto.java
│   │   ├── MessageDto.java
│   │   └── SuggestionsResponse.java
│   ├── entity/
│   │   ├── ConversationEntity.java    # Conversation history entity
│   │   └── AiSessionEntity.java       # Session management entity
│   ├── repository/
│   │   ├── ConversationRepository.java
│   │   └── AiSessionRepository.java
│   └── function/
│       ├── CreateProjectFunction.java
│       ├── AddMembersFunction.java
│       ├── AddExpenseRecordFunction.java
│       ├── GetSettlementFunction.java
│       ├── ListProjectsFunction.java
│       └── GetProjectDetailsFunction.java
```

### Available AI Functions

1. **createProject**: Create a new expense sharing project
   - Parameters: projectName, description, members (list)
   - Example: "Create a project 'Weekend Dinner' with members Alice, Bob"

2. **addMembers**: Add new members to an existing project
   - Parameters: projectId, memberNames (list)
   - Example: "Add members Charlie and David to project 5"

3. **addExpenseRecord**: Record an expense transaction
   - Parameters: projectId, payer, amount, expenseType, payDate, consumers (list), remark
   - Example: "Record today's lunch, Alice paid 50 yuan, shared by 3 people"

4. **getSettlement**: Query project settlement status
   - Parameters: projectId
   - Example: "Check settlement status for project 5"

5. **listProjects**: List all projects for the current user
   - Parameters: None
   - Example: "Show me all my projects"

6. **getProjectDetails**: Get detailed information about a specific project
   - Parameters: projectId
   - Example: "Tell me about project 5"

7. **getExpenseDetails**: Query expense details for analysis
   - Parameters: projectId, filters (optional)
   - Example: "Show me all lunch expenses in project 5"

### Configuration

#### Environment Variables

Set the following environment variable before running the application:

```bash
export OPENAI_API_KEY=your-api-key-here
```

#### Application Properties

Configuration in `start/src/main/resources/application.properties`:

```properties
# Spring AI Configuration
spring.ai.openai.api-key=${OPENAI_API_KEY:your-api-key-here}
spring.ai.openai.base-url=${OPENAI_BASE_URL:https://api.openai.com}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.chat.options.temperature=0.7

# AI Feature Configuration
app.ai.chat.enabled=true
app.ai.chat.max-history-rounds=10
```

### API Endpoints

**Chat Endpoints:**
- **POST /expense/ai/chat**: Send a message to AI assistant
  - Request body: `{"message": "用户消息", "conversationId": "会话ID（可选）"}`
  - Response: `{"conversationId": "...", "reply": "AI回复", "suggestions": [...]}`

**Session Management Endpoints:**
- **GET /expense/ai/sessions**: List all user sessions
- **POST /expense/ai/sessions**: Create new session
- **GET /expense/ai/sessions/{id}**: Get session details
- **PUT /expense/ai/sessions/{id}**: Rename session
- **DELETE /expense/ai/sessions/{id}**: Delete session
- **GET /expense/ai/sessions/{id}/messages**: Get session messages

### Database Schema

#### AI Conversation Table (ai_conversation)

- **Purpose**: Stores conversation history
- **Key fields**:
  - conversation_id: Unique conversation identifier
  - user_id: User who owns the conversation
  - role: Message role (user/assistant/system)
  - content: Message content
  - created_at: Message timestamp
- **Indexes**: conversation_id, user_id, created_at

#### AI Session Table (ai_chat_session)

- **Purpose**: Stores AI chat sessions for multi-conversation management
- **Key fields**:
  - id: Unique session identifier
  - user_id: User who owns the session
  - title: Session title (auto-generated from first message)
  - created_at: Session creation timestamp
  - updated_at: Last update timestamp
- **Indexes**: user_id, created_at, updated_at

### Testing

Run AI module tests:

```bash
# Run all AI module tests
mvn test -pl share-expense-ai

# Run integration test
mvn test -pl share-expense-ai -Dtest=AiChatIntegrationTest
```

### Security Considerations

- **API Key**: Never commit OpenAI API key to version control
- **User Context**: All functions execute with authenticated user's context
- **Parameter Validation**: Functions validate parameters before execution
- **Session Isolation**: Users can only access their own sessions
- **Rate Limiting**: Implemented to prevent API abuse
- **Intent Validation**: Protection against prompt injection attacks

## Business Logic Overview

### Expense Settlement Calculation
The core business logic in `share-expense-domain` handles:

1. **Fee Calculation**: Each expense record is split equally among consumers
   - `MemberRecordFee`: Fee per member per expense record
   - `MemberProjectFee`: Total fee per member across all records in a project
   - Settlement amount = Total paid - Total consumed

2. **Project Locking**: Projects can be locked to prevent further modifications
   - Controlled by `ExpenseProject.locked` field
   - Version field used for optimistic locking

3. **Member Management**: Project members must be added before they can be:
   - Listed as payers in expense records
   - Listed as consumers in expense records

4. **Validation Rules**:
   - Amount must be positive with max 2 decimal places
   - Payment date and expense type are required
   - All consumers must be valid project members

### Key Domain Classes
- `User`: User entity with password hashing and JWT token generation
- `ExpenseProject`: Project entity with fee calculation logic (`calculateMemberFees()`)
- `ExpenseRecord`: Individual expense with consumer tracking
- `ShareTokenHelper`: Generates tokens for project sharing without authentication

## Recent Updates (Updated: 2025-01-02)

### Major Migration: MyBatis → Spring Data JPA

The data access layer has been completely migrated from MyBatis to Spring Data JPA for better ORM support, type safety, and developer productivity.

**Key Changes:**
- **Removed**: All MyBatis mappers (`*Mapper.java`) and XML configuration
- **Added**: Spring Data JPA repositories (`*Repository.java`)
- **Enhanced**: Entity objects with JPA annotations and DDD aggregate patterns
- **Configuration**: Added `JpaConfig.java` for transaction management and entity scanning
- **Base Entity**: Introduced `BaseEntity.java` for common audit fields (id, created_at, updated_at)

**JPA Features Implemented:**
- Entity relationships and cascade operations
- Optimistic locking with `@Version` field
- Automatic timestamp tracking with `@CreatedDate` and `@LastModifiedDate`
- Custom repository methods with derived queries
- Transaction management with `@Transactional`

**Migration Benefits:**
- No manual SQL writing for CRUD operations
- Automatic dirty checking and change tracking
- Better performance with lazy loading and batch operations
- Type-safe queries derived from method names
- Easier testing with Spring Boot test support

### AI Assistant Enhancements

The AI Assistant module has been significantly enhanced with new features and refactored architecture:

**New Features:**
1. **Multi-Session Management**: Users can create and manage multiple conversation sessions
2. **Session Persistence**: Sessions are saved to `ai_chat_session` table with auto-generated titles
3. **Dynamic Suggestions**: AI provides follow-up action suggestions based on conversation context
4. **Rate Limiting**: Implemented rate limiting service to prevent API abuse
5. **Intent Validation**: Added protection against prompt injection attacks
6. **Project Query by Name**: `getSettlement` function now supports project name lookup instead of just ID

**Architecture Refactoring:**
- Removed custom `AiFunction` annotations in favor of Spring AI's standard `@Tool` annotation
- Simplified function registration - no manual registry needed
- Extracted services: `MessagePersister`, `SuggestionGenerator`, `IntentValidationService`
- Better separation of concerns with dedicated providers and validators
- Improved test coverage with comprehensive unit and integration tests

**New AI Functions:**
- `listProjects`: List all projects for the current user
- `getProjectDetails`: Get detailed project information
- `getExpenseDetails`: Query and analyze expense records with filters

**Test Coverage:**
- Added `AiSessionServiceTest` with 555 lines of comprehensive session management tests
- Added `AiSessionControllerTest` with 290 lines of controller endpoint tests
- Added `AiChatIntegrationTest` for full-stack testing
- Added `RateLimitServiceTest` with 198 lines

### Test Coverage Improvements

**New Integration Tests:**
- `ExpenseProjectGatewayImplTest`: 292 lines covering project CRUD operations
- `ExpenseProjectGatewayOptimisticLockTest`: 219 lines testing concurrent modification scenarios
- `UserGatewayImplTest`: 182 lines testing user management operations

**Test Infrastructure:**
- Added `logback-test.xml` for better test logging
- Improved test configuration with proper database setup
- Enhanced test data builders with `ExpenseProjectBuilder`

### API Path Standardization

All AI Assistant endpoints have been moved under the `/expense` prefix for consistency:
- `/api/ai/chat` → `/expense/ai/chat`
- `/api/ai/sessions` → `/expense/ai/sessions`

### Documentation Updates

- Removed outdated design documents from `docs/plans/` directory
- Consolidated AI architecture into this CLAUDE.md file
- Updated database schema documentation with new tables
- Enhanced API documentation with session management endpoints

### Code Quality Improvements

- Replaced mutable DTOs with immutable `record` classes for better thread safety
- Improved error handling with global exception handler
- Enhanced code documentation and comments
- Better naming conventions throughout the codebase
- Removed unnecessary configuration files (e.g., `.vscode/settings.json`)

## Important Notes for Developers

### JPA Entity Management

When working with JPA entities:
- Always use `@Transactional` for write operations to ensure proper entity state management
- Be aware of lazy loading - access relationships within a transactional context
- Use repository methods instead of manual entity manipulation when possible
- Clear entity manager after bulk operations to avoid memory issues
- Test optimistic locking scenarios with concurrent updates

### AI Function Development

When adding new AI functions:
- Use Spring AI's `@Tool` annotation instead of custom annotations
- Functions are automatically discovered and registered via component scanning
- Implement proper parameter validation within the function
- Return clear, user-friendly error messages
- Add comprehensive tests for each new function
- Consider rate limiting for expensive operations

### Testing Strategy

- Unit tests (`*Test.java`) for individual components
- Integration tests (`*IT.java`) for database operations and full request flows
- Use `@DataJpaTest` for repository testing
- Use `@SpringBootTest` for full context testing
- Mock external dependencies (AI APIs) in unit tests
- Use test builders for creating test data objects


Always use context7 when I need code generation, setup or configuration steps, or
library/API documentation. This means you should automatically use the Context7 MCP
tools to resolve library id and get library docs without me having to explicitly ask.
