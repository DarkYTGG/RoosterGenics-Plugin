package com.dark.roostergenics.items;

import com.dark.roostergenics.util.Mm;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo de objetos equipables (Held Items) de combate RoosterGenics para gallos de combate.
 * Renderizado exclusivo con MiniMessage + gradientes hex + Small Caps.
 * PROHIBIDO el uso de '§' o ChatColor legacy.
 */
public enum RoosterHeldItem {

    BANDA_FOCUS(
            "Banda Focus",
            "banda_focus",
            Material.TOTEM_OF_UNDYING,
            // ── Display Name (gradiente dorado ámbar) ──
            "<bold><gradient:#FFD700:#FFA500>Banda Focus</gradient></bold>",
            // ── Quote (gradiente cálido sutil) ──
            List.of(
                    "<gradient:#FFD700:#E8A317>\"Una cinta resistente que otorga al gallo</gradient>",
                    "<gradient:#FFD700:#E8A317>una tenacidad inquebrantable ante el golpe final\"</gradient>"
            ),
            // ── Ability Name ──
            "Resistencia Extrema",
            // ── Ability gradient colors (start, end) ──
            "#FFD700", "#FFA500",
            // ── Ability Description ──
            List.of(
                    "<#B0B0B0>Si el gallo tiene 100% de vida y recibe un golpe mortal,",
                    "<#B0B0B0>sobrevive milagrosamente con 1 punto de salud."
            ),
            // ── Stats lines ──
            List.of(
                    "<gradient:#4ADE80:#22C55E>+1 Resurrección de Emergencia (1 HP)</gradient>",
                    "<gradient:#FFD700:#FFA500>◆ Consumo: 1 uso garantizado por combate</gradient>"
            ),
            // ── "Al equipar en el Palenque:" color ──
            "#FFD700", "#E8A317"
    ),

    RESTOS(
            "Restos de Trigo",
            "restos",
            Material.HAY_BLOCK,
            "<bold><gradient:#7CFC00:#32CD32>Restos de Trigo Dorado</gradient></bold>",
            List.of(
                    "<gradient:#7CFC00:#32CD32>\"Un selecto puñado de semillas doradas que</gradient>",
                    "<gradient:#7CFC00:#32CD32>revitaliza el plumaje durante el combate\"</gradient>"
            ),
            "Regeneración Continua",
            "#7CFC00", "#32CD32",
            List.of(
                    "<#B0B0B0>Cada 4 segundos de combate en el palenque, el gallo",
                    "<#B0B0B0>restaura un 4% de su salud máxima automáticamente."
            ),
            List.of(
                    "<gradient:#4ADE80:#22C55E>+4% Salud Máxima cada 4 segundos</gradient>",
                    "<gradient:#15803D:#166534>◆ Regeneración pasiva permanente</gradient>"
            ),
            "#7CFC00", "#32CD32"
    ),

    VIDASFERA(
            "Vidasfera de Rubí",
            "vidasfera",
            Material.ENDER_EYE,
            "<bold><gradient:#FF4444:#CC0000>Vidasfera de Rubí</gradient></bold>",
            List.of(
                    "<gradient:#FF4444:#CC0000>\"Una gema carmesí que desata un poder voraz a</gradient>",
                    "<gradient:#FF4444:#CC0000>cambio de consumir la vitalidad de su portador\"</gradient>"
            ),
            "Poder Desbordante",
            "#FF4444", "#CC0000",
            List.of(
                    "<#B0B0B0>Aumenta la fuerza de todos los picotazos en un 30%, pero",
                    "<#B0B0B0>el gallo pierde un 5% de su salud actual al conectar."
            ),
            List.of(
                    "<gradient:#FF4444:#CC0000>+30% Potencia de Ataque Directo</gradient>",
                    "<gradient:#8B0000:#660000>-5% Salud Actual por Golpe Conectado</gradient>"
            ),
            "#FF4444", "#CC0000"
    ),

