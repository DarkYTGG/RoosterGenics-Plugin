package com.dark.roostergenics.trainer;

import com.dark.roostergenics.genetics.Allele;
import com.dark.roostergenics.genetics.GenePair;
import com.dark.roostergenics.genetics.RoosterGenetics;
import com.dark.roostergenics.genetics.RoosterTrait;
import com.dark.roostergenics.items.RoosterHeldItem;
import com.dark.roostergenics.model.RoosterCombatStats;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.model.RoosterNameGenerator;

import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;

/**
 * Generador de Rivales de Combate Adaptativos:
 * Escala dinámicamente las estadísticas, nivel y rasgos del gallo rival
 * según el nivel y poder del gallo del jugador, garantizando siempre una batalla justa y emocionante.
 */
public class AdaptiveTrainer {

    private static final Random RNG = new Random();

    /**
     * Genera un gallo rival que se adapta con precisión a las estadísticas del gallo del jugador.
     */
    public static RoosterData createScaledOpponent(RoosterData playerRooster, String trainerName) {
        RoosterCombatStats pStats = playerRooster.calculateCombatStats();
        int playerLvl = playerRooster.getLevel();

        // Variación sutil de ±6% para dinamismo
        double hpVar = 0.94 + RNG.nextDouble() * 0.12;
        double atkVar = 0.94 + RNG.nextDouble() * 0.12;
        double defVar = 0.94 + RNG.nextDouble() * 0.12;

        double scaledHp = Math.max(90.0, Math.min(240.0, pStats.getMaxHp() * hpVar));
        double scaledAtk = Math.max(6.0, Math.min(26.0, pStats.getAttack() * atkVar));
        double scaledDef = Math.max(0.0, Math.min(12.0, pStats.getDefense() * defVar));
        int scaledSpeed = Math.max(8, Math.min(28, pStats.getAttackSpeedTicks() + (RNG.nextInt(3) - 1)));
        double scaledEva = Math.max(0.04, Math.min(0.45, pStats.getEvasion()));
        double scaledAcc = Math.max(0.10, Math.min(0.50, pStats.getAccuracy()));

        GenePair<Double> hpLocus = new GenePair<>(new Allele<>(scaledHp, Allele.Dominance.CODOMINANT), new Allele<>(scaledHp, Allele.Dominance.CODOMINANT));
        GenePair<Double> atkLocus = new GenePair<>(new Allele<>(scaledAtk, Allele.Dominance.CODOMINANT), new Allele<>(scaledAtk, Allele.Dominance.CODOMINANT));
        GenePair<Double> defLocus = new GenePair<>(new Allele<>(scaledDef, Allele.Dominance.CODOMINANT), new Allele<>(scaledDef, Allele.Dominance.CODOMINANT));
        GenePair<Integer> spdLocus = new GenePair<>(new Allele<>(scaledSpeed, Allele.Dominance.CODOMINANT), new Allele<>(scaledSpeed, Allele.Dominance.CODOMINANT));
        GenePair<Double> evLocus = new GenePair<>(new Allele<>(scaledEva, Allele.Dominance.CODOMINANT), new Allele<>(scaledEva, Allele.Dominance.CODOMINANT));
        GenePair<Double> accLocus = new GenePair<>(new Allele<>(scaledAcc, Allele.Dominance.CODOMINANT), new Allele<>(scaledAcc, Allele.Dominance.CODOMINANT));
        GenePair<Integer> agLocus = new GenePair<>(new Allele<>(7, Allele.Dominance.CODOMINANT), new Allele<>(7, Allele.Dominance.CODOMINANT));
        GenePair<Double> scLocus = new GenePair<>(new Allele<>(1.0, Allele.Dominance.CODOMINANT), new Allele<>(1.0, Allele.Dominance.CODOMINANT));
        GenePair<String> plLocus = new GenePair<>(new Allele<>(RoosterGenetics.PLUMAGE_VARIANTS[RNG.nextInt(RoosterGenetics.PLUMAGE_VARIANTS.length)], Allele.Dominance.CODOMINANT), new Allele<>(RoosterGenetics.PLUMAGE_VARIANTS[RNG.nextInt(RoosterGenetics.PLUMAGE_VARIANTS.length)], Allele.Dominance.CODOMINANT));
        GenePair<String> cbLocus = new GenePair<>(new Allele<>(RoosterGenetics.COMB_STYLES[RNG.nextInt(RoosterGenetics.COMB_STYLES.length)], Allele.Dominance.CODOMINANT), new Allele<>("CRESTA_SIMPLE", Allele.Dominance.CODOMINANT));

        // Seleccionar 1 o 2 rasgos favorables al azar
        EnumSet<RoosterTrait> traits = EnumSet.noneOf(RoosterTrait.class);
        RoosterTrait[] posTraits = {
                RoosterTrait.GARRAS_AFILADAS, RoosterTrait.PIEL_DE_HIERRO, RoosterTrait.FURIA_PRIMAL,
                RoosterTrait.PASO_FANTASMAL, RoosterTrait.CANTO_DE_GUERRA, RoosterTrait.REFLEJOS_DE_RAYO, RoosterTrait.ENERGIA_SIN_FIN
        };
        traits.add(posTraits[RNG.nextInt(posTraits.length)]);

        RoosterGenetics genetics = new RoosterGenetics(hpLocus, atkLocus, defLocus, spdLocus, evLocus, accLocus, agLocus, scLocus, plLocus, cbLocus, traits);
        String name = RoosterNameGenerator.generateName(RNG);
        RoosterData data = new RoosterData(UUID.randomUUID(), UUID.randomUUID(), name + " (" + trainerName + ")", genetics);
        data.setLevel(playerLvl);

        // Asignar un Held Item complementario
        RoosterHeldItem[] items = RoosterHeldItem.values();
        data.setHeldItem(items[RNG.nextInt(items.length)]);
        return data;
    }
}
