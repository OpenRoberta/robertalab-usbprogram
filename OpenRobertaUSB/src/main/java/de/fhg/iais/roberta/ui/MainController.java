package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.connection.SerialLoggingTask;
import de.fhg.iais.roberta.connection.arduino.ArduinoUSBConnector;
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

public class MainController extends Observable {
    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private List<IConnector> connectorList = null;
    private IConnector connector = null;
    private final MainView mainView;
    private boolean connected;
    private final ResourceBundle rb;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final SerialMonitorView serialMonitorView;
    private Future<Void> serialLoggingFuture = null;

    private final RobotSearchObserver robotSearchObserver;
    private final ConnectorObserver connectorObserver;
    private final SerialLoggingObserver serialLoggingObserver;

    public MainController(ResourceBundle rb) {
        this.mainView = new MainView(rb, new MainViewListener(this));
        this.mainView.setVisible(true);
        this.rb = rb;
        this.connected = false;

        this.serialMonitorView = new SerialMonitorView(this.rb, new SerialMonitorListener(this));
        this.serialMonitorView.setVisible(false);

        this.robotSearchObserver = new RobotSearchObserver(this);
        this.connectorObserver = new ConnectorObserver(this);
        this.serialLoggingObserver = new SerialLoggingObserver(this);
    }

    public RobotSearchObserver getRobotSearchObserver() {
        return this.robotSearchObserver;
    }

    void setConnectorList(List<IConnector> connectorList) {
        this.connectorList = new ArrayList<>(connectorList);

        List<String> robotNames = this.connectorList.stream().map(IConnector::getBrickName).collect(Collectors.toList());
        this.mainView.showRobotList(robotNames);
    }

    void setSelectedRobot(int index) {
        setChanged();
        notifyObservers(this.connectorList.get(index));
    }

    public IConnector getConnector() {
        return this.connector;
    }

    void setState(State state) {
        switch ( state ) {
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                this.connected = false;

                this.mainView.setWaitForConnect();

                if ( this.connector instanceof ArduinoUSBConnector ) {
                    this.mainView.showArduinoMenu();
                    this.mainView.setArduinoMenuText(this.connector.getBrickName());
                }
                break;
            case WAIT_FOR_SERVER:
                this.mainView.setNew(this.rb.getString("token") + ' ' + this.connector.getToken());
                this.mainView.setWaitForServer();
                break;
            case RECONNECT:
                this.mainView.setConnectButtonText(this.rb.getString("disconnect"));
            case WAIT_FOR_CMD:
                this.connected = true;
                this.mainView.setNew(this.rb.getString("name") + ' ' + this.connector.getBrickName());
                this.mainView.setWaitForCmd();
                break;
            case DISCOVER:
                this.setDiscover();
                break;
            case WAIT_UPLOAD:
                this.stopSerialLogging();
                break;
            case WAIT_EXECUTION:
                this.mainView.setWaitExecution();
                if ( this.serialMonitorView.isVisible() ) {
                    this.restartSerialLogging();
                }
                break;
            case UPDATE_SUCCESS:
                this.showAttentionPopup("restartInfo");
                break;
            case UPDATE_FAIL:
                this.showAttentionPopup("updateFail");
                break;
            case ERROR_HTTP:
                this.showAttentionPopup("httpErrorInfo");
                break;
            case ERROR_DOWNLOAD:
                this.showAttentionPopup("downloadFail");
                break;
            case ERROR_BRICK:
                this.showAttentionPopup("httpBrickInfo");
                break;
            case TOKEN_TIMEOUT:
                this.showAttentionPopup("tokenTimeout");
                break;
            default:
                break;
        }
    }

    public void setConnector(IConnector usbCon) {
        LOG.debug("setConnector");
        this.mainView.hideRobotList();
        this.connector = usbCon;
        ((Observable) this.connector).addObserver(this.connectorObserver);

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

    private void showConfigErrorPopup(String errors) {
        ORAPopup.showPopup(this.mainView, this.rb.getString("attention"), this.rb.getString("errorReadConfig") + errors, null);
    }

    void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.mainView.setDiscover();
    }

    void showAdvancedOptions() {
        LOG.debug("showAdvancedOptions");
        this.mainView.showAdvancedOptions();
    }

    void checkForValidCustomServerAddressAndUpdate() {
        LOG.debug("checkForValidCustomServerAddressAndUpdate");
        if ( this.mainView.isCustomAddressSelected() ) {
            String ip = this.mainView.getCustomIP();
            String port = this.mainView.getCustomPort();
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

    void closeApplication() {
        LOG.debug("closeApplication");
        if ( this.connected ) {
            String[] buttons = {
                this.rb.getString("close"), this.rb.getString("cancel")
            };
            int n = ORAPopup.showPopup(
                this.mainView,
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


    void showSerialMonitor() {
        LOG.debug("showSerialMonitor");

        this.serialMonitorView.setVisible(true);

        restartSerialLogging();
    }

    void restartSerialLogging() {
        LOG.debug("restartSerialLogging");
        stopSerialLogging();

        // TODO improve
        if ( this.connector instanceof ArduinoUSBConnector ) {
            this.serialLoggingFuture =
                this.executorService.submit(new SerialLoggingTask(this.serialLoggingObserver, ((ArduinoUSBConnector) this.connector).getPort(), this.serialMonitorView
                    .getSerialRate()));
        }
    }

    void appendSerial(byte[] readBuffer) {
        SwingUtilities.invokeLater(() -> this.serialMonitorView.appendText(readBuffer));
    }

    void clearSerialLog() {
        this.serialMonitorView.clearText();
    }

    void stopSerialLogging() {
        if ( this.serialLoggingFuture != null ) {
            this.serialLoggingFuture.cancel(true);
        }
    }

    private void showAttentionPopup(String key) {
        ORAPopup.showPopup(this.mainView, this.rb.getString("attention"),
            this.rb.getString(key), null);
    }

    void showAboutPopup() {
        ORAPopup.showPopup(
            this.mainView,
            this.rb.getString("about"),
            this.rb.getString("aboutInfo"),
            new ImageIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/iais_logo.gif"))).getImage()
                .getScaledInstance(100, 27, java.awt.Image.SCALE_AREA_AVERAGING)));
    }
}
