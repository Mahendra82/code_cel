package com.celonis.challenge.services;

import com.celonis.challenge.model.ProjectGenerationTask;
import com.celonis.challenge.model.ProjectGenerationTaskRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectGenerationTaskRepository repository;

    @Test
    void counterTaskCompletes() {
        ProjectGenerationTask t = new ProjectGenerationTask();
        t.setName("counter");
        t.setX(1);
        t.setY(3);
        t = taskService.createTask(t);

        taskService.executeTask(t.getId());

        String taskId = t.getId();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ProjectGenerationTask latest = repository.findById(taskId).orElseThrow();
            assertThat(latest.getStatus()).isEqualTo(ProjectGenerationTask.Status.COMPLETED);
            assertThat(latest.getProgress()).isEqualTo(3);
        });
    }

    @Test
    void counterTaskCancels() {
        ProjectGenerationTask t = new ProjectGenerationTask();
        t.setName("counter");
        t.setX(1);
        t.setY(5);
        t = taskService.createTask(t);

        taskService.executeTask(t.getId());

        taskService.cancel(t.getId());

        String taskId = t.getId();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ProjectGenerationTask latest = repository.findById(taskId).orElseThrow();
            assertThat(latest.getStatus()).isEqualTo(ProjectGenerationTask.Status.CANCELLED);
        });
    }
}


