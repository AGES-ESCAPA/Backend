package com.escapa.backend.infrastructure.persistence.entity;

import com.escapa.backend.infrastructure.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "regular_users")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@NoArgsConstructor
public class RegularUserEntity extends UserEntity {

    @Column(name = "cpf", unique = true)
    private String cpf;

    @Column(name = "phone")
    private String phone;

    public RegularUserEntity(String id, String name, String email, String role, String cpf, String phone) {
        super(id, name, email, role);
        this.cpf = cpf;
        this.phone = phone;
    }
}
