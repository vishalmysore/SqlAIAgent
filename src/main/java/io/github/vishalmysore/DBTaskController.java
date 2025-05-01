package io.github.vishalmysore;

import io.github.vishalmysore.domain.SendTaskResponse;
import io.github.vishalmysore.domain.TaskSendParams;
import io.github.vishalmysore.server.DyanamicTaskContoller;
import lombok.extern.java.Log;

@Log
public class DBTaskController extends DyanamicTaskContoller {
    public  SendTaskResponse sendTask(TaskSendParams taskSendParams) {
        SendTaskResponse response= sendTask(taskSendParams, new CustomTaskCallback());
        log.info("Task sent with ID: " + response.toString());
        return response;
    }
}
