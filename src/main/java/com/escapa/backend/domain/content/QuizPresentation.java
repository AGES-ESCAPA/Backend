package com.escapa.backend.domain.content;

import java.util.ArrayList;
import java.util.List;

public final class QuizPresentation {

    private QuizPresentation() {
    }

    public static List<StudentQuizQuestion> withoutCorrectAnswers(List<QuizQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }
        final List<StudentQuizQuestion> visible = new ArrayList<>(questions.size());
        for (final QuizQuestion question : questions) {
            final List<StudentQuizOption> options = question.options() == null
                    ? List.of()
                    : question.options().stream()
                            .map(option -> new StudentQuizOption(option.id(), option.text()))
                            .toList();
            visible.add(new StudentQuizQuestion(question.id(), question.prompt(), options));
        }
        return List.copyOf(visible);
    }
}