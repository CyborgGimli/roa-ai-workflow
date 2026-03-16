# **mcp.md**

MCP usage instructions for AI.

**Available servers:**
- `github` - Fetch files from GitHub repositories
- `chrome-devtools` - Browser automation (navigate, click, type, inspect HTML)
- `swagger` - OpenAPI/Swagger schema introspection for API contract discovery
- `jira` - Fetch Jira tickets and search issues

---

## GitHub MCP

### Available Tool

Only `get_file_contents` to be used for fetching README files from the provided repos below.

**CRITICAL CONSTRAINT:**
- ONLY fetch files that end with `README.md`
- ONLY use exact paths from the tables below
- NEVER browse, search, or explore repository directories
- NEVER fetch files from `/src`, `/main`, `/test`, or any code directories

### ROA Documentation Sources

**Organization:** `CyborgCodeSyndicate`

**roa-libraries** repo - Framework modules:

| Module | File Path |
|--------|-----------|
| Main | `README.md` |
| UI Interactor | `ui-interactor/README.md` |
| UI Adapter | `ui-interactor-test-framework-adapter/README.md` |
| API Interactor | `api-interactor/README.md` |
| API Adapter | `api-interactor-test-framework-adapter/README.md` |
| DB Interactor | `db-interactor/README.md` |
| DB Adapter | `db-interactor-test-framework-adapter/README.md` |
| Assertions | `assertions/README.md` |
| Test Framework | `test-framework/README.md` |
| Archetype | `roa-archetype/README.md` |

**roa-example-projects** repo:

| File Path | Purpose |
|-----------|---------|
| `README.md` | Example project patterns |

**Valid examples:**
- `README.md`
- `ui-interactor/README.md`
- `test-framework/README.md`

**Invalid examples (NEVER use):**
- `ui-complex-test-framework/src/main/java/...`
- `roa-libraries/ui-interactor/src/...`
- Any path containing `/src/`, `/main/`, `/test/`, `/java/`

```
Use get_file_contents with:
- owner: "CyborgCodeSyndicate"
- repo: "roa-libraries" or "roa-example-projects"
- path: file path from table above
```

### Path Validation

Before calling `get_file_contents`, verify:
1. Path is EXACTLY as listed in the tables above
2. Path ends with `README.md`
3. You are NOT constructing a path by exploring/guessing

### Fetch README When

DO NOT pre-load all READMEs at session start.

Fetch only when:
- Working on task and framework contract is unclear
- Instruction file says "check README" or "see framework docs"
- Need pattern not available in current project

### Steps to Fetch

1. Encounter reference (e.g., "check ui-interactor README for X")
2. Check: was this README already fetched this session?
3. IF already fetched → use existing content
4. IF not fetched → call `get_file_contents`
5. Use content to complete current task

### Re-fetch Rules

- Already fetched this session → DO NOT fetch again, use existing
- New session → fetch when needed
- Unsure if content is complete → re-fetch to confirm

### Source of Truth

- `roa-libraries` READMEs = source-of-truth for framework contracts and APIs
- `roa-example-projects` README = source-of-truth for implementation patterns
- Use exact paths from table above - don't guess
- If file not found, report error - don't make up content

**If local instructions conflict with repo README:**
1. Present both sources to user
2. Explain the difference
3. Ask user which to follow
4. Do not decide on your own

**Do NOT:**
- Guess file paths not in the table
- Assume repo structure without checking
- Make up content if fetch fails
- Browse or list repository directories
- Fetch any file that is NOT a README.md
- Search inside `/src`, `/main`, `/test`, or any subdirectory not listed
- Use `search_repositories`, `list_commits`, or any tool other than `get_file_contents`
- Append anything to paths except exactly as listed (e.g., NEVER do `ui-complex-test-framework/src/...`)

---

## DevTools MCP (Browser)

### Prerequisites (MANDATORY - Do Before Using Any DevTools Tools)

