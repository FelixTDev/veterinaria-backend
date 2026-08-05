# Auth Module Design

## Goal

Implement authentication and authorization for veterinary staff using Spring Security OAuth2 Resource Server with Nimbus JWT, without modifying the PostgreSQL schema.

## Constraints

- Keep `spring.jpa.hibernate.ddl-auto=validate`.
- Do not modify PostgreSQL tables or relationships.
- Reuse existing `Usuario`, `Rol`, `UsuarioRol`, and `CodigoRecuperacion`.
- Use stateless security with Bearer JWT.
- Do not implement a custom JWT `OncePerRequestFilter` unless a technical blocker appears.
- `JWT_SECRET` is mandatory.
- Mail configuration must come from `MAIL_*` environment variables.
- Tests must use Testcontainers PostgreSQL, never H2 or `veterinaria_db`.

## Architecture

### Security

- `SecurityConfiguration` will switch from HTTP Basic to stateless OAuth2 Resource Server.
- `NimbusJwtDecoder` will validate access tokens and recovery tokens signed with the shared secret.
- `NimbusJwtEncoder` will issue:
  - access tokens for `/api/v1/auth/login`
  - recovery tokens for `/api/v1/auth/recuperacion/validar-codigo`
- A JWT authentication converter will map claim `roles` to `ROLE_*`.
- Protected API access will additionally require claim `purpose=access` so that a `recoveryToken` cannot authenticate requests.

### Auth module

- `AuthController` will expose:
  - `POST /api/v1/auth/login`
  - `GET /api/v1/auth/me`
  - `POST /api/v1/auth/cambiar-password`
  - `POST /api/v1/auth/recuperacion/solicitar`
  - `POST /api/v1/auth/recuperacion/validar-codigo`
  - `POST /api/v1/auth/recuperacion/restablecer`
- `AuthService` will handle login, current user lookup, and password change.
- `PasswordRecoveryService` will handle recovery requests, code validation, and password reset.
- `JwtTokenService` will encapsulate token issuance and direct decoding for the recovery flow.
- `CorreoService` will abstract delivery so tests can use a fake implementation.
- `PasswordPolicyValidator` will centralize password rules.

### Data access

- `UsuarioRepository` will gain case-insensitive lookup and fetch support needed for auth flows.
- `UsuarioRolRepository` will be used to resolve user roles.
- `CodigoRecuperacionRepository` will gain queries for current usable codes and invalidation of older ones.

## Flows

### Login

1. Receive `correo` and `password`.
2. Resolve user by normalized email.
3. Reject inactive users.
4. Reject temporarily blocked users.
5. Compare password with `BCryptPasswordEncoder`.
6. On failure, increment `intentos_fallidos`; on the fifth failure, set `bloqueado_hasta` to now plus 15 minutes.
7. On success, reset failed attempts, clear block, update `ultimo_acceso`, resolve roles, issue access token with `purpose=access`.

### Recovery

1. `solicitar`: always return a generic response; when user exists, generate a six-digit code, hash it, invalidate previous active codes, persist the new record, and send mail.
2. `validar-codigo`: check user, hash, expiration, and unused state; issue `recoveryToken` with `purpose=recovery`.
3. `restablecer`: inside a transaction, validate `recoveryToken`, verify password policy, update password hash, mark code as used, set `usado_en`, reset failed attempts, and clear blocking.

## Error handling

- Add module-specific exceptions and a global exception handler.
- Use `401` for invalid authentication and token failures.
- Use `403` for denied access.
- Use `423` for temporarily blocked accounts.
- Keep generic recovery responses to avoid account enumeration.

## Testing

- Unit tests for login outcomes, blocking, JWT issuance/validation, password policy, and recovery lifecycle.
- Integration tests with Testcontainers PostgreSQL for login, `/me`, 401 handling, role mapping, blocking persistence, and password recovery persistence.
- Mail sending must be faked or mocked in tests.
