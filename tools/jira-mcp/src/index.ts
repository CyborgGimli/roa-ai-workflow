import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";

const JIRA_HOST = process.env.JIRA_HOST ?? "";
const JIRA_EMAIL = process.env.JIRA_EMAIL ?? "";
const JIRA_API_TOKEN = process.env.JIRA_API_TOKEN ?? "";

function getAuthHeader(): string {
  const credentials = Buffer.from(`${JIRA_EMAIL}:${JIRA_API_TOKEN}`).toString("base64");
  return `Basic ${credentials}`;
}

async function jiraFetch(path: string): Promise<unknown> {
  const url = `${JIRA_HOST}/rest/api/3${path}`;
  const response = await fetch(url, {
    headers: {
      Authorization: getAuthHeader(),
      Accept: "application/json",
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Jira API error ${response.status}: ${text}`);
  }

  return response.json();
}

interface JiraIssueFields {
  summary: string;
  description: unknown;
  status: { name: string };
  labels: string[];
}

interface JiraIssue {
  key: string;
  fields: JiraIssueFields;
}

interface JiraSearchResult {
  issues: JiraIssue[];
  total: number;
}

function extractIssueData(issue: JiraIssue) {
  const fields = issue.fields;
  const description = extractDescription(fields.description);

  return {
    key: issue.key,
    summary: fields.summary,
    description,
    status: fields.status?.name ?? "Unknown",
    labels: fields.labels ?? [],
  };
}

function extractDescription(description: unknown): string {
  if (!description) {
    return "";
  }
  if (typeof description === "string") {
    return description;
  }
  const doc = description as { content?: Array<{ content?: Array<{ text?: string }> }> };
  if (doc.content) {
    return doc.content
      .flatMap((block) => block.content ?? [])
      .map((inline) => inline.text ?? "")
      .join("")
      .trim();
  }
  return "";
}

const server = new Server(
  { name: "jira-mcp", version: "1.0.0" },
  { capabilities: { tools: {} } }
);

server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: "jira_get_issue",
      description: "Get a Jira issue by its key. Returns summary, description, status, and labels.",
      inputSchema: {
        type: "object",
        properties: {
          issue_key: {
            type: "string",
            description: "The Jira issue key, e.g. DEV-123",
          },
        },
        required: ["issue_key"],
      },
    },
    {
      name: "jira_search_issues",
      description: "Search issues in the DEV project using a text query or JQL filter.",
      inputSchema: {
        type: "object",
        properties: {
          query: {
            type: "string",
            description: "Text to search for in issue summary/description, or a full JQL expression.",
          },
          max_results: {
            type: "number",
            description: "Maximum number of results to return (default: 20, max: 50).",
          },
        },
        required: ["query"],
      },
    },
  ],
}));

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  if (name === "jira_get_issue") {
    const issueKey = (args as { issue_key: string }).issue_key;
    const fields = "summary,description,status,labels";
    const data = await jiraFetch(`/issue/${issueKey}?fields=${fields}`) as JiraIssue;
    const issue = extractIssueData(data);

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(issue, null, 2),
        },
      ],
    };
  }

  if (name === "jira_search_issues") {
    const { query, max_results } = args as { query: string; max_results?: number };
    const limit = Math.min(max_results ?? 20, 50);

    const isJql = /\b(AND|OR|ORDER BY|project\s*=|status\s*=|assignee\s*=)\b/i.test(query);
    const jql = isJql
      ? query
      : `project = DEV AND text ~ "${query.replace(/"/g, '\\"')}" ORDER BY created DESC`;

    const encoded = encodeURIComponent(jql);
    const fields = "summary,description,status,labels";
    const data = await jiraFetch(
      `/issue/search?jql=${encoded}&maxResults=${limit}&fields=${fields}`
    ) as JiraSearchResult;

    const issues = data.issues.map(extractIssueData);

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify({ total: data.total, issues }, null, 2),
        },
      ],
    };
  }

  throw new Error(`Unknown tool: ${name}`);
});

async function main() {
  if (!JIRA_HOST || !JIRA_EMAIL || !JIRA_API_TOKEN) {
    process.stderr.write(
      "Error: JIRA_HOST, JIRA_EMAIL, and JIRA_API_TOKEN environment variables are required.\n"
    );
    process.exit(1);
  }

  const transport = new StdioServerTransport();
  await server.connect(transport);
  process.stderr.write("Jira MCP server running on stdio\n");
}

main().catch((err) => {
  process.stderr.write(`Fatal error: ${err}\n`);
  process.exit(1);
});
