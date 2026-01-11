# Security Policy

## 🔒 Security Overview

Eagle Bank API implements multiple layers of security to protect user data and ensure secure banking operations. This document outlines our security measures.

---

## 🛡️ Security Measures Implemented

### Authentication & Authorization

| Feature | Implementation | Status |
|---------|----------------|--------|
| **JWT Authentication** | Stateless token-based auth with HMAC-SHA256 | ✅ Implemented |
| **Password Hashing** | BCrypt with strength factor 12 | ✅ Implemented |
| **Token Expiry** | 24-hour validity | ✅ Implemented |
| **Authorization Checks** | Resource-level ownership validation | ✅ Implemented |
| **HTTPS Ready** | Production deployment recommendation | ⚠️ Configuration required |

---

## 🔐 Security Configuration

### JWT Secret Key

**⚠️ Important**: The default JWT secret is for development only!

For production, set a strong secret via environment variable:

```bash
export JWT_SECRET="your-secure-256-bit-secret-key-change-this-in-production"
```

**Requirements:**
- Minimum 256 bits (32+ characters)
- Random and unique
- Never commit to version control
- Rotate periodically

**Generate a secure secret:**
```bash
# Using OpenSSL
openssl rand -base64 32

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

### Database Security

**Development (H2):**
- In-memory database (data lost on restart)
- Console enabled for debugging
- No persistent storage

**Production Recommendations:**
- Use PostgreSQL/MySQL with encrypted connections
- Disable H2 console
- Use connection pooling
- Enable database encryption at rest
- Implement regular backups
- Use database user with minimal privileges

### HTTPS/TLS

This API should **always** be deployed behind HTTPS in production.

**Why?**
- Protects JWT tokens in transit
- Prevents man-in-the-middle attacks
- Encrypts sensitive data (passwords, account details)

**Configuration (Spring Boot + Let's Encrypt):**
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEY_STORE_PASSWORD}
    key-store-type: PKCS12
```

---

## 🚨 Known Limitations (Development Build)

This is a **development/demo** build. The following are NOT production-ready:

| Limitation | Impact | Production Solution |
|------------|--------|---------------------|
| Default JWT secret | Anyone can forge tokens | Use strong, unique secret via env variable |
| H2 in-memory database | Data lost on restart | PostgreSQL/MySQL with persistence |
| No rate limiting | API abuse possible | Implement rate limiting (e.g., Bucket4j) |
| No HTTPS enforcement | Data in transit vulnerable | Deploy behind reverse proxy with TLS |
| No multi-factor auth | Single factor only | Add SMS/email verification |
| No password complexity | Weak passwords allowed | Add password strength validator |
| No account lockout | Brute force possible | Implement account lockout after N failures |
| No audit logging | No accountability trail | Add comprehensive audit logs |
| No IP whitelisting | Access from anywhere | Restrict by IP ranges |
| No encryption at rest | Database stored plaintext | Enable database encryption |

---

## 🔒 Security Best Practices for Deployment

### Environment Variables

```bash
# JWT Configuration
export JWT_SECRET="<strong-random-secret>"
export JWT_EXPIRATION="86400000"  # 24 hours

# Database Configuration
export DB_URL="jdbc:postgresql://localhost:5432/eaglebank"
export DB_USERNAME="eaglebank_user"
export DB_PASSWORD="<strong-password>"

# Application Configuration
export ALLOWED_ORIGINS="https://yourdomain.com"
export LOG_LEVEL="INFO"
```

## 🧪 Security Testing

### Run Security Tests

```bash
# Run all tests including security scenarios
./gradlew test

# Run specific security tests
./gradlew test --tests "*Security*"
./gradlew test --tests "*Auth*"
```

### Manual Security Testing

**Test JWT validation:**
```bash
# Missing token
curl -X GET http://localhost:8080/v1/accounts

# Invalid token
curl -X GET http://localhost:8080/v1/accounts \
  -H "Authorization: Bearer invalid.token.here"

# Expired token
curl -X GET http://localhost:8080/v1/accounts \
  -H "Authorization: Bearer <expired-token>"
```

**Test authorization:**
```bash
# Try accessing another user's resources
curl -X GET http://localhost:8080/v1/users/usr-other-user \
  -H "Authorization: Bearer <your-token>"
# Should return 403 Forbidden
```

**Test input validation:**
```bash
# Invalid email
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -d '{"email": "not-an-email"}'
# Should return 400 Bad Request
```

---

## 📋 Security Checklist for Production

Before deploying to production, ensure:

- [ ] JWT secret changed from default
- [ ] HTTPS/TLS configured
- [ ] Database encryption enabled
- [ ] Rate limiting implemented
- [ ] Audit logging enabled
- [ ] Error messages don't leak sensitive info
- [ ] Dependencies updated (no known CVEs)
- [ ] Security headers configured
- [ ] CORS properly configured
- [ ] API keys/secrets in environment variables
- [ ] Regular security audits scheduled
- [ ] Incident response plan in place
- [ ] Backup and recovery tested
- [ ] Monitoring and alerting configured

---

## 📚 Security Resources

### Standards & Compliance

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [GDPR](https://gdpr.eu/) (for EU users)

### Tools

- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)
- [Snyk](https://snyk.io/) - Vulnerability scanning
- [SonarQube](https://www.sonarqube.org/) - Code quality & security
- [ZAP](https://www.zaproxy.org/) - Security testing

---
