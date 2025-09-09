package com.celonis.challenge.controllers;

import com.celonis.challenge.model.ProjectGenerationTask;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void missingAuthHeaderReturns401() throws Exception {
        ProjectGenerationTask t = new ProjectGenerationTask();
        t.setName("n");
        String body = objectMapper.writeValueAsString(t);
        mockMvc.perform(post("/api/tasks/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validationErrorReturns400Json() throws Exception {
        ProjectGenerationTask t = new ProjectGenerationTask();
        t.setName("");
        t.setX(-1);
        t.setY(-2);
        String body = objectMapper.writeValueAsString(t);
        mockMvc.perform(post("/api/tasks/")
                .header("Celonis-Auth", "totally_secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
}


