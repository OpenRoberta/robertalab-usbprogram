package de.fhg.iais.roberta.connection.nao;

import de.fhg.iais.roberta.usb.Robot;

import java.net.InetAddress;

public class Nao implements Robot {
    private final String name;
    private final InetAddress address;

    public Nao(String name, InetAddress address) {
        this.name = name;
        this.address = address;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public InetAddress getAddress() {
        return this.address;
    }
}
