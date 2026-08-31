package com.escapa.backend.infrastructure.persistence.entity;

import com.escapa.backend.infrastructure.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "company")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class CompanyEntity extends UserEntity {

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "cnpj_id", unique = true)
    private String cnpjId;

    @Column(name = "company_email")
    private String companyEmail;

    @Column(name = "matricula")
    private Integer matricula;

    @OneToMany(mappedBy = "company")
    private List<CompanyCourseEntity> companyCourses = new ArrayList<>();

    public CompanyEntity(String id, String name, String email, String role,
                         String companyName, String cnpjId, String companyEmail, Integer matricula) {
        super(id, name, email, role);
        this.companyName = companyName;
        this.cnpjId = cnpjId;
        this.companyEmail = companyEmail;
        this.matricula = matricula;
    }
}
