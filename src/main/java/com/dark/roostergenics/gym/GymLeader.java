package com.dark.roostergenics.gym;

import com.dark.roostergenics.genetics.Allele;
import com.dark.roostergenics.genetics.GenePair;
import com.dark.roostergenics.genetics.RoosterGenetics;
import com.dark.roostergenics.genetics.RoosterTrait;
import com.dark.roostergenics.items.RoosterHeldItem;
import com.dark.roostergenics.model.RoosterData;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Los 6 Líderes de Gimnasio del Ecosistema RoosterGenics del Circuito Oficial.
 */
public enum GymLeader {

    VIENTO(
            1,
            "Zephyr",
            "Gimnasio del Viento",
            "Medalla Pluma",
            10,
            Material.FEATHER,
            "#38BDF8",
            "Velocidad Aérea y Evasión",
            RoosterHeldItem.PLUMA_VELOZ,
            Set.of(RoosterTrait.ENERGIA_SIN_FIN, RoosterTrait.PASO_FANTASMAL),
            120.0, 10.0, 3.0, 14, 0.22, 0.15, "PLAINS"
    ),

    CANTERA(
            2,
            "Damián",
            "Gimnasio de la Cantera",
            "Medalla Espolón de Hierro",
            18,
            Material.IRON_INGOT,
            "#94A3B8",
            "Defensa Acorazada y Firmeza",
            RoosterHeldItem.CHALECO_ASALTO,
            Set.of(RoosterTrait.PIEL_DE_HIERRO, RoosterTrait.CORAZON_COLOSO),
            150.0, 13.0, 6.5, 18, 0.10, 0.15, "MESETA"
    ),

    PANTANO(
            3,
            "Vesper",
            "Gimnasio del Pantano",
            "Medalla Neurotoxina",
            26,
            Material.SPIDER_EYE,
            "#10B981",
            "Veneno Letal y Desgaste",
            RoosterHeldItem.BAYA_MELOC,
            Set.of(RoosterTrait.ESPOLON_VENENOSO, RoosterTrait.VAMPIRO_ALADO),
            175.0, 15.5, 5.0, 16, 0.15, 0.18, "PANTANO"
    ),

    VOLCAN(
            4,
            "Ignis",
            "Gimnasio del Volcán",
            "Medalla Fénix",
            34,
            Material.BLAZE_POWDER,
            "#EF4444",
            "Fuego Primal y Resurrección",
            RoosterHeldItem.POLVORA_FENIX,
            Set.of(RoosterTrait.PICOTAZO_IGNIS, RoosterTrait.SANGRE_DE_FENIX, RoosterTrait.FURIA_PRIMAL),
            200.0, 19.0, 6.0, 14, 0.18, 0.22, "DESIERTO"
    ),

    SOMBRAS(
            5,
            "Morfeo",
            "Gimnasio de las Sombras",
            "Medalla Espectral",
            42,
            Material.AMETHYST_SHARD,
            "#A855F7",
            "Golpes Críticos y Contragolpes",
            RoosterHeldItem.PERISCOPIO,
            Set.of(RoosterTrait.PASO_FANTASMAL_EVO, RoosterTrait.REFLEJOS_DE_RAYO, RoosterTrait.SUERTE_DIVINA),
            220.0, 22.0, 7.5, 12, 0.30, 0.35, "PALE_GARDEN"
    ),

    TITAN(
            6,
            "Goliat",
            "Gimnasio del Titán",
            "Medalla Obsidiana",
            50,
            Material.OBSIDIAN,
            "#F59E0B",
            "Fuerza Bruta y Rotura de Blindaje",
            RoosterHeldItem.CINTA_ELEGIDA,
            Set.of(RoosterTrait.GARRAS_AFILADAS_EVO, RoosterTrait.PIEL_DE_HIERRO_EVO, RoosterTrait.FURIA_PRIMAL_EVO),
            240.0, 26.0, 10.0, 10, 0.25, 0.30, "TAIGA"
    );

    private final int order;
    private final String leaderName;
    private final String gymName;
    private final String badgeName;
    private final int recommendedLevel;
    private final Material badgeMaterial;
    private final String themeHex;
    private final String specialty;
    private final RoosterHeldItem signatureItem;
    private final Set<RoosterTrait> signatureTraits;
    private final double hp;
    private final double atk;
    private final double def;
    private final int speedTicks;
    private final double evasion;
    private final double crit;
    private final String plumage;

