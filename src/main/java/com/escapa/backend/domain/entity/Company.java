package com.escapa.backend.domain.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, of = {"companyName", "cnpjId"})
public class Company extends User {
    private String companyName;
    private String cnpjId;
    private String companyEmail;
    private Integer matricula;
    private List<CompanyCourse> companyCourses = new ArrayList<>();

    public Company(Integer id, String email, String password, String userType, LocalDateTime createdAt,
                   String companyName, String cnpjId, String companyEmail, Integer matricula) {
        super(id, email, password, userType, createdAt, new ArrayList<>());
        this.companyName = companyName;
        this.cnpjId = cnpjId;
        this.companyEmail = companyEmail;
        this.matricula = matricula;
    }
}