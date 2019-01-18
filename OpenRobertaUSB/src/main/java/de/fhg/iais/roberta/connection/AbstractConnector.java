package de.fhg.iais.roberta.connection;

import de.fhg.iais.roberta.util.PropertyHelper;
import de.fhg.iais.roberta.util.IOraListener;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractConnector implements IConnector {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractConnector.class);

    private final Collection<IOraListener<State>> listeners = new ArrayList<>();

    private final String serverAddress;

    protected ServerCommunicator serverCommunicator = null;

    protected JSONObject brickData = null;

    protected State state = State.DISCOVER; // First state when program starts
    protected String token = "";
    protected String brickName;
    protected boolean userDisconnect = false;

    protected AbstractConnector(String brickName) {
        String serverIp = PropertyHelper.getInstance().getProperty("serverIp");
        String serverPort = PropertyHelper.getInstance().getProperty("serverPort");
        this.serverAddress = serverIp + ':' + serverPort;
        this.brickName = brickName;
    }

    private boolean running = false;

    @Override
    public void run() {
        this.running = true;
        LOG.info("Starting {} connector thread.", this.brickName);
        setupServerCommunicator();
        LOG.info("Server address {}", this.serverAddress);
        while ( this.running ) {
            runLoopBody();
        }
    }

    @Override
    public void interrupt() {
        this.running = false;

        this.state = State.DISCOVER;
        fire(this.state);
    }

    protected abstract void runLoopBody();

    @Override
    public void userPressConnectButton() {
        this.state = State.CONNECT_BUTTON_IS_PRESSED;
    }

    @Override
    public void userPressDisconnectButton() {
        LOG.info("DISCONNECTING by user");
        this.userDisconnect = true;
        this.state = State.DISCOVER;
        fire(this.state);
    }

    @Override
    public void close() {
        userPressDisconnectButton();
    }

    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public String getBrickName() {
        return this.brickName;
    }

    @Override
    public void updateFirmware() {
        // no firmware update intended for general robots
    }

    @Override
    public void updateCustomServerAddress(String customServerAddress) {
        this.serverCommunicator.setServerAddress(customServerAddress);
        LOG.info("Now using custom address {}", customServerAddress);
    }

    @Override
    public void resetToDefaultServerAddress() {
        this.serverCommunicator.setServerAddress(this.serverAddress);
        LOG.info("Now using default address {}", this.serverAddress);
    }

    private void setupServerCommunicator() {
        this.serverCommunicator = new ServerCommunicator(this.serverAddress);
    }

    /**
     * Reset the USB program to the start state (discover).
     *
     * @param additionalErrorMessage Display a popup with error message. If this is null, we do not want to display the tooltip.
     */
    protected void reset(State additionalErrorMessage) {
        if ( !this.userDisconnect && (additionalErrorMessage != null) ) {
            fire(additionalErrorMessage);
        }
        this.userDisconnect = false;
        this.state = State.DISCOVER;
        fire(this.state);
    }

    @Override
    public void registerListener(IOraListener<State> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<State> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(State object) {
        for ( IOraListener<State> listener : this.listeners ) {
            listener.update(object);
        }
    }
}
