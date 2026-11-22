package com.escapa.backend.application.usecase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.escapa.backend.application.model.StudentCourseCurriculum;
import com.escapa.backend.domain.content.LessonStatus;
import com.escapa.backend.domain.entity.Content;
import com.escapa.backend.domain.entity.Module;

/**
 * Calcula o status (COMPLETED/AVAILABLE/LOCKED) de cada aula de um curso para um
 * aluno, com base no progresso e na regra de progressao sequencial do curso.
 * Com a regra desligada, modulos e aulas ficam liberados sem ordem. Com a regra
 * ligada, o modulo seguinte so abre depois do anterior e cada aula espera a
 * anterior. Compartilhada entre a grade de aulas (US-13) e, futuramente, a API
 * de conteudo da aula (US-11), para recusar aulas bloqueadas.
 */
public class LessonStatusCalculator {

    /**
     * @param modules                    modulos do curso, ordenados, cada um com suas aulas ordenadas
     * @param completedContentIds        ids das aulas que o aluno ja concluiu neste curso
     * @param prerequisiteModuleIdsFinder devolve os ids dos modulos pre-requisito de um modulo
     * @param requireSequentialProgress  se o curso exige concluir o modulo anterior antes do proximo
     * @param enrolled                   se o aluno tem matricula; sem ela so aulas {@code isFree} ficam AVAILABLE
     */
    public StudentCourseCurriculum calculate(
            UUID courseId,
            List<Module> modules,
            List<UUID> completedContentIds,
            Function<UUID, List<UUID>> prerequisiteModuleIdsFinder,
            boolean requireSequentialProgress,
            boolean enrolled
    ) {
        final Set<UUID> completed = new HashSet<>(completedContentIds);
        final List<Content> orderedLessons = flattenOrderedLessons(modules);

        final List<StudentCourseCurriculum.Module> moduleResults = new ArrayList<>();
        int courseCompletedCount = 0;
        int courseTotalCount = 0;

        for (final Module module : modules) {
            final StudentCourseCurriculum.Module moduleResult = buildModule(
                    module, completed, orderedLessons, modules, prerequisiteModuleIdsFinder,
                    requireSequentialProgress, enrolled);
            moduleResults.add(moduleResult);
            courseCompletedCount += moduleResult.completedLessons();
            courseTotalCount += moduleResult.totalLessons();
        }

        return new StudentCourseCurriculum(courseId, courseCompletedCount, courseTotalCount, moduleResults);
    }

    private StudentCourseCurriculum.Module buildModule(
            Module module,
            Set<UUID> completed,
            List<Content> orderedLessons,
            List<Module> modules,
            Function<UUID, List<UUID>> prerequisiteModuleIdsFinder,
            boolean requireSequentialProgress,
            boolean enrolled
    ) {
        final boolean hasPendingPrerequisite = hasPendingPrerequisite(
                module, completed, modules, prerequisiteModuleIdsFinder);
        final List<StudentCourseCurriculum.Lesson> lessonResults = new ArrayList<>();
        int moduleCompletedCount = 0;

        for (final Content lesson : module.getContents()) {
            final LessonStatus status = resolveStatus(
                    lesson, completed, orderedLessons, hasPendingPrerequisite,
                    requireSequentialProgress, enrolled);
            if (status == LessonStatus.COMPLETED) {
                moduleCompletedCount++;
            }
            lessonResults.add(new StudentCourseCurriculum.Lesson(
                    lesson.getId(), lesson.getTitle(), lesson.getOrder(),
                    lesson.getType() != null ? lesson.getType().name() : null,
                    lesson.getDurationMinutes(), status));
        }

        final boolean moduleLocked = !lessonResults.isEmpty() && lessonResults.stream()
                .allMatch(l -> l.status() == LessonStatus.LOCKED);
        return new StudentCourseCurriculum.Module(
                module.getId(), module.getTitle(), module.getOrder(), moduleLocked,
                moduleCompletedCount, lessonResults.size(), lessonResults);
    }

    private LessonStatus resolveStatus(
            Content lesson,
            Set<UUID> completed,
            List<Content> orderedLessons,
            boolean modulePrerequisitePending,
            boolean requireSequentialProgress,
            boolean enrolled
    ) {
        if (completed.contains(lesson.getId())) {
            return LessonStatus.COMPLETED;
        }
        final boolean isFree = Boolean.TRUE.equals(lesson.getIsFree());
        // Sem matricula: amostra gratuita liberada; o restante permanece bloqueado.
        if (!enrolled) {
            return isFree ? LessonStatus.AVAILABLE : LessonStatus.LOCKED;
        }
        if (requireSequentialProgress && modulePrerequisitePending) {
            return LessonStatus.LOCKED;
        }
        if (requireSequentialProgress && isBlockedBySequentialProgress(lesson, orderedLessons, completed)) {
            return LessonStatus.LOCKED;
        }
        return LessonStatus.AVAILABLE;
    }

    private boolean isBlockedBySequentialProgress(Content lesson, List<Content> orderedLessons, Set<UUID> completed) {
        final int index = orderedLessons.indexOf(lesson);
        if (index <= 0) {
            // Primeira aula do curso: nunca bloqueada pela regra sequencial.
            return false;
        }
        final Content previous = orderedLessons.get(index - 1);
        return !completed.contains(previous.getId());
    }

    private boolean hasPendingPrerequisite(
            Module module,
            Set<UUID> completed,
            List<Module> allModules,
            Function<UUID, List<UUID>> prerequisiteModuleIdsFinder
    ) {
        final List<UUID> prerequisiteModuleIds = prerequisiteModuleIdsFinder.apply(module.getId());
        if (prerequisiteModuleIds.isEmpty()) {
            return false;
        }
        for (final UUID prerequisiteModuleId : prerequisiteModuleIds) {
            final Module prerequisiteModule = allModules.stream()
                    .filter(m -> m.getId().equals(prerequisiteModuleId))
                    .findFirst()
                    .orElse(null);
            if (prerequisiteModule == null) {
                continue;
            }
            final boolean allCompleted = prerequisiteModule.getContents().stream()
                    .allMatch(c -> completed.contains(c.getId()));
            if (!allCompleted) {
                return true;
            }
        }
        return false;
    }

    private List<Content> flattenOrderedLessons(List<Module> modules) {
        final List<Content> lessons = new ArrayList<>();
        for (final Module module : modules) {
            lessons.addAll(module.getContents());
        }
        return lessons;
    }
}