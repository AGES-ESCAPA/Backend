package com.escapa.backend.adapters.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.escapa.backend.application.model.LessonDetails;
import com.escapa.backend.application.model.LessonSupplement;
import com.escapa.backend.domain.content.ContentType;
import com.escapa.backend.domain.content.QuizOption;
import com.escapa.backend.domain.content.QuizQuestion;
import com.escapa.backend.domain.entity.Content;

class StudentLessonResponseQuizTest {

    @Test
    void shouldExposeQuizQuestionsWithoutCorrectFlagsAndThePassPercentage() {
        final UUID questionId = UUID.randomUUID();
        final UUID correctId = UUID.randomUUID();
        final Content content = new Content();
        content.setId(UUID.randomUUID());
        content.setModuleId(UUID.randomUUID());
        content.setTitle("Quiz");
        content.setDescription("Desc");
        content.setType(ContentType.QUIZ);
        content.setOrder(1);
        content.setQuizQuestions(List.of(new QuizQuestion(
                questionId,
                "Pergunta",
                List.of(
                        new QuizOption(correctId, "Correta", true),
                        new QuizOption(UUID.randomUUID(), "Errada", false)))));

        final StudentLessonResponse response = StudentLessonResponse.from(new LessonDetails(
                content,
                UUID.randomUUID(),
                "Módulo",
                1,
                false,
                80,
                new LessonSupplement(List.of(), List.of())));

        assertEquals(ContentType.QUIZ, response.type());
        assertEquals(80, response.minQuizPassPercentage());
        assertEquals(1, response.quizQuestions().size());
        assertEquals(questionId, response.quizQuestions().get(0).id());
        assertEquals(2, response.quizQuestions().get(0).options().size());
        assertEquals(correctId, response.quizQuestions().get(0).options().get(0).id());
        assertEquals("Correta", response.quizQuestions().get(0).options().get(0).text());
    }
}