    CASCO_DENTADO(
            "Espuela Dentada",
            "casco_dentado",
            Material.IRON_HELMET,
            "<bold><gradient:#FFB347:#FF8C00>Espuela Dentada</gradient></bold>",
            List.of(
                    "<gradient:#FFB347:#FF8C00>\"Afiladas espuelas forjadas de metal que castigan</gradient>",
                    "<gradient:#FFB347:#FF8C00>ferozmente al rival ante cada contacto físico\"</gradient>"
            ),
            "Contragolpe Punzante",
            "#FFB347", "#FF8C00",
            List.of(
                    "<#B0B0B0>Al recibir un impacto directo en combate, devuelve",
                    "<#B0B0B0>reactivamente un 20% del daño sufrido al agresor."
            ),
            List.of(
                    "<gradient:#FFB347:#FF8C00>+20% Daño Reflejado (Espinas)</gradient>",
                    "<gradient:#DAA520:#B8860B>◆ Reactivo a todo impacto directo</gradient>"
            ),
            "#FFB347", "#FF8C00"
    ),

    PERISCOPIO(
            "Periscopio de Halcón",
            "periscopio",
            Material.SPYGLASS,
            "<bold><gradient:#00CED1:#4169E1>Periscopio de Halcón</gradient></bold>",
            List.of(
                    "<gradient:#00CED1:#4169E1>\"Un lente telescópico cristalino que revela</gradient>",
                    "<gradient:#00CED1:#4169E1>los puntos más vulnerables de la coraza rival\"</gradient>"
            ),
            "Precisión Letal",
            "#00CED1", "#4169E1",
            List.of(
                    "<#B0B0B0>Otorga una puntería telescópica que eleva la probabilidad",
                    "<#B0B0B0>de golpes críticos y amplifica severamente su daño."
            ),
            List.of(
                    "<gradient:#00CED1:#4169E1>+25% Probabilidad de Golpe Crítico</gradient>",
                    "<gradient:#1E90FF:#0000CD>+1.8x Daño Multiplicador Crítico</gradient>"
            ),
            "#00CED1", "#4169E1"
    ),

    CHALECO_ASALTO(
            "Chaleco Blindado",
            "chaleco_asalto",
            Material.CHAINMAIL_CHESTPLATE,
            "<bold><gradient:#6495ED:#4B0082>Chaleco Asalto Blindado</gradient></bold>",
            List.of(
                    "<gradient:#6495ED:#4B0082>\"Un blindaje de tejido compuesto que amortigua</gradient>",
                    "<gradient:#6495ED:#4B0082>los impactos más brutales y lacerantes de la arena\"</gradient>"
            ),
            "Blindaje Pesado",
            "#6495ED", "#4B0082",
            List.of(
                    "<#B0B0B0>Refuerza la coraza corporal del ejemplar, reduciendo",
                    "<#B0B0B0>el daño sufrido ante picotazos y embestidas rivales."
            ),
            List.of(
                    "<gradient:#6495ED:#4B0082>+30% Defensa Base del Gallo</gradient>",
                    "<gradient:#483D8B:#2E0854>◆ Mitigación pasiva de daño físico</gradient>"
            ),
            "#6495ED", "#4B0082"
    ),

    BAYA_ZIDRA(
            "Baya Zidra de Granja",
            "baya_zidra",
            Material.SWEET_BERRIES,
            "<bold><gradient:#FFD700:#FF6347>Baya Zidra de Granja</gradient></bold>",
            List.of(
                    "<gradient:#FFD700:#FF6347>\"Un fruto silvestre jugoso y nutritivo que devuelve</gradient>",
                    "<gradient:#FFD700:#FF6347>las fuerzas en los momentos de mayor peligro\"</gradient>"
            ),
            "Alivio de Emergencia",
            "#FFD700", "#FF6347",
            List.of(
                    "<#B0B0B0>Cuando la salud desciende por debajo del 50% en combate,",
                    "<#B0B0B0>consume la baya y restaura al instante un 25% de salud."
            ),
            List.of(
                    "<gradient:#4ADE80:#22C55E>+25% Salud Máxima Inmediata (<50% HP)</gradient>",
                    "<gradient:#FFD700:#FF8C00>◆ Consumo: 1 uso de curación por combate</gradient>"
            ),
            "#FFD700", "#FF6347"
    ),

