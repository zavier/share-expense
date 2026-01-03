# CLAUDE.md

This file provides project context for Claude Code when working with this codebase.

## Project Overview

Expense sharing application (记账分摊软件) built with Java 21 and Spring Boot 3.2.0 using COLA (Clean Object-Oriented and Layered Architecture) framework.

**Core Features:**
- Web frontend (amis) + WeChat mini-program
- AI Assistant with natural language interaction
- Multi-user expense tracking and settlement calculation

## Architecture

**COLA Modules:**
- `share-expense-client` - External contracts (CQRS: Command/Query objects)
- `share-expense-adapter` - REST controllers and WeChat adapters
- `share-expense-app` - Application service layer (Executors)
- `share-expense-domain` - Domain models and gateway interfaces
- `share-expense-infrastructure` - Spring Data JPA repositories
- `share-expense-ai` - AI Assistant module
- `start` - Spring Boot application

**Key Patterns:** CQRS, Gateway, Hexagonal Architecture

## Key Directories

```
share-expense-ai/src/main/java/com/github/zavier/ai/
├── function/           # AI tool functions (@Tool annotation)
├── service/           # ChatModelProvider, CachedSuggestionService
├── concurrent/        # Lock management (WeakHashMapLockManager)
├── resolver/          # ProjectIdentifierResolver
└── monitoring/        # AI call monitoring system
    ├── controller/     # REST controllers for monitoring APIs
    ├── service/       # Monitoring business logic
    ├── repository/    # JPA repositories
    ├── entity/        # JPA entities
    ├── dto/           # Data transfer objects
    └── context/       # AI call context and advisor
    └── advisor/       # AOP advisors for automatic monitoring

share-expense-infrastructure/src/main/java/com/github/zavier/
├── expense/           # Expense JPA entities and repositories
├── project/           # Project JPA entities and gateways
└── user/              # User JPA entities and gateways
```

## Development Commands

```bash
# Build and run
mvn clean compile
cd start && mvn spring-boot:run

# Testing
mvn test                              # All tests
mvn test -pl share-expense-ai         # Module tests
mvn test -Dtest=ClassName            # Single test class

# Database
mysql -u root -p share_expense < share-expense-infrastructure/src/main/resources/expense.sql
```

## Standards

- **JPA entities**: Always use `@Transactional` for write operations
- **AI functions**: Use `@Tool` annotation, extend `BaseExpenseFunction`
- **Naming**:
  - Commands: `*AddCmd`, `*UpdateCmd`
  - Queries: `*ListQry`, `*DetailQry`
  - Executors: `*CmdExe`, `*QryExe`
  - Repositories: `*Repository` (Spring Data JPA)
  - Tests: `*Test.java` (unit), `*IT.java` (integration)

## AI Module

**Models:**
- DeepSeek (`deepseek-chat`): Primary chat model
- LongCat (`LongCat-Flash-Chat`): Fast suggestion generation

**Environment Variables:**
```bash
export DEEPSEEK_API_KEY=your-key
export LONGCAT_API_KEY=your-key
export MYSQL_PWD=mysql-password
```

**AI Functions:** All prefixed with `Expense*` (e.g., `ExpenseCreateProjectFunction`)
- Use `ProjectIdentifierResolver` for flexible project lookup (ID or name)
- Return structured data with `ExpenseResponseFormat` DTOs

**Suggestion Caching:**
- Session table: Active cache (read/write/delete)
- Conversation table: Audit snapshot (append-only)
- `WeakHashMapLockManager` for concurrent control

**AI Monitoring:**
- Automatic recording of all AI calls with timing, status, and error details
- Performance statistics with P50/P90/P99 latency percentiles
- Error analysis and trend analysis
- REST APIs for querying monitoring data
- Integration via AOP advisors (`@Around` advice on AI service methods)

## Important Notes

- **Port**: 8081
- **Database**: MySQL 8.0+, database name `share_expense`
- **ORM**: Spring Data JPA (migrated from MyBatis)
- **Authentication**: JWT tokens with BCrypt password hashing
- **AI Security**: Conversation ownership validation enforced

## Common Patterns

**Expense settlement:**
- `MemberRecordFee`: Fee per member per record
- `MemberProjectFee`: Total fee per member
- Settlement = Total paid - Total consumed

**Project locking:** Controlled by `ExpenseProject.locked` field with optimistic locking

## Further Documentation

- `docs/ai-suggestion-cache-implementation.md` - Caching strategy details
- `docs/ai-fix-tool-calling-hallucination.md` - Tool calling validation
- `docs/lock-manager-design.md` - Lock manager architecture
- `README.md` - Comprehensive project documentation
