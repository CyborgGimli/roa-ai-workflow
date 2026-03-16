---
name: roa-ui-architect
description: "Workflow used for setting up test framework and writing automation test cases for UI tech"
model: sonnet
---

# ROA UI Architect Agent - Task List

## Prerequisites

**Task:** Read the relevant `app-knowledge-{app}.yaml` file (e.g., `app-knowledge-zerobank.yaml`, `app-knowledge-reqres.yaml`) describing the AUT (pages, features, flows, authentication). If missing, request from user.

**Task:** Use github mcp to read the ROA README file, follow the prerequisites in `.claude/mcp.md`

**Task:** Before using DevTools MCP, follow the prerequisites in `.claude/mcp.md` (start Chrome with remote debugging). 
Then explore AUT via DevTools MCP - navigate pages, verify locators using DevTools tools. Never guess selectors.

**Task:** Load the pandora skill for any roa framework references, the pandora files have precedence and 
should be taken as primary examples when the code does not compile (imports are incorrect, ui components have bad overrides, quest usage...)

**Task:** Review CLAUDE.md files in subfolders for implementation patterns:
- `ui/CLAUDE.md` → AppUiService facade methods
- `ui/authentication/CLAUDE.md` → LoginCredentials and BaseLoginClient
- `ui/types/CLAUDE.md` → ComponentType enums with `getType()`
- `ui/elements/CLAUDE.md` → UiElement enums with `enumImpl()` and hooks
- `ui/components/CLAUDE.md` → Component implementations with `@ImplementationOfType`
- `ui/interceptor/CLAUDE.md` → Network interception (optional)

**Task:** After each phase, present deliverables and request user direction if needed.

---

## Execution Rules (MANDATORY)

These rules are NON-NEGOTIABLE. Violating them will result in broken code and wasted effort.
**After ANY task that generates or modifies Java code:**

1. Run `mvn compile`
2. If **SUCCESS** → proceed to next task
3. If **FAILURE** → load pandora skill or re-read the relevant roa-libraries or
4. **NEVER move to the next task with compilation errors**

---

## Phase 1: Discovery

**Task:** Navigate AUT pages via DevTools MCP and identify main user flows.

**Task:** Identify authentication mechanism (form login, SSO, etc.).

**Task:** Identify UI framework (Bootstrap, Material, Vaadin, custom).

**Task:** Identify all data lists/grids in the application.

**Task:** Define ~20 regression scenarios (happy paths + key negatives) grouped by feature area.

**Task:** Identify scenario dependencies.

**Checkpoint:** Confirm scenarios with user before proceeding.

**Task:** List required test data (credentials, entities, form values, expected results).

**Task:** Inspect AUT elements per scenario using MCP (buttons, inputs, selects, radios, tables, alerts, modals).

---

## Phase 2: Project Configuration

**Task:** Update `src/main/resources/config.properties`:
- Set `ui.base.url`
- Set component types (e.g., `input.default.type=BOOTSTRAP_INPUT`)

**Compile Gate:** Run `mvn compile` to verify configuration is valid.

---

## Phase 3: Test Data Strategy

**Task:** Structure discovered data in appropriate locations:
- **Config properties** (`test_data-{env}.properties`) for credentials and env-specific values
- **Constants** for static values (messages, labels)
- **DataCreator factories** for dynamic/typed models via `@Craft`

**Task:** Update `DataProperties` interface with `@Key` annotations.

**Task:** Plan data model classes for complex objects.

**Task:** Create domain models with `@InsertionElement` for auto-form-fill.

**Output:** `Data`, `DataCreator`, `DataCreatorFunctions`, domain models.

**Compile Gate:** Run `mvn compile`. Resolve ALL errors using Recovery Protocol before Phase 4.

---

## Phase 4: Component Architecture

**Task:** Map UI components needed for scenarios.

**Task:** Check which components exist in framework vs need implementation.

**Task:** Register components in `AppUiService` with facade methods: `browser()`, `button()`, `input()`, `table()`, `alert()`, `link()`, etc.

**Task:** Create component type enums in `ui/types/` (one per category: InputFieldTypes, ButtonFieldTypes, etc.):
- Implement matching `*ComponentType` interface
- Include nested `Data` class for `@ImplementationOfType` references

**Task:** Implement components in `ui/components/<type>/`:
- Extend `BaseComponent`, implement ROA interface
- Add `@ImplementationOfType(FieldTypes.Data.TYPE_NAME)`
- Use Smart APIs: `findSmartElement()`, `getDomProperty()`
- Add interaction methods: `click()`, `insert()`, `getValue()`, `isEnabled()`, `getErrorMessage()`

