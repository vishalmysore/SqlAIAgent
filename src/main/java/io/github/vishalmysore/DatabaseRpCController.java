package io.github.vishalmysore;

import io.github.vishalmysore.domain.JsonRpcRequest;
import io.github.vishalmysore.server.A2ATaskController;
import io.github.vishalmysore.server.JsonRpcController;
import lombok.extern.java.Log;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Log
public class DatabaseRpCController extends JsonRpcController {
    @PostMapping
    public Object handleRpc(@RequestBody JsonRpcRequest request) {
        log.info(request.toString());
        return super.handleRpc(request);
    }
    @Override
    public A2ATaskController getTaskController() {
        return new DBTaskController();
    }
}
