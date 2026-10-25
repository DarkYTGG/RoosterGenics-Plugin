package com.dark.roostergenics.genetics;

import java.util.Arrays;

/**
 * Sistema de mutaciones con fases Base y Evolucionada (RoosterGenics Genetics Engine).
 *
 * Reglas fundamentales:
 *  - Cada mutación tiene una forma BASE y opcionalmente una forma EVOLUCIONADA.
 *  - Un gallo puede tener un máximo de 3 mutaciones evolucionadas de forma natural.
 *  - Si un progenitor tiene una mutación EVOLUCIONADA, la cría la hereda al 100%
 *    pero SIEMPRE en su forma BASE (reversión obligatoria).
 */
public enum RoosterTrait {

    // =========================================================================
    // Rasgos Positivos BASE (Mutaciones Favorables — Fase 1)
    // =========================================================================
    GARRAS_AFILADAS("Garras Afiladas",
            "Ignora el 25% de la armadura/defensa del oponente.",
            true, false, null),

    PIEL_DE_HIERRO("Piel de Hierro",
            "Reduce el daño recibido por impacto directo en un 15%.",
            true, false, null),

    FURIA_PRIMAL("Furia Primal",
            "Aumenta el ataque en un 30% cuando su vida está por debajo del 35%.",
            true, false, null),

    SANGRE_DE_FENIX("Sangre de Fénix",
            "Una vez por combate, si cae derrotado, revive con el 30% de salud.",
            true, false, null),

    PASO_FANTASMAL("Paso Fantasmal",
            "Incrementa la probabilidad de esquiva en un +12%.",
            true, false, null),

    ESPOLON_VENENOSO("Espolón Venenoso",
            "25% de probabilidad de aplicar veneno al asestar un golpe.",
            true, false, null),

    CANTO_DE_GUERRA("Canto de Guerra",
            "Comienza el combate con +20% de velocidad de ataque durante los primeros 8s.",
            true, false, null),

    PICOTAZO_IGNIS("Picotazo Ígneo",
            "20% de probabilidad de prender fuego al oponente durante 3s al golpear.",
            true, false, null),

    VAMPIRO_ALADO("Sed de Sangre",
            "Restaura un 15% del daño infligido como salud en cada impacto directo.",
            true, false, null),

    CORAZON_COLOSO("Constitución Titánica",
            "Aumenta la salud máxima base en un +25% de forma pasiva.",
            true, false, null),

    REFLEJOS_DE_RAYO("Reflejos de Rayo",
            "Al esquivar un ataque con éxito, contraataca instantáneamente al rival.",
            true, false, null),

    INTIMIDACION("Mirada Intimidante",
            "Reduce el ataque del oponente en un 15% durante todo el combate.",
            true, false, null),

    GOLPE_DEMOLEDOR("Espolón de Titanio",
            "Los golpes críticos tienen un 30% de probabilidad de aturdir al rival por 1.2s.",
            true, false, null),

    ENERGIA_SIN_FIN("Cardio Imparable",
            "Reduce el enfriamiento entre picotazos en 2 ticks permanentes.",
            true, false, null),

    ARMADURA_ESPINA("Plumaje Espinado",
            "Devuelve pasivamente un 12% del daño directo recibido al rival.",
            true, false, null),

    SUERTE_DIVINA("Bendición del Palenque",
            "Incrementa la probabilidad de asestar golpes críticos en un +15%.",
            true, false, null),

    // =========================================================================
    // Rasgos Positivos EVOLUCIONADOS (Mutaciones Favorables — Fase 2)
    // =========================================================================
    GARRAS_AFILADAS_EVO("Garras de Obsidiana",
            "Ignora el 40% de la DEF del oponente + 10% sangrado por turno.",
            true, true, null), // baseForm se fija en el bloque static

    PIEL_DE_HIERRO_EVO("Coraza de Diamante",
            "Reduce el daño recibido en un 25% + inmune a veneno.",
            true, true, null),

    FURIA_PRIMAL_EVO("Berserker Carmesí",
            "Aumenta el ataque en un 50% (<40% HP) + lifesteal del 8%.",
            true, true, null),

    SANGRE_DE_FENIX_EVO("Inmortal del Alba",
            "Revive con 50% HP (1x) + 3s de invulnerabilidad temporal.",
            true, true, null),

    PASO_FANTASMAL_EVO("Sombra Etérea",
            "Evasión +20% + contraataque automático al esquivar (30% chance).",
            true, true, null),

    ESPOLON_VENENOSO_EVO("Ponzoña Letal",
            "40% envenenar + 15% parálisis que anula el siguiente ataque.",
            true, true, null),

    CANTO_DE_GUERRA_EVO("Himno del Coliseo",
            "+30% SPD durante 12s iniciales + intimidación -10% ATK rival.",
            true, true, null),

    PICOTAZO_IGNIS_EVO("Infierno Emplumado",
            "35% fuego 4s + explosión visual de partículas AoE.",
            true, true, null),

    VAMPIRO_ALADO_EVO("Devorador de Almas",
            "25% lifesteal + crece escala +2% por cada K.O. acumulado.",
            true, true, null),

