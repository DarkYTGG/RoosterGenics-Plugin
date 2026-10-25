package com.dark.roostergenics.arena.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gestiona el arrendamiento atómico de ejecución física para proteger contra
 * comandos desfasados (stale commands) entre épocas de sesión y evitar duplicaciones.
 */
public class PhysicalExecutionLease {

    private final AtomicLong activeEpoch = new AtomicLong(1L);
    private final AtomicLong nextCommandId = new AtomicLong(0L);
    private final Map<UUID, AtomicLong> lastAppliedCommandPerEntity = new ConcurrentHashMap<>();

    public long getActiveEpoch() {
        return activeEpoch.get();
    }

    public long nextEpoch() {
        return activeEpoch.incrementAndGet();
    }

    public long generateNextCommandId() {
        return nextCommandId.incrementAndGet();
    }

    /**
     * Valida si un comando es aplicable físicamente:
     * 1. La época del comando debe coincidir exactamente con la época activa.
     * 2. El commandId debe ser estrictamente mayor que el último commandId aplicado a esta entidad.
     */
    public boolean validateAndApply(UUID entityId, long commandEpoch, long commandId) {
        if (commandEpoch != activeEpoch.get()) {
            return false; // Época obsoleta (el combate cambió de fase o terminó)
        }
        AtomicLong lastApplied = lastAppliedCommandPerEntity.computeIfAbsent(entityId, k -> new AtomicLong(-1L));
        while (true) {
            long currentLast = lastApplied.get();
            if (commandId <= currentLast) {
                return false; // Comando duplicado o desordenado
            }
            if (lastApplied.compareAndSet(currentLast, commandId)) {
                return true; // Exclusivamente aplicado por este hilo
            }
        }
    }

    public long getLastAppliedCommandId(UUID entityId) {
        AtomicLong last = lastAppliedCommandPerEntity.get(entityId);
        return last != null ? last.get() : -1L;
    }

    public void resetForEntity(UUID entityId) {
        lastAppliedCommandPerEntity.remove(entityId);
    }
}
