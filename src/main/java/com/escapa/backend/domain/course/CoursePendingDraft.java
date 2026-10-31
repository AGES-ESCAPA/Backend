package com.escapa.backend.domain.course;

import com.escapa.backend.domain.entity.Course;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Alterações de um curso já publicado que ainda não devem aparecer para os alunos.
 * A linha publicada do curso permanece intacta até a publicação aplicar este rascunho.
 */
public record CoursePendingDraft(
        String title,
        String shortDescription,
        String description,
        String thumbnailUrl,
        String teaserVideoUrl,
        UUID instructorId,
        String category,
        String level,
        Integer durationTime,
        Integer deadline,
        Double price,
        List<String> learningObjectives,
        Boolean requireSequentialProgress,
        Boolean enforceDeadlineBlock,
        List<UUID> prerequisiteIds) {

    public static CoursePendingDraft fromPublished(Course course, List<UUID> prerequisiteIds) {
        return new CoursePendingDraft(
                course.getTitle(),
                course.getShortDescription(),
                course.getDescription(),
                course.getThumbnailUrl(),
                course.getTeaserVideoUrl(),
                course.getInstructor() != null ? course.getInstructor().getId() : null,
                course.getCategory(),
                course.getLevel(),
                course.getDurationTime(),
                course.getDeadline(),
                course.getPrice(),
                copyStrings(course.getLearningObjectives()),
                course.getRequireSequentialProgress(),
                course.getEnforceDeadlineBlock(),
                copyIds(prerequisiteIds));
    }

    public CoursePendingDraft applyUpdate(
            String title,
            String shortDescription,
            String description,
            String thumbnailUrl,
            String teaserVideoUrl,
            UUID instructorId,
            String category,
            String level,
            Integer durationTime,
            Integer deadline,
            Double price,
            List<String> learningObjectives,
            Boolean requireSequentialProgress,
            Boolean enforceDeadlineBlock) {
        return new CoursePendingDraft(
                title != null && !title.trim().isBlank() ? title.trim() : this.title,
                shortDescription != null ? shortDescription : this.shortDescription,
                description != null ? description : this.description,
                thumbnailUrl != null ? blankToNull(thumbnailUrl) : this.thumbnailUrl,
                teaserVideoUrl != null ? teaserVideoUrl : this.teaserVideoUrl,
                instructorId != null ? instructorId : this.instructorId,
                category != null ? category : this.category,
                level != null ? level : this.level,
                durationTime != null ? durationTime : this.durationTime,
                deadline != null ? deadline : this.deadline,
                price != null ? price : this.price,
                learningObjectives != null ? copyStrings(learningObjectives) : copyStrings(this.learningObjectives),
                requireSequentialProgress != null ? requireSequentialProgress : this.requireSequentialProgress,
                enforceDeadlineBlock != null ? enforceDeadlineBlock : this.enforceDeadlineBlock,
                copyIds(this.prerequisiteIds));
    }

    public CoursePendingDraft withRules(boolean requireSequentialProgress, boolean enforceDeadlineBlock) {
        return new CoursePendingDraft(
                title, shortDescription, description, thumbnailUrl, teaserVideoUrl, instructorId,
                category, level, durationTime, deadline, price, copyStrings(learningObjectives),
                requireSequentialProgress, enforceDeadlineBlock, copyIds(prerequisiteIds));
    }

    public boolean containsPrerequisite(UUID prerequisiteId) {
        return prerequisiteIds != null && prerequisiteIds.contains(prerequisiteId);
    }

    public CoursePendingDraft withPrerequisite(UUID prerequisiteId) {
        final List<UUID> ids = new ArrayList<>(copyIds(prerequisiteIds));
        ids.add(prerequisiteId);
        return new CoursePendingDraft(
                title, shortDescription, description, thumbnailUrl, teaserVideoUrl, instructorId,
                category, level, durationTime, deadline, price, copyStrings(learningObjectives),
                requireSequentialProgress, enforceDeadlineBlock, List.copyOf(ids));
    }

    public CoursePendingDraft withoutPrerequisite(UUID prerequisiteId) {
        final List<UUID> ids = new ArrayList<>(copyIds(prerequisiteIds));
        ids.remove(prerequisiteId);
        return new CoursePendingDraft(
                title, shortDescription, description, thumbnailUrl, teaserVideoUrl, instructorId,
                category, level, durationTime, deadline, price, copyStrings(learningObjectives),
                requireSequentialProgress, enforceDeadlineBlock, List.copyOf(ids));
    }

    public void applyTo(Course course) {
        course.setTitle(title);
        course.setShortDescription(shortDescription);
        course.setDescription(description);
        course.setThumbnailUrl(thumbnailUrl);
        course.setTeaserVideoUrl(teaserVideoUrl);
        course.setCategory(category);
        course.setLevel(level);
        course.setDurationTime(durationTime);
        course.setDeadline(deadline);
        course.setPrice(price);
        course.setLearningObjectives(new ArrayList<>(copyStrings(learningObjectives)));
        if (requireSequentialProgress != null) {
            course.setRequireSequentialProgress(requireSequentialProgress);
        }
        if (enforceDeadlineBlock != null) {
            course.setEnforceDeadlineBlock(enforceDeadlineBlock);
        }
    }

    private static String blankToNull(String value) {
        final String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<String> copyStrings(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<UUID> copyIds(List<UUID> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
