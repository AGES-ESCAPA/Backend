package com.escapa.backend.domain.content;

import java.util.List;

import com.escapa.backend.domain.entity.Content;

/**
 * Campos obrigatorios variam conforme o tipo da aula: VIDEO precisa de player e
 * duracao, FILE precisa do arquivo e TEXT precisa do proprio texto. Materiais
 * complementares valem para qualquer tipo.
 */
public final class ContentTypeRules {

    private ContentTypeRules() {
    }

    public static void validate(Content content) {
        if (content.getType() == null) {
            throw new IllegalArgumentException("Type is required");
        }
        switch (content.getType()) {
            case VIDEO -> validateVideo(content);
            case FILE -> requireUrl(content);
            case TEXT -> requireDescription(content);
            case QUIZ -> validateQuiz(content);
            default -> throw new IllegalArgumentException("Unsupported content type: " + content.getType());
        }
        if (content.getResources() != null) {
            content.getResources().forEach(ContentTypeRules::validateResource);
        }
    }

    private static void validateResource(ContentResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resources must not contain null items");
        }
        if (!ContentResource.TYPE_LINK.equals(resource.type()) && !ContentResource.TYPE_FILE.equals(resource.type())) {
            throw new IllegalArgumentException("resources.type must be 'link' or 'file'");
        }
        if (isBlank(resource.title())) {
            throw new IllegalArgumentException("resources.title is required");
        }
        if (isBlank(resource.url())) {
            throw new IllegalArgumentException("resources.url is required");
        }
        final String url = resource.url().trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("resources.url must start with http:// or https://");
        }
    }

    private static void validateVideo(Content content) {
        requireUrl(content);
        if (content.getDurationMinutes() == null) {
            throw new IllegalArgumentException("durationMinutes is required for content type VIDEO");
        }
        if (content.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("durationMinutes must be greater than zero");
        }
    }

    private static void requireUrl(Content content) {
        if (isBlank(content.getUrl())) {
            throw new IllegalArgumentException("url is required for content type " + content.getType());
        }
    }

    private static void requireDescription(Content content) {
        if (isBlank(content.getDescription())) {
            throw new IllegalArgumentException("description is required for content type TEXT");
        }
    }

    private static void validateQuiz(Content content) {
        final List<QuizQuestion> questions = content.getQuizQuestions();
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("quizQuestions must contain at least one question for content type QUIZ");
        }
        for (final QuizQuestion question : questions) {
            validateQuizQuestion(question);
        }
    }

    private static void validateQuizQuestion(QuizQuestion question) {
        if (question == null) {
            throw new IllegalArgumentException("quizQuestions must not contain null items");
        }
        if (question.id() == null) {
            throw new IllegalArgumentException("quizQuestions.id is required");
        }
        if (isBlank(question.prompt())) {
            throw new IllegalArgumentException("quizQuestions.prompt is required");
        }
        final List<QuizOption> options = question.options();
        if (options == null || options.size() < 2) {
            throw new IllegalArgumentException("quizQuestions.options must contain at least two options");
        }
        int correctCount = 0;
        for (final QuizOption option : options) {
            if (option == null) {
                throw new IllegalArgumentException("quizQuestions.options must not contain null items");
            }
            if (option.id() == null) {
                throw new IllegalArgumentException("quizQuestions.options.id is required");
            }
            if (isBlank(option.text())) {
                throw new IllegalArgumentException("quizQuestions.options.text is required");
            }
            if (option.correct()) {
                correctCount++;
            }
        }
        if (correctCount != 1) {
            throw new IllegalArgumentException("quizQuestions.options must contain exactly one correct option");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }
}
