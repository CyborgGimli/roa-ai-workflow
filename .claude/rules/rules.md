---
trigger: always_on
description: 
globs: 
---

# **rules.md**

**Java Version:** Java 17+

## Naming Conventions

| Element | Style | Example |
|---------|-------|---------|
| Classes | PascalCase | `UserService`, `LoginTest` |
| Methods | camelCase | `createUser`, `validateResponse` |
| Variables | camelCase | `userId`, `expectedValue` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRIES`, `DEFAULT_TIMEOUT` |
| Packages | lowercase | `io.cyborgcode.project.ui` |
| Test methods | actionConditionResult | `loginValidCredentialsSuccess` |
| Enum constants | UPPER_SNAKE_CASE | `ACTIVE_STATUS`, `GET_USER` |

---

## Code Style

### Imports
* No wildcard imports (`import java.util.*`) - import specific classes
* Remove unused imports
* Use static imports for constants and utility methods

### Variables
* Use `final` for variables that don't change
* Use `var` when type is obvious: `var users = new ArrayList<String>()`
* Avoid single-letter names except in loops (`i`, `j`)
* Extract magic numbers to named constants

### Methods
* Keep methods short and focused (under 30 lines)
* Limit parameters to 4; use objects for more
* Return early to reduce nesting

### Classes
* One public class per file
* Keep classes under 300 lines; split if larger

### Class Structure Order
1. Static constants
2. Instance variables
3. Constructors
4. Public methods
5. Protected methods
6. Private methods
7. Nested classes/enums

### Access Modifiers
* Use `private` by default for fields and methods
* Use `protected` only when inheritance is intended
* Use `public` only for API exposed to other classes

### Braces
* Always use braces for `if`, `for`, `while` - even single-line blocks
* Opening brace on same line as statement

### Strings
* Use text blocks for multi-line strings
* Use `String.format()` for complex string building
* Avoid string concatenation in loops

### Boolean Expressions
* Never compare to `true` or `false` explicitly
* Use `if (isValid)` not `if (isValid == true)`
* Use `if (!isActive)` not `if (isActive == false)`

### Equality
* Use `.equals()` for object comparison
* Use `==` only for primitives and null checks
* Put constants on left side: `"value".equals(input)`

### Ternary Operator
* Use only for simple expressions
* Never nest ternary operators
* If complex, use if-else instead

## Exception Handling

* Catch specific exceptions, not generic `Exception`
* Never use empty catch blocks - log or rethrow
* Use try-with-resources for streams, connections, files
* Include meaningful error messages with context

## Null Safety

* Never return `null` from public methods - use `Optional<T>`
* Prefer empty collections over null: `List.of()` not `null`
* Validate parameters with `Objects.requireNonNull()` when needed

## Collections & Generics

* Use interface types: `List<String>` not `ArrayList<String>`
* Never use raw types: `List<String>` not `List`
* Use `List.of()`, `Map.of()`, `Set.of()` for immutable collections

## Immutability

* Prefer `final` fields - set once in constructor
* Use Java records for simple data carriers
* Avoid setters unless mutability is required
* Return defensive copies of mutable fields (lists, dates)

## Logging

* Use SLF4J/Logback, never `System.out.println()`
* Use appropriate levels: ERROR for failures, WARN for issues, INFO for events, DEBUG for details
* Never log sensitive data (passwords, tokens, personal info)
* Include context in log messages: what happened, relevant IDs

## Static Methods

* Use only for stateless utility methods
* Never maintain state in static fields
* Prefer instance methods when behavior depends on object state
* Group related static methods in utility classes with private constructor

## Design Principles

### Single Responsibility
* One class = one purpose
* One method = one action
* If a method does A and B, split into two methods

### DRY (Don't Repeat Yourself)
* Extract duplicated code into shared methods
* If same logic appears twice, refactor into single location
* Use constants for repeated values

### Composition Over Inheritance
* Prefer has-a over is-a relationships
* Use inheritance only for true "is-a" relationships
* Favor delegation over extending classes

## Input Validation

* Validate input early - fail fast
* Throw exceptions for invalid state, don't return null or magic values
* Use meaningful exception messages: what was expected vs what was received
* Validate at system boundaries (API input, file input, user input)

## Annotations

* Always use `@Override` when overriding methods
* Use `@Deprecated` with `@deprecated` Javadoc explaining alternative
* Use `@SuppressWarnings` only with specific warning name and comment why

## Enums

* Prefer enums over constant classes for related values
* Enums can have fields, constructors, and methods - use them
* Use enums to replace boolean parameters when meaning is unclear

## Type Safety

* Be specific with return types - avoid returning `Object`
* Avoid `instanceof` checks - use polymorphism instead
* Use generics to ensure compile-time type safety

## Code Generation

Follow existing patterns in the codebase - don't invent new approaches.
Keep it simple - prefer straightforward solutions over clever ones.
Generate complete, compilable code - no placeholders or TODOs.
Match the style of surrounding code exactly.
Use existing utilities/helpers before creating new ones.
Always verify code compiles before considering a task complete.

---

## Forbidden

* ❌ Hardcoded credentials or secrets
* ❌ `System.out.println()` - use logging
* ❌ Empty catch blocks
* ❌ Raw types (`List` instead of `List<String>`)
* ❌ Wildcard imports
* ❌ Commented-out code
* ❌ `Thread.sleep()` in tests
* ❌ Narrative comments (`// Get the user`, `// Click button`)
* ❌ Unused imports, variables, or methods
* ❌ Multiple blank lines in a row
* ❌ Adding error handling unless explicitly needed
* ❌ Adding null checks unless value can actually be null
* ❌ Creating abstractions for one-time operations