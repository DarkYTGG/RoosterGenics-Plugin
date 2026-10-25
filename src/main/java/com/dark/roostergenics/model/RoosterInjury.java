package com.dark.roostergenics.model;

/**
 * Representa una lesión de combate temporal que un gallo puede sufrir
 * al ser noqueado o castigado severamente en el palenque.
 * A diferencia de los defectos genéticos permanentes, las lesiones temporales
 * se pueden sanar mediante atención médica en el Centro de Gallos o cuidados específicos.
 */
public enum RoosterInjury {

    ALA_LASTIMADA(
            "Ala Lastimada",
            "Reduce la velocidad de movimiento y la evasión en un 15%.",
            "Tratamiento con vendajes en el Centro de Gallos o reposo en el corral."
    ),

    OJO_HINCHADO(
            "Ojo Hinchado",
            "Reduce la precisión de picotazo en un 20%.",
            "Lavado ocular en el Centro de Gallos o alimentación con semillas de calabaza."
    ),

    ESPOLON_AGRIETADO(
            "Espolón Agrietado",
            "Reduce el ataque en un 20% y anula los golpes críticos.",
            "Reparación y vendaje en el Centro de Gallos o alimentar con Baya Meloc."
    ),

    SHOCK_DE_COMBATE(
            "Shock de Combate",
            "15% de probabilidad de vacilar o retroceder por pánico ante el rival.",
            "Caricias de afecto (/rg acariciar) o reposo tranquilo junto a su entrenador."
    ),

    FATIGA_MUSCULAR(
            "Fatiga Muscular",
            "Aumenta el enfriamiento entre picotazos en +3 ticks.",
            "Nutrición con semillas de trigo o descanso en el Centro de Gallos."
    ),

    HERIDA_ABIERTA(
            "Herida Abierta",
            "Sufre sangrado continuo durante los primeros 10 segundos del asalto.",
            "Atención médica de urgencia en el Centro de Gallos o dar Baya Zidra."
    );

    private final String displayName;
    private final String description;
    private final String cureHint;

    RoosterInjury(String displayName, String description, String cureHint) {
        this.displayName = displayName;
        this.description = description;
        this.cureHint = cureHint;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getCureHint() {
        return cureHint;
    }
}
