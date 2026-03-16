## description
**Goal:** define UI component “types” used for ROA type-based component resolution.

**Provide in this folder:**
- Enums per component category that implement the matching ROA `*ComponentType` interface:
  - inputs → implements `InputComponentType`
  - buttons → implements `ButtonComponentType`
  - etc.
- Each enum constant represents a concrete UI technology/style variant(while exploring the app via MCP you need to figure out what type of tech is used and give names for the components):
  - examples: `BOOTSTRAP_INPUT_TYPE`, `VA_INPUT_TYPE`, `MATERIAL_INPUT_TYPE`, ...

**How this is used**
- `ui/elements/*Fields` enums assign a component type to each element.
- `ui/components/**` provide implementations annotated with `@ImplementationOfType(<FieldTypes.Data.*>)`.
- ROA resolves the correct component implementation based on the element’s `componentType()`.

**Annotation name constants**
- Each enum may have a nested `Data` class containing `String` constants used by `@ImplementationOfType(...)`.

**Source of truth:** `.claude/skills/pandora/SKILL.md` for metadata and usage of ROA framework classes.

---

## Important
**Don't implement Table types that implement `TableComponentType` interface **

## ⚠️ CRITICAL RULES

### ComponentType Interface Contract
**Component type enums implement `ComponentType` interface which requires `getType()` method:**

```java
// ✅ CORRECT - ComponentType interface
public enum InputFieldTypes implements InputComponentType {
    BOOTSTRAP_INPUT_TYPE;

    @Override
    public Enum<?> getType() { return this; }  // MUST be getType()
}

// ❌ WRONG - This is for UiElement, NOT ComponentType
public enum InputFieldTypes implements InputComponentType {
    @Override
    public Enum<?> enumImpl() { return this; }  // WRONG METHOD NAME
    }
```    
**Rule:** `ComponentType` = `getType()` | `UiElement` = `enumImpl()`

## example

### Type enum (pattern: inputs)
```java
package <your.package>.ui.types;

import io.cyborgcode.roa.ui.components.input.InputComponentType;

public enum InputFieldTypes implements InputComponentType {

  BOOTSTRAP_INPUT_TYPE;

  public static final class Data {
    public static final String BOOTSTRAP_INPUT = "BOOTSTRAP_INPUT_TYPE";
    private Data() {}
  }

  @Override
  public Enum<?> getType() {
    return this;
  }
}
```

### Wiring summary (pattern)
- `ui/types/InputFieldTypes` defines available input types.
- `ui/elements/InputFields` assigns one type per element constant (e.g., `BOOTSTRAP_INPUT_TYPE`).
- `ui/components/input/<Impl>` uses `@ImplementationOfType(InputFieldTypes.Data.BOOTSTRAP_INPUT)` to bind the concrete implementation.

---

## dos
- Create one enum per component category under `ui/types` (InputFieldTypes, ButtonFieldTypes, ...).
- Name constants as `..._TYPE` and keep them stable; these are part of the “resolution contract”.
- Always implement the correct `*ComponentType` interface for that category.
- Always implement:
  - `Enum<?> getType() { return this; }`
- Keep the nested `Data` class:
  - include string constants used by `@ImplementationOfType(...)`
  - keep constant values matching the enum constant names exactly
- Use types to represent UI technology/style variants (Bootstrap, Vaadin, etc.), not specific pages or business concepts.

---

## donts
- Don't use TableComponentType and write TableFieldTypes
- Don’t put locators here (locators belong to `ui/elements`).
- Don’t put interaction logic here (interaction belongs to `ui/components`).
- Don’t keep multiple categories in one enum (inputs/buttons/selects separate).
- Don’t change constant names casually; it breaks resolution mappings and annotations.
