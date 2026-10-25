package com.dark.roostergenics.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper centralizado de MiniMessage para renderización visual unificada.
 * Todos los nombres/lores de ítems y GUIs DEBEN pasar por esta clase.
 * Uso exclusivo de gradientes hex, cero legacy § o colores base de Minecraft.
 */
public final class Mm {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Mm() {}

    /**
     * Parsea un string MiniMessage a Component.
     */
    public static Component text(String miniMsg) {
        return MM.deserialize(miniMsg);
    }

    /**
     * Parsea con <!italic> forzado al inicio — obligatorio para display names y lore de ítems custom.
     */
    public static Component noItalic(String miniMsg) {
        return MM.deserialize("<!italic>" + miniMsg);
    }

    /**
     * Genera una lista de Components de lore, cada línea prefijada con <!italic>.
     */
    public static List<Component> lore(String... lines) {
        List<Component> list = new ArrayList<>(lines.length);
        for (String line : lines) {
            list.add(MM.deserialize("<!italic>" + line));
        }
        return list;
    }

    /**
     * Genera una lista de Components de lore desde una List de strings.
     */
    public static List<Component> lore(List<String> lines) {
        List<Component> list = new ArrayList<>(lines.size());
        for (String line : lines) {
            list.add(MM.deserialize("<!italic>" + line));
        }
        return list;
    }

    /**
     * Transforma texto a Small Caps (minúsculas estilizadas).
     * Las mayúsculas (A-Z) y símbolos permanecen intactos para efecto RPG jerárquico.
     */
    public static String toSmallCaps(String text) {
        if (text == null) return "";
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            chars[i] = switch (c) {
                case 'a', 'á' -> 'ᴀ';
                case 'b' -> 'ʙ';
                case 'c' -> 'ᴄ';
                case 'd' -> 'ᴅ';
                case 'e', 'é' -> 'ᴇ';
                case 'f' -> 'ꜰ';
                case 'g' -> 'ɢ';
                case 'h' -> 'ʜ';
                case 'i', 'í' -> 'ɪ';
                case 'j' -> 'ᴊ';
                case 'k' -> 'ᴋ';
                case 'l' -> 'ʟ';
                case 'm' -> 'ᴍ';
                case 'n', 'ñ' -> 'ɴ';
                case 'o', 'ó' -> 'ᴏ';
                case 'p' -> 'ᴘ';
                case 'q' -> 'ǫ';
                case 'r' -> 'ʀ';
                case 's' -> 's';
                case 't' -> 'ᴛ';
                case 'u', 'ú' -> 'ᴜ';
                case 'v' -> 'ᴠ';
                case 'w' -> 'ᴡ';
                case 'x' -> 'x';
                case 'y' -> 'ʏ';
                case 'z' -> 'ᴢ';
                default -> c;
            };
        }
        return new String(chars);
    }
}
