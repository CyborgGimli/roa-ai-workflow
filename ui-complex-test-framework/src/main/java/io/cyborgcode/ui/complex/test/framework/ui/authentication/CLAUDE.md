## description
**Goal:** enable UI login for tests via `@AuthenticateViaUi`.

**Provide in this folder:**
- A `LoginCredentials` implementation (per role, if needed).
- A `BaseLoginClient` implementation (per application / UI variant).

**Consumed by:**
- Tests annotated with `@AuthenticateViaUi(credentials = <...>, type = <...>)`.

**Framework contract (ROA):**
- Reads `username()` / `password()` from the selected `LoginCredentials`.
- Executes `loginImpl(...)` of the selected `BaseLoginClient`.
- Confirms login using `successfulLoginElementLocator()`.

**Source of truth:** `.claude/skills/pandora/SKILL.md` for metadata and usage of ROA framework classes.

---

## example

### Credentials class (pattern)
```java
package <your.package>.ui.authentication;

import io.cyborgcode.roa.ui.authentication.LoginCredentials;
import <your.package>.data.test_data.Data;

public class <Role>Credentials implements LoginCredentials {

  @Override
  public String username() {
    return Data.testData().username(); // or role-specific accessor if available
  }

  @Override
  public String password() {
    return Data.testData().password(); // or role-specific accessor if available
  }
}
```

### UI login client (pattern)
```java
package <your.package>.ui.authentication;

import io.cyborgcode.roa.ui.authentication.BaseLoginClient;
import io.cyborgcode.roa.ui.service.fluent.UiServiceFluent;
import org.openqa.selenium.By;

import static io.cyborgcode.roa.ui.config.UiConfigHolder.getUiConfig;

import <your.package>.ui.elements.InputFields;
import <your.package>.ui.elements.ButtonFields;

public class <App>UiLogin extends BaseLoginClient {

  @Override
  protected <T extends UiServiceFluent<?>> void loginImpl(T uiService, String username, String password) {
    uiService
      .getNavigation().navigate(getUiConfig().baseUrl())
      .getInputField().insert(InputFields.USERNAME_FIELD, username)
      .getInputField().insert(InputFields.PASSWORD_FIELD, password)
      .getButtonField().click(ButtonFields.SIGN_IN_BUTTON);
  }

  @Override
  protected By successfulLoginElementLocator() {
    // stable element present only after successful login
    return By.tagName("<stable-app-shell-tag-or-css>");
  }
}
```

### Test annotation usage (pattern)
```java
import io.cyborgcode.roa.ui.authentication.AuthenticateViaUi;

@Test
@AuthenticateViaUi(credentials = <Role>Credentials.class, type = <App>UiLogin.class)
void myTest(Quest quest) {
  // authenticated test start
}
```

---

## dos
- Use `cacheCredentials = true` when multiple tests use the same credentials/login client:
  - only the first test performs a real UI login
  - subsequent tests reuse the cached authenticated session to speed up execution
- Create `LoginCredentials` implementations:
  - one per role (admin/user/...) when credentials differ
  - source values from centralized test-data/config (e.g., `Data.testData()`), not constants
- Create `BaseLoginClient` implementations:
  - one per application / UI variant when login flow differs
  - keep `loginImpl(...)` limited to the login workflow only
  - implement `successfulLoginElementLocator()` with a stable post-login locator
- Use element registries/enums (e.g., `InputFields`, `ButtonFields`) for interactions.

---

## donts
- Don’t hardcode credentials in code.
- Don’t place page objects, general flows, or business logic here.
- Don’t duplicate login steps inside tests if `@AuthenticateViaUi` is used.
- Don’t use flaky success locators (toasts, spinners, dynamic/localized text).
- Don’t add unrelated setup/navigation steps into `loginImpl(...)`.
