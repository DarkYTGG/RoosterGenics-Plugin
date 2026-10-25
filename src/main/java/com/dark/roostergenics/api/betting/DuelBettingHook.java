package com.dark.roostergenics.api.betting;

import com.dark.roostergenics.arena.DuelArenaSession;
import com.dark.roostergenics.model.RoosterData;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interfaz SPI para que plugins de economía o casino (como DarkCasino)
 * puedan inyectar lógica de apuestas monetarias, escrow y apuestas de espectadores
 * en los duelos 1v1 de Roostergenics.
 */
public interface DuelBettingHook {

    /**
     * Valida si el retador y el retado pueden apostar ese monto.
     */
    boolean canChallengeWithBet(Player challenger, Player target, double betAmount);

    /**
     * Deduce y bloquea los fondos en Escrow cuando el duelo es aceptado.
     */
    boolean lockDuelEscrow(Player challenger, Player target, double betAmount);

    /**
     * Se dispara al comenzar la fase de apuestas previas en la arena.
     */
    void onDuelStart(DuelArenaSession session);

    /**
     * Procesa los pagos de apuestas (duelistas y espectadores) al haber un ganador.
     */
    void onDuelVictory(DuelArenaSession session, Player winner, RoosterData winnerRooster,
                       Player loser, RoosterData loserRooster);

    /**
     * Reembolsa las apuestas en caso de empate técnico.
     */
    void onDuelDraw(DuelArenaSession session);

    /**
     * Reembolsa las apuestas si el combate fue abortado o cancelado.
     */
    void onDuelAbort(DuelArenaSession session, String reason);

    /**
     * Registra la apuesta de un espectador sobre un peleador en cuenta regresiva.
     */
    boolean placeSpectatorBet(Player spectator, String targetFighterName, double amount);

    /**
     * Devuelve los nombres de los peleadores que se encuentran actualmente en fase de apuestas.
     */
    List<String> getActiveFighterNamesInBetting();

    /**
     * Deposita una recompensa monetaria al jugador (por ejemplo, premio por vencer a un entrenador o líder).
     */
    default boolean depositReward(Player player, double amount, String reason) {
        return false;
    }
}