**Task:** Create element enums in `ui/elements/` (InputFields, ButtonFields, SelectFields, AlertFields etc.):
- Map readable names to locators and component types
- Add `before()`/`after()` hooks for async elements

**Task:** For EVERY identified data grid/list:
- Create row model class with `@TableInfo` and `@TableCellLocator`
- Add entry to `Tables` enum
- Add table-based tests using `.table().readTable()` and `.table().validate()`

**Compile Gate:** Run `mvn compile`. This phase creates many ROA implementations - if errors occur, read Pandora metadata for the specific component interfaces before fixing. Resolve ALL errors before Phase 5.

---

## Phase 5: Authentication

**Task:** Create credentials class implementing `LoginCredentials`.

**Task:** Create login client extending `BaseLoginClient`:
- Define `loginImpl()`
- Define `successfulLoginElementLocator()`

**Task:** Use `@AuthenticateViaUi` in tests to reuse authentication.

**Compile Gate:** Run `mvn compile`. If `LoginCredentials` or `BaseLoginClient` errors occur, read Pandora metadata for `io.cyborgcode.roa.ui.authentication.*` classes. Resolve ALL errors before Phase 6.

---

## Phase 6: Custom Services

**Task:** Create custom services extending `FluentService` with `@Ring` annotation.
**Task:** Extract repeated multi-step flows into domain methods (navigate→action→validate).
**Task:** Register custom services in `base/Rings.java`.

**Compile Gate:** Run `mvn compile`. If `FluentService` or `@Ring` errors occur, read Pandora metadata for `io.cyborgcode.roa.rings.*` classes. Resolve ALL errors before Phase 7.

---

## Phase 7: Test Data & Lifecycle

**Task:** Set up `DataCreator` enum + `DataCreatorFunctions`:
- Build test data objects in factory methods
- Use `@Craft(model = DataCreator.Data.MODEL)` to inject in tests
- Use `Late<T>` for runtime-dependent data

**Task:** Create `Preconditions` enum + `PreconditionFunctions`:
- Use `@Journey` for reusable setup (navigation, data creation)

**Task:** If UI actions trigger API calls needing validation:
- Create `RequestsInterceptor` enum with URL patterns to match requests

**Task:** If stored data from preconditions needs access:
- Use `retrieve()` helper with storage namespace and key

**Task:** If tests create data requiring cleanup:
- Implement `DataCleaner` enum + `DataCleanerFunctions`
- Use `@Ripper(targets = {...})` to ensure cleanup on failure

**Compile Gate:** Run `mvn compile`. This phase uses many ROA annotations (`@Craft`, `@Journey`, `@Ripper`). If errors occur, read Pandora metadata for the specific annotation. Resolve ALL errors before Phase 8.

---

## Phase 8: Test Implementation

**Important:** Review the test methods in the test classes in order to see they make sense. Each test should have validation, not empty validation with a comment !!!

**Task:** Before creating test files:
- Check existing folder structure under `src/test/java/`
- Place UI tests in the existing `ui/` folder (or module-specific folder)

**Task:** When running tests with maven command you need to have `-Pe2e` as part of the command to run the tests.

**Task:** Write tests extending `BaseQuest`:
- Use `@AuthenticateViaUi` for login (not `@Journey`)
- Use `@InterceptRequests` for API responses from UI actions
- Use `@Journey` for navigation and preconditions
- Use `@Craft` for test data injection
- Use `@Ripper` for cleanup
- Tag with `@Smoke` or `@Regression`

**Task:** Prefer `insertion().insertData(model)` over manual field inserts.

**Task:** Prefer custom rings for complex flows; keep test body minimal.

**Task:** Use validations for interacted UI elements.

**Task:** For table interactions, use per-row validation: `tableRowExtractor(Tables.ORDERS, "searchCriteria")`.

**Task:** Run `mvn compile` and resolve all errors (imports, types, missing implementations).

**Task:** Run first few test methods one by one and analyze failures (locators, data, timing, app defects) until you fix most of the problems.

**Task:** Run all tests and analyze failures (locators, data, timing, app defects).

**Task:** Fix issues and rerun until all tests pass.

**Task:** Run `mvn clean install` as quality gate.

**Task:** Stabilize flaky tests with lifecycle hooks or retry logic.