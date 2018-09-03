package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.connection.SerialLoggingTask;
import de.fhg.iais.roberta.connection.arduino.ArduinoUSBConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static de.fhg.iais.roberta.usb.USBProgram.closeProgram;

public class UIController implements Observer {
    private static final Logger LOG = LoggerFactory.getLogger(UIController.class);

    private Map<String, IConnector> connectorMap = new HashMap<>();
    private IConnector connector;
    private final ConnectionView conView;
    private boolean connected;
    private final ResourceBundle rb;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SerialMonitor serialMonitor;
    private Future<Void> serialLoggingFuture;

    public UIController(ConnectionView conView, ResourceBundle rb) {
        this.conView = conView;
        this.conView.setVisible(true);
        this.rb = rb;
        this.connected = false;

        ConnectionViewListener listener = new ConnectionViewListener(this);
        this.conView.setWindowListener(listener);
        this.conView.setConnectActionListener(listener);

        this.serialMonitor = new SerialMonitor(this.rb, new SerialMonitorListener(this));
        this.serialMonitor.setVisible(false);
    }

    public void setConnectorMap(List<IConnector> connectorList) {
        LOG.debug("setConnectorMap");
        for (IConnector conn : connectorList) {
            this.connectorMap.put(conn.getBrickName(), conn);
        }
        this.conView.showRobotList(this.connectorMap);
    }

    public IConnector getSelectedRobot() {
//        LOG.info("getSelectedRobot");
        return this.connectorMap.get(this.conView.getSelectedRobot());
    }

    public IConnector getConnector() {
        return this.connector;
    }

    public void setConnector(IConnector usbCon) {
        LOG.debug("setConnector");
        this.conView.hideRobotList();
        this.connector = usbCon;
        ((Observable) this.connector).addObserver(this);

        LOG.info("GUI setup done. Using {}", usbCon.getClass().getSimpleName());
    }

    public void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.conView.setDiscover();
    }

    public ResourceBundle getRb() {
        return this.rb;
    }


    public void showAdvancedOptions() {
        LOG.debug("showAdvancedOptions");
        this.conView.showAdvancedOptions();
    }

    public void checkForValidCustomServerAddressAndUpdate() {
        LOG.debug("checkForValidCustomServerAddressAndUpdate");
        if ( this.conView.isCustomAddressSelected() ) {
            String ip = this.conView.getCustomIP();
            String port = this.conView.getCustomPort();
            if ( (ip != null) && (port != null) && !ip.isEmpty() && !port.isEmpty() ) {
                String address = ip + ':' + port;
                LOG.info("Valid custom address {}", address);
                this.connector.updateCustomServerAddress(address);
            } else {
                LOG.info("Invalid custom address (null or empty) - Using default address");
                this.connector.resetToDefaultServerAddress();
            }
        } else {
            this.connector.resetToDefaultServerAddress();
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void closeApplication() {
        LOG.debug("closeApplication");
        if ( this.connected ) {
            String[] buttons = {
                this.rb.getString("close"),
                this.rb.getString("cancel")
            };
            int n =
                ORAPopup.showPopup(
                    this.conView,
                    this.rb.getString("attention"),
                    this.rb.getString("confirmCloseInfo"),
                    new ImageIcon(getClass().getClassLoader().getResource("images/Roberta.png")),
                    buttons);
            if ( n == 0 ) {
                if ( this.connector != null ) {
                    this.connector.close();
                    try {
                        Thread.sleep(500); // give NXTUSBBTConnector time to play disconnect melody? :-)
                    } catch ( InterruptedException e ) {
                        // ok
                    }
                }
                closeProgram();
            }
        } else {
            closeProgram();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        State state = (State) arg;
        LOG.debug("update {}", state);
        switch ( state ) {
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                //this.conView.setNew(this.connector.getBrickName());
                this.connected = false;
                this.conView.setWaitForConnect();

                if (this.connector instanceof ArduinoUSBConnector) {
                    this.conView.showArduinoMenu();
                    this.conView.setArduinoMenuText(this.connector.getBrickName());
                }

                break;
            case WAIT_FOR_SERVER:
                this.conView.setNew(this.rb.getString("token") + ' ' + this.connector.getToken());
                this.conView.setWaitForServer();
                break;
            case RECONNECT:
                this.conView.setConnectButtonText(this.rb.getString("disconnect"));
            case WAIT_FOR_CMD:
                this.connected = true;
                this.conView.setNew(this.rb.getString("name") + ' ' + this.connector.getBrickName());
                this.conView.setWaitForCmd();
                break;
            case DISCOVER:
                this.connected = false;
                this.conView.setDiscover();
                break;
            case WAIT_UPLOAD:
                stopSerialLogging();
                break;
            case WAIT_EXECUTION:
                this.conView.setWaitExecution();
                if (this.serialMonitor.isVisible()) {
                    restartSerialLogging();
                }
                break;
            case UPDATE_SUCCESS:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("restartInfo"), null);
                break;
            case UPDATE_FAIL:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("updateFail"), null);
                break;
            case ERROR_HTTP:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("httpErrorInfo"), null);
                break;
            case ERROR_DOWNLOAD:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("downloadFail"), null);
                break;
            case ERROR_BRICK:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("httpBrickInfo"), null);
                break;
            case TOKEN_TIMEOUT:
                ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("tokenTimeout"), null);
                break;
            default:
                break;
        }
    }

    public void showAboutPopup() {
        LOG.debug("showAboutPopup");
        ORAPopup.showPopup(
            this.conView,
            this.rb.getString("about"),
            this.rb.getString("aboutInfo"),
            new ImageIcon(
                new ImageIcon(getClass().getClassLoader().getResource("images/iais_logo.gif"))
                    .getImage()
                    .getScaledInstance(100, 27, java.awt.Image.SCALE_AREA_AVERAGING)));
    }

    public void showSerialMonitor() {
        LOG.debug("showSerialMonitor");

        this.serialMonitor.setVisible(true);

        restartSerialLogging();
    }

    public void restartSerialLogging() {
        LOG.debug("restartSerialLogging");
        stopSerialLogging();

        // TODO improve
        if (this.connector instanceof ArduinoUSBConnector) {
            this.serialLoggingFuture = this.executorService.submit(
                new SerialLoggingTask(this,
                    ((ArduinoUSBConnector) this.connector).getPort(),
                    this.serialMonitor.getSerialRate()));
        }
    }

    public void appendSerial(byte[] readBuffer) {
        SwingUtilities.invokeLater(() -> this.serialMonitor.appendText(readBuffer));
    }

    public void clearSerialLog() {
        this.serialMonitor.clearText();
    }

    public void stopSerialLogging() {
        LOG.debug("stopSerialLogging");
        if (this.serialLoggingFuture != null) {
            this.serialLoggingFuture.cancel(true);
        }
    }
}
