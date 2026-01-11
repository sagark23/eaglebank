# 🏦 Eagle Bank REST API

<div align="center">

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen.svg?style=for-the-badge&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/Tests-100%2B%20Passing-success.svg?style=for-the-badge&logo=junit5)](build/test-results)
[![Build](https://img.shields.io/badge/Build-Passing-success.svg?style=for-the-badge&logo=gradle)](build.gradle)
[![Coverage](https://img.shields.io/badge/Coverage-Excellent-brightgreen.svg?style=for-the-badge&logo=codecov)](src/test)

**A production-ready banking REST API demonstrating enterprise-level Java development**

</div>

---

## 📖 Overview

**Eagle Bank API** is a comprehensive REST API implementation for a fictional banking system, showcasing senior-level software engineering practices. Built as a demonstration of production-ready code, it implements complete user management, bank account operations, and transaction processing with enterprise-grade security and reliability.

### 🌟 Key Features

<table>
<tr>
<td width="50%" valign="top">

**Core Functionality**
- 🔐 JWT-based authentication
- 👤 Complete user lifecycle management
- 🏦 Bank account CRUD operations  
- 💰 Deposit & withdrawal transactions
- 🔒 Resource-level authorization
- 📊 Transaction history tracking

</td>
<td width="50%" valign="top">

**Technical Excellence**
- ✅ 100+ passing tests (unit + integration)
- 🛡️ BCrypt password hashing
- 🔄 Optimistic locking for concurrency
- 💵 BigDecimal for precise money handling
- 📝 OpenAPI 3.1 compliant
- 🎯 Clean architecture & SOLID principles

</td>
</tr>
</table>

---

## 🏗️ Architecture

### System Architecture

```
┌─────────────┐
│   Client    │ (Postman, Browser, Mobile App)
└──────┬──────┘
       │ HTTPS + JWT
┌──────▼──────────────────────────────────┐
│     Spring Boot Application             │
│  ┌────────────────────────────────────┐ │
│  │  JWT Authentication Filter         │ │
│  └────────────┬───────────────────────┘ │
│  ┌────────────▼───────────────────────┐ │
│  │  Controllers (REST Endpoints)      │ │
│  └────────────┬───────────────────────┘ │
│  ┌────────────▼───────────────────────┐ │
│  │  Services (Business Logic)         │ │
│  └────────────┬───────────────────────┘ │
│  ┌────────────▼───────────────────────┐ │
│  │  Repositories (Data Access)        │ │
│  └────────────┬───────────────────────┘ │
│  ┌────────────▼───────────────────────┐ │
│  │  Entities (Domain Model)           │ │
│  └────────────────────────────────────┘ │
└────────────────┬────────────────────────┘
                 │ JDBC
┌────────────────▼────────────────────────┐
│      H2 Database (In-Memory)            │
└─────────────────────────────────────────┘
```

### Technology Stack

- **Framework**: Spring Boot 3.5.9
- **Language**: Java 21
- **Database**: H2 (in-memory for development/testing)
- **Security**: Spring Security + JWT
- **ORM**: Spring Data JPA (Hibernate)
- **Validation**: Bean Validation (Hibernate Validator)
- **Build Tool**: Gradle
- **Testing**: JUnit 5, Spring Boot Test

---

## 🚀 Getting Started

### ✅ Prerequisites

Before you begin, ensure you have the following installed:

| Tool | Minimum Version | Purpose | Installation |
|------|----------------|---------|--------------|
| ☕ Java JDK | 21+ | Runtime environment | [Download](https://openjdk.java.net/) |
| 📦 Gradle | 8.0+ | Build tool (wrapper included) | [Download](https://gradle.org/) |
| 💻 IDE | Any | Development (IntelliJ recommended) | [IntelliJ IDEA](https://www.jetbrains.com/idea/) |
| 🔧 Git | 2.0+ | Version control | [Download](https://git-scm.com/) |

> **Note**: The project uses the Gradle Wrapper, so you don't need to install Gradle separately.

### 📥 Installation & Setup

**1️⃣ Clone the repository**
```bash
git clone https://github.com/sagark23/eaglebank.git
cd eaglebank
```

**2️⃣ Build the project**
```bash
./gradlew clean build
```
This will:
- Download all dependencies
- Compile the source code
- Run all tests (100+ tests)
- Generate the JAR file

**3️⃣ Run the application**
```bash
./gradlew bootRun
```
The API will start on `http://localhost:8080`

**4️⃣ Verify the application is running**
```bash
curl http://localhost:8080/v1/auth/login
# Should return 400 (requires credentials)
```

### 🌐 Access Points

Once running, you can access:

| Service | URL | Credentials |
|---------|-----|-------------|
| 🌍 API Base URL | `http://localhost:8080` | JWT Token (after login) |
| 📚 Swagger UI | `http://localhost:8080/swagger-ui.html` | Interactive API documentation |
| 📄 OpenAPI Spec | `http://localhost:8080/v3/api-docs` | OpenAPI 3.0 JSON |
| 🗄️ H2 Database Console | `http://localhost:8080/h2-console` | See below ⬇️ |
| 💚 Health Check | `http://localhost:8080/actuator/health` | Application health status |

**H2 Console Login:**
- **JDBC URL**: `jdbc:h2:mem:eaglebank`
- **Username**: `sa`
- **Password**: _(leave blank)_

### ⚙️ Configuration

The application can be configured via `src/main/resources/application.yaml`:

```yaml
# Key configuration options
server:
  port: 8080                    # Change API port

jwt:
  secret: your-secret-key       # Set via JWT_SECRET env variable
  expiration: 86400000          # Token expiry (24 hours)

spring:
  datasource:
    url: jdbc:h2:mem:eaglebank  # H2 in-memory database
```

For production, set environment variables:
```bash
export JWT_SECRET="your-secure-256-bit-secret-key"
./gradlew bootRun
```

---

## 🎯 Quick Start Guide

### 🚀 5-Minute Setup

Follow this workflow to get started with the Eagle Bank API:

```
1️⃣  Create User (No Auth)
    ↓
2️⃣  Login & Get JWT Token
    ↓
3️⃣  Create Bank Account (With Token)
    ↓
4️⃣  Make Deposit
    ↓
5️⃣  Make Withdrawal (if sufficient funds)
```

### 1. Create a User

```bash
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass123",
    "phoneNumber": "+447123456789",
    "address": {
      "line1": "123 Main Street",
      "town": "London",
      "county": "Greater London",
      "postcode": "SW1A 1AA"
    }
  }'
```

**Response**: `201 Created`
```json
{
  "id": "usr-abc123def456",
  "name": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "+447123456789",
  "address": { ... },
  "createdTimestamp": "2024-01-10T10:00:00Z",
  "updatedTimestamp": "2024-01-10T10:00:00Z"
}
```

### 2. Login to Get JWT Token

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123"
  }'
```

**Response**: `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "usr-abc123def456",
    "name": "John Doe",
    "email": "john@example.com"
  }
}
```

### 3. Create a Bank Account

```bash
TOKEN="<your-jwt-token>"

curl -X POST http://localhost:8080/v1/accounts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Savings Account",
    "accountType": "personal"
  }'
```

**Response**: `201 Created`
```json
{
  "accountNumber": "01234567",
  "sortCode": "10-10-10",
  "name": "My Savings Account",
  "accountType": "personal",
  "balance": 0.00,
  "currency": "GBP",
  "createdTimestamp": "2024-01-10T10:05:00Z",
  "updatedTimestamp": "2024-01-10T10:05:00Z"
}
```

### 4. Make a Deposit

```bash
curl -X POST http://localhost:8080/v1/accounts/01234567/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00,
    "currency": "GBP",
    "type": "deposit",
    "reference": "Initial deposit"
  }'
```

**Response**: `201 Created`
```json
{
  "id": "tan-xyz789abc123",
  "amount": 500.00,
  "currency": "GBP",
  "type": "deposit",
  "reference": "Initial deposit",
  "userId": "usr-abc123def456",
  "createdTimestamp": "2024-01-10T10:10:00Z"
}
```

### 5. Make a Withdrawal

```bash
curl -X POST http://localhost:8080/v1/accounts/01234567/transactions \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 150.00,
    "currency": "GBP",
    "type": "withdrawal",
    "reference": "ATM withdrawal"
  }'
```

**Response**: `201 Created` (if sufficient funds) or `422 Unprocessable Entity` (if insufficient)

---

## 🔐 Security

### Authentication Flow

1. User registers via `POST /v1/users` (no authentication required)
2. User logs in via `POST /v1/auth/login` with email and password
3. Server validates credentials and returns JWT token
4. Client includes token in `Authorization: Bearer <token>` header for all subsequent requests
5. Server validates token and extracts user identity for authorization checks

### Authorization Rules

- Users can only access their own resources
- Attempting to access another user's resources returns `403 Forbidden`
- Resources that don't exist return `404 Not Found`

### Password Security

- Passwords are hashed using BCrypt with strength factor 12
- Passwords are never returned in API responses
- Minimum password length: 8 characters

---

## 🗄️ Database Schema

### Key Entities

**User**
- ID: `usr-{alphanumeric}` (e.g., usr-abc123def456)
- Fields: name, email, password, phone, address
- Relationships: One-to-Many with BankAccount

**BankAccount**
- ID: `01{6-digits}` (e.g., 01234567)
- Fields: accountNumber, sortCode, name, accountType, balance, currency
- Constraints: Balance between 0.00 and 10,000.00 GBP
- Relationships: Many-to-One with User, One-to-Many with Transaction

**Transaction**
- ID: `tan-{alphanumeric}` (e.g., tan-xyz789abc)
- Fields: transactionId, amount, currency, type (deposit/withdrawal), reference
- Immutable: Cannot be updated or deleted
- Relationships: Many-to-One with BankAccount and User

---

## 🧪 Testing

### Quick Test Commands

```bash
# Run all tests
./gradlew test

# Run with detailed output
./gradlew test --info

# Run specific test class
./gradlew test --tests UserControllerIntegrationTest

# Generate HTML test report
./gradlew test
open build/reports/tests/test/index.html
```

---

## 📦 Project Structure

```
com.eaglebank/
├── config/              Spring Security, JWT configuration
├── controller/          REST endpoints (UserController, AccountController, etc.)
├── service/             Business logic and authorization
├── repository/          Data access layer (Spring Data JPA)
├── domain/              JPA entities (User, BankAccount, Transaction)
├── dto/                 Request/Response DTOs
├── mapper/              Entity ↔ DTO conversion (MapStruct)
├── security/            JWT filter, UserDetails implementation
├── exception/           Custom exceptions and global error handler
├── validator/           Custom validation logic
└── util/                Utility classes (ID generators)
```

---

