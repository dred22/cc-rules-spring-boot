# Testing Rules
- Test class naming: `[ClassName]Test.java`.
- Use `@ExtendWith(MockitoExtension.class)` for unit tests.
- Use `@WebMvcTest` for controller tests; mock all dependencies with `@MockitoBean`.
- Follow the AAA pattern, commenting each section:
  ```
  // Arrange
  // Act
  // Assert
  ```
- Test names: `should_[expected]_when_[condition]`.
- One assertion block per test.
- Never test private methods.

# Integration Testing Rules
- Test class naming: `[ClassName]IntegrationTest.java` (matches the Surefire `**/*IntegrationTest` include pattern bound to the `integration-test` phase).
- Annotate with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` to load the full application context.
- Use WireMock (`wiremock-standalone`) to stub external Feign/REST calls; never hit real third-party or downstream services.
- Use `spring-boot-restclient` / `spring-boot-resttestclient` (`TestRestTemplate` or `RestTestClient`) to invoke endpoints through the real HTTP stack rather than `MockMvc`.
- Reset or isolate state between tests (e.g. transactional rollback, table truncation, or dedicated container per test class) to keep tests independent and repeatable.
- Follow the same AAA commenting pattern and `should_[expected]_when_[condition]` naming as unit tests.
- One assertion block per test; use AssertJ `assertThat`, never `assertEquals`.
- Never test private methods; exercise behavior only through public APIs/endpoints.
- Keep integration tests excluded from the `test` phase and run only in the `integration-test` phase, mirroring the `unit-tests` vs `integration-tests` Surefire executions in `pom.xml`.
