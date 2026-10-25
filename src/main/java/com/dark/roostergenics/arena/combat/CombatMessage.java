package com.dark.roostergenics.arena.combat;

import com.dark.roostergenics.arena.SpectatorBet;
import java.util.UUID;

/**
 * Jerarquía sellada de mensajes inmutables entregados al RegionScheduler
 * dueño lógico de la simulación de combate.
 */
public sealed interface CombatMessage permits
        CombatMessage.PlayerQuit,
        CombatMessage.EntityRetired,
        CombatMessage.FighterDied,
        CombatMessage.Forfeit,
        CombatMessage.SpectatorBetMsg,
        CombatMessage.AdminAbort,
        CombatMessage.Timeout,
        CombatMessage.ExternalDamage {

    record PlayerQuit(UUID playerId, String reason) implements CombatMessage {}

    record EntityRetired(UUID entityId, long sessionEpoch) implements CombatMessage {}

    record FighterDied(UUID entityId, long sessionEpoch) implements CombatMessage {}

    record Forfeit(UUID playerId, String reason) implements CombatMessage {}

    record SpectatorBetMsg(SpectatorBet bet) implements CombatMessage {}

    record AdminAbort(String reason) implements CombatMessage {}

    record Timeout(String reason) implements CombatMessage {}

    record ExternalDamage(UUID victimId, double damage) implements CombatMessage {}
}
