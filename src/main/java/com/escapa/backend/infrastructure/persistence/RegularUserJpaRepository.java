package com.escapa.backend.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.escapa.backend.application.model.StudentContact;
import com.escapa.backend.infrastructure.persistence.entity.RegularUserEntity;

@Repository
public interface RegularUserJpaRepository extends JpaRepository<RegularUserEntity, UUID> {

    boolean existsByCpf(String cpf);

    @Query("SELECT new com.escapa.backend.application.model.StudentContact(r.cpf, r.phone) "
            + "FROM RegularUserEntity r WHERE r.id = :userId")
    Optional<StudentContact> findContactByUserId(@Param("userId") UUID userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "INSERT INTO regular_users (user_id, phone) VALUES (:userId, :phone) "
            + "ON CONFLICT (user_id) DO UPDATE SET phone = EXCLUDED.phone", nativeQuery = true)
    void upsertPhone(@Param("userId") UUID userId, @Param("phone") String phone);
}