    GymLeader(int order, String leaderName, String gymName, String badgeName, int recommendedLevel,
              Material badgeMaterial, String themeHex, String specialty, RoosterHeldItem signatureItem,
              Set<RoosterTrait> signatureTraits, double hp, double atk, double def, int speedTicks,
              double evasion, double crit, String plumage) {
        this.order = order;
        this.leaderName = leaderName;
        this.gymName = gymName;
        this.badgeName = badgeName;
        this.recommendedLevel = recommendedLevel;
        this.badgeMaterial = badgeMaterial;
        this.themeHex = themeHex;
        this.specialty = specialty;
        this.signatureItem = signatureItem;
        this.signatureTraits = signatureTraits;
        this.hp = hp;
        this.atk = atk;
        this.def = def;
        this.speedTicks = speedTicks;
        this.evasion = evasion;
        this.crit = crit;
        this.plumage = plumage;
    }

    public int getOrder() { return order; }
    public String getLeaderName() { return leaderName; }
    public String getGymName() { return gymName; }
    public String getBadgeName() { return badgeName; }
    public int getRecommendedLevel() { return recommendedLevel; }
    public Material getBadgeMaterial() { return badgeMaterial; }
    public String getThemeHex() { return themeHex; }
    public String getSpecialty() { return specialty; }
    public RoosterHeldItem getSignatureItem() { return signatureItem; }

    public String getSkinName() {
        return switch (this) {
            case VIENTO -> "Grian";
            case CANTERA -> "FitMC";
            case PANTANO -> "Etho";
            case VOLCAN -> "Technoblade";
            case SOMBRAS -> "iDarkt";
            case TITAN -> "ElRichMC";
        };
    }

    public RoosterData createGymRooster() {
        GenePair<Double> hpLocus = new GenePair<>(new Allele<>(hp, Allele.Dominance.CODOMINANT), new Allele<>(hp, Allele.Dominance.CODOMINANT));
        GenePair<Double> atkLocus = new GenePair<>(new Allele<>(atk, Allele.Dominance.CODOMINANT), new Allele<>(atk, Allele.Dominance.CODOMINANT));
        GenePair<Double> defLocus = new GenePair<>(new Allele<>(def, Allele.Dominance.CODOMINANT), new Allele<>(def, Allele.Dominance.CODOMINANT));
        GenePair<Integer> spdLocus = new GenePair<>(new Allele<>(speedTicks, Allele.Dominance.CODOMINANT), new Allele<>(speedTicks, Allele.Dominance.CODOMINANT));
        GenePair<Double> evLocus = new GenePair<>(new Allele<>(evasion, Allele.Dominance.CODOMINANT), new Allele<>(evasion, Allele.Dominance.CODOMINANT));
        GenePair<Double> accLocus = new GenePair<>(new Allele<>(0.35, Allele.Dominance.CODOMINANT), new Allele<>(0.35, Allele.Dominance.CODOMINANT));
        GenePair<Integer> agLocus = new GenePair<>(new Allele<>(8, Allele.Dominance.CODOMINANT), new Allele<>(8, Allele.Dominance.CODOMINANT));
        GenePair<Double> scLocus = new GenePair<>(new Allele<>(1.15, Allele.Dominance.CODOMINANT), new Allele<>(1.15, Allele.Dominance.CODOMINANT));
        GenePair<String> plLocus = new GenePair<>(new Allele<>(plumage, Allele.Dominance.CODOMINANT), new Allele<>(plumage, Allele.Dominance.CODOMINANT));
        GenePair<String> cbLocus = new GenePair<>(new Allele<>("DOBLE_CORONA", Allele.Dominance.CODOMINANT), new Allele<>("DOBLE_CORONA", Allele.Dominance.CODOMINANT));

        RoosterGenetics genetics = new RoosterGenetics(hpLocus, atkLocus, defLocus, spdLocus, evLocus, accLocus, agLocus, scLocus, plLocus, cbLocus, EnumSet.copyOf(signatureTraits));
        RoosterData data = new RoosterData(UUID.randomUUID(), UUID.randomUUID(), "Gallo de " + leaderName, genetics);
        data.setLevel(recommendedLevel);
        data.setHeldItem(signatureItem);
        return data;
    }

    public static GymLeader getByOrder(int order) {
        for (GymLeader g : values()) {
            if (g.order == order) return g;
        }
        return null;
    }
}
