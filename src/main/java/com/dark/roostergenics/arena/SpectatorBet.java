package com.dark.roostergenics.arena;

import java.util.UUID;

/**
 * Registro de apuesta de espectador en un combate de palenque 1v1.
 */
public class SpectatorBet {

    private final UUID bettorId;
    private final UUID chosenPlayerId;
    private final double amount;
    private final long timestamp;

    public SpectatorBet(UUID bettorId, UUID chosenPlayerId, double amount) {
        this.bettorId = bettorId;
        this.chosenPlayerId = chosenPlayerId;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getBettorId() {
        return bettorId;
    }

    public UUID getChosenPlayerId() {
        return chosenPlayerId;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
