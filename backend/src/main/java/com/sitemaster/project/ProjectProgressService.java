package com.sitemaster.project;

import com.sitemaster.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalculates a project's progress % as the average progress of its tasks.
 * Kept separate so the trigger can move (Phase 4 recalculates when the PM
 * approves a daily report) without touching the formula.
 */
@Service
public class ProjectProgressService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectProgressService(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public int recalculate(Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        if (taskRepository.countByProject(projectId) == 0) {
            return project.getProgress();
        }
        int progress = (int) Math.round(taskRepository.averageProgress(projectId));
        project.updateProgress(progress);
        return progress;
    }
}
