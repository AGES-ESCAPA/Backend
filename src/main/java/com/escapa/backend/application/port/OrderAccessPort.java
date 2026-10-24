package com.escapa.backend.application.port;

import com.escapa.backend.application.model.Enrollment;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Liberação de acesso que acompanha um pedido aprovado (US-27): matrícula do aluno em
 * {@code user_courses}, curso da empresa em {@code company_courses} e assentos da empresa.
 * Roda na mesma transação do pedido, então uma falha desfaz o pedido e o acesso juntos.
 */
public interface OrderAccessPort {

    Optional<Enrollment> findStudentEnrollment(UUID userId, UUID courseId);

    /**
     * Cria a matrícula com progresso zero. Se o aluno já tinha uma matrícula (por exemplo, vencida),
     * renova só as datas e mantém o progresso já feito.
     */
    void saveStudentEnrollment(UUID userId, UUID courseId, LocalDate startDate, LocalDate expirationDate);

    Optional<Enrollment> findCompanyCourse(UUID companyId, UUID courseId);

    /** Cria o curso da empresa ou, se já existia, renova as datas. */
    void saveCompanyCourse(UUID companyId, UUID courseId, LocalDate startDate, LocalDate expirationDate);

    /** Soma assentos em {@code company.seats_total}, sem liberar nenhum curso. */
    void addCompanySeats(UUID companyId, int quantity);
}
