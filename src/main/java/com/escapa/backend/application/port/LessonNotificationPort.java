package com.escapa.backend.application.port;

import java.util.UUID;

/**
 * Envio de notificações relacionadas a uma aula. Mantém o caso de uso alheio ao
 * progresso dos alunos e à forma como a notificação é persistida.
 */
public interface LessonNotificationPort {

    /** Notifica os alunos que já concluíram a aula de que ela foi atualizada e deve ser refeita. */
    void notifyLessonUpdated(UUID lessonId, String lessonTitle);
}
