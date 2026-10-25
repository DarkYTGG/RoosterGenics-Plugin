package com.dark.roostergenics.model;

import com.dark.roostergenics.genetics.RoosterGenetics;
import com.dark.roostergenics.genetics.RoosterTrait;
import com.dark.roostergenics.items.RoosterHeldItem;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Registro integral de un gallo virtual (almacenamiento desacoplado sin entidades persistentes en el mundo).
 */
public class RoosterData {

    private final UUID roosterId;
    private UUID ownerId;
    private String name;
    private RoosterGenetics genetics;
    private RoosterHealthStatus healthStatus;
    private long knockedOutUntilMillis;

    private int wins;
    private int losses;
    private int draws;
    private int consecutiveLosses;
    private final Set<RoosterInjury> temporaryInjuries = EnumSet.noneOf(RoosterInjury.class);

    public static final int MAX_LEVEL = 50;

    private int level;
    private int experience;
    private int evHp;
    private int evAttack;
    private int evDefense;
    private int evSpeed;

    private UUID fatherId;
    private UUID motherId;
    private int generation;
    private double inbreedingCoefficient;
    private long createdTimestamp;
    private RoosterHeldItem heldItem;
    private int bondLevel;

    public RoosterData(UUID roosterId, UUID ownerId, String name, RoosterGenetics genetics) {
        this.roosterId = roosterId != null ? roosterId : UUID.randomUUID();
        this.ownerId = ownerId;
        this.name = name != null ? name : "Gallo de Combate";
        this.genetics = genetics;
        this.healthStatus = RoosterHealthStatus.HEALTHY;
        this.knockedOutUntilMillis = 0L;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.level = 1;
        this.experience = 0;
        this.evHp = 0;
        this.evAttack = 0;
        this.evDefense = 0;
        this.evSpeed = 0;
        this.fatherId = null;
        this.motherId = null;
        this.generation = 1;
        this.inbreedingCoefficient = 0.0;
        this.createdTimestamp = System.currentTimeMillis();
        this.heldItem = null;
        this.bondLevel = 50;
    }

