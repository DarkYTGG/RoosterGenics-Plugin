package com.dark.roostergenics.arena.combat;

/**
 * Estados del ciclo de vida de un combate 1v1 en el palenque.
 */
public enum CombatState {
    CREATED,
    BETTING_WINDOW,
    FIGHTING,
    ENDING,
    FINISHED,
    CANCELLED;

    public boolean isTerminal() {
        return this == ENDING || this == FINISHED || this == CANCELLED;
    }

    public boolean isFighting() {
        return this == FIGHTING;
    }

    public boolean canAcceptBets() {
        return this == BETTING_WINDOW;
    }
}
