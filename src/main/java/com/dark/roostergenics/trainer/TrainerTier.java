package com.dark.roostergenics.trainer;

import com.dark.roostergenics.genetics.RoosterTrait;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Set;

/**
 * Niveles de dificultad y jerarquía para los Entrenadores Galleros NPC.
 */
public enum TrainerTier {

    APRENDIZ(
            "Aprendiz del Palenque",
            NamedTextColor.GREEN,
            1,
            105.0,
            8.0,
            2.0,
            18,
            25.0,
            1,
            Set.of()
    ),
    VETERANO(
            "Veterano de Peleas",
            NamedTextColor.AQUA,
            2,
            145.0,
            13.0,
            5.0,
            15,
            60.0,
            2,
            Set.of(RoosterTrait.GARRAS_AFILADAS)
    ),
    MAESTRO(
            "Maestro Gallero",
            NamedTextColor.GOLD,
            3,
            185.0,
            18.0,
            8.0,
            12,
            120.0,
            3,
            Set.of(RoosterTrait.FURIA_PRIMAL, RoosterTrait.PIEL_DE_HIERRO)
    ),
    LEYENDA(
            "Leyenda del Palenque",
            NamedTextColor.LIGHT_PURPLE,
            4,
            235.0,
            24.0,
            11.0,
            9,
            300.0,
            5,
            Set.of(RoosterTrait.SANGRE_DE_FENIX, RoosterTrait.ESPOLON_VENENOSO, RoosterTrait.CANTO_DE_GUERRA)
    );

    private final String title;
    private final NamedTextColor color;
    private final int level;
    private final double baseHp;
    private final double baseAttack;
    private final double baseDefense;
    private final int attackSpeedTicks;
    private final double rewardCoins;
    private final int rewardCages;
    private final Set<RoosterTrait> traits;

    TrainerTier(String title,
                NamedTextColor color,
                int level,
                double baseHp,
                double baseAttack,
                double baseDefense,
                int attackSpeedTicks,
                double rewardCoins,
                int rewardCages,
                Set<RoosterTrait> traits) {
        this.title = title;
        this.color = color;
        this.level = level;
        this.baseHp = baseHp;
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.attackSpeedTicks = attackSpeedTicks;
        this.rewardCoins = rewardCoins;
        this.rewardCages = rewardCages;
        this.traits = traits;
    }

    public String getTitle() { return title; }
    public NamedTextColor getColor() { return color; }
    public int getLevel() { return level; }
    public double getBaseHp() { return baseHp; }
    public double getBaseAttack() { return baseAttack; }
    public double getBaseDefense() { return baseDefense; }
    public int getAttackSpeedTicks() { return attackSpeedTicks; }
    public double getRewardCoins() { return rewardCoins; }
    public int getRewardCages() { return rewardCages; }
    public Set<RoosterTrait> getTraits() { return traits; }

    public String getLegacyColor() {
        if (color == NamedTextColor.GREEN) return "§a";
        if (color == NamedTextColor.AQUA) return "§b";
        if (color == NamedTextColor.GOLD) return "§6";
        if (color == NamedTextColor.LIGHT_PURPLE) return "§d";
        return "§e";
    }

    public static TrainerTier fromString(String str) {
        if (str == null) return APRENDIZ;
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return switch (str.toLowerCase()) {
                case "1", "novato", "aprendiz" -> APRENDIZ;
                case "2", "medio", "veterano" -> VETERANO;
                case "3", "dificil", "maestro" -> MAESTRO;
                case "4", "elite", "leyenda" -> LEYENDA;
                default -> APRENDIZ;
            };
        }
    }
}
