package com.dark.roostergenics.model;

public enum RoosterHealthStatus {
    HEALTHY("Saludable", "§a"),
    TIRED("Fatigado", "§e"),
    KNOCKED_OUT("Noqueado", "§6"),
    DEAD("Muerto", "§4");

    private final String displayName;
    private final String colorCode;

    RoosterHealthStatus(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getFormatted() {
        return colorCode + displayName;
    }
}
