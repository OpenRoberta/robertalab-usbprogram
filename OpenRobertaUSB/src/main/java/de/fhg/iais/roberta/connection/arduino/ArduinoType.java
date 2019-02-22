package de.fhg.iais.roberta.connection.arduino;

public enum ArduinoType {
    UNO ("uno", "Uno"),
    MEGA ("mega", "Mega"),
    NANO ("nano", "Nano"),
    BOB3 ("bob3", "BOB3"),
    BOTNROLL ("ardu", "Bot'n Roll"),
    MBOT ("mbot", "mBot"),
    NONE ("none", "none");

    private final String text;
    private final String prettyText;

    ArduinoType(String text, String prettyText) {
        this.text = text;
        this.prettyText = prettyText;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public String getPrettyText() {
        return this.prettyText;
    }

    public static ArduinoType fromString(String text) {
        for (ArduinoType b : ArduinoType.values()) {
            if (text.equalsIgnoreCase(b.toString())) {
                return b;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
