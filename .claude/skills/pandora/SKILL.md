---
name: roa-pandora-metadata
description: Metadata for ROA framework classes. Use when writing code with io.cyborgcode.roa dependencies.
---

# ROA Metadata Skill

Metadata for Java classes from `io.cyborgcode.roa.*` dependencies is generated in `target/pandora/metadata/`.

Each ROA class has one JSON file, named by its full package + class name, for example:
- target/pandora/metadata/io.cyborgcode.roa.api.core.ApiClient.json

For token efficiency, open only the specific JSON file(s) you need for the classes you are working with.
If the Pandora JSON files are missing in the target directory, generate them by running: `mvn pandora:open -U`

**Important** Use it always as a source of truth for the classes you are working with. Any time you don't understand how to use some class from Roa framework(io.cyborgcode.roa) check the metadata file.
If something is not compiling when using classes from Roa framework(io.cyborgcode.roa) check the metadata file.

## JSON Structure

```json
{
  "type": {
    "id": "io.cyborgcode.roa.api.core.ApiClient",
    "typeKind": "CLASS",
    "description": "Client for making API requests",
    "tags": ["api", "http"],
    "creation": "BUILDER",
    "preference": "PREFER",
    "exampleFilesPath": "examples/api-client",
    "extra": {"category": "core"},
    "fields": [...],
    "methods": [...],
    "annotations": [...]
  },
  "usages": [
    {
      "id": "io.cyborgcode.roa.api.core.ApiClient",
      "summary": "Basic client creation and request execution",
      "usages": [
        {
          "code": "ApiClient client = ApiClient.builder().baseUrl(baseUrl).build();\nclient.request(endpoint).execute();",
          "description": "Create an ApiClient via builder and execute a request",
          "level": "CORE",
          "contextHint": "Use as the default pattern for simple API calls"
        }
      ]
    }
  ]
}
```

### Root Level

- **type** — The class metadata
- **usages** — Real-world usage examples for this class

### type Object

- **id** — Fully qualified class name (package + class name)
- **type** — Java type: `CLASS`, `INTERFACE`, `ENUM`, `RECORD`, `ANNOTATION`, `EXCEPTION`
- **description** — What this type does and when it should be used.
- **tags** — Categories for filtering (e.g., `["api", "ui", "db"]`)
- **creation** — How to create instances: `CONSTRUCTOR`, `BUILDER`, `STATIC_FACTORY`, `ENUM_CONSTANT`, `SINGLETON`, `PROVIDED`, `AUTO`
- **preference** — Advisory selection bias (soft ranking, not a hard rule): `PREFER` = choose by default, `AVOID` = last resort, `FORBID` = never use, `AUTO` = no bias.
- **exampleFilesPath** — Path to example files for this class
- **extra** — Framework-specific metadata (e.g., `{"type": "endpoint"}`)
- **fields** — Array of field metadata
- **methods** — Array of method metadata
- **annotations** — Annotations on this class

### methods Array

Each method has:

- **id** — Method signature: `request(io.cyborgcode.roa.Endpoint)
- **description** — What this method does and when it should be used.
- **tags** — Method-level tags
- **preference** — Method-level preference
- **returnType** — Fully qualified return type
- **varArgs** — `true` if last parameter accepts multiple values
- **exampleFilesPath** — Path to examples for this method
- **extra** — Method-specific metadata
- **parameters** — Ordered array of parameter metadata
- **availableOptions** — Allowed values/options for this method only when the declaring Java element is an annotation (`typeKind == "ANNOTATION"` at the root `type` object)
- **annotations** — Annotations on this method

### parameters Array

Each parameter has:

- **index** — Position (0-based): pass arguments in this order
- **name** — Parameter name
- **type** — Fully qualified type
- **typeKind** — Java type
- **description** — What this parameter does
- **creation** — How to create the value
- **primitive** — `true` for primitives (int, boolean, etc.)
- **tags** — Parameter-level tags
- **availableOptions** — Valid values (see interpretation below)
- **extra** — Parameter-specific metadata
- **annotations** — Annotations on this parameter

### fields Array

Each field has:

- **name** — Field name
- **type** — Fully qualified type
- **typeKind** — Java type
- **description** — What this field does
- **creation** — How to create the value
- **primitive** — `true` for primitives
- **tags** — Field-level tags
- **availableOptions** — Valid values
- **extra** — Field-specific metadata
- **annotations** — Annotations on this field

### usages Array

Each usage entry has:

- **id** — Class or method this demonstrates
- **summary** — Brief description
- **usages** — Array of usage items:
  - **code** — Code snippet
  - **description** — What the example shows
  - **level** — Complexity: `CORE`, `ADVANCED`
  - **contextHint** — When to use this pattern

## availableOptions Interpretation

This field appears on methods, parameters, and fields:

- **null** — No restriction. Choose any appropriate value.
- **[]** (empty) — Valid options exist but aren't defined yet. Create the app-level implementation first, then run `mvn pandora:open -U`.
- **["value1", "value2"]** — Prefer values from this list. If a required app-owned option is missing, create it, regenerate metadata (`mvn pandora:open -U`), then re-open the file and use the new option.

Note: **On method level it is used only when the metadata file describes an annotation** (`type.typeKind == "ANNOTATION"`).

## When to Regenerate

Run `mvn pandora:open -U` after changing any class that:

1. Implements `Endpoint`
2. Implements `DbQuery`
3. Implements `AccordionUiElement`
4. Implements `AlertUiElement`
5. Implements `ButtonUiElement`
6. Implements `CheckboxUiElement`
7. Implements `InputUiElement`
8. Implements `LinkUiElement`
9. Implements `LoaderUiElement`
10. Implements `ModalUiElement`
11. Implements `RadioUiElement`
12. Implements `SelectUiElement`
13. Implements `TabUiElement`
14. Implements `ToggleUiElement`
15. Implements `TableElement`
16. Implements `AccordionComponentType`
17. Implements `AlertComponentType`
18. Implements `ButtonComponentType`
19. Implements `CheckboxComponentType`
20. Implements `InputComponentType`
21. Implements `LinkComponentType`
22. Implements `ItemListComponentType`
23. Implements `LoaderComponentType`
24. Implements `ModalComponentType`
25. Implements `RadioComponentType`
26. Implements `SelectComponentType`
27. Implements `TabComponentType`
28. Implements `TableComponentType`

## When Pandora Metadata Is Unavailable

**If `target/pandora/metadata/` is empty or doesn't exist:**

1. Run `mvn pandora:open -U` (may take several minutes)
2. If the command times out, **retry once** with a longer timeout
3. If it still fails, **STOP and ask the user** to:
    - Check Maven dependencies are resolved (`mvn dependency:tree`)
    - Run `mvn compile` first
    - Verify GitHub Packages authentication

## Rules

1. Strongly prefer using classes that have metadata files in `target/pandora/metadata/`
2. Strongly prefer using methods listed in the class metadata
3. Pass arguments in the order defined by `parameters[].index`
4. Prefer using values from `availableOptions` when populated.
5. Check `usages` for correct calling patterns
6. Regenerate metadata after modifying ROA implementations
7. If two methods share the same name, always match by `parameters[].type`, not by intuition.
8. ALWAYS stop and report if metadata is unavailable