**IMPORTANT:** Before attempting to use ANY DevTools MCP tools (`navigate`, `screenshot`, `click`, `type`, `get_html`, `evaluate`), you MUST ensure Chrome is running with remote debugging enabled.

**Step 1: Start Chrome with Remote Debugging**

**Step 2: Verify Connection**

After starting Chrome, wait 2-3 seconds, then attempt a simple DevTools command (e.g., `navigate` to the AUT URL). If the command fails with a connection error, Chrome may not have started properly - retry the start command.

**DO NOT:**
- Ask the user to manually start Chrome - always start it programmatically via Bash
- Attempt DevTools commands without first ensuring Chrome is running
- Assume Chrome is already running from a previous session

### Before Using (After Chrome is Running)

Read the relevant `app-knowledge-{app}.yaml` (e.g., `app-knowledge-zerobank.yaml`, `app-knowledge-reqres.yaml`) to get:
- AUT base URL
- Available pages and paths
- Login credentials (if needed for navigation)

### Available Tools

| Tool | Purpose | When to Use |                                                                                                                                                  
  |------|---------|-------------|                                                                                                                                                  
| `navigate` | Navigate browser to URL | Opening AUT pages |                                                                                                                      
| `screenshot` | Capture current page screenshot | Verify page state, identify elements |                                                                                         
| `click` | Click element by selector | Interact with buttons, links |                                                                                                            
| `type` | Type text into input field | Fill forms for navigation |                                                                                                               
| `get_html` | Get page HTML source | Inspect DOM structure, find selectors |                                                                                                     
| `evaluate` | Run JavaScript in browser | Extract element attributes, complex queries |  

### DevTools MCP - Authentication Pattern

**CRITICAL:** When filling login forms, you MUST fill each field separately.

#### WRONG (DO NOT DO THIS):
```javascript
// This fills both credentials in username field
await page.type('#username', credentials.username + credentials.password);
```
CORRECT (DO THIS):

```javascript
// Step 1: Find and fill username field
await page.type('input[name="username"]', 'username');

// Step 2: Find and fill password field  
await page.type('input[name="password"]', 'password');

// Step 3: Click login button
await page.click('input[name="signin"]');
```

**Always:**
Use the relevant `app-knowledge-{app}.yaml` to get authentication.test_credentials.username and .password

### How to Use

**Workflow for extracting locators:**

1. **Navigate to page:**
   ```
   Use navigate with url from the relevant app-knowledge-{app}.yaml (baseUrl + page path)
   ```

2. **Get page HTML to understand structure:**
   ```
   Use get_html to see element hierarchy
   ```

3. **Test selector uniqueness:**
   ```
   Use evaluate with script: "document.querySelectorAll('<selector>').length"
   Result should be 1 for unique selector
   ```

4. **Get element attributes:**
   ```
   Use evaluate with script: "document.querySelector('<selector>').getAttribute('id')"
   ```

**Locator priority (most to least stable):**
1. `id` attribute: `By.id("login-btn")`
2. `name` attribute: `By.name("username")`
3. Stable CSS class: `By.cssSelector(".login-form .submit-btn")`
4. Tag + attribute: `By.cssSelector("input[type='email']")`
5. XPath (last resort): `By.xpath("//button[text()='Login']")`

**Rules:**
- Never guess locators - always verify with evaluate first
- Confirm selector returns exactly 1 element
- Check if element is inside iframe (need different handling)
- Note elements that appear after delay (need wait strategies)
- Prefer short, stable selectors over long paths

**Do NOT:**
- Use locators without testing them in browser first
- Assume element exists - verify with evaluate
- Use XPath indexes like `//div[3]/button[1]` - too brittle
- Use translated text or dynamic classes as selectors

**If element not found:**
- Report to user with page context
- Suggest alternative approaches
- Don't invent a locator

---

## Swagger MCP (OpenAPI Schema)

### Purpose

Fetch and introspect OpenAPI/Swagger specs to discover API contracts without guessing. Used during API test generation to confirm endpoint paths, methods, parameters, request bodies, response shapes, and status codes.

### When to Use

