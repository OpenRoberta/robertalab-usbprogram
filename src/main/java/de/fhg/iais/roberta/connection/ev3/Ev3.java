package de.fhg.iais.roberta.connection.ev3;

import de.fhg.iais.roberta.usb.Robot;

public class Ev3 implements Robot {
    private final String name;

    public Ev3(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
