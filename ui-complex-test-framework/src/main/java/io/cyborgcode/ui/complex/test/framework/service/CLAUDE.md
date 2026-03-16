## description
**Goal:** domain/business-level fluent services (“custom services”) that orchestrate lower-level rings (UI/API/DB) into readable actions.

**Provide in this folder:**
- Services extending `io.cyborgcode.roa.framework.chain.FluentService`
- Services registered as rings via `@Ring("<Name>")`
- Methods represent business actions / flows (login, create order, validate order, etc.)

**How this fits the architecture**
- Tests should prefer calling domain-level methods from these services rather than repeating UI/API/DB steps.
- Services delegate to lower-level rings for technical interactions:
  - `RING_OF_UI` for Selenium operations (inputs/buttons/selects/browser/etc.)
  - `RING_OF_API` for REST calls
  - `RING_OF_DB` for DB queries and validations

**Contract**
- Service methods should be fluent:
  - return `this` (so tests can chain)
- Access the service from tests using `quest.use(Rings.RING_OF_CUSTOM)` (or the ring constant used by the project).

**Source of truth:** `.claude/skills/pandora/SKILL.md` for metadata and usage of ROA framework classes.

---

## example

### Service structure (pattern)
```java
import io.cyborgcode.roa.framework.annotation.Ring;
import io.cyborgcode.roa.framework.chain.FluentService;

import static <your.package>.base.Rings.RING_OF_UI;

@Ring("Custom")
public class CustomService extends FluentService {

  public CustomService login(Seller seller) {
    quest.use(RING_OF_UI)
      .browser().navigate(getUiConfig().baseUrl())
      .insertion().insertData(seller)
      .button().click(SIGN_IN_BUTTON);
    return this;
  }

  public CustomService createOrder(Order order) {
    quest.use(RING_OF_UI)
      .button().click(NEW_ORDER_BUTTON)
      .insertion().insertData(order)
      .button().click(PLACE_ORDER_BUTTON);
    return this;
  }
}
```

### Usage (pattern)
```java
quest
  .use(Rings.RING_OF_CUSTOM)
  .login(seller)
  .createOrder(order)
  .validateOrder(order);
```

---

## dos
- Put domain/business flows here (login, create order, validate order, edit order, etc.).
- Keep methods fluent (`return this`) and chainable.
- Delegate technical steps to lower-level rings:
  - use `quest.use(RING_OF_UI)` for elements/components interactions
  - use `quest.use(RING_OF_API)` for HTTP validation
  - use `quest.use(RING_OF_DB)` for DB checks
- Use `ui/elements` enums (ButtonFields/InputFields/SelectFields/...) instead of raw locators.
- Prefer `insertion().insertData(model)` when models are annotated with `@InsertionElement`.
- Store/read created entities via quest storage when needed:
  - use consistent storage keys (e.g., `StorageKeysTest.PRE_ARGUMENTS`, `DataCreator` keys)

---

## donts
- Don’t duplicate low-level UI sequences in tests when a reusable service method exists.
- Don’t put raw locators/driver calls in tests; keep them inside rings/services/components.
- Don’t create “god services”:
  - split services by domain area if they grow too large (auth/order/admin/etc.)
- Don’t mix unrelated responsibilities in a single method (keep each method purpose-focused).
- Don’t store secrets/tokens directly in services; source them from config/data utilities.