    PLUMA_VELOZ(
            "Pluma del Viento",
            "pluma_veloz",
            Material.FEATHER,
            "<bold><gradient:#E0E0E0:#B0C4DE>Pluma del Viento</gradient></bold>",
            List.of(
                    "<gradient:#E0E0E0:#B0C4DE>\"Una pluma etérea que canaliza ráfagas de aire</gradient>",
                    "<gradient:#E0E0E0:#B0C4DE>para acelerar cada movimiento del combatiente\"</gradient>"
            ),
            "Impulso Aerodinámico",
            "#E0E0E0", "#B0C4DE",
            List.of(
                    "<#B0B0B0>Otorga una cadencia de ataque frenética, reduciendo el",
                    "<#B0B0B0>enfriamiento entre picotazos a cambio de ligereza defensiva."
            ),
            List.of(
                    "<gradient:#E0E0E0:#B0C4DE>+20% Velocidad de Ataque (-20% Cooldown)</gradient>",
                    "<gradient:#F87171:#DC2626>-10% Defensa Corporal</gradient>"
            ),
            "#E0E0E0", "#B0C4DE"
    ),

    CINTA_ELEGIDA(
            "Cinta Elegida de Campeón",
            "cinta_elegida",
            Material.LEAD,
            "<bold><gradient:#FF4500:#FF8C00>Cinta Elegida de Campeón</gradient></bold>",
            List.of(
                    "<gradient:#FF4500:#FF8C00>\"Una faja sagrada de cuero rojo que concentra</gradient>",
                    "<gradient:#FF4500:#FF8C00>todo el poder en el primer golpe de combate\"</gradient>"
            ),
            "Impacto Concentrado",
            "#FF4500", "#FF8C00",
            List.of(
                    "<#B0B0B0>El primer picotazo conectado en combate inflige un 40% más",
                    "<#B0B0B0>de daño masivo, pero reduce ligeramente la precisión posterior."
            ),
            List.of(
                    "<gradient:#4ADE80:#22C55E>+40% Daño en el Primer Impacto</gradient>",
                    "<gradient:#F87171:#DC2626>-8% Precisión en Golpes Siguientes</gradient>"
            ),
            "#FF4500", "#FF8C00"
    ),

    CASCABEL_CONCHA(
            "Cascabel Marino de Concha",
            "cascabel_concha",
            Material.NAUTILUS_SHELL,
            "<bold><gradient:#00FFFF:#008080>Cascabel Marino de Concha</gradient></bold>",
            List.of(
                    "<gradient:#00FFFF:#008080>\"Una caracola marina que resuena con un eco acuático</gradient>",
                    "<gradient:#00FFFF:#008080>sanando al portador con cada golpe asestado\"</gradient>"
            ),
            "Drenaje Marino",
            "#00FFFF", "#008080",
            List.of(
                    "<#B0B0B0>Al conectar cualquier impacto directo contra el oponente,",
                    "<#B0B0B0>drena un 12% del daño infligido y lo convierte en salud propia."
            ),
            List.of(
                    "<gradient:#4ADE80:#22C55E>+12% Daño Convertido en Salud (Lifesteal)</gradient>",
                    "<gradient:#00CED1:#4682B4>◆ Drenaje continuo por impacto</gradient>"
            ),
            "#00FFFF", "#008080"
    ),

    BAYA_MELOC(
            "Baya Meloc Antídoto",
            "baya_meloc",
            Material.GLOW_BERRIES,
            "<bold><gradient:#FF69B4:#FF1493>Baya Meloc Antídoto</gradient></bold>",
            List.of(
                    "<gradient:#FF69B4:#FF1493>\"Una baya dulce de pulpa rosada con propiedades</gradient>",
                    "<gradient:#FF69B4:#FF1493>neutralizadoras contra las toxinas más letales\"</gradient>"
            ),
            "Purificación Instantánea",
            "#FF69B4", "#FF1493",
            List.of(
                    "<#B0B0B0>Si el gallo sufre envenenamiento en combate, consume",
                    "<#B0B0B0>la baya de inmediato purgando toda toxina residual."
            ),
            List.of(
                    "<gradient:#4ADE80:#22C55E>Purga Automática de Todo Veneno</gradient>",
                    "<gradient:#FF69B4:#C71585>◆ Consumo: 1 uso garantizado por combate</gradient>"
            ),
            "#FF69B4", "#FF1493"
    ),

