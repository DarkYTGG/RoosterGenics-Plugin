package com.dark.roostergenics.arena;

import java.util.UUID;

/**
 * Representa una solicitud de duelo 1v1 pendiente entre dos jugadores.
 */
public class DuelChallenge {

    private final UUID challengerId;
    private final UUID challengedId;
    private final double wagerAmount;
    private final long createdAt;
    public static final long TIMEOUT_MILLIS = 60 * 1000L; // 60 segundos de validez

    public DuelChallenge(UUID challengerId, UUID challengedId, double wagerAmount) {
        this.challengerId = challengerId;
        this.challengedId = challengedId;
        this.wagerAmount = Math.max(0.0, wagerAmount);
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getChallengerId() {
        return challengerId;
    }

    public UUID getChallengedId() {
        return challengedId;
    }

    public double getWagerAmount() {
        return wagerAmount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > TIMEOUT_MILLIS;
    }
}
