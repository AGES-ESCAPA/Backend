package com.escapa.backend.application.port;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Retira o acesso liberado por um pedido (US-28). Nada é apagado: a matrícula, o
 * progresso e o certificado ficam no banco e só deixam de dar acesso.
 */
public interface OrderAccessRevocationPort {

    /** Revoga a matrícula do aluno no curso. */
    void revokeStudentCourseAccess(UUID userId, UUID courseId, LocalDateTime revokedAt);

    /** Revoga o curso da empresa e as matrículas dos colaboradores da empresa nesse curso. */
    void revokeCompanyCourseAccess(UUID companyId, UUID courseId, LocalDateTime revokedAt);
}