    CORAZON_COLOSO_EVO("Coloso Primordial",
            "+40% HP máx + regeneración pasiva de 1% cada 4s.",
            true, true, null),

    REFLEJOS_DE_RAYO_EVO("Instinto Relámpago",
            "Contraataque garantizado al esquivar + stun de 0.6s al rival.",
            true, true, null),

    INTIMIDACION_EVO("Terror Ancestral",
            "Reduce ATK rival en 25% + 10% chance de hacerle perder su turno.",
            true, true, null),

    GOLPE_DEMOLEDOR_EVO("Martinete Demoledor",
            "50% stun 1.8s en crit + ignora 30% DEF adicional.",
            true, true, null),

    ENERGIA_SIN_FIN_EVO("Motor Perpetuo",
            "-4 ticks cooldown permanente + inmune a stun.",
            true, true, null),

    ARMADURA_ESPINA_EVO("Armadura de Espinas",
            "Refleja 20% del daño recibido + aplica slow de 1s.",
            true, true, null),

    SUERTE_DIVINA_EVO("Destino Manifiesto",
            "+25% crit chance + multiplicador x2.0 en vez de x1.5.",
            true, true, null),

    // =========================================================================
    // Rasgos Negativos BASE (Defectos Genéticos — Fase 1)
    // =========================================================================
    COJERA("Cojera Congénita",
            "Reduce la velocidad de movimiento y evasión en un 25%.",
            false, false, null),

    ASUSTADIZO("Asustadizo",
            "15% de probabilidad de retroceder por pánico en lugar de atacar.",
            false, false, null),

    HUESOS_FRAGILES("Huesos Frágiles",
            "Recibe un 20% más de daño ante golpes críticos.",
            false, false, null),

    PICO_ROTO("Pico Roto",
            "Reduce el ataque base en 3 puntos.",
            false, false, null),

    ESTERILIDAD("Esterilidad",
            "No puede reproducirse ni transmitir su linaje en el criadero.",
            false, false, null),

    MIOPIA_CONGENITA("Miopía Congénita",
            "Reduce la precisión de ataque; 15% de probabilidad de fallar el golpe.",
            false, false, null),

    HEMOFILIA("Hemofilia",
            "Sufre sangrado continuo al recibir golpes críticos del oponente.",
            false, false, null),

    ASMA_DE_CORRAL("Fatiga Respiratoria",
            "Aumenta el enfriamiento de ataque en +3 ticks.",
            false, false, null),

    PIEL_DE_PAPEL("Piel de Papel",
            "Aumenta todo el daño directo recibido en un 15% adicional.",
            false, false, null),

    ESPURIO_NERVIOSO("Temblores Nerviosos",
            "10% de probabilidad en cada turno de tropezar y perder el ataque.",
            false, false, null),

    // =========================================================================
    // Rasgos Negativos EVOLUCIONADOS (Defectos Degenerativos — Fase 2)
    // =========================================================================
    COJERA_EVO("Parálisis Progresiva",
            "-40% evasión/movimiento + 5% chance de inmovilización total por turno.",
            false, true, null),

    ASUSTADIZO_EVO("Terror Paralizante",
            "25% retroceder por pánico + pierde 2 turnos consecutivos.",
            false, true, null),

    HUESOS_FRAGILES_EVO("Esqueleto Cristalino",
            "+35% daño por crit recibido + chance de fractura (-5 ATK temporal).",
            false, true, null),

    PICO_ROTO_EVO("Mandíbula Dislocada",
            "-5 ATK base + 10% chance de fallo completo del golpe.",
            false, true, null),

    ESTERILIDAD_EVO("Colapso Genético",
            "No puede reproducirse + -10% a TODAS las estadísticas de combate.",
            false, true, null),

    MIOPIA_CONGENITA_EVO("Ceguera de Corral",
            "25% fallo de golpe + reduce probabilidad de crit en 50%.",
            false, true, null),

    HEMOFILIA_EVO("Hemorragia Masiva",
            "Sangrado en cualquier golpe recibido + x2 duración del sangrado.",
            false, true, null),

    ASMA_DE_CORRAL_EVO("Colapso Pulmonar",
            "+5 ticks cooldown + 5% chance de desmayo (pierde turno).",
            false, true, null),

    PIEL_DE_PAPEL_EVO("Fragilidad Terminal",
            "+25% daño recibido + no puede equipar objetos de combate.",
            false, true, null),

    ESPURIO_NERVIOSO_EVO("Convulsiones Crónicas",
            "20% tropiezo + aplica veneno a sí mismo por 2s.",
            false, true, null);

    /** Máximo de mutaciones evolucionadas que un gallo puede tener de forma natural. */
    public static final int MAX_EVOLVED_NATURAL = 3;

    private final String displayName;
    private final String description;
    private final boolean positive;
    private final boolean evolved;
    private RoosterTrait baseForm;    // null si ES la forma base; apunta a la base si es evo
    private RoosterTrait evolvedForm; // null si ES la forma evo o no tiene evo; apunta a la evo si es base

