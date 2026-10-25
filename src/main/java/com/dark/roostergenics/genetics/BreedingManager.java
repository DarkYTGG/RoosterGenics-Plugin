package com.dark.roostergenics.genetics;

import com.dark.roostergenics.model.RoosterData;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;

/**
 * Motor de genética y crianza mendeliana con coeficiente de endogamia (COI de Sewall Wright),
 * herencia reversiva de mutaciones evolucionadas y cálculo de depresión por consanguinidad.
 *
 * REGLA CRÍTICA DE HERENCIA REVERSIVA:
 *   Si un progenitor posee una mutación EVOLUCIONADA (positiva o negativa),
 *   la cría la hereda al 100% GARANTIZADO pero en su forma BASE (sin evolucionar).
 *   El descendiente deberá entrenarla para volver a evolucionarla.
 */
public class BreedingManager {

    private final SecureRandom rng = new SecureRandom();
    private final Map<UUID, UUID> lastFatherMap = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastMotherMap = new java.util.concurrent.ConcurrentHashMap<>();

    public void recordSelectedParents(UUID playerId, UUID fatherId, UUID motherId) {
        if (fatherId != null) lastFatherMap.put(playerId, fatherId);
        if (motherId != null) lastMotherMap.put(playerId, motherId);
    }

    public UUID getLastFather(UUID playerId) {
        return lastFatherMap.get(playerId);
    }

    public UUID getLastMother(UUID playerId) {
        return lastMotherMap.get(playerId);
    }

    public void cleanupPlayer(UUID playerId) {
        if (playerId != null) {
            lastFatherMap.remove(playerId);
            lastMotherMap.remove(playerId);
        }
    }


    /**
     * Cruza dos ejemplares y produce un nuevo gallo con herencia mendeliana,
     * evaluación de consanguinidad, herencia reversiva y posibles mutaciones fenotípicas.
     */
    public RoosterData breed(RoosterData father, RoosterData mother, UUID ownerId, Function<UUID, RoosterData> lineageLookup) {
        if (father.isSterile() || mother.isSterile()) {
            throw new IllegalStateException("Uno o ambos ejemplares presentan el defecto de Esterilidad.");
        }

        double coi = calculateInbreedingCoefficient(father, mother, lineageLookup);
        RoosterGenetics fGen = father.getGenetics();
        RoosterGenetics mGen = mother.getGenetics();

        // Herencia de Loci
        GenePair<Double> hp = inheritDouble(fGen.getMaxHpLocus(), mGen.getMaxHpLocus(), 5.0, 90.0, 240.0);
        GenePair<Double> atk = inheritDouble(fGen.getAttackLocus(), mGen.getAttackLocus(), 1.0, 6.0, 26.0);
        GenePair<Double> def = inheritDouble(fGen.getDefenseLocus(), mGen.getDefenseLocus(), 0.5, 0.0, 12.0);
        GenePair<Integer> spd = inheritInt(fGen.getAttackSpeedTicksLocus(), mGen.getAttackSpeedTicksLocus(), 1, 8, 28);
        GenePair<Double> eva = inheritDouble(fGen.getEvasionLocus(), mGen.getEvasionLocus(), 0.02, 0.04, 0.45);
        GenePair<Double> acc = inheritDouble(fGen.getAccuracyLocus(), mGen.getAccuracyLocus(), 0.02, 0.05, 0.50);
        GenePair<Integer> agg = inheritInt(fGen.getAggressionLocus(), mGen.getAggressionLocus(), 1, 1, 10);
        GenePair<Double> scale = inheritDouble(fGen.getScaleLocus(), mGen.getScaleLocus(), 0.04, 0.70, 1.40);
        GenePair<String> plumage = inheritString(fGen.getPlumageLocus(), mGen.getPlumageLocus(), RoosterGenetics.PLUMAGE_VARIANTS);
        GenePair<String> comb = inheritString(fGen.getCombLocus(), mGen.getCombLocus(), RoosterGenetics.COMB_STYLES);

        // ══════════════════════════════════════════════════════════════════════
        // HERENCIA REVERSIVA DE MUTACIONES (RoosterGenics Engine)
        // ══════════════════════════════════════════════════════════════════════
        Set<RoosterTrait> childTraits = EnumSet.noneOf(RoosterTrait.class);

        // PASO 1: Herencia GARANTIZADA de mutaciones EVOLUCIONADAS como forma BASE
        // Las mutaciones evolucionadas de los padres se transmiten al 100% pero REVERTIDAS a base
        Set<RoosterTrait> guaranteedBaseFromEvolved = EnumSet.noneOf(RoosterTrait.class);
        for (RoosterTrait t : fGen.getTraits()) {
            if (t.isEvolved()) {
                guaranteedBaseFromEvolved.add(t.getBaseForm());
            }
        }
        for (RoosterTrait t : mGen.getTraits()) {
            if (t.isEvolved()) {
                guaranteedBaseFromEvolved.add(t.getBaseForm()); // Set previene duplicados
            }
        }
        childTraits.addAll(guaranteedBaseFromEvolved);

        // PASO 2: Herencia probabilística estándar de mutaciones BASE de los progenitores (35% cada una)
        for (RoosterTrait t : fGen.getTraits()) {
            if (!t.isEvolved() && !childTraits.contains(t) && rng.nextDouble() < 0.35) {
                childTraits.add(t);
            }
        }
        for (RoosterTrait t : mGen.getTraits()) {
            if (!t.isEvolved() && !childTraits.contains(t) && rng.nextDouble() < 0.35) {
                childTraits.add(t);
            }
        }

        // PASO 3: Depresión por Consanguinidad (Endogamia — COI > 0.10)
        if (coi > 0.10) {
            double defectChance = Math.min(0.80, coi * 1.6);
            if (rng.nextDouble() < defectChance) {
                RoosterTrait[] defects = RoosterTrait.getNegativeTraits(); // Solo formas BASE
                childTraits.add(defects[rng.nextInt(defects.length)]);
            }
        }

        // PASO 4: Vigor Híbrido (Heterosis si COI es muy bajo y líneas genéticas distintas)
        if (coi < 0.04) {
            if (rng.nextDouble() < 0.12) {
                RoosterTrait[] beneficial = RoosterTrait.getPositiveTraits(); // Solo formas BASE
                childTraits.add(beneficial[rng.nextInt(beneficial.length)]);
            }
        }

        RoosterGenetics childGenetics = new RoosterGenetics(hp, atk, def, spd, eva, acc, agg, scale, plumage, comb, childTraits);

        int generation = Math.max(father.getGeneration(), mother.getGeneration()) + 1;
        String name = com.dark.roostergenics.model.RoosterNameGenerator.generateBreedingName(father, mother, generation, rng);

        RoosterData child = new RoosterData(UUID.randomUUID(), ownerId, name, childGenetics);
        child.setFatherId(father.getRoosterId());
        child.setMotherId(mother.getRoosterId());
        child.setGeneration(generation);
        child.setInbreedingCoefficient(Math.round(coi * 1000.0) / 1000.0);

        return child;
    }

    /**
     * Calcula el coeficiente de endogamia (COI) de Sewall Wright hasta 4 generaciones de ancestros.
     */
    public double calculateInbreedingCoefficient(RoosterData father, RoosterData mother, Function<UUID, RoosterData> lineageLookup) {
        if (father == null || mother == null) return 0.0;
        if (father.getRoosterId().equals(mother.getRoosterId())) return 1.0;

        // Padres directos
        if (father.getRoosterId().equals(mother.getFatherId()) || father.getRoosterId().equals(mother.getMotherId())) return 0.25;
        if (mother.getRoosterId().equals(father.getFatherId()) || mother.getRoosterId().equals(father.getMotherId())) return 0.25;

        // Rastreo de ancestros con sus niveles generacionales
        Map<UUID, Integer> fatherAncestors = getAncestorsWithDepth(father.getRoosterId(), 1, 4, lineageLookup);
        Map<UUID, Integer> motherAncestors = getAncestorsWithDepth(mother.getRoosterId(), 1, 4, lineageLookup);

        double coiCalc = 0.0;
        for (Map.Entry<UUID, Integer> entry : fatherAncestors.entrySet()) {
            UUID commonAncestor = entry.getKey();
            if (motherAncestors.containsKey(commonAncestor)) {
                int depthFather = entry.getValue();
                int depthMother = motherAncestors.get(commonAncestor);
                coiCalc += Math.pow(0.5, depthFather + depthMother + 1);
            }
        }

        return Math.min(1.0, coiCalc);
    }

    private Map<UUID, Integer> getAncestorsWithDepth(UUID roosterId, int currentDepth, int maxDepth, Function<UUID, RoosterData> lineageLookup) {
        Map<UUID, Integer> result = new HashMap<>();
        if (roosterId == null || currentDepth > maxDepth) return result;

        RoosterData data = lineageLookup.apply(roosterId);
        if (data == null) return result;

        if (data.getFatherId() != null) {
            result.put(data.getFatherId(), currentDepth);
            result.putAll(getAncestorsWithDepth(data.getFatherId(), currentDepth + 1, maxDepth, lineageLookup));
        }
        if (data.getMotherId() != null) {
            result.put(data.getMotherId(), currentDepth);
            result.putAll(getAncestorsWithDepth(data.getMotherId(), currentDepth + 1, maxDepth, lineageLookup));
        }

        return result;
    }

    private GenePair<Double> inheritDouble(GenePair<Double> fPair, GenePair<Double> mPair, double mutDelta, double min, double max) {
        Allele<Double> pAllele = rng.nextBoolean() ? fPair.getPaternal() : fPair.getMaternal();
        Allele<Double> mAllele = rng.nextBoolean() ? mPair.getPaternal() : mPair.getMaternal();

        double pVal = pAllele.getValue();
        double mVal = mAllele.getValue();

        if (rng.nextDouble() < 0.06) {
            pVal += (rng.nextDouble() * 2 - 1) * mutDelta;
            pVal = Math.max(min, Math.min(max, pVal));
        }
        if (rng.nextDouble() < 0.06) {
            mVal += (rng.nextDouble() * 2 - 1) * mutDelta;
            mVal = Math.max(min, Math.min(max, mVal));
        }

        return new GenePair<>(new Allele<>(pVal, pAllele.getDominance()), new Allele<>(mVal, mAllele.getDominance()));
    }

    private GenePair<Integer> inheritInt(GenePair<Integer> fPair, GenePair<Integer> mPair, int mutDelta, int min, int max) {
        Allele<Integer> pAllele = rng.nextBoolean() ? fPair.getPaternal() : fPair.getMaternal();
        Allele<Integer> mAllele = rng.nextBoolean() ? mPair.getPaternal() : mPair.getMaternal();

        int pVal = pAllele.getValue();
        int mVal = mAllele.getValue();

        if (rng.nextDouble() < 0.06) {
            pVal += (rng.nextInt(3) - 1) * mutDelta;
            pVal = Math.max(min, Math.min(max, pVal));
        }
        if (rng.nextDouble() < 0.06) {
            mVal += (rng.nextInt(3) - 1) * mutDelta;
            mVal = Math.max(min, Math.min(max, mVal));
        }

        return new GenePair<>(new Allele<>(pVal, pAllele.getDominance()), new Allele<>(mVal, mAllele.getDominance()));
    }

    private GenePair<String> inheritString(GenePair<String> fPair, GenePair<String> mPair, String[] pool) {
        Allele<String> pAllele = rng.nextBoolean() ? fPair.getPaternal() : fPair.getMaternal();
        Allele<String> mAllele = rng.nextBoolean() ? mPair.getPaternal() : mPair.getMaternal();

        String pVal = pAllele.getValue();
        String mVal = mAllele.getValue();

        if (rng.nextDouble() < 0.03) {
            pVal = pool[rng.nextInt(pool.length)];
        }
        if (rng.nextDouble() < 0.03) {
            mVal = pool[rng.nextInt(pool.length)];
        }

        return new GenePair<>(new Allele<>(pVal, pAllele.getDominance()), new Allele<>(mVal, mAllele.getDominance()));
    }
}
