package com.celonis.challenge.controllers;

import com.celonis.challenge.model.ProjectGenerationTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createExecuteAndProgress() throws Exception {
        ProjectGenerationTask t = new ProjectGenerationTask();
        t.setName("counter");
        t.setX(1);
        t.setY(2);

        String body = objectMapper.writeValueAsString(t);

        String response = mockMvc.perform(post("/api/tasks/")
                        .header("Celonis-Auth", "totally_secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ProjectGenerationTask created = objectMapper.readValue(response, ProjectGenerationTask.class);

        mockMvc.perform(post("/api/tasks/" + created.getId() + "/execute")
                        .header("Celonis-Auth", "totally_secret"))
                .andExpect(status().isNoContent());

        // progress endpoint returns the task with status/progress
        mockMvc.perform(get("/api/tasks/" + created.getId() + "/progress")
                        .header("Celonis-Auth", "totally_secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()));
    }
}


