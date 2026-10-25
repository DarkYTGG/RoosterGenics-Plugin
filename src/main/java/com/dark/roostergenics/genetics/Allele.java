package com.dark.roostergenics.genetics;

import java.util.Objects;

/**
 * Representa un alelo genético individual con su valor y tipo de dominancia.
 */
public class Allele<T> {

    public enum Dominance {
        DOMINANT,
        RECESSIVE,
        CODOMINANT
    }

    private final T value;
    private final Dominance dominance;

    public Allele(T value, Dominance dominance) {
        this.value = Objects.requireNonNull(value, "El valor del alelo no puede ser nulo");
        this.dominance = Objects.requireNonNull(dominance, "La dominancia no puede ser nula");
    }

    public T getValue() {
        return value;
    }

    public Dominance getDominance() {
        return dominance;
    }

    public boolean isDominant() {
        return dominance == Dominance.DOMINANT;
    }

    public boolean isRecessive() {
        return dominance == Dominance.RECESSIVE;
    }

    public boolean isCodominant() {
        return dominance == Dominance.CODOMINANT;
    }

    @Override
    public String toString() {
        return value + " (" + dominance.name().charAt(0) + ")";
    }
}
