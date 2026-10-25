package com.dark.roostergenics.genetics;

/**
 * Representa un par diploide de alelos (Locus) con un alelo paterno y uno materno.
 * Modela la expresión fenotípica mendeliana (dominancia completa o codominancia).
 */
public class GenePair<T> {

    private final Allele<T> paternal;
    private final Allele<T> maternal;

    public GenePair(Allele<T> paternal, Allele<T> maternal) {
        this.paternal = paternal;
        this.maternal = maternal;
    }

    public Allele<T> getPaternal() {
        return paternal;
    }

    public Allele<T> getMaternal() {
        return maternal;
    }

    /**
     * Calcula el valor fenotípico expresado según las reglas de dominancia mendeliana.
     */
    @SuppressWarnings("unchecked")
    public T getExpressedValue() {
        if (paternal.isDominant() && maternal.isRecessive()) {
            return paternal.getValue();
        }
        if (maternal.isDominant() && paternal.isRecessive()) {
            return maternal.getValue();
        }

        // Si es numérico (Double), se promedian los dos alelos
        if (paternal.getValue() instanceof Double pVal && maternal.getValue() instanceof Double mVal) {
            double avg = (pVal + mVal) / 2.0;
            return (T) Double.valueOf(avg);
        }

        if (paternal.getValue() instanceof Integer pVal && maternal.getValue() instanceof Integer mVal) {
            int avg = (int) Math.round((pVal + mVal) / 2.0);
            return (T) Integer.valueOf(avg);
        }

        return paternal.getValue();
    }
}
