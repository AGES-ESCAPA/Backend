package com.escapa.backend.application.port;

import com.escapa.backend.domain.entity.User;

public interface RegularUserRepositoryPort {

    User save(User user, String cpf, String phone);

    boolean existsByCpf(String cpf);
}
