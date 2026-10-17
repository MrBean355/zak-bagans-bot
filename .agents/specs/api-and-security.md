# Specification: Web API & Security Architecture

This specification defines the REST API endpoints, DTO contracts, authentication, CSRF handling, and Content Security
Policy in `PhraseApiController` and `SecurityConfig`.

- **Primary Implementation**:
  - Controller: `src/main/kotlin/com/github/mrbean355/zakbot/controller/PhraseApiController.kt`
  - Global Exception Handler: `src/main/kotlin/com/github/mrbean355/zakbot/controller/GlobalExceptionHandler.kt`
  - Security Config: `src/main/kotlin/com/github/mrbean355/zakbot/config/SecurityConfig.kt`
  - User Bootstrapping: `src/main/kotlin/com/github/mrbean355/zakbot/config/UserInitializer.kt`
- **Unit Tests**:
  - `src/test/kotlin/com/github/mrbean355/zakbot/controller/PhraseApiControllerTest.kt`
  - `src/test/kotlin/com/github/mrbean355/zakbot/controller/GlobalExceptionHandlerTest.kt`

---

## 1. Security Invariants for AI Agents

1. **Endpoint Access**:
  - `GET /api/phrases`: Must remain public (`permitAll`) so the public frontend can display quotes.
  - `POST /api/phrases`: Must strictly require `hasRole("ADMIN")`.
  - `/admin.html`: Must strictly require `hasRole("ADMIN")`.
  - All other static assets (`/`, `/index.html`, `/robots.txt`, `/sitemap.xml`): `permitAll`.
2. **SPA CSRF Token Protocol**:
  - Backend sends token in `XSRF-TOKEN` cookie via `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
  - Mutating client calls (e.g. `POST /api/phrases`) must pass the token value in the `X-XSRF-TOKEN` header.
3. **Seed Usages Invariant**: When adding a quote via `POST /api/phrases`, its `usages` MUST be set to
   `findMinUsagesByType(type) ?: 0`. Never set it to 0 unconditionally, as that would cause a newly added phrase to
   starve existing phrases that have already been chosen many times.
4. **Validation Constraints**: Content and source lengths are strictly capped at 255 characters to fit database columns.

---

## 2. API Contract & Endpoints

### 2.1 `GET /api/phrases`

Returns all phrases in the database.

- **Request**: No parameters.
- **Response**: `200 OK`
- **Model (`PhraseDto`)**:
  ```kotlin
  data class PhraseDto(
      val id: Int,
      val content: String,
      val type: PhraseType,
      val source: String?
  )
  ```

### 2.2 `POST /api/phrases`

Creates a new phrase entry.

- **Security**: Requires HTTP Basic Auth or session authenticated with `ROLE_ADMIN`.
- **Request Body (`CreatePhraseDto`)**:
  ```kotlin
  data class CreatePhraseDto(
      @field:NotBlank
      @field:Size(max = 255)
      val content: String,
      val type: PhraseType,
      @field:Size(max = 255)
      val source: String?
  )
  ```
- **Response**: `201 Created` returning the saved `PhraseDto`.

### 2.3 Error Handling (`GlobalExceptionHandler`)

- Validation errors (`MethodArgumentNotValidException`): Returns `400 Bad Request` with field error descriptions.
- Missing or malformed requests: Returns structured error messages.

---

## 3. Spring Security Architecture

```kotlin
@Configuration
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(HttpMethod.GET, "/api/phrases").permitAll()
                    .requestMatchers("/api/**", "/admin.html").hasRole("ADMIN")
                    .anyRequest().permitAll()
            }
            .formLogin(Customizer.withDefaults())
            .httpBasic(Customizer.withDefaults())
            .csrf { csrf ->
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(SpaCsrfTokenRequestHandler())
            }
            .addFilterAfter(CsrfCookieFilter(), BasicAuthenticationFilter::class.java)
            .headers { headers ->
                headers.contentSecurityPolicy { csp ->
                    csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src https://fonts.gstatic.com")
                }
            }
        return http.build()
    }
}
```

---

## 4. Admin Account Bootstrapping (`UserInitializer`)

Executed on application startup (`CommandLineRunner`):

- Checks `adminProperties.password` (from environment variable `ADMIN_PASSWORD`).
- If present: creates or updates the admin account using BCrypt encoding.
- If absent, `environment.matchesProfiles("dev")`, and `appUserRepository.count() == 0`:
  - Automatically provisions `admin` / `password`.
- If absent in production and no user exists: skips account creation with a logged warning.

---

## 5. Verification Commands

```bash
# Run API and Exception Handler tests
./gradlew test --tests "*PhraseApiControllerTest*" --tests "*GlobalExceptionHandlerTest*"
```
