# Dependency Injection & Testing Guidelines

This document outlines the architectural standards for dependency injection and unit testing in this repository.

---

## 1. Explicit Constructor Injection

All Spring-managed classes (`@Service`, `@Component`, `@Configuration`, `@Controller`, `@RestController`) must declare
their dependencies explicitly via primary constructor injection.

### Invariant: No Default Parameter Values on Dependencies

Constructors of Spring-managed components must **never** specify default parameter values for their dependencies or
configuration properties:

```kotlin
// ❌ BAD: Hides missing DI wiring and creates lazy test seams
@Service
class MyService(
    private val dependency: DependencyService = DependencyService(),
    private val properties: MyProperties = MyProperties(),
)

// ✅ GOOD: Dependencies are mandatory and explicitly supplied
@Service
class MyService(
    private val dependency: DependencyService,
    private val properties: MyProperties,
)
```

### Why This Rule Exists

1. **Clear Architectural Boundaries**: Every component's external dependencies are transparent and strictly required.
2. **Fail Fast**: Spring will fail fast during application startup if any required dependency or
   `@ConfigurationProperties`
   bean is missing, rather than silently falling back to unconfigured default instances.
3. **Intentional Testing**: Prevents tests from silently relying on production defaults, ensuring test setups are
   explicit
   and deliberate about the state of their dependencies.

---

## 2. Unit Testing Conventions

When instantiating Spring components in unit tests:

1. **Explicit Mocks and Fakes**: Always supply an explicit MockK mock (`mockk()`), fake, or real test instance for each
   constructor parameter.
2. **Centralized Test Fixtures**: Configure common dependencies in the test's `@BeforeEach fun setUp()` method. If a
   specific
   test requires a non-standard configuration (e.g., custom properties or flags), construct a targeted instance with the
   desired properties explicitly passed.

```kotlin
// ✅ GOOD: Explicitly configured dependencies in unit test
class MyServiceTest {
    @MockK
    private lateinit var dependency: DependencyService

    private lateinit var service: MyService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service = MyService(
            dependency = dependency,
            properties = MyProperties(enabled = true),
        )
    }
}
```
