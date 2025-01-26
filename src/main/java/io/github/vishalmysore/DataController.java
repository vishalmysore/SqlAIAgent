package io.github.vishalmysore;
import com.t4a.api.ActionGroup;
import com.t4a.api.GroupInfo;

import com.t4a.predict.PredictionLoader;

import com.t4a.processor.*;

import com.t4a.processor.spring.SpringOpenAIProcessor;
import io.github.vishalmysore.service.DerbyService;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.java.Log;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Log
@RestController
public class DataController {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DerbyService derbyService;

    @Operation(summary = "Execute any action based on prompt", description = " Try out with any of these prompts <br>" +
            " 1) start database server <br>" +
            " 2) add a new user named vishal <br>" +
            " 3) remove user named vishal <br> " +
            " 4) how many vishal are there in system "
    )
    @ApiResponses(value = {


    })
    @GetMapping("/actionOpenAI")
    public String actOnPromptWithOpenAI(@RequestParam("prompt") String prompt) {
        AIProcessor processor = new SpringOpenAIProcessor(applicationContext);
        try {

            Object object = processor.processSingleAction(prompt);
            String answer =  processor.query(prompt,object);
            return answer;
        } catch (AIProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
