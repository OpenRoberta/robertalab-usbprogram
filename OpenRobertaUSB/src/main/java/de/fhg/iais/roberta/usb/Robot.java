package de.fhg.iais.roberta.usb;

public enum Robot {
    NONE("none"),
    EV3("EV3"),
    ARDUINO("Arduino");

    private final String name;

    Robot(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
