package com.escapa.backend.application.port;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Passos da desvinculação de um colaborador (US-33). Nada é apagado além do vínculo com a empresa:
 * matrículas, progresso e certificados ficam no banco. Os passos são chamados dentro da transação
 * do caso de uso, então uma falha em qualquer um desfaz a desvinculação inteira.
 */
public interface CompanyEmployeeUnlinkPort {

    /** Remove o vínculo e libera a vaga. Devolve {@code false} se o colaborador não é da empresa. */
    boolean removeLink(UUID companyId, UUID employeeId);

    /**
     * Revoga as matrículas do colaborador nos cursos da empresa. Cursos que ele comprou por conta
     * própria (pedido individual aprovado) não são revogados. Devolve quantas matrículas foram revogadas.
     */
    int revokeCompanyCourseEnrollments(UUID companyId, UUID employeeId, LocalDateTime revokedAt);

    /** Expira os convites em aberto do colaborador nessa empresa. Devolve quantos foram invalidados. */
    int invalidatePendingInvites(UUID companyId, UUID employeeId, LocalDateTime now);
}