    POLVORA_FENIX(
            "Pólvora Ígnea de Fénix",
            "polvora_fenix",
            Material.BLAZE_POWDER,
            "<bold><gradient:#FF8C00:#FF0000>Pólvora Ígnea de Fénix</gradient></bold>",
            List.of(
                    "<gradient:#FF8C00:#FF0000>\"Cenizas incandescentes que estallan en llamas</gradient>",
                    "<gradient:#FF8C00:#FF0000>cuando el portador recibe un daño devastador\"</gradient>"
            ),
            "Retaliación Ardiente",
            "#FF8C00", "#FF0000",
            List.of(
                    "<#B0B0B0>Al recibir un golpe que supere el 25% de la salud máxima,",
                    "<#B0B0B0>estalla en fuego quemando al agresor durante 4 segundos."
            ),
            List.of(
                    "<gradient:#FF4500:#B22222>Quemadura Reactiva de 4s al Recibir Golpe Fuerte</gradient>",
                    "<gradient:#FFA500:#FF8C00>◆ Activación por impacto masivo (>25% HP)</gradient>"
            ),
            "#FF8C00", "#FF0000"
    ),

    AMULETO_PLOMO(
            "Amuleto Pesado de Plomo",
            "amuleto_plomo",
            Material.NETHERITE_SCRAP,
            "<bold><gradient:#708090:#2F4F4F>Amuleto Pesado de Plomo</gradient></bold>",
            List.of(
                    "<gradient:#708090:#2F4F4F>\"Un pesado talismán forjado en plomo ancestral</gradient>",
                    "<gradient:#708090:#2F4F4F>que afianza las patas del luchador al cuadrilátero\"</gradient>"
            ),
            "Anclaje de Acero",
            "#708090", "#2F4F4F",
            List.of(
                    "<#B0B0B0>Otorga una firmeza colosal: anula todo empuje y concede",
                    "<#B0B0B0>inmunidad total a aturdimientos y desorientaciones en combate."
            ),
            List.of(
                    "<gradient:#4ADE80:#22C55E>Inmunidad Total a Empuje (Knockback Cero)</gradient>",
                    "<gradient:#708090:#2F4F4F>Inmunidad a Aturdimiento y Caídas</gradient>"
            ),
            "#708090", "#2F4F4F"
    ),

    SEMILLA_MILAGROSA(
            "Semilla Nutritiva Milagrosa",
            "semilla_milagrosa",
            Material.PUMPKIN_SEEDS,
            "<bold><gradient:#32CD32:#228B22>Semilla Nutritiva Milagrosa</gradient></bold>",
            List.of(
                    "<gradient:#32CD32:#228B22>\"Una semilla silvestre bendecida que amplifica</gradient>",
                    "<gradient:#32CD32:#228B22>la asimilación de nutrientes durante la batalla\"</gradient>"
            ),
            "Vigor Botánico",
            "#32CD32", "#228B22",
            List.of(
                    "<#B0B0B0>Amplifica un 50% cualquier efecto de regeneración o curación",
                    "<#B0B0B0>que reciba el gallo durante el desarrollo del combate."
            ),
            List.of(
                    "<gradient:#4ADE80:#22C55E>+50% Efectividad de Toda Curación/Regeneración</gradient>",
                    "<gradient:#2E8B57:#006400>◆ Sinergia máxima con Restos de Trigo y Bayas</gradient>"
            ),
            "#32CD32", "#228B22"
    );

