import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { CallToolRequestSchema, ListResourcesRequestSchema, ListToolsRequestSchema, ReadResourceRequestSchema, } from "@modelcontextprotocol/sdk/types.js";
import puppeteer from "puppeteer-core";
import { Browserbase } from "@browserbasehq/sdk";
import fs from 'node:fs/promises';

const logFilePath = '/temp/proxy_server.log';



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
  try {
    const response = await fetch("http://localhost:7860/mcp/list-tools", {
      method: "GET",
      headers: { "Content-Type": "application/json" }
    });

    if (!response.ok) {
      const errorMessage = `Failed to fetch tools: ${response.statusText}`;
     // await logToFile(errorMessage);
      throw new Error(errorMessage);
    }

    const data = await response.json();
   // await logToFile(`Available tools from Spring Boot: ${JSON.stringify(data, null, 2)}`);
    return {
      tools: data.tools,
    };
  } catch (error) {
    console.error("Error listing tools:", error);
    throw error;
  }
});

// Handler: Call a tool by proxying to Spring Boot
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  try {
  fs.appendFile(logFilePath, "receivedResponseLog", 'utf8');
    // 🔍 Log the outgoing request
    const outgoingRequestLog = "➡️ Sending request to Spring Boot:\n" + JSON.stringify({
      name: request.params.name,
      arguments: request.params.arguments ?? {},
    }, null, 2);
  //  await logToFile(outgoingRequestLog);

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
      const errorMessage = `❌ Tool call failed: ${response.statusText}\n🔻 Error response body: ${errorText}`;
      await logToFile(errorMessage);
      throw new Error(`Tool call failed: ${response.statusText}`);
    }

    // ✅ Log the response data
    const data = await response.json();
    const receivedResponseLog = "✅ Received response from Spring Boot:\n" + JSON.stringify(data, null, 2);
      fs.appendFile(logFilePath, receivedResponseLog, 'utf8');

    return data; // Must match CallToolResponseSchema
  } catch (error) {
    console.error("Error calling tool:", error);
    throw error;
  }
});


// Launch server over stdio
async function runServer() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
 // await logToFile("Proxy server is running on stdio...");
}

runServer().catch(console.error);