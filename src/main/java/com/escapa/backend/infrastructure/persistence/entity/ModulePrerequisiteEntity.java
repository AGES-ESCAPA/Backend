package com.escapa.backend.infrastructure.persistence.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Relacao N:N autorreferenciada: modulos bloqueantes que precisam ser concluidos
 * antes deste modulo ser liberado para o aluno.
 */
@Entity
@Table(name = "module_prerequisites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModulePrerequisiteEntity {

    @EmbeddedId
    private ModulePrerequisiteId id;

    @ManyToOne
    @MapsId("moduleId")
    @JoinColumn(name = "module_id", nullable = false)
    private ModuleEntity module;

    @ManyToOne
    @MapsId("prerequisiteModuleId")
    @JoinColumn(name = "prerequisite_module_id", nullable = false)
    private ModuleEntity prerequisiteModule;
}
