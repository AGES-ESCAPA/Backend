package com.escapa.backend.application.usecase;

import com.escapa.backend.application.dto.ChangeLogEntry;
import com.escapa.backend.application.dto.PageResult;
import com.escapa.backend.application.port.CourseChangeLogRepositoryPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fake em memória de {@link CourseChangeLogRepositoryPort} para testes unitários.
 */
final class InMemoryCourseChangeLogRepositoryPort implements CourseChangeLogRepositoryPort {

    private record Entry(UUID courseId, ChangeLogEntry entry) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Map<UUID, String> adminNamesById = new HashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    void registerAdmin(UUID id, String name) {
        adminNamesById.put(id, name);
    }

    List<ChangeLogEntry> entriesFor(UUID courseId) {
        return entries.stream()
                .filter(e -> e.courseId().equals(courseId))
                .map(Entry::entry)
                .toList();
    }

    @Override
    public List<ChangeLogEntry> findRecentByCourseId(UUID courseId, int limit) {
        return entriesFor(courseId).stream()
                .sorted(Comparator.comparing(ChangeLogEntry::createdAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public PageResult<ChangeLogEntry> findPageByCourseId(UUID courseId, int page, int size) {
        final List<ChangeLogEntry> all = entriesFor(courseId).stream()
                .sorted(Comparator.comparing(ChangeLogEntry::createdAt).reversed())
                .toList();
        final int start = Math.min(page * size, all.size());
        final int end = Math.min(start + size, all.size());
        final int totalPages = all.isEmpty() ? 0 : (all.size() + size - 1) / size;
        return new PageResult<>(all.subList(start, end), page, size, all.size(), totalPages);
    }

    @Override
    public void save(UUID courseId, UUID changedById, String description, int majorVersion, int minorVersion) {
        final String changedByName = changedById == null ? "Sistema" : adminNamesById.getOrDefault(
                changedById, "Sistema");
        entries.add(new Entry(courseId, new ChangeLogEntry(
                UUID.randomUUID(), description, changedByName, majorVersion, minorVersion,
                LocalDateTime.now().plusNanos(sequence.incrementAndGet()))));
    }
}
