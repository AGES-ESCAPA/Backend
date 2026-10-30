package com.escapa.backend.application.model;

/** Quantos colaboradores uma empresa tem vinculados e quantos deles estão ativos. */
public record CompanyEmployeeCounts(int total, int active) {
}
