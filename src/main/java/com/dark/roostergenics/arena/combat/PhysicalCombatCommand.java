package com.dark.roostergenics.arena.combat;

import org.bukkit.util.Vector;
import java.util.UUID;

/**
 * Comando inmutable generado por el RegionScheduler lógico para ser ejecutado
 * físicamente por el EntityScheduler de un gallo o entidad participante.
 */
public record PhysicalCombatCommand(
        long sessionEpoch,
        long commandId,
        UUID targetEntityId,
        PhysicalCommandType type,
        Vector vectorParam,
        String textParam,
        double doubleParam,
        int intParam,
        Object extraPayload
) {
    public static PhysicalCombatCommand of(long sessionEpoch, long commandId, UUID entityId, PhysicalCommandType type) {
        return new PhysicalCombatCommand(sessionEpoch, commandId, entityId, type, null, null, 0.0, 0, null);
    }
}
