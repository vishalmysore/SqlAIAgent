package io.github.vishalmysore;


import io.github.vishalmysore.mcp.domain.*;
import io.github.vishalmysore.mcp.server.MCPToolsController;
import lombok.extern.java.Log;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.*;
@Log
@RestController
@RequestMapping("/mcp")
public class MCPController extends MCPToolsController {



    @GetMapping("/list-tools")
    public ResponseEntity<Map<String, List<Tool>>> listTools() {
        Map<String, List<Tool>> response = new HashMap<>();
        response.put("tools", super.getToolsResult().getTools());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/call-tool")
    public ResponseEntity<CallToolResult> callTool(@RequestBody ToolCallRequest request) {
          return super.callTool(request);
       }


}
