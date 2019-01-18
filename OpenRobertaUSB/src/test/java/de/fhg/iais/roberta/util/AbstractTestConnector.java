package de.fhg.iais.roberta.util;

import de.fhg.iais.roberta.connection.IConnector;

public abstract class AbstractTestConnector implements IConnector {
    @Override
    public void userPressConnectButton() {

    }

    @Override
    public void userPressDisconnectButton() {

    }

    @Override
    public void close() {

    }

    @Override
    public String getToken() {
        return null;
    }

    @Override
    public String getBrickName() {
        return null;
    }

    @Override
    public void updateFirmware() {

    }

    @Override
    public void updateCustomServerAddress(String customServerAddress) {

    }

    @Override
    public void resetToDefaultServerAddress() {

    }

    @Override
    public Boolean call() throws Exception {
        return null;
    }

    @Override
    public void registerListener(IOraListener<State> listener) {

    }

    @Override
    public void unregisterListener(IOraListener<State> listener) {

    }

    @Override
    public void fire(State object) {

    }
}