    RoosterTrait(String displayName, String description, boolean positive, boolean evolved, RoosterTrait baseForm) {
        this.displayName = displayName;
        this.description = description;
        this.positive = positive;
        this.evolved = evolved;
        this.baseForm = baseForm;
    }

    // Enlazar relaciones Base ↔ Evolucionada en bloque estático
    static {
        linkPair(GARRAS_AFILADAS, GARRAS_AFILADAS_EVO);
        linkPair(PIEL_DE_HIERRO, PIEL_DE_HIERRO_EVO);
        linkPair(FURIA_PRIMAL, FURIA_PRIMAL_EVO);
        linkPair(SANGRE_DE_FENIX, SANGRE_DE_FENIX_EVO);
        linkPair(PASO_FANTASMAL, PASO_FANTASMAL_EVO);
        linkPair(ESPOLON_VENENOSO, ESPOLON_VENENOSO_EVO);
        linkPair(CANTO_DE_GUERRA, CANTO_DE_GUERRA_EVO);
        linkPair(PICOTAZO_IGNIS, PICOTAZO_IGNIS_EVO);
        linkPair(VAMPIRO_ALADO, VAMPIRO_ALADO_EVO);
        linkPair(CORAZON_COLOSO, CORAZON_COLOSO_EVO);
        linkPair(REFLEJOS_DE_RAYO, REFLEJOS_DE_RAYO_EVO);
        linkPair(INTIMIDACION, INTIMIDACION_EVO);
        linkPair(GOLPE_DEMOLEDOR, GOLPE_DEMOLEDOR_EVO);
        linkPair(ENERGIA_SIN_FIN, ENERGIA_SIN_FIN_EVO);
        linkPair(ARMADURA_ESPINA, ARMADURA_ESPINA_EVO);
        linkPair(SUERTE_DIVINA, SUERTE_DIVINA_EVO);

        linkPair(COJERA, COJERA_EVO);
        linkPair(ASUSTADIZO, ASUSTADIZO_EVO);
        linkPair(HUESOS_FRAGILES, HUESOS_FRAGILES_EVO);
        linkPair(PICO_ROTO, PICO_ROTO_EVO);
        linkPair(ESTERILIDAD, ESTERILIDAD_EVO);
        linkPair(MIOPIA_CONGENITA, MIOPIA_CONGENITA_EVO);
        linkPair(HEMOFILIA, HEMOFILIA_EVO);
        linkPair(ASMA_DE_CORRAL, ASMA_DE_CORRAL_EVO);
        linkPair(PIEL_DE_PAPEL, PIEL_DE_PAPEL_EVO);
        linkPair(ESPURIO_NERVIOSO, ESPURIO_NERVIOSO_EVO);
    }

    private static void linkPair(RoosterTrait base, RoosterTrait evo) {
        base.evolvedForm = evo;
        evo.baseForm = base;
    }

    // ── Getters ──

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public boolean isPositive() { return positive; }

    /** @return true si esta es la forma EVOLUCIONADA de una mutación. */
    public boolean isEvolved() { return evolved; }

    /** @return true si esta es la forma BASE de una mutación (puede tener evolución). */
    public boolean isBaseForm() { return !evolved; }

    /**
     * Si esta es una forma evolucionada, devuelve la forma base correspondiente.
     * Si ya es base, se devuelve a sí misma.
     */
    public RoosterTrait getBaseForm() {
        return baseForm != null ? baseForm : this;
    }

    /**
     * Si esta es una forma base, devuelve la forma evolucionada correspondiente.
     * Si ya es evolucionada o no tiene evolución, devuelve null.
     */
    public RoosterTrait getEvolvedForm() {
        return evolvedForm;
    }

    /** @return true si esta forma base tiene una evolución disponible. */
    public boolean hasEvolution() {
        return evolvedForm != null;
    }

    // ── Queries estáticas ──

    public static RoosterTrait[] getPositiveTraits() {
        return Arrays.stream(values()).filter(t -> t.isPositive() && !t.isEvolved()).toArray(RoosterTrait[]::new);
    }

    public static RoosterTrait[] getNegativeTraits() {
        return Arrays.stream(values()).filter(t -> !t.isPositive() && !t.isEvolved()).toArray(RoosterTrait[]::new);
    }

    public static RoosterTrait[] getPositiveEvolvedTraits() {
        return Arrays.stream(values()).filter(t -> t.isPositive() && t.isEvolved()).toArray(RoosterTrait[]::new);
    }

    public static RoosterTrait[] getNegativeEvolvedTraits() {
        return Arrays.stream(values()).filter(t -> !t.isPositive() && t.isEvolved()).toArray(RoosterTrait[]::new);
    }

    public static RoosterTrait[] getAllBaseTraits() {
        return Arrays.stream(values()).filter(t -> !t.isEvolved()).toArray(RoosterTrait[]::new);
    }

    public static RoosterTrait[] getAllEvolvedTraits() {
        return Arrays.stream(values()).filter(RoosterTrait::isEvolved).toArray(RoosterTrait[]::new);
    }

    public static RoosterTrait fromId(String id) {
        if (id == null) return null;
        for (RoosterTrait t : values()) {
            if (t.name().equalsIgnoreCase(id) || t.displayName.equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }
}
