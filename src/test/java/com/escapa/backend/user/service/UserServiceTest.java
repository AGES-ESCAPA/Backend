package com.escapa.backend.user.service;

import com.escapa.backend.user.dto.CreateUserRequest;
import com.escapa.backend.user.entity.UserEntity;
import com.escapa.backend.user.entity.UserStatus;
import com.escapa.backend.user.exception.EmailAlreadyUsedException;
import com.escapa.backend.user.exception.UserNotFoundException;
import com.escapa.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Só regra: normalização, unicidade de email, hash. Repositório e encoder são mocks,
 * então nada aqui precisa de Spring nem de banco.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserWithNormalizedDataAndHashedPassword() {
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed:password123");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final UserEntity user = userService.create(
                new CreateUserRequest(" Maria Silva ", " MARIA@email.com ", "password123", "student "));

        assertNotNull(user.getId());
        assertEquals("Maria Silva", user.getName());
        assertEquals("maria@email.com", user.getEmail());
        assertEquals("STUDENT", user.getRole());
        assertEquals("hashed:password123", user.getPasswordHash());
        assertNotEquals("password123", user.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void shouldPersistExactlyWhatWasBuilt() {
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.create(new CreateUserRequest("Joao", "joao@email.com", "password123", "TEACHER"));

        final ArgumentCaptor<UserEntity> saved = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(saved.capture());
        assertEquals("joao@email.com", saved.getValue().getEmail());
        assertEquals("TEACHER", saved.getValue().getRole());
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCaseBeforeTouchingPasswordOrDatabase() {
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(true);

        final EmailAlreadyUsedException ex = assertThrows(EmailAlreadyUsedException.class,
                () -> userService.create(
                        new CreateUserRequest("Outra Maria", "MARIA@email.com", "password123", "STUDENT")));

        assertEquals(EmailAlreadyUsedException.CODE, ex.getCode());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldReturnUserWhenFound() {
        final UUID id = UUID.randomUUID();
        final UserEntity stored = new UserEntity(id, "Maria", "maria@email.com", "hash", "STUDENT",
                LocalDateTime.now());
        when(userRepository.findById(id)).thenReturn(Optional.of(stored));

        assertSame(stored, userService.getById(id));
    }

    @Test
    void shouldThrowNotFoundWithCodeWhenUserDoesNotExist() {
        final UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        final UserNotFoundException ex = assertThrows(UserNotFoundException.class, () -> userService.getById(id));

        assertEquals(UserNotFoundException.CODE, ex.getCode());
    }

    @Test
    void shouldListAllUsers() {
        final UserEntity a = new UserEntity(UUID.randomUUID(), "A", "a@email.com", "h", "STUDENT",
                LocalDateTime.now());
        final UserEntity b = new UserEntity(UUID.randomUUID(), "B", "b@email.com", "h", "TEACHER",
                LocalDateTime.now());
        when(userRepository.findAll()).thenReturn(List.of(a, b));

        assertEquals(List.of(a, b), userService.list());
    }
}