    public UUID getRoosterId() {
        return roosterId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoosterGenetics getGenetics() {
        return genetics;
    }

    public void setGenetics(RoosterGenetics genetics) {
        this.genetics = genetics;
    }

    public RoosterHealthStatus getHealthStatus() {
        checkKnockoutExpiry();
        return healthStatus;
    }

    public void setHealthStatus(RoosterHealthStatus healthStatus) {
        this.healthStatus = healthStatus;
    }

    public long getKnockedOutUntilMillis() {
        return knockedOutUntilMillis;
    }

    public void setKnockedOutUntilMillis(long knockedOutUntilMillis) {
        this.knockedOutUntilMillis = knockedOutUntilMillis;
        if (knockedOutUntilMillis > System.currentTimeMillis()) {
            this.healthStatus = RoosterHealthStatus.KNOCKED_OUT;
        }
    }

    public void knockOut(long durationMillis) {
        this.knockedOutUntilMillis = System.currentTimeMillis() + durationMillis;
        this.healthStatus = RoosterHealthStatus.KNOCKED_OUT;
    }

    public void reviveOrHeal() {
        this.knockedOutUntilMillis = 0L;
        this.healthStatus = RoosterHealthStatus.HEALTHY;
    }

    public boolean isKnockedOut() {
        checkKnockoutExpiry();
        return healthStatus == RoosterHealthStatus.KNOCKED_OUT;
    }

    public boolean isDead() {
        return healthStatus == RoosterHealthStatus.DEAD;
    }

    public boolean isUsableForCombat() {
        checkKnockoutExpiry();
        return healthStatus == RoosterHealthStatus.HEALTHY || healthStatus == RoosterHealthStatus.TIRED;
    }

    public boolean isSterile() {
        return genetics.hasTrait(RoosterTrait.ESTERILIDAD);
    }

    private void checkKnockoutExpiry() {
        if (healthStatus == RoosterHealthStatus.KNOCKED_OUT) {
            if (knockedOutUntilMillis > 0 && System.currentTimeMillis() >= knockedOutUntilMillis) {
                this.healthStatus = RoosterHealthStatus.HEALTHY;
                this.knockedOutUntilMillis = 0L;
            }
        }
    }

    public long getRemainingKnockoutSeconds() {
        checkKnockoutExpiry();
        if (healthStatus != RoosterHealthStatus.KNOCKED_OUT) return 0L;
        long diff = knockedOutUntilMillis - System.currentTimeMillis();
        return Math.max(0L, diff / 1000L);
    }

    public int getWins() { return wins; }
    public void addWin() {
        this.wins++;
        this.consecutiveLosses = 0;
    }

    public int getLosses() { return losses; }
    public void addLoss() {
        this.losses++;
        this.consecutiveLosses++;
    }

    public int getConsecutiveLosses() { return consecutiveLosses; }
    public void setConsecutiveLosses(int consecutiveLosses) { this.consecutiveLosses = Math.max(0, consecutiveLosses); }
    public void resetConsecutiveLosses() { this.consecutiveLosses = 0; }
    public void incrementConsecutiveLosses() { this.consecutiveLosses++; }

    public Set<RoosterInjury> getTemporaryInjuries() { return temporaryInjuries; }
    public boolean hasInjuries() { return !temporaryInjuries.isEmpty(); }
    public boolean hasInjury(RoosterInjury injury) { return temporaryInjuries.contains(injury); }
    public boolean addInjury(RoosterInjury injury) { return temporaryInjuries.add(injury); }
    public boolean removeInjury(RoosterInjury injury) { return temporaryInjuries.remove(injury); }
    public void clearInjuries() { temporaryInjuries.clear(); }

    public int getDraws() { return draws; }
    public void addDraw() { this.draws++; }

    public int getTotalFights() { return wins + losses + draws; }

    public double getWinRate() {
        int total = getTotalFights();
        if (total == 0) return 0.0;
        return ((double) wins / total) * 100.0;
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, Math.min(MAX_LEVEL, level)); }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }

    public int getXpForNextLevel() {
        return getXpForLevel(this.level);
    }

    public static int getXpForLevel(int lvl) {
        if (lvl >= MAX_LEVEL) return 0;
        return 100 + (lvl - 1) * 65;
    }

    public int addExperience(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        this.experience += amount;
        int levelsGained = 0;
        while (level < MAX_LEVEL && experience >= getXpForNextLevel()) {
            int needed = getXpForNextLevel();
            experience -= needed;
            level++;
            levelsGained++;
        }
        if (level >= MAX_LEVEL) {
            experience = 0;
        }
        return levelsGained;
    }

    public int getEvHp() { return evHp; }
    public void addEvHp(int amount) { this.evHp = Math.min(100, this.evHp + amount); }

    public int getEvAttack() { return evAttack; }
    public void addEvAttack(int amount) { this.evAttack = Math.min(100, this.evAttack + amount); }

    public int getEvDefense() { return evDefense; }
    public void addEvDefense(int amount) { this.evDefense = Math.min(100, this.evDefense + amount); }

    public int getEvSpeed() { return evSpeed; }
    public void addEvSpeed(int amount) { this.evSpeed = Math.min(100, this.evSpeed + amount); }

    public int getTotalEvs() {
        return evHp + evAttack + evDefense + evSpeed;
    }

    public UUID getFatherId() { return fatherId; }
    public void setFatherId(UUID fatherId) { this.fatherId = fatherId; }

    public UUID getMotherId() { return motherId; }
    public void setMotherId(UUID motherId) { this.motherId = motherId; }

    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }

    public double getInbreedingCoefficient() { return inbreedingCoefficient; }
    public void setInbreedingCoefficient(double inbreedingCoefficient) { this.inbreedingCoefficient = inbreedingCoefficient; }

    public long getCreatedTimestamp() { return createdTimestamp; }

    public RoosterHeldItem getHeldItem() { return heldItem; }
    public void setHeldItem(RoosterHeldItem heldItem) { this.heldItem = heldItem; }
    public boolean hasHeldItem() { return heldItem != null; }

    public int getBondLevel() { return bondLevel; }
    public void setBondLevel(int bondLevel) { this.bondLevel = Math.max(0, Math.min(100, bondLevel)); }

    public RoosterCombatStats calculateCombatStats() {
        return new RoosterCombatStats(this);
    }

    public void serializeTo(ConfigurationSection sec) {
        sec.set("roosterId", roosterId.toString());
        sec.set("ownerId", ownerId != null ? ownerId.toString() : null);
        sec.set("name", name);
        sec.set("healthStatus", healthStatus.name());
        sec.set("knockedOutUntilMillis", knockedOutUntilMillis);
        sec.set("wins", wins);
        sec.set("losses", losses);
        sec.set("draws", draws);
        sec.set("level", level);
        sec.set("experience", experience);
        sec.set("evHp", evHp);
        sec.set("evAttack", evAttack);
        sec.set("evDefense", evDefense);
        sec.set("evSpeed", evSpeed);
        sec.set("fatherId", fatherId != null ? fatherId.toString() : null);
        sec.set("motherId", motherId != null ? motherId.toString() : null);
        sec.set("generation", generation);
        sec.set("inbreedingCoefficient", inbreedingCoefficient);
        sec.set("createdTimestamp", createdTimestamp);
        sec.set("heldItem", heldItem != null ? heldItem.name() : null);
        sec.set("bondLevel", bondLevel);
        sec.set("consecutiveLosses", consecutiveLosses);

        List<String> injList = new ArrayList<>();
        for (RoosterInjury inj : temporaryInjuries) {
            injList.add(inj.name());
        }
        sec.set("injuries", injList);

        ConfigurationSection genSec = sec.createSection("genetics");
        genetics.serializeTo(genSec);
    }

    public static RoosterData deserializeFrom(ConfigurationSection sec) {
        UUID rId = UUID.fromString(sec.getString("roosterId"));
        String oStr = sec.getString("ownerId");
        UUID oId = oStr != null ? UUID.fromString(oStr) : null;
        String name = sec.getString("name", "Gallo");

        ConfigurationSection genSec = sec.getConfigurationSection("genetics");
        RoosterGenetics gen = RoosterGenetics.deserializeFrom(genSec);

        RoosterData data = new RoosterData(rId, oId, name, gen);
        try {
            data.healthStatus = RoosterHealthStatus.valueOf(sec.getString("healthStatus", "HEALTHY"));
        } catch (Exception e) {
            data.healthStatus = RoosterHealthStatus.HEALTHY;
        }
        data.knockedOutUntilMillis = sec.getLong("knockedOutUntilMillis", 0L);
        data.wins = sec.getInt("wins", 0);
        data.losses = sec.getInt("losses", 0);
        data.consecutiveLosses = sec.getInt("consecutiveLosses", 0);
        data.draws = sec.getInt("draws", 0);
        data.level = Math.max(1, Math.min(MAX_LEVEL, sec.getInt("level", 1)));
        data.experience = Math.max(0, sec.getInt("experience", 0));
        data.evHp = sec.getInt("evHp", 0);
        data.evAttack = sec.getInt("evAttack", 0);
        data.evDefense = sec.getInt("evDefense", 0);
        data.evSpeed = sec.getInt("evSpeed", 0);
        data.bondLevel = Math.max(0, Math.min(100, sec.getInt("bondLevel", 50)));

        List<String> savedInjuries = sec.getStringList("injuries");
        if (savedInjuries != null) {
            for (String s : savedInjuries) {
                try {
                    data.addInjury(RoosterInjury.valueOf(s));
                } catch (Exception ignored) {}
            }
        }

        String fStr = sec.getString("fatherId");
        if (fStr != null && !fStr.isEmpty()) data.fatherId = UUID.fromString(fStr);

        String mStr = sec.getString("motherId");
        if (mStr != null && !mStr.isEmpty()) data.motherId = UUID.fromString(mStr);

        data.generation = sec.getInt("generation", 1);
        data.inbreedingCoefficient = sec.getDouble("inbreedingCoefficient", 0.0);
        data.createdTimestamp = sec.getLong("createdTimestamp", System.currentTimeMillis());

        String itemStr = sec.getString("heldItem");
        if (itemStr != null && !itemStr.isEmpty()) {
            data.heldItem = RoosterHeldItem.fromId(itemStr);
        }

        data.checkKnockoutExpiry();
        return data;
    }
}
