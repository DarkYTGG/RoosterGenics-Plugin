package com.dark.roostergenics.training;

import com.dark.roostergenics.model.RoosterData;
import com.dark.roostergenics.model.RoosterHealthStatus;
import com.dark.roostergenics.storage.RoosterRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de entrenamiento de atributos de esfuerzo (EVs) para gallos de combate.
 */
public class RoosterTrainingManager {

    public static final int MAX_SINGLE_EV = 100;
    public static final int MAX_TOTAL_EVS = 250;
    public static final int EV_GAIN_PER_SESSION = 3;
    public static final long COOLDOWN_MILLIS = 60 * 1000L; // 1 minuto de fatiga entre sesiones

    private final RoosterRepository repository;
    private final Map<UUID, Long> trainingCooldowns = new ConcurrentHashMap<>();

    public RoosterTrainingManager(RoosterRepository repository) {
        this.repository = repository;
    }

    public void resetCooldown(UUID roosterId) {
        trainingCooldowns.remove(roosterId);
    }

    public void resetAllCooldowns() {
        trainingCooldowns.clear();
    }

    public enum TrainingType {
        HP("Nutrición de Resistencia (HP)", NamedTextColor.GREEN),
        ATTACK("Golpeo a Maniquí (Ataque)", NamedTextColor.RED),
        DEFENSE("Coraza de Espolón (Defensa)", NamedTextColor.BLUE),
        SPEED("Cardio y Reflejos (Velocidad)", NamedTextColor.YELLOW);

        private final String displayName;
        private final NamedTextColor color;

        TrainingType(String displayName, NamedTextColor color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public NamedTextColor getColor() { return color; }
    }

    public boolean train(Player player, RoosterData rooster, TrainingType type) {
        if (rooster == null) {
            player.sendMessage(Component.text("No se encontró ningún gallo para entrenar.", NamedTextColor.RED));
            return false;
        }

        if (rooster.isKnockedOut() || rooster.isDead()) {
            player.sendMessage(Component.text("¡Tu gallo está noqueado o indispuesto! Déjalo descansar.", NamedTextColor.RED));
            return false;
        }

        UUID roosterId = rooster.getRoosterId();
        long now = System.currentTimeMillis();
        long lastTrained = trainingCooldowns.getOrDefault(roosterId, 0L);

        if (now - lastTrained < COOLDOWN_MILLIS) {
            long remainingSeconds = (COOLDOWN_MILLIS - (now - lastTrained)) / 1000L;
            player.sendMessage(Component.text("Tu gallo está fatigado por el entrenamiento. Espera " + remainingSeconds + "s.", NamedTextColor.YELLOW));
            return false;
        } else if (lastTrained != 0L) {
            trainingCooldowns.remove(roosterId);
            if (trainingCooldowns.size() > 500) {
                trainingCooldowns.entrySet().removeIf(e -> now - e.getValue() >= COOLDOWN_MILLIS);
            }
        }

        if (rooster.getTotalEvs() >= MAX_TOTAL_EVS) {
            player.sendMessage(Component.text("¡Este ejemplar ya ha alcanzado el límite máximo total de esfuerzo (" + MAX_TOTAL_EVS + " EVs)!", NamedTextColor.RED));
            return false;
        }

        boolean success = false;
        switch (type) {
            case HP -> {
                if (rooster.getEvHp() < MAX_SINGLE_EV) {
                    rooster.addEvHp(EV_GAIN_PER_SESSION);
                    success = true;
                }
            }
            case ATTACK -> {
                if (rooster.getEvAttack() < MAX_SINGLE_EV) {
                    rooster.addEvAttack(EV_GAIN_PER_SESSION);
                    success = true;
                }
            }
            case DEFENSE -> {
                if (rooster.getEvDefense() < MAX_SINGLE_EV) {
                    rooster.addEvDefense(EV_GAIN_PER_SESSION);
                    success = true;
                }
            }
            case SPEED -> {
                if (rooster.getEvSpeed() < MAX_SINGLE_EV) {
                    rooster.addEvSpeed(EV_GAIN_PER_SESSION);
                    success = true;
                }
            }
        }

        if (!success) {
            player.sendMessage(Component.text("Este atributo ya ha alcanzado su máximo (" + MAX_SINGLE_EV + " EVs).", NamedTextColor.RED));
            return false;
        }

        trainingCooldowns.put(roosterId, now);
        rooster.setHealthStatus(RoosterHealthStatus.HEALTHY);
        repository.saveRooster(rooster);

        // Feedback inmersivo
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.0, 0), 12, 0.4, 0.4, 0.4, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        player.sendMessage(Component.text("¡Entrenamiento completado! ", NamedTextColor.GREEN)
                .append(Component.text(rooster.getName(), NamedTextColor.GOLD))
                .append(Component.text(" ha mejorado en ", NamedTextColor.GREEN))
                .append(Component.text(type.getDisplayName(), type.getColor()))
                .append(Component.text(" (+ " + EV_GAIN_PER_SESSION + " EVs).", NamedTextColor.GREEN)));

        return true;
    }
}
