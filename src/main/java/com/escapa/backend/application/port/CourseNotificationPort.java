package com.escapa.backend.application.port;

import java.util.UUID;

/**
 * Envio de notificações relacionadas a um curso. Mantém o caso de uso alheio a
 * matrículas, entidades JPA e à forma como a notificação é persistida.
 */
public interface CourseNotificationPort {

    /** Notifica todos os alunos com matrícula ativa de que o curso foi publicado. */
    void notifyCoursePublished(UUID courseId);
}
