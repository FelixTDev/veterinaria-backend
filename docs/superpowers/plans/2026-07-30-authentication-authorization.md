# Authentication Authorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build stateless worker authentication, authorization, password change, and password recovery on top of the existing PostgreSQL-backed user model.

**Architecture:** Use Spring Security OAuth2 Resource Server with Nimbus for Bearer token validation and `JwtEncoder` for token issuance. Keep business logic inside modular auth services, reuse existing JPA entities/repositories, and preserve the current schema validated by Hibernate.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Security 7, Spring Data JPA, PostgreSQL, Testcontainers, Mockito, BCrypt, Spring Mail

## Global Constraints

- Maintain Spring Security OAuth2 Resource Server + Nimbus.
- Do not use a custom JWT `OncePerRequestFilter` unless a technical blocker appears.
- Keep the application stateless.
- Do not modify the PostgreSQL schema.
- Reuse the existing entities and repositories.
- Add only necessary queries.
- Use BCrypt for passwords and recovery codes.
- Keep recovery responses generic.
- Use a `recoveryToken` with `purpose=recovery` and short expiration.
- Mark recovery codes as used only during password reset.
- Execute password reset inside a transaction.
- Do not send real mail in tests.
- Use Testcontainers PostgreSQL for integration tests.
- Do not use H2 or `veterinaria_db` for tests.

---
