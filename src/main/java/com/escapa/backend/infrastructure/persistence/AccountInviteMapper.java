package com.escapa.backend.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import com.escapa.backend.domain.user.AccountInvite;
import com.escapa.backend.infrastructure.persistence.entity.AccountInviteEntity;

public final class AccountInviteMapper {

    private AccountInviteMapper() {
    }

    public static AccountInviteEntity toEntity(AccountInvite invite) {
        final UUID id = invite.getId() != null ? invite.getId() : UUID.randomUUID();
        final LocalDateTime createdAt = invite.getCreatedAt() != null ? invite.getCreatedAt() : LocalDateTime.now();
        return new AccountInviteEntity(id, invite.getUserId(), invite.getCompanyId(), invite.getTokenHash(),
                invite.getExpiresAt(), invite.getUsedAt(), createdAt);
    }

    public static void updateEntity(AccountInviteEntity entity, AccountInvite invite) {
        entity.setExpiresAt(invite.getExpiresAt());
        entity.setUsedAt(invite.getUsedAt());
    }

    public static AccountInvite toDomain(AccountInviteEntity entity) {
        return new AccountInvite(entity.getId(), entity.getUserId(), entity.getCompanyId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getUsedAt(), entity.getCreatedAt());
    }
}
