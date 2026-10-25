package com.dark.roostergenics.trainer;

import com.dark.roostergenics.genetics.Allele;
import com.dark.roostergenics.genetics.GenePair;
import com.dark.roostergenics.genetics.RoosterGenetics;
import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.model.RoosterNameGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.dark.roostergenics.npc.PlayerNpc;

import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;

/**
 * Modelo de datos de un Entrenador Gallero NPC con su gallo asignado y estadísticas de combate.
 */
public class RoosterTrainer {

    private final UUID trainerId;
    private String name;
    private TrainerTier tier;
    private String skinName;
    private Location location;
    private RoosterData rooster;
    private int wins;
    private int losses;

    public RoosterTrainer(UUID trainerId, String name, TrainerTier tier, String skinName, Location location) {
        this.trainerId = trainerId != null ? trainerId : UUID.randomUUID();
        this.name = name;
        this.tier = tier != null ? tier : TrainerTier.APRENDIZ;
        this.skinName = PlayerNpc.resolveSkinName(skinName);
        this.location = location != null ? location.clone() : null;
        this.wins = 0;
        this.losses = 0;
        this.rooster = generateTrainerRooster(this.trainerId, this.tier);
    }

    public static RoosterData generateTrainerRooster(UUID trainerId, TrainerTier tier) {
        Random rng = new Random();
        double hp = tier.getBaseHp();
        double atk = tier.getBaseAttack();
        double def = tier.getBaseDefense();
        int spd = tier.getAttackSpeedTicks();

        GenePair<Double> hpLocus = new GenePair<>(new Allele<>(hp, Allele.Dominance.CODOMINANT), new Allele<>(hp, Allele.Dominance.CODOMINANT));
        GenePair<Double> atkLocus = new GenePair<>(new Allele<>(atk, Allele.Dominance.CODOMINANT), new Allele<>(atk, Allele.Dominance.CODOMINANT));
        GenePair<Double> defLocus = new GenePair<>(new Allele<>(def, Allele.Dominance.CODOMINANT), new Allele<>(def, Allele.Dominance.CODOMINANT));
        GenePair<Integer> spdLocus = new GenePair<>(new Allele<>(spd, Allele.Dominance.CODOMINANT), new Allele<>(spd, Allele.Dominance.CODOMINANT));
        GenePair<Double> evaLocus = new GenePair<>(new Allele<>(0.10 + (tier.getLevel() * 0.03), Allele.Dominance.CODOMINANT), new Allele<>(0.10, Allele.Dominance.CODOMINANT));
        GenePair<Double> accLocus = new GenePair<>(new Allele<>(0.15 + (tier.getLevel() * 0.03), Allele.Dominance.CODOMINANT), new Allele<>(0.15, Allele.Dominance.CODOMINANT));
        GenePair<Integer> aggLocus = new GenePair<>(new Allele<>(5 + tier.getLevel(), Allele.Dominance.CODOMINANT), new Allele<>(5, Allele.Dominance.CODOMINANT));
        GenePair<Double> scaleLocus = new GenePair<>(new Allele<>(0.90 + (tier.getLevel() * 0.08), Allele.Dominance.CODOMINANT), new Allele<>(1.0, Allele.Dominance.CODOMINANT));
        GenePair<String> plumageLocus = new GenePair<>(new Allele<>(RoosterGenetics.PLUMAGE_VARIANTS[rng.nextInt(RoosterGenetics.PLUMAGE_VARIANTS.length)], Allele.Dominance.CODOMINANT), new Allele<>(RoosterGenetics.PLUMAGE_VARIANTS[rng.nextInt(RoosterGenetics.PLUMAGE_VARIANTS.length)], Allele.Dominance.CODOMINANT));
        GenePair<String> combLocus = new GenePair<>(new Allele<>(RoosterGenetics.COMB_STYLES[rng.nextInt(RoosterGenetics.COMB_STYLES.length)], Allele.Dominance.CODOMINANT), new Allele<>("CRESTA_SIMPLE", Allele.Dominance.CODOMINANT));

        RoosterGenetics gen = new RoosterGenetics(hpLocus, atkLocus, defLocus, spdLocus, evaLocus, accLocus, aggLocus, scaleLocus, plumageLocus, combLocus, tier.getTraits());
        String roosterName = RoosterNameGenerator.generateName(rng);

        RoosterData data = new RoosterData(UUID.randomUUID(), trainerId, roosterName, gen);
        data.setLevel(tier.getLevel());
        return data;
    }

    public UUID getTrainerId() { return trainerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TrainerTier getTier() { return tier; }
    public void setTier(TrainerTier tier) { this.tier = tier; }
    public String getSkinName() { return skinName; }
    public void setSkinName(String skinName) { this.skinName = PlayerNpc.resolveSkinName(skinName); }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location != null ? location.clone() : null; }
    public RoosterData getRooster() { return rooster; }
    public void setRooster(RoosterData rooster) { this.rooster = rooster; }
    public int getWins() { return wins; }
    public void addWin() { this.wins++; }
    public int getLosses() { return losses; }
    public void addLoss() { this.losses++; }

    public void serializeTo(ConfigurationSection sec) {
        sec.set("trainerId", trainerId.toString());
        sec.set("name", name);
        sec.set("tier", tier.name());
        sec.set("skinName", skinName);
        sec.set("wins", wins);
        sec.set("losses", losses);
        if (location != null && location.getWorld() != null) {
            sec.set("world", location.getWorld().getName());
            sec.set("x", location.getX());
            sec.set("y", location.getY());
            sec.set("z", location.getZ());
            sec.set("yaw", location.getYaw());
            sec.set("pitch", location.getPitch());
        }
        if (rooster != null) {
            ConfigurationSection rSec = sec.createSection("rooster");
            rooster.serializeTo(rSec);
        }
    }

    public static RoosterTrainer deserializeFrom(ConfigurationSection sec) {
        UUID id = UUID.fromString(sec.getString("trainerId", UUID.randomUUID().toString()));
        String name = sec.getString("name", "Entrenador Gallero");
        TrainerTier tier = TrainerTier.fromString(sec.getString("tier", "APRENDIZ"));
        String skin = PlayerNpc.resolveSkinName(sec.getString("skinName", "random"));

        Location loc = null;
        String wName = sec.getString("world");
        if (wName != null) {
            World w = Bukkit.getWorld(wName);
            if (w != null) {
                double x = sec.getDouble("x");
                double y = sec.getDouble("y");
                double z = sec.getDouble("z");
                float yaw = (float) sec.getDouble("yaw", 0.0);
                float pitch = (float) sec.getDouble("pitch", 0.0);
                loc = new Location(w, x, y, z, yaw, pitch);
            }
        }

        RoosterTrainer trainer = new RoosterTrainer(id, name, tier, skin, loc);
        trainer.wins = sec.getInt("wins", 0);
        trainer.losses = sec.getInt("losses", 0);

        ConfigurationSection rSec = sec.getConfigurationSection("rooster");
        if (rSec != null) {
            trainer.rooster = RoosterData.deserializeFrom(rSec);
        }

        return trainer;
    }
}
