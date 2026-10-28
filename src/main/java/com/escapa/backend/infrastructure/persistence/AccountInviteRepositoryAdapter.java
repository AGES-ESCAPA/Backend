package com.escapa.backend.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.escapa.backend.application.port.AccountInviteRepositoryPort;
import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.infrastructure.persistence.entity.AccountInviteEntity;

@Repository
public class AccountInviteRepositoryAdapter implements AccountInviteRepositoryPort {

    private final AccountInviteJpaRepository accountInviteJpaRepository;

    public AccountInviteRepositoryAdapter(AccountInviteJpaRepository accountInviteJpaRepository) {
        this.accountInviteJpaRepository = accountInviteJpaRepository;
    }

    @Override
    public AccountInvite save(AccountInvite invite) {
        final AccountInviteEntity entity;
        if (invite.getId() != null) {
            entity = accountInviteJpaRepository.findById(invite.getId())
                    .orElseGet(() -> AccountInviteMapper.toEntity(invite));
            AccountInviteMapper.updateEntity(entity, invite);
        } else {
            entity = AccountInviteMapper.toEntity(invite);
        }
        return AccountInviteMapper.toDomain(accountInviteJpaRepository.save(entity));
    }

    @Override
    public Optional<AccountInvite> findByTokenHash(String tokenHash) {
        return accountInviteJpaRepository.findByTokenHash(tokenHash).map(AccountInviteMapper::toDomain);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return accountInviteJpaRepository.existsByUserId(userId);
    }
}