- **Phase 1 of API test generation** — discover all endpoints before writing any code
- **When fixing test failures** — confirm actual endpoint contract vs what was implemented
- **When endpoint behavior is unclear** — check required headers, auth schemes, status codes

### Available Tools

| Tool | Purpose | When to Use |
|------|---------|-------------|
| `get_openapi_spec` | Fetch the full OpenAPI spec document | First step — load entire spec |
| `list_operations` | List all available endpoints (method + path) | Get overview of what can be tested |
| `get_operation` | Get details for a specific endpoint | Confirm params, body schema, response codes |

### How to Use

**Step 1: Load the spec**
```
Use get_openapi_spec with the URL from app-knowledge-{app}.yaml (api.swaggerUrl)
```

**Step 2: List available operations**
```
Use list_operations to get all endpoint method + path combinations
```

**Step 3: Inspect individual endpoints**
```
Use get_operation with method + path to get:
- Required/optional query params
- Path parameters
- Request body schema
- Response schemas per status code
- Required headers
- Auth requirements
```

### Discovery Workflow (API Test Generation)

1. Load spec via `get_openapi_spec`
2. Call `list_operations` → identify all testable endpoints
3. For each endpoint you will test → call `get_operation`
4. Extract: paths, methods, params, bodies, status codes, auth requirements
5. Use this as the sole source of truth for Phase 1 scenario definition
6. **Never guess** endpoint contracts — always verify against the spec

### Authentication Information

The Swagger spec `securitySchemes` section defines the auth method:
- `apiKey` → use as a header (check `in: header` + `name` for the header name)
- `http bearer` → `Authorization: Bearer <token>`
- `oauth2` → follow the token flow defined in the spec

### Rules

- ALWAYS fetch the spec before defining scenarios or endpoints
- NEVER hardcode paths not present in the spec
- If spec is unavailable → report to user, do not guess
- Use `example` values from the spec when available for test data
- Use `required` field from spec to determine mandatory vs optional params
- Check all defined status codes — test both happy path and documented error codes

**Do NOT:**
- Assume endpoint paths, methods, or parameters without checking the spec
- Ignore documented error status codes in test scenarios
- Use undocumented fields or headers not present in the spec

---

## Jira MCP

### Purpose

Fetch Jira ticket data (summary, description, labels, status) to drive test implementation.
Used at the start of every jira-claude-agent workflow run to read the ticket requirements.

### Available Tools

| Tool | Purpose | When to Use |
|------|---------|-------------|
| `jira_get_issue` | Fetch a single issue by key | Always — STEP 1 of the workflow |
| `jira_search_issues` | Search issues by text or JQL | When finding related tickets or context |

### How to Use

**Fetch a ticket:**
```
Use jira_get_issue with issue_key = "DEV-123"
```

**Search issues:**
```
Use jira_search_issues with query = "text to search" or a JQL string
Max results defaults to 10 if not specified
```

### Returned Fields

`jira_get_issue` returns:
- `key` — the ticket key (e.g., `DEV-123`)
- `summary` — short title, used verbatim as the PR title suffix
- `description` — full ticket description containing requirements and acceptance criteria
- `status` — current ticket status
- `labels` — array of labels (e.g., `test:api`, `increase:coverage`, `regression`)

### Authentication

Credentials are injected automatically via environment variables — do not hardcode or construct them.
The MCP server reads `JIRA_HOST`, `JIRA_EMAIL`, and `JIRA_API_TOKEN` from the environment.

### Rules

- ALWAYS call `jira_get_issue` as the first action — never proceed without reading the ticket
- If the tool call fails, abort immediately and report the error — do not guess requirements
- Extract `summary` exactly as returned — do not paraphrase for the PR title
- Read `labels` to confirm module and intent match what the workflow resolved
- `description` is the source of truth for acceptance criteria — not the summary

**Do NOT:**
- Call `jira_search_issues` unless the ticket description references related tickets
- Modify or interpret the summary — use it verbatim
- Proceed if `jira_get_issue` returns an error or empty response