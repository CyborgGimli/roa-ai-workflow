---
trigger: always_on
description: 
globs: 
---

# **best-practices.md**

## **Overview**
This document provides guidelines and best practices for writing maintainable, efficient, and reliable 
tests using the ROA framework. These are recommendations that improve code quality but are not strictly enforced.

### Universal Testing Best Practices
**Test Independence**
* Each test must run independently in any order
* Tests should not depend on execution sequence
* No shared mutable state between tests
* Generate unique test data per execution to avoid conflicts

**Environment Agnostic Tests**
* Tests should be data-agnostic and not depend on a single environment
* Use configuration files (`test_data-{env}.properties`) for environment-specific data
* Avoid hardcoding environment-specific values (URLs, credentials, IDs)

**Test Execution Time**
* Individual tests should complete within 1 minute
* Long-running tests should be moved to integration test suites

**Test Structure**
* Use descriptive test names that explain the scenario
* Keep test methods focused on single scenarios

**Test Readability**
* Use @DisplayName for business-readable test names
* Extract complex logic to custom service rings
* Use meaningful variable names that convey intent
* For UI tests don't provide the web url directly instead call `getUiConfig().baseUrl()` method
* Use element enum constants, never raw locators in tests
* Name element constants descriptively (e.g., `LOGIN_BUTTON` not `BTN1`)

**Test Method Length**
* Keep test methods concise (recommended: under 50 lines)
* Extract complex flows into custom service ring methods
* Break down lengthy tests into smaller, focused scenarios

**ROA Framework Best Practices**
* `Quest` and DSL Chaining
* Use fluent DSL chaining for readability
* Keep quest chains focused and linear
* Always end with `.complete()`

**Data Management**
* Use `@Craft` instead of building objects in tests
* Use `Late<@Craft>` for lazy instantiation when needed
* Store constants in dedicated constant classes
* Use `@StaticData` annotation for constant retrieval

**Validation Practices**
* Provide at least one validation in each test
* Use soft assertions (`.soft(true)`) for multiple related validations
* Use framework assertion builders for declarative validation
* Use `.validate(() -> {})` for custom validation logic

**Authentication**
* Use `@AuthenticateViaApi` or `@AuthenticateViaUi` for automatic authentication
* Define credentials in dedicated classes
* Avoid manual login steps in test methods

**Assertion Best Practices**
* Meaningful Assertions
* Provide meaningful assertion messages
* Avoid brittle assertions (e.g., exact timestamp matching)
* Use appropriate assertion types for the validation

### Forbidden Practices

**Universal (All Modules)**
❌ Never hardcode credentials, API keys, or tokens in test code
❌ Never use `System.out.println()` for logging (use logging framework)
❌ Never use wildcard imports (e.g., `import java.util.*`)
❌ Never use empty catch blocks
❌ Never use raw types (e.g., `List` instead of `List<String>`)
❌ Never share mutable state between tests
❌ Never use `Thread.sleep()` in production code (use proper synchronization)
❌ Never concatenate SQL queries with user input (use parameterized queries)

**ROA Framework**
❌ Never forget `.complete()` at the end of Quest chains
❌ Never build test data objects in test methods (use `@Craft`)
❌ Never skip validation in tests (every test needs at least one assertion)
❌ Never hardcode test data (use `@Craft` or configuration files)
❌ Never create tests longer than 50 lines without extracting to service rings
❌ Never create environment-dependent tests

**ROA UI Framework**
❌ Never try to access `quest.getDriver()` (not exposed by Quest)
❌ Never use `findElement()` in component implementations (use `findSmartElement()`)

**ROA API Framework**
❌ Never hardcode API URLs (use configuration files)
❌ Never parse JSON responses manually (use framework JsonPath support)
❌ Never ignore HTTP status codes in validation
❌ Never create duplicate endpoint definitions

**ROA Database Framework**
❌ Never concatenate SQL queries with parameters (use parameterized queries)
❌ Never leave database connections open (framework manages them)
❌ Never hardcode database credentials (use configuration files)