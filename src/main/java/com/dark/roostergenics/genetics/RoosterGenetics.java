package com.dark.roostergenics.genetics;

import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;

import java.security.SecureRandom;
import java.util.EnumSet;
import java.util.Set;

/**
 * Genoma diploide completo de un gallo de combate (RoosterGenics Engine).
 * Modela 7 loci de combate, 3 loci fenotípicos visuales y un conjunto de rasgos/mutaciones.
 */
public class RoosterGenetics {

    public static final String[] PLUMAGE_VARIANTS = {
            "PLAINS", "DESIERTO", "ARTICO", "JUNGLA",
            "PANTANO", "TAIGA", "SAVANNA", "MESETA",
            "ISLA_CHAMPINON", "CHERRY_GROVE", "PALE_GARDEN", "DAPPLED_FOREST"
    };

    public static final String[] COMB_STYLES = {
            "CRESTA_SIMPLE", "CRESTA_ROSA", "CRESTA_FRIJOL", "DOBLE_CORONADA"
    };

    // Loci de Combate
    private GenePair<Double> maxHpLocus;
    private GenePair<Double> attackLocus;
    private GenePair<Double> defenseLocus;
    private GenePair<Integer> attackSpeedTicksLocus;
    private GenePair<Double> evasionLocus;
    private GenePair<Double> accuracyLocus;
    private GenePair<Integer> aggressionLocus;

    // Loci Fenotípicos Visuales
    private GenePair<Double> scaleLocus;
    private GenePair<String> plumageLocus;
    private GenePair<String> combLocus;

    // Rasgos y Mutaciones Activas
    private final Set<RoosterTrait> traits;

    public RoosterGenetics(GenePair<Double> maxHpLocus,
                           GenePair<Double> attackLocus,
                           GenePair<Double> defenseLocus,
                           GenePair<Integer> attackSpeedTicksLocus,
                           GenePair<Double> evasionLocus,
                           GenePair<Double> accuracyLocus,
                           GenePair<Integer> aggressionLocus,
                           GenePair<Double> scaleLocus,
                           GenePair<String> plumageLocus,
                           GenePair<String> combLocus,
                           Set<RoosterTrait> traits) {
        this.maxHpLocus = maxHpLocus;
        this.attackLocus = attackLocus;
        this.defenseLocus = defenseLocus;
        this.attackSpeedTicksLocus = attackSpeedTicksLocus;
        this.evasionLocus = evasionLocus;
        this.accuracyLocus = accuracyLocus;
        this.aggressionLocus = aggressionLocus;
        this.scaleLocus = scaleLocus;
        this.plumageLocus = plumageLocus;
        this.combLocus = combLocus;
        this.traits = EnumSet.noneOf(RoosterTrait.class);
        if (traits != null) {
            this.traits.addAll(traits);
        }
    }

    public double getExpressedMaxHp() {
        return Math.max(90.0, maxHpLocus.getExpressedValue());
    }

    public double getExpressedAttack() {
        return Math.max(7.0, attackLocus.getExpressedValue());
    }

    public double getExpressedDefense() {
        return Math.max(0.0, defenseLocus.getExpressedValue());
    }

    public int getExpressedAttackSpeedTicks() {
        return Math.max(8, Math.min(30, attackSpeedTicksLocus.getExpressedValue()));
    }

    public double getExpressedEvasion() {
        return Math.max(0.05, Math.min(0.45, evasionLocus.getExpressedValue()));
    }

    public double getExpressedAccuracy() {
        return Math.max(0.05, Math.min(0.50, accuracyLocus.getExpressedValue()));
    }

    public int getExpressedAggression() {
        return Math.max(1, Math.min(10, aggressionLocus.getExpressedValue()));
    }

    public double getExpressedScale() {
        return Math.max(0.75, Math.min(1.35, scaleLocus.getExpressedValue()));
    }

    public String getExpressedPlumage() {
        return plumageLocus.getExpressedValue();
    }

    public String getPlumageId() {
        return plumageLocus.getExpressedValue();
    }

    public String getPlumage() {
        return getPlumageDisplayName(getExpressedPlumage());
    }

    public String getExpressedComb() {
        return combLocus.getExpressedValue();
    }

    public double getScale() { return getExpressedScale(); }
    public String getComb() { return getExpressedComb(); }

    /**
     * Mapea el bioma nativo del bloque donde se encuentra el gallo a una de las 12 variantes oficiales de plumaje.
     */
    @SuppressWarnings({"deprecation", "removal"})
    public static String getPlumageForBiome(Biome biome) {
        if (biome == null) return "PLAINS";
        String name;
        try {
            name = (biome.getKey() != null ? biome.getKey().getKey() : biome.name()).toLowerCase();
        } catch (Throwable t) {
            name = biome.name().toLowerCase();
        }
        if (name.contains("pale")) return "PALE_GARDEN";
        if (name.contains("cherry")) return "CHERRY_GROVE";
        if (name.contains("mushroom")) return "ISLA_CHAMPINON";
        if (name.contains("badlands") || name.contains("mesa")) return "MESETA";
        if (name.contains("desert")) return "DESIERTO";
        if (name.contains("snow") || name.contains("ice") || name.contains("frozen")) return "ARTICO";
        if (name.contains("jungle") || name.contains("bamboo")) return "JUNGLA";
        if (name.contains("swamp") || name.contains("mangrove")) return "PANTANO";
        if (name.contains("taiga")) return "TAIGA";
        if (name.contains("savanna")) return "SAVANNA";
        if (name.contains("forest") || name.contains("birch") || name.contains("dark") || name.contains("grove") || name.contains("wood")) return "DAPPLED_FOREST";
        return "PLAINS";
    }

    /**
     * Retorna el nombre estético y formateado con acentos para mostrar en menús, lore y chat.
     */
    public static String getPlumageDisplayName(String id) {
        if (id == null) return "Plains";
        switch (id.toUpperCase()) {
            case "PLAINS": return "Plains";
            case "DESIERTO": return "Desierto";
            case "ARTICO": return "Ártico";
            case "JUNGLA": return "Jungla";
            case "PANTANO": return "Pantano";
            case "TAIGA": return "Taiga";
            case "SAVANNA": return "Savanna";
            case "MESETA": return "Meseta";
            case "ISLA_CHAMPINON": return "Isla Champiñón";
            case "CHERRY_GROVE": return "Cherry Grove";
            case "PALE_GARDEN": return "Pale Garden";
            case "DAPPLED_FOREST": return "Dappled Forest";
            // Compatibilidad hacia atrás con registros antiguos
            case "BLANCO_LEGHORN":
            case "LEGHORN": return "Plains";
            case "ROJO_FENIX": return "Savanna";
            case "NEGRO_AZABACHE":
            case "AZABACHE": return "Pale Garden";
            case "GIRO_AZTECA":
            case "GIRO": return "Meseta";
            case "DORADO_IMPERIAL":
            case "IMPERIAL": return "Desierto";
            case "CENIZA_ESPECTRAL":
            case "CENIZO": return "Ártico";
            case "AZUL_ANDALUZ": return "Pantano";
            case "BARRADO_MUD": return "Dappled Forest";
            default:
                String[] words = id.replace('_', ' ').toLowerCase().split(" ");
                StringBuilder sb = new StringBuilder();
                for (String w : words) {
                    if (w.isEmpty()) continue;
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
                }
                return sb.toString();
        }
    }

    public GenePair<Double> getMaxHpLocus() { return maxHpLocus; }
    public GenePair<Double> getAttackLocus() { return attackLocus; }
    public GenePair<Double> getDefenseLocus() { return defenseLocus; }
    public GenePair<Integer> getAttackSpeedTicksLocus() { return attackSpeedTicksLocus; }
    public GenePair<Double> getEvasionLocus() { return evasionLocus; }
    public GenePair<Double> getAccuracyLocus() { return accuracyLocus; }
    public GenePair<Integer> getAggressionLocus() { return aggressionLocus; }
    public GenePair<Double> getScaleLocus() { return scaleLocus; }
    public GenePair<String> getPlumageLocus() { return plumageLocus; }
    public GenePair<String> getCombLocus() { return combLocus; }

    public GenePair<Double> getHpGene() { return maxHpLocus; }
    public GenePair<Double> getAttackGene() { return attackLocus; }
    public GenePair<Double> getDefenseGene() { return defenseLocus; }
    public GenePair<Integer> getSpeedGene() { return attackSpeedTicksLocus; }
    public GenePair<Double> getEvasionGene() { return evasionLocus; }
    public GenePair<Double> getAccuracyGene() { return accuracyLocus; }
    public GenePair<Integer> getAggressionGene() { return aggressionLocus; }

    public Set<RoosterTrait> getTraits() {
        return traits;
    }

    public boolean hasTrait(RoosterTrait trait) {
        return traits.contains(trait);
    }

    /**
     * Verifica si el gallo posee esta mutación en cualquiera de sus formas (base o evolucionada).
     */
    public boolean hasTraitOrEvolved(RoosterTrait baseTrait) {
        if (traits.contains(baseTrait)) return true;
        if (baseTrait.hasEvolution() && traits.contains(baseTrait.getEvolvedForm())) return true;
        if (baseTrait.isEvolved() && traits.contains(baseTrait.getBaseForm())) return true;
        return false;
    }

    /**
     * Cuenta cuántas mutaciones evolucionadas posee este genoma.
     */
    public int countEvolvedTraits() {
        int count = 0;
        for (RoosterTrait t : traits) {
            if (t.isEvolved()) count++;
        }
        return count;
    }

    /**
     * Verifica si el gallo puede evolucionar una mutación de forma natural
     * (debe tener menos de MAX_EVOLVED_NATURAL evolucionadas).
     */
    public boolean canEvolveNaturally() {
        return countEvolvedTraits() < RoosterTrait.MAX_EVOLVED_NATURAL;
    }

    /**
     * Intenta evolucionar una mutación base a su forma evolucionada.
     * @param baseTrait La mutación en forma base a evolucionar.
     * @param bypassLimit Si es true, ignora el límite de MAX_EVOLVED_NATURAL (uso admin).
     * @return true si la evolución fue exitosa.
     */
    public boolean evolveTrait(RoosterTrait baseTrait, boolean bypassLimit) {
        if (baseTrait == null || baseTrait.isEvolved()) return false;
        if (!baseTrait.hasEvolution()) return false;
        if (!traits.contains(baseTrait)) return false;
        if (!bypassLimit && !canEvolveNaturally()) return false;

        traits.remove(baseTrait);
        traits.add(baseTrait.getEvolvedForm());
        return true;
    }

    /**
     * Degrada una mutación evolucionada a su forma base (reversión).
     */
    public boolean devolveTrait(RoosterTrait evolvedTrait) {
        if (evolvedTrait == null || !evolvedTrait.isEvolved()) return false;
        if (!traits.contains(evolvedTrait)) return false;

        traits.remove(evolvedTrait);
        traits.add(evolvedTrait.getBaseForm());
        return true;
    }

    public void setMaxHp(double val) {
        this.maxHpLocus = new GenePair<>(new Allele<>(val, Allele.Dominance.CODOMINANT), new Allele<>(val, Allele.Dominance.CODOMINANT));
    }

    public void setAttack(double val) {
        this.attackLocus = new GenePair<>(new Allele<>(val, Allele.Dominance.CODOMINANT), new Allele<>(val, Allele.Dominance.CODOMINANT));
    }

    public void setDefense(double val) {
        this.defenseLocus = new GenePair<>(new Allele<>(val, Allele.Dominance.CODOMINANT), new Allele<>(val, Allele.Dominance.CODOMINANT));
    }

    public void setAttackSpeedTicks(int val) {
        this.attackSpeedTicksLocus = new GenePair<>(new Allele<>(val, Allele.Dominance.CODOMINANT), new Allele<>(val, Allele.Dominance.CODOMINANT));
    }

    public void setScale(double val) {
        this.scaleLocus = new GenePair<>(new Allele<>(val, Allele.Dominance.CODOMINANT), new Allele<>(val, Allele.Dominance.CODOMINANT));
    }

    public void addTrait(RoosterTrait trait) {
        this.traits.add(trait);
    }

    public void removeTrait(RoosterTrait trait) {
        this.traits.remove(trait);
    }

    public void clearTraits() {
        this.traits.clear();
    }

    /**
     * Genera un genoma silvestre de Generación 1 con distribución probabilística natural y adaptación por bioma.
     * Solo genera mutaciones en forma BASE (los gallos salvajes nunca tienen mutaciones evolucionadas).
     */
    public static RoosterGenetics generateWild(SecureRandom rng, Biome biome) {
        double hpP = 120.0 + rng.nextInt(61);
        double hpM = 120.0 + rng.nextInt(61);
        Allele.Dominance domHp = (rng.nextDouble() < 0.5) ? Allele.Dominance.DOMINANT : Allele.Dominance.RECESSIVE;
        GenePair<Double> hp = new GenePair<>(new Allele<>(hpP, domHp), new Allele<>(hpM, Allele.Dominance.CODOMINANT));

        double atP = 10.0 + rng.nextInt(7);
        double atM = 10.0 + rng.nextInt(7);
        GenePair<Double> at = new GenePair<>(new Allele<>(atP, Allele.Dominance.DOMINANT), new Allele<>(atM, Allele.Dominance.RECESSIVE));

        double defP = 2.0 + rng.nextInt(5);
        double defM = 2.0 + rng.nextInt(5);
        GenePair<Double> def = new GenePair<>(new Allele<>(defP, Allele.Dominance.CODOMINANT), new Allele<>(defM, Allele.Dominance.CODOMINANT));

        int spdP = 14 + rng.nextInt(9);
        int spdM = 14 + rng.nextInt(9);
        GenePair<Integer> spd = new GenePair<>(new Allele<>(spdP, Allele.Dominance.DOMINANT), new Allele<>(spdM, Allele.Dominance.RECESSIVE));

        double evaP = (10 + rng.nextInt(16)) / 100.0;
        double evaM = (10 + rng.nextInt(16)) / 100.0;
        GenePair<Double> eva = new GenePair<>(new Allele<>(evaP, Allele.Dominance.CODOMINANT), new Allele<>(evaM, Allele.Dominance.CODOMINANT));

        double accP = (12 + rng.nextInt(17)) / 100.0;
        double accM = (12 + rng.nextInt(17)) / 100.0;
        GenePair<Double> acc = new GenePair<>(new Allele<>(accP, Allele.Dominance.DOMINANT), new Allele<>(accM, Allele.Dominance.RECESSIVE));

        int aggP = 3 + rng.nextInt(6);
        int aggM = 3 + rng.nextInt(6);
        GenePair<Integer> agg = new GenePair<>(new Allele<>(aggP, Allele.Dominance.CODOMINANT), new Allele<>(aggM, Allele.Dominance.CODOMINANT));

        double scP = 0.85 + (rng.nextInt(31) / 100.0);
        double scM = 0.85 + (rng.nextInt(31) / 100.0);
        GenePair<Double> sc = new GenePair<>(new Allele<>(scP, Allele.Dominance.CODOMINANT), new Allele<>(scM, Allele.Dominance.CODOMINANT));

        String pluP;
        String pluM;
        if (biome != null) {
            String nativePlumage = getPlumageForBiome(biome);
            pluP = nativePlumage;
            pluM = (rng.nextDouble() < 0.85) ? nativePlumage : PLUMAGE_VARIANTS[rng.nextInt(PLUMAGE_VARIANTS.length)];
        } else {
            pluP = PLUMAGE_VARIANTS[rng.nextInt(PLUMAGE_VARIANTS.length)];
            pluM = PLUMAGE_VARIANTS[rng.nextInt(PLUMAGE_VARIANTS.length)];
        }
        GenePair<String> plu = new GenePair<>(new Allele<>(pluP, Allele.Dominance.DOMINANT), new Allele<>(pluM, Allele.Dominance.RECESSIVE));

        String combP = COMB_STYLES[rng.nextInt(COMB_STYLES.length)];
        String combM = COMB_STYLES[rng.nextInt(COMB_STYLES.length)];
        GenePair<String> comb = new GenePair<>(new Allele<>(combP, Allele.Dominance.DOMINANT), new Allele<>(combM, Allele.Dominance.RECESSIVE));

        // Solo rasgos BASE para gallos salvajes (nunca evolucionados)
        Set<RoosterTrait> tr = EnumSet.noneOf(RoosterTrait.class);
        if (rng.nextDouble() < 0.35) {
            RoosterTrait[] baseTraits = RoosterTrait.getAllBaseTraits();
            tr.add(baseTraits[rng.nextInt(baseTraits.length)]);
        }

        return new RoosterGenetics(hp, at, def, spd, eva, acc, agg, sc, plu, comb, tr);
    }

    public static RoosterGenetics generateWild(SecureRandom rng) {
        return generateWild(rng, null);
    }

    /**
     * Genera un genoma silvestre fijando de manera determinista la variante de plumaje solicitada.
     */
    public static RoosterGenetics generateWildWithPlumage(SecureRandom rng, String fixedPlumage) {
        RoosterGenetics wild = generateWild(rng, null);
        String targetPlu = (fixedPlumage != null && !fixedPlumage.isBlank()) ? fixedPlumage.toUpperCase() : "PLAINS";
        wild.plumageLocus = new GenePair<>(new Allele<>(targetPlu, Allele.Dominance.DOMINANT), new Allele<>(targetPlu, Allele.Dominance.RECESSIVE));
        return wild;
    }

    /**
     * Resuelve sinónimos y alias (español e inglés) a la variante canónica de plumaje.
     */
    public static String resolvePlumageAlias(String query) {
        if (query == null || query.isBlank()) return "PLAINS";
        String q = query.trim().toLowerCase();
        if (q.contains("arid") || q.contains("árid") || q.contains("savann") || q.contains("saban")) {
            return "SAVANNA";
        }
        if (q.contains("calid") || q.contains("cálid") || q.contains("warm") || q.contains("desiert") || q.contains("desert") || q.contains("badland") || q.contains("meset") || q.contains("mesa")) {
            return "DESIERTO";
        }
        if (q.contains("salvaj") || q.contains("wild") || q.contains("jungl") || q.contains("bamboo") || q.contains("bambú")) {
            return "JUNGLA";
        }
        if (q.contains("fri") || q.contains("frí") || q.contains("cold") || q.contains("artic") || q.contains("ártic") || q.contains("nieve") || q.contains("ice") || q.contains("polar")) {
            return "ARTICO";
        }
        if (q.contains("pino") || q.contains("pine") || q.contains("pinar") || q.contains("taiga") || q.contains("abeto") || q.contains("spruce")) {
            return "TAIGA";
        }
        if (q.contains("cienag") || q.contains("ciénag") || q.contains("pantan") || q.contains("mire") || q.contains("swamp") || q.contains("manglar") || q.contains("mangrove")) {
            return "PANTANO";
        }
        if (q.contains("comun") || q.contains("común") || q.contains("plain") || q.contains("llanur") || q.contains("bosque") || q.contains("forest") || q.contains("dapple") || q.contains("default")) {
            return "PLAINS";
        }
        for (String v : PLUMAGE_VARIANTS) {
            if (v.equalsIgnoreCase(query)) return v;
        }
        return "PLAINS";
    }

    /**
     * Retorna el nombre estético de la variante de textura CEM.
     */
    public static String getVariantTextureName(String plumage) {
        if (plumage == null) return "Común";
        return switch (plumage.toUpperCase()) {
            case "SAVANNA" -> "Árido";
            case "DESIERTO", "MESETA" -> "Cálido";
            case "JUNGLA" -> "Salvaje";
            case "ARTICO" -> "Frío";
            case "TAIGA", "PINAR", "PINO" -> "Pinar";
            case "PANTANO" -> "Ciénaga";
            default -> "Común";
        };
    }

    /**
     * Retorna el nombre corto y coloreado del plumaje para el nametag visible.
     */
    public static String getShortPlumageDisplay(String plumage) {
        if (plumage == null) return "§fComún";
        return switch (plumage.toUpperCase()) {
            case "SAVANNA" -> "§eÁrido";
            case "DESIERTO", "MESETA" -> "§6Cálido";
            case "JUNGLA" -> "§cSalvaje";
            case "ARTICO" -> "§bFrío";
            case "TAIGA", "PINAR", "PINO" -> "§2Pinar";
            case "PANTANO" -> "§2Ciénaga";
            default -> "§fComún";
        };
    }

    /**
     * Retorna el índice numérico (1-7) de la variante para mapeo en OptiFine CEM / ETF.
     */
    public static int getVariantIndex(String plumage) {
        if (plumage == null) return 1;
        return switch (plumage.toUpperCase()) {
            case "SAVANNA" -> 2;                        // arid_chicken (Savanna)
            case "ARTICO" -> 3;                         // cold_chicken (Ártico)
            case "PANTANO" -> 4;                        // mire_chicken (Ciénaga)
            case "DESIERTO", "MESETA" -> 5;             // warm_chicken (Desierto - Cálido)
            case "JUNGLA" -> 6;                         // wild_chicken (Jungla - Salvaje)
            case "TAIGA", "PINAR", "PINO" -> 7;         // pine_chicken (Pinar / Taiga)
            default -> 1;                               // chicken (Común - Llanura & Bosque)
        };
    }

    /**
     * Retorna un sufijo de códigos de color invisibles (0 píxeles renderizados) para que ETF / EMF
     * detecte la variante exacta sin mostrar texto de plumaje en el nametag visible.
     */
    public static String getInvisibleVariantTag(String plumage) {
        return "§r§0§" + getVariantIndex(plumage);
    }

    public void serializeTo(ConfigurationSection sec) {
        saveDoublePair(sec.createSection("hp"), maxHpLocus);
        saveDoublePair(sec.createSection("attack"), attackLocus);
        saveDoublePair(sec.createSection("defense"), defenseLocus);
        saveIntPair(sec.createSection("speed"), attackSpeedTicksLocus);
        saveDoublePair(sec.createSection("evasion"), evasionLocus);
        saveDoublePair(sec.createSection("accuracy"), accuracyLocus);
        saveIntPair(sec.createSection("aggression"), aggressionLocus);
        saveDoublePair(sec.createSection("scale"), scaleLocus);
        saveStringPair(sec.createSection("plumage"), plumageLocus);
        saveStringPair(sec.createSection("comb"), combLocus);

        ConfigurationSection trSec = sec.createSection("traits");
        int i = 0;
        for (RoosterTrait t : traits) {
            trSec.set(String.valueOf(i++), t.name());
        }
    }

    public static RoosterGenetics deserializeFrom(ConfigurationSection sec) {
        GenePair<Double> hp = loadDoublePair(sec.getConfigurationSection("hp"), 140.0);
        GenePair<Double> at = loadDoublePair(sec.getConfigurationSection("attack"), 12.0);
        GenePair<Double> def = loadDoublePair(sec.getConfigurationSection("defense"), 3.0);
        GenePair<Integer> spd = loadIntPair(sec.getConfigurationSection("speed"), 18);
        GenePair<Double> eva = loadDoublePair(sec.getConfigurationSection("evasion"), 0.15);
        GenePair<Double> acc = loadDoublePair(sec.getConfigurationSection("accuracy"), 0.20);
        GenePair<Integer> agg = loadIntPair(sec.getConfigurationSection("aggression"), 5);
        GenePair<Double> sc = loadDoublePair(sec.getConfigurationSection("scale"), 1.0);
        GenePair<String> plu = loadStringPair(sec.getConfigurationSection("plumage"), "PLAINS");
        GenePair<String> comb = loadStringPair(sec.getConfigurationSection("comb"), "CRESTA_SIMPLE");

        Set<RoosterTrait> tr = EnumSet.noneOf(RoosterTrait.class);
        ConfigurationSection trSec = sec.getConfigurationSection("traits");
        if (trSec != null) {
            for (String key : trSec.getKeys(false)) {
                try {
                    tr.add(RoosterTrait.valueOf(trSec.getString(key)));
                } catch (Exception ignored) {}
            }
        }

        return new RoosterGenetics(hp, at, def, spd, eva, acc, agg, sc, plu, comb, tr);
    }

    private static void saveDoublePair(ConfigurationSection sec, GenePair<Double> pair) {
        sec.set("p.val", pair.getPaternal().getValue());
        sec.set("p.dom", pair.getPaternal().getDominance().name());
        sec.set("m.val", pair.getMaternal().getValue());
        sec.set("m.dom", pair.getMaternal().getDominance().name());
    }

    private static GenePair<Double> loadDoublePair(ConfigurationSection sec, double defVal) {
        if (sec == null) return new GenePair<>(new Allele<>(defVal, Allele.Dominance.DOMINANT), new Allele<>(defVal, Allele.Dominance.RECESSIVE));
        double pv = sec.getDouble("p.val", defVal);
        Allele.Dominance pd = Allele.Dominance.valueOf(sec.getString("p.dom", "DOMINANT"));
        double mv = sec.getDouble("m.val", defVal);
        Allele.Dominance md = Allele.Dominance.valueOf(sec.getString("m.dom", "RECESSIVE"));
        return new GenePair<>(new Allele<>(pv, pd), new Allele<>(mv, md));
    }

    private static GenePair<Integer> loadIntPair(ConfigurationSection sec, int defVal) {
        if (sec == null) return new GenePair<>(new Allele<>(defVal, Allele.Dominance.DOMINANT), new Allele<>(defVal, Allele.Dominance.RECESSIVE));
        int pv = sec.getInt("p.val", defVal);
        Allele.Dominance pd = Allele.Dominance.valueOf(sec.getString("p.dom", "DOMINANT"));
        int mv = sec.getInt("m.val", defVal);
        Allele.Dominance md = Allele.Dominance.valueOf(sec.getString("m.dom", "RECESSIVE"));
        return new GenePair<>(new Allele<>(pv, pd), new Allele<>(mv, md));
    }

    private static void saveIntPair(ConfigurationSection sec, GenePair<Integer> pair) {
        sec.set("p.val", pair.getPaternal().getValue());
        sec.set("p.dom", pair.getPaternal().getDominance().name());
        sec.set("m.val", pair.getMaternal().getValue());
        sec.set("m.dom", pair.getMaternal().getDominance().name());
    }

    private static void saveStringPair(ConfigurationSection sec, GenePair<String> pair) {
        sec.set("p.val", pair.getPaternal().getValue());
        sec.set("p.dom", pair.getPaternal().getDominance().name());
        sec.set("m.val", pair.getMaternal().getValue());
        sec.set("m.dom", pair.getMaternal().getDominance().name());
    }

    private static GenePair<String> loadStringPair(ConfigurationSection sec, String defVal) {
        if (sec == null) return new GenePair<>(new Allele<>(defVal, Allele.Dominance.DOMINANT), new Allele<>(defVal, Allele.Dominance.RECESSIVE));
        String pv = sec.getString("p.val", defVal);
        Allele.Dominance pd = Allele.Dominance.valueOf(sec.getString("p.dom", "DOMINANT"));
        String mv = sec.getString("m.val", defVal);
        Allele.Dominance md = Allele.Dominance.valueOf(sec.getString("m.dom", "RECESSIVE"));
        return new GenePair<>(new Allele<>(pv, pd), new Allele<>(mv, md));
    }
}
