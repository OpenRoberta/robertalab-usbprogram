package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.connection.SerialLoggingTask;
import de.fhg.iais.roberta.connection.arduino.ArduinoUSBConnector;
import de.fhg.iais.roberta.usb.RobotSearchTask;
import de.fhg.iais.roberta.util.ObserverObservable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class UIController extends ObserverObservable {
    private static final Logger LOG = LoggerFactory.getLogger(UIController.class);

    private List<IConnector> connectorList = null;
    private IConnector connector = null;
    private final ConnectionView conView;
    private boolean connected;
    private final ResourceBundle rb;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SerialMonitor serialMonitor;
    private Future<Void> serialLoggingFuture = null;

    public UIController(ConnectionView conView, ResourceBundle rb) {
        this.conView = conView;
        this.conView.setVisible(true);
        this.rb = rb;
        this.connected = false;

        ConnectionViewListener listener = new ConnectionViewListener(this);
        this.conView.setWindowListener(listener);
        this.conView.setConnectActionListener(listener);
        this.conView.setListSelectionListener(listener);

        this.serialMonitor = new SerialMonitor(this.rb, new SerialMonitorListener(this));
        this.serialMonitor.setVisible(false);
    }

    public void setConnectorList(List<IConnector> connectorList) {
        this.connectorList = new ArrayList<>(connectorList);

        List<String> robotNames = this.connectorList.stream().map(IConnector::getBrickName).collect(Collectors.toList());
        this.conView.showRobotList(robotNames);
    }

    public void setSelectedRobot(int index) {
        setChanged();
        notifyObservers(this.connectorList.get(index));
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

        if ( this.connector instanceof ArduinoUSBConnector ) {
            ArduinoUSBConnector arduinoUSBConnector = (ArduinoUSBConnector) this.connector;
            Map<Integer, String> errors = arduinoUSBConnector.getReadIdFileErrors();
            if ( !errors.isEmpty() ) {
                StringBuilder sb = new StringBuilder(200);
                sb.append(System.lineSeparator());
                for ( Entry<Integer, String> entry : errors.entrySet() ) {
                    sb.append("Line ").append(entry.getKey()).append(": ").append(this.rb.getString(entry.getValue())).append(System.lineSeparator());
                }
                LOG.error("Something went wrong when loading the arduino id file:{}", sb);
                showConfigErrorPopup(sb.toString());
            }
        }
    }

    public void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.conView.setDiscover();
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
            if ( ip.isEmpty() ) {
                LOG.info("Invalid custom address (null or empty) - Using default address");
                this.connector.resetToDefaultServerAddress();
            } else {
                if ( port.isEmpty() ) {
                    LOG.info("Valid custom ip {}, using default ports", ip);
                    this.connector.updateCustomServerAddress(ip);
                } else {
                    String address = ip + ':' + port;
                    LOG.info("Valid custom address {}", address);
                    this.connector.updateCustomServerAddress(address);
                }
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
                this.rb.getString("close"), this.rb.getString("cancel")
            };
            int n = ORAPopup.showPopup(
                this.conView,
                this.rb.getString("attention"),
                this.rb.getString("confirmCloseInfo"),
                new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/Roberta.png"))),
                buttons);
            if ( n == 0 ) {
                if ( this.connector != null ) {
                    this.connector.close();
                }
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        // TODO improve this
        if ( observable instanceof RobotSearchTask ) {
            setConnectorList((List<IConnector>) o);
        } else if ( observable instanceof IConnector ) {
            State state = (State) o;
            LOG.debug("update {}", state);
            switch ( state ) {
                case WAIT_FOR_CONNECT_BUTTON_PRESS:
                    //this.conView.setNew(this.connector.getBrickName());
                    this.connected = false;
                    this.conView.setWaitForConnect();

                    if ( this.connector instanceof ArduinoUSBConnector ) {
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
                    if ( this.serialMonitor.isVisible() ) {
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
    }

    public void showAboutPopup() {
        LOG.debug("showAboutPopup");
        ORAPopup.showPopup(
            this.conView,
            this.rb.getString("about"),
            this.rb.getString("aboutInfo"),
            new ImageIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/iais_logo.gif"))).getImage()
                .getScaledInstance(100, 27, java.awt.Image.SCALE_AREA_AVERAGING)));
    }

    public void showConfigErrorPopup(String errors) {
        ORAPopup.showPopup(this.conView, this.rb.getString("attention"), this.rb.getString("errorReadConfig") + errors, null);
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
        if ( this.connector instanceof ArduinoUSBConnector ) {
            this.serialLoggingFuture =
                this.executorService.submit(new SerialLoggingTask(this, ((ArduinoUSBConnector) this.connector).getPort(), this.serialMonitor.getSerialRate()));
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
        if ( this.serialLoggingFuture != null ) {
            this.serialLoggingFuture.cancel(true);
        }
    }
}