    private final String displayName;
    private final String id;
    private final Material material;
    private final String miniMessageName;        // MiniMessage para display name del ItemStack
    private final List<String> quoteLines;       // MiniMessage para quote descriptiva
    private final String abilityName;            // Texto plano para Small Caps
    private final String abilityGradStart;       // Hex inicio del gradiente de la habilidad
    private final String abilityGradEnd;         // Hex fin del gradiente de la habilidad
    private final List<String> abilityDescLines; // MiniMessage para descripción de habilidad
    private final List<String> statLines;        // MiniMessage para estadísticas
    private final String equipGradStart;         // Hex inicio "Al equipar en el Palenque:"
    private final String equipGradEnd;           // Hex fin "Al equipar en el Palenque:"

    RoosterHeldItem(String displayName, String id, Material material,
                    String miniMessageName,
                    List<String> quoteLines,
                    String abilityName,
                    String abilityGradStart, String abilityGradEnd,
                    List<String> abilityDescLines,
                    List<String> statLines,
                    String equipGradStart, String equipGradEnd) {
        this.displayName = displayName;
        this.id = id;
        this.material = material;
        this.miniMessageName = miniMessageName;
        this.quoteLines = quoteLines;
        this.abilityName = abilityName;
        this.abilityGradStart = abilityGradStart;
        this.abilityGradEnd = abilityGradEnd;
        this.abilityDescLines = abilityDescLines;
        this.statLines = statLines;
        this.equipGradStart = equipGradStart;
        this.equipGradEnd = equipGradEnd;
    }

    // ── Getters ──

    public String getDisplayName() { return displayName; }
    public String getId() { return id; }
    public Material getMaterial() { return material; }

    /**
     * Devuelve el Component del display name renderizado con MiniMessage + <!italic>.
     */
    public Component getFormattedNameComponent() {
        return Mm.noItalic(miniMessageName);
    }

    /**
     * Construye la línea "Hᴀʙɪʟɪᴅᴀᴅ: Nᴏᴍʙʀᴇ" con gradiente hex y Small Caps.
     */
    public String getFormattedAbilityMM() {
        String smallCaps = Mm.toSmallCaps("Habilidad: " + abilityName);
        return "<bold><gradient:" + abilityGradStart + ":" + abilityGradEnd + ">" + smallCaps + "</gradient></bold>";
    }

    /**
     * Construye el lore completo como List<Component> con MiniMessage.
     * Cada línea lleva <!italic> para anular la italización default de Minecraft.
     */
    public List<Component> getLoreComponents() {
        return getLoreComponents(null);
    }

    public List<Component> getLoreComponents(String actionPromptMM) {
        List<Component> lore = new ArrayList<>();

        // Quote descriptiva
        for (String q : quoteLines) {
            lore.add(Mm.noItalic(q));
        }
        lore.add(Component.empty());

        // Habilidad: Small Caps + gradiente
        lore.add(Mm.noItalic(getFormattedAbilityMM()));

        // Descripción de la habilidad con los colores del ítem
        for (String desc : abilityDescLines) {
            String clean = desc.replace("<#B0B0B0>", "");
            lore.add(Mm.noItalic("<gradient:" + abilityGradStart + ":" + abilityGradEnd + ">" + clean + "</gradient>"));
        }
        lore.add(Component.empty());

        // "Al equipar en el Palenque:"
        lore.add(Mm.noItalic("<gradient:" + equipGradStart + ":" + equipGradEnd + ">Al equipar en el Palenque:</gradient>"));

        // Stats
        for (String s : statLines) {
            lore.add(Mm.noItalic(s));
        }

        // Acción opcional (ej. "Clic para equipar")
        if (actionPromptMM != null && !actionPromptMM.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Mm.noItalic(actionPromptMM));
        }

        return lore;
    }

    /**
     * Devuelve la descripción combinada como array de Strings planos (para /rg info).
     */
    public String[] getDescription() {
        List<String> combined = new ArrayList<>();
        combined.addAll(abilityDescLines);
        combined.addAll(statLines);
        return combined.toArray(new String[0]);
    }

    public static RoosterHeldItem fromId(String id) {
        if (id == null) return null;
        for (RoosterHeldItem item : values()) {
            if (item.id.equalsIgnoreCase(id) || item.name().equalsIgnoreCase(id)) {
                return item;
            }
        }
        return null;
    }
}