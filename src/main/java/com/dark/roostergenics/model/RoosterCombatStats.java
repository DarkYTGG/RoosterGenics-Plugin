package com.dark.roostergenics.model;

import com.dark.roostergenics.genetics.RoosterGenetics;
import com.dark.roostergenics.genetics.RoosterTrait;

/**
 * Calculador de atributos efectivos de combate combinando genética,
 * entrenamiento de EVs y rasgos pasivos activos.
 */
public class RoosterCombatStats {

    private final double maxHp;
    private final double attack;
    private final double defense;
    private final int attackSpeedTicks;
    private final double evasion;
    private final double accuracy;
    private final int aggression;
    private final double scale;

    public RoosterCombatStats(RoosterData data) {
        RoosterGenetics gen = data.getGenetics();

        int lvlBonus = Math.max(0, data.getLevel() - 1);

        // Base genética + EV points + Bonificación de Nivel (progresión por batallas)
        double calculatedHp = gen.getExpressedMaxHp() + (data.getEvHp() * 0.75) + (lvlBonus * 3.5);
        double calculatedAtk = gen.getExpressedAttack() + (data.getEvAttack() * 0.20) + (lvlBonus * 0.65);
        double calculatedDef = gen.getExpressedDefense() + (data.getEvDefense() * 0.15) + (lvlBonus * 0.40);

        // Velocidad: a menor ticks, más rápido ataca (EVs + veteranía de combate)
        int speedReduction = (data.getEvSpeed() / 25) + (lvlBonus / 12);
        int calculatedSpeedTicks = Math.max(7, gen.getExpressedAttackSpeedTicks() - speedReduction);

        double calculatedEvasion = gen.getExpressedEvasion() + (lvlBonus * 0.002);
        double calculatedAccuracy = gen.getExpressedAccuracy() + (lvlBonus * 0.003);
        int calculatedAggression = gen.getExpressedAggression() + (lvlBonus / 8);
        double calculatedScale = gen.getExpressedScale() + (lvlBonus * 0.004);

        // Aplicación de Rasgos Mutacionales
        if (gen.hasTrait(RoosterTrait.PASO_FANTASMAL)) {
            calculatedEvasion += 0.12;
        }
        if (gen.hasTrait(RoosterTrait.COJERA)) {
            calculatedEvasion = Math.max(0.02, calculatedEvasion - 0.25);
            calculatedSpeedTicks += 3;
        }
        if (gen.hasTrait(RoosterTrait.PICO_ROTO)) {
            calculatedAtk = Math.max(3.0, calculatedAtk - 3.0);
        }
        if (gen.hasTrait(RoosterTrait.CORAZON_COLOSO)) {
            calculatedHp *= 1.25;
        }
        if (gen.hasTrait(RoosterTrait.ENERGIA_SIN_FIN)) {
            calculatedSpeedTicks = Math.max(5, calculatedSpeedTicks - 2);
        }
        if (gen.hasTrait(RoosterTrait.MIOPIA_CONGENITA)) {
            calculatedAccuracy = Math.max(0.10, calculatedAccuracy - 0.20);
        }
        if (gen.hasTrait(RoosterTrait.ASMA_DE_CORRAL)) {
            calculatedSpeedTicks += 3;
        }

        // Modificadores de Objeto Equipado (Held Item)
        if (data.getHeldItem() != null) {
            switch (data.getHeldItem()) {
                case CHALECO_ASALTO -> calculatedDef *= 1.30;
                case PLUMA_VELOZ -> {
                    calculatedSpeedTicks = Math.max(5, (int) Math.round(calculatedSpeedTicks * 0.80));
                    calculatedDef = Math.max(0.0, calculatedDef * 0.90);
                }
                default -> {}
            }
        }

        this.maxHp = Math.round(calculatedHp * 10.0) / 10.0;
        this.attack = Math.round(calculatedAtk * 10.0) / 10.0;
        this.defense = Math.round(calculatedDef * 10.0) / 10.0;
        this.attackSpeedTicks = calculatedSpeedTicks;
        this.evasion = Math.min(0.50, Math.round(calculatedEvasion * 100.0) / 100.0);
        this.accuracy = Math.min(0.85, Math.round(calculatedAccuracy * 100.0) / 100.0);
        this.aggression = calculatedAggression;
        this.scale = calculatedScale;
    }

    public double getMaxHp() { return maxHp; }
    public double getAttack() { return attack; }
    public double getDefense() { return defense; }
    public int getAttackSpeedTicks() { return attackSpeedTicks; }
    public double getEvasion() { return evasion; }
    public double getAccuracy() { return accuracy; }
    public int getAggression() { return aggression; }
    public double getScale() { return scale; }
}
