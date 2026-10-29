package com.escapa.backend.infrastructure.persistence.entity;

import com.escapa.backend.infrastructure.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "regular_users")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
public class RegularUserEntity extends UserEntity implements Persistable<UUID> {

    @Transient
    private boolean persisted;

    @Column(name = "cpf", unique = true)
    private String cpf;

    @Column(name = "phone")
    private String phone;

    public RegularUserEntity(UUID id, String name, String email, String passwordHash, String role,
                             LocalDateTime createdAt, String cpf, String phone) {
        super(id, name, email, passwordHash, role, createdAt);
        this.cpf = cpf;
        this.phone = phone;
    }

    @Override
    public boolean isNew() {
        return !persisted;
    }

    @PostLoad
    @PostPersist
    void markPersisted() {
        persisted = true;
    }
}
