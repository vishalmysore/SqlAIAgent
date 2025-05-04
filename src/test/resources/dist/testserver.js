import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListResourcesRequestSchema, ListToolsRequestSchema, ReadResourceRequestSchema, } from "@modelcontextprotocol/sdk/types.js";
import puppeteer from "puppeteer-core";
import { Browserbase } from "@browserbasehq/sdk";
// Create server
const server = new Server({
  name: "springboot-proxy",
  version: "1.0.0",
}, {
  capabilities: {
    tools: {},  // We'll load tools dynamically from Spring Boot
  },
});

// Handler: List tools from Spring Boot
server.setRequestHandler(ListToolsRequestSchema, async () => {
  const response = await fetch("http://localhost:7860/mcp/list-tools", {
    method: "GET",
    headers: { "Content-Type": "application/json" }
  });

  if (!response.ok) {
    throw new Error(`Failed to fetch tools: ${response.statusText}`);
  }

  const data = await response.json();
 // console.log("Available tools from Spring Boot:", JSON.stringify(data, null, 2));
  return {
    tools: data.tools,
  };
});

// Handler: Call a tool by proxying to Spring Boot
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  // 🔍 Log the outgoing request
  console.log("➡️ Sending request to Spring Boot:");
  console.log(JSON.stringify({
    name: request.params.name,
    arguments: request.params.arguments ?? {},
  }, null, 2));

  const response = await fetch("http://localhost:7860/mcp/call-tool", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: request.params.name,
      arguments: request.params.arguments ?? {},
    }),
  });

  // ❌ Log failure if not OK
  if (!response.ok) {
    const errorText = await response.text();  // Read error body
    console.error(`❌ Tool call failed: ${response.statusText}`);
    console.error("🔻 Error response body:", errorText);
    throw new Error(`Tool call failed: ${response.statusText}`);
  }

  // ✅ Log the response data
  const data = await response.json();
  console.log("✅ Received response from Spring Boot:");
  console.log(JSON.stringify(data, null, 2));

  return data; // Must match CallToolResponseSchema
});


// Launch server over stdio
async function runServer() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
 // console.log("Proxy server is running on stdio...");
}

runServer().catch(console.error);
