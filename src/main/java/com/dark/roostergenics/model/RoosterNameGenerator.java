package com.dark.roostergenics.model;

import java.util.List;
import java.util.Random;

/**
 * Generador de nombres aleatorios estrafalarios, graciosos y rudos de gallos RoosterGenics.
 */
public class RoosterNameGenerator {

    private static final List<String> UNIQUE_NAMES = List.of(
            "Barnaby", "Pickles", "T-Bone", "Nugget", "Cluck Norris", "Sir Peckalot", "Waffles", "Spike",
            "Chico", "Buster", "Chucky", "Pancho", "Mefisto", "Tito", "Boris", "Goliath",
            "Rusty", "Fang", "Pip", "Mojo", "Tyson", "Kiko", "Cheto", "Bruno",
            "Zeus", "Thor", "Nacho", "Panko", "Sardina", "Frijol", "Mordisco", "Gordo",
            "Chispita", "Pachón", "Bambam", "Chicharito", "Paco", "Margarito", "Calamardo", "Satanás",
            "Gutsy", "Scrapper", "Bandit", "Lucifer", "Shadow", "Rascal", "Gizmo", "Porky",
            "Macarrón", "Patitas", "Calígula", "Drácula", "Gárgola", "Tiburón", "Trompitas", "Pólvora",
            "Cascarón", "Dinamita", "Chamusco", "Habanero", "Jalapeño", "Taquito", "Burrito", "Torbellino",
            "Calavera", "Veneno", "Toxina", "Plumífero", "Crestudo", "Espolón", "Fénix", "Rambo",
            "Terminator", "Depredador", "Gladiador", "Vikingo", "Espartano", "Ninja", "Samurái", "César",
            "Atila", "Genghis", "Napoleón", "Hércules", "Aquiles", "Perseo", "Ulises", "Loki",
            "Odín", "Ares", "Hades", "Poseidón", "Cronos", "Titán", "Atlas", "Apolo",
            "Hermes", "Vulcano", "Marte", "Júpiter", "Pegaso", "Minotauro", "Cerbero", "Grifo",
            "Basilisco", "Wyvern", "Dragón", "Gólem", "Arlequín", "Bandido", "Pirata", "Corsario",
            "Filibustero", "Almirante", "Comodoro", "Sargento", "Comandante", "Patrón", "Sensei", "Nigromante"
    );

    private static final List<String> TITLED_NAMES = List.of(
            "El Destripador", "Don Picotazo", "Pata Chueca", "Pico Mocho", "El Verdugo", "Coronel Sanders",
            "El Matador", "Espolón Letal", "Garras de Fuego", "Sangre Brava", "Ojo Loco", "Gallito Ciego",
            "Cresta Roja", "Pelo en Pecho", "Malas Pulgas", "El Chupacabras", "Terror Nocturno", "Furia Roja",
            "El Sicario", "Pico Fino", "Malasuerte", "El Pistolero", "Despiadado", "Pico Plateado",
            "El Caudillo", "Pluma Brava", "Tormento", "Sombra Veloz", "El Fantasma", "Pata de Palo",
            "Pico de Acero", "El Rompehuesos", "Traga Maíz", "El Carnicero", "El Tuerto", "Rompecaras",
            "Calamidad", "El Gallero", "El Apache", "Relámpago Negro", "Tragabalas", "Machete",
            "El Alacrán", "Pico Venenoso", "El Chato", "El Bravo", "El Bronco", "Centella",
            "El Monstruo", "El Diablo", "El Padrino", "El Patrón", "Siete Mares", "Rey Espolón"
    );

    private static final List<String> PREFIXES = List.of(
            "Don", "Sir", "Lord", "Capitán", "Coronel", "Gran", "Viejo", "Pequeño",
            "Maestro", "Doctor", "Tío", "El Terco", "El Loco", "Fray"
    );

    private static final List<String> BASE_NOUNS = List.of(
            "Barnaby", "Nugget", "Pickles", "Picotazo", "Pico", "Pollo", "Plumas", "Garras",
            "Gallardo", "Trueno", "Cresta", "Sombra", "Veneno", "Fuego", "Tiburón", "Toro",
            "Navaja", "Buster", "Chucky", "Gordo", "Chispita", "Pancho", "Frijolito"
    );

    private static final String[] ROMAN_NUMERALS = {
            "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    };

    public static String generateName(Random rng) {
        int roll = rng.nextInt(100);
        if (roll < 45) {
            return UNIQUE_NAMES.get(rng.nextInt(UNIQUE_NAMES.size()));
        } else if (roll < 80) {
            return TITLED_NAMES.get(rng.nextInt(TITLED_NAMES.size()));
        } else {
            String p = PREFIXES.get(rng.nextInt(PREFIXES.size()));
            String n = BASE_NOUNS.get(rng.nextInt(BASE_NOUNS.size()));
            return p + " " + n;
        }
    }

    public static String generateBreedingName(RoosterData father, RoosterData mother, int generation, Random rng) {
        if (father != null && rng.nextInt(100) < 30) {
            String fName = father.getName();
            for (String num : ROMAN_NUMERALS) {
                if (fName.endsWith(" " + num)) {
                    fName = fName.substring(0, fName.length() - (num.length() + 1));
                    break;
                }
            }
            if (fName.endsWith(" Jr.")) {
                fName = fName.substring(0, fName.length() - 4);
            }

            int numIdx = Math.min(generation - 1, ROMAN_NUMERALS.length - 1);
            if (numIdx >= 0 && generation > 1) {
                return fName + " " + ROMAN_NUMERALS[numIdx];
            } else {
                return fName + " Jr.";
            }
        }
        return generateName(rng);
    }
}
