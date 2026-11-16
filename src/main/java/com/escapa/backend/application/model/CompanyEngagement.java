package com.escapa.backend.application.model;

import java.util.UUID;

/** Empresa parceira ordenada pelo total de matrículas elegíveis dos colaboradores vinculados. */
public record CompanyEngagement(UUID companyId, String name, long enrollments) {
}
