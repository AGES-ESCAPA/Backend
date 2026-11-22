package com.escapa.backend.domain.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class QuizPresentationTest {

    @Test
    void shouldReturnEmptyForNullOrEmptyQuestions() {
        assertTrue(QuizPresentation.withoutCorrectAnswers(null).isEmpty());
        assertTrue(QuizPresentation.withoutCorrectAnswers(List.of()).isEmpty());
    }

    @Test
    void shouldStripCorrectFlagsAndTolerateNullOptions() {
        final UUID questionId = UUID.randomUUID();
        final UUID optionId = UUID.randomUUID();
        final List<StudentQuizQuestion> visible = QuizPresentation.withoutCorrectAnswers(List.of(
                new QuizQuestion(
                        questionId,
                        "Pergunta",
                        List.of(new QuizOption(optionId, "A", true), new QuizOption(UUID.randomUUID(), "B", false))),
                new QuizQuestion(UUID.randomUUID(), "Sem opções", null)));

        assertEquals(2, visible.size());
        assertEquals(questionId, visible.get(0).id());
        assertEquals(2, visible.get(0).options().size());
        assertEquals(optionId, visible.get(0).options().get(0).id());
        assertEquals("A", visible.get(0).options().get(0).text());
        assertTrue(visible.get(1).options().isEmpty());
    }
}
