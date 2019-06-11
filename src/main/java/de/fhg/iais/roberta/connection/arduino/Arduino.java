package de.fhg.iais.roberta.connection.arduino;

import de.fhg.iais.roberta.usb.Robot;

public class Arduino implements Robot {
    private final ArduinoType type;
    private final String port;

    public Arduino(ArduinoType type, String port) {
        this.type = type;
        this.port = port;
    }

    @Override
    public String getName() {
        return this.type.getPrettyText();
    }

    public ArduinoType getType() {
        return this.type;
    }

    public String getPort() {
        return this.port;
    }
}
