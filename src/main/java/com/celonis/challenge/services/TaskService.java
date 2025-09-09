package com.celonis.challenge.services;

import com.celonis.challenge.exceptions.InternalException;
import com.celonis.challenge.exceptions.NotFoundException;
import com.celonis.challenge.model.ProjectGenerationTask;
import com.celonis.challenge.model.ProjectGenerationTaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class TaskService {

    private final ProjectGenerationTaskRepository projectGenerationTaskRepository;

    private final FileService fileService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();
    
    public TaskService(ProjectGenerationTaskRepository projectGenerationTaskRepository,
                       FileService fileService) {
        this.projectGenerationTaskRepository = projectGenerationTaskRepository;
        this.fileService = fileService;
    }

    public List<ProjectGenerationTask> listTasks() {
        return projectGenerationTaskRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public ProjectGenerationTask createTask(ProjectGenerationTask projectGenerationTask) {
        projectGenerationTask.setId(null);
        projectGenerationTask.setCreationDate(new Date());
        // Initialize counter task defaults
        if (projectGenerationTask.getStatus() == null) {
            projectGenerationTask.setStatus(ProjectGenerationTask.Status.PENDING);
        }
        if (projectGenerationTask.getProgress() == null) {
            projectGenerationTask.setProgress(projectGenerationTask.getX() != null ? projectGenerationTask.getX() : 0);
        }
        projectGenerationTask.setCancelRequested(false);
        return projectGenerationTaskRepository.save(projectGenerationTask);
    }

    public ProjectGenerationTask getTask(String taskId) {
        return get(taskId);
    }

    @org.springframework.transaction.annotation.Transactional
    public ProjectGenerationTask update(String taskId, ProjectGenerationTask projectGenerationTask) {
        ProjectGenerationTask existing = get(taskId);
        existing.setCreationDate(projectGenerationTask.getCreationDate());
        existing.setName(projectGenerationTask.getName());
        return projectGenerationTaskRepository.save(existing);
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(String taskId) {
        projectGenerationTaskRepository.deleteById(taskId);
    }

    public void executeTask(String taskId) {
        ProjectGenerationTask task = get(taskId);
        // If x and y are provided, run counter task; else fallback to original behavior
        if (task.getX() != null && task.getY() != null) {
            if (runningTasks.containsKey(taskId)) {
                return; // already running
            }
            task.setStatus(ProjectGenerationTask.Status.RUNNING);
            projectGenerationTaskRepository.save(task);

            Future<?> future;
            try {
                future = executorService.submit(() -> {
                    try {
                        int current = task.getProgress() != null ? task.getProgress() : task.getX();
                        int target = task.getY();
                        while (current <= target) {
                            ProjectGenerationTask latest = get(taskId);
                            if (Boolean.TRUE.equals(latest.getCancelRequested())) {
                                latest.setStatus(ProjectGenerationTask.Status.CANCELLED);
                                projectGenerationTaskRepository.save(latest);
                                return;
                            }
                            latest.setProgress(current);
                            projectGenerationTaskRepository.save(latest);
                            if (current == target) {
                                latest.setStatus(ProjectGenerationTask.Status.COMPLETED);
                                projectGenerationTaskRepository.save(latest);
                                return;
                            }
                            current++;
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                latest.setStatus(ProjectGenerationTask.Status.CANCELLED);
                                projectGenerationTaskRepository.save(latest);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        ProjectGenerationTask latest = get(taskId);
                        latest.setStatus(ProjectGenerationTask.Status.FAILED);
                        projectGenerationTaskRepository.save(latest);
                    } finally {
                        runningTasks.remove(taskId);
                    }
                });
            } catch (RejectedExecutionException ree) {
                task.setStatus(ProjectGenerationTask.Status.PENDING);
                projectGenerationTaskRepository.save(task);
                throw new com.celonis.challenge.exceptions.OverloadedException("System overloaded; try again later");
            }
            runningTasks.put(taskId, future);
            return;
        }

        URL url = Thread.currentThread().getContextClassLoader().getResource("challenge.zip");
        if (url == null) {
            throw new InternalException("Zip file not found");
        }
        try {
            fileService.storeResult(taskId, url);
        } catch (Exception e) {
            throw new InternalException(e);
        }
    }

    public ProjectGenerationTask cancel(String taskId) {
        ProjectGenerationTask task = get(taskId);
        if (!Boolean.TRUE.equals(task.getCancelRequested())) {
            task.setCancelRequested(true);
            Future<?> future = runningTasks.get(taskId);
            if (future != null) {
                future.cancel(true);
            }
        }
        return projectGenerationTaskRepository.save(task);
    }

    @javax.annotation.PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    private ProjectGenerationTask get(String taskId) {
        Optional<ProjectGenerationTask> projectGenerationTask = projectGenerationTaskRepository.findById(taskId);
        return projectGenerationTask.orElseThrow(NotFoundException::new);
    }

    // Cleanup tasks that are not executed after 7 days
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldPendingTasks() {
        Date now = new Date();
        List<ProjectGenerationTask> tasks = projectGenerationTaskRepository.findAll();
        for (ProjectGenerationTask t : tasks) {
            if (t.getStatus() == ProjectGenerationTask.Status.PENDING && t.getCreationDate() != null) {
                long ageMs = now.getTime() - t.getCreationDate().getTime();
                if (ageMs > 7L * 24 * 60 * 60 * 1000) {
                    projectGenerationTaskRepository.deleteById(t.getId());
                }
            }
        }
    }
}
