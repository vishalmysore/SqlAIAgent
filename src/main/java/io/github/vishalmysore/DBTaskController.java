package io.github.vishalmysore;


import com.t4a.processor.AIProcessor;
import com.t4a.processor.OpenAiActionProcessor;
import io.github.vishalmysore.a2a.domain.SendTaskResponse;
import io.github.vishalmysore.a2a.domain.TaskSendParams;
import io.github.vishalmysore.a2a.server.DyanamicTaskContoller;
import lombok.extern.java.Log;

@Log
public class DBTaskController extends DyanamicTaskContoller {

    private OpenAiActionProcessor openAiActionProcessor = new OpenAiActionProcessor();


    public SendTaskResponse sendTask(TaskSendParams taskSendParams) {
        SendTaskResponse response= sendTask(taskSendParams, new CustomTaskCallback());
        log.info("Task sent with ID: " + response.toString());
        return response;
    }
}
