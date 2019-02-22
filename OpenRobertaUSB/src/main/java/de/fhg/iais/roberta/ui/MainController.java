package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.usb.Robot;
import de.fhg.iais.roberta.util.IOraListenable;
import de.fhg.iais.roberta.util.IOraListener;
import de.fhg.iais.roberta.util.IOraUiListener;
import de.fhg.iais.roberta.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements IController, IOraListenable<Robot> {
    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private final Collection<IOraListener<Robot>> listeners = new ArrayList<>();

    // View related
    private final ResourceBundle rb;
    private final MainView mainView;

    private static final int MAX_ADDRESS_ENTRIES = 5;
    private static final String CUSTOM_ADDRESSES_FILENAME = "customaddresses.txt";
    private static final String ADDRESS_DELIMITER = " "; // colon may be used in ipv6 addresses
    private Deque<Pair<String, String>> customAddresses = new ArrayDeque<>();

    // For the robot selection if there is more than one robot available
    private List<Robot> robotList = null;

    private boolean connected;

    private IConnector connector = null;

    // Child controllers of the main controller, this includes other windows/JFrames that are launched from the main controller
    private SerialMonitorController serialMonitorController = null;

    public MainController(ResourceBundle rb) {
        this.mainView = new MainView(rb, new MainViewListener());
        this.mainView.setVisible(true);
        this.rb = rb;
        this.connected = false;

        this.customAddresses.addAll(loadCustomAddresses());
        this.mainView.setCustomAddresses(this.customAddresses);
    }

    public void setRobotList(List<Robot> robotList) {
        this.robotList = new ArrayList<>(robotList);
        this.mainView.showRobotList(this.robotList.stream().map(Enum::toString).collect(Collectors.toList()));
    }

    @Override
    public void setState(State state) {
        LOG.info("Setting state to {}", state);
        switch ( state ) {
            case WAIT_FOR_CONNECT_BUTTON_PRESS:
                this.connected = false;

                this.mainView.setWaitForConnect();

                if (this.connector.getRobot() == Robot.ARDUINO) {
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
                setDiscover();
                break;
            case WAIT_EXECUTION:
                this.mainView.setWaitExecution();
                break;
            case UPDATE_SUCCESS:
                showAttentionPopup("restartInfo", "");
                break;
            case UPDATE_FAIL:
                showAttentionPopup("updateFail", "");
                break;
            case ERROR_HTTP:
                showAttentionPopup("httpErrorInfo", "");
                break;
            case ERROR_DOWNLOAD:
                showAttentionPopup("downloadFail", "");
                break;
            case ERROR_BRICK:
                showAttentionPopup("httpBrickInfo", "");
                break;
            case TOKEN_TIMEOUT:
                showAttentionPopup("tokenTimeout", "");
                break;
            default:
                break;
        }
    }

    @Override
    public void setConnector(IConnector connector) {
        LOG.debug("setConnector: {}", connector.getRobot());
        this.connector = connector;
        this.connector.registerListener(this::setState);

        this.mainView.hideRobotList();

        // Serial monitor is only needed for arduino based robots
        if (connector.getRobot() == Robot.ARDUINO) {
            this.serialMonitorController = new SerialMonitorController(this.rb);
            this.serialMonitorController.setConnector(connector);
        }
    }

    public void showConfigErrorPopup(Map<Integer, String> errors) {
        StringBuilder sb = new StringBuilder(200);
        sb.append(System.lineSeparator());
        for ( Entry<Integer, String> entry : errors.entrySet() ) {
            sb.append("Line ").append(entry.getKey()).append(": ").append(this.rb.getString(entry.getValue())).append(System.lineSeparator());
        }
        LOG.error("Errors in config file:{}", sb);

        showAttentionPopup("errorReadConfig", sb.toString());
    }

    private void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.mainView.setDiscover();
    }

    private void showAttentionPopup(String key, String additionalInfo) {
        OraPopup.showPopup(this.mainView, this.rb.getString("attention"),
            this.rb.getString(key) + additionalInfo, null);
    }

    private void addCustomAddress(Pair<String, String> address) {
        if (validatePort(address.getSecond())) {
            this.customAddresses.addFirst(address);
            this.customAddresses = this.customAddresses.stream().distinct().limit(MAX_ADDRESS_ENTRIES).collect(Collectors.toCollection(ArrayDeque::new));
            this.mainView.setCustomAddresses(this.customAddresses);
        }
    }

    private void saveCustomAddresses() {
        // space as delimiter, colon may be used in ipv6
        List<String> collect = this.customAddresses.stream().map(address -> address.getFirst() + ADDRESS_DELIMITER + address.getSecond()).limit(
            MAX_ADDRESS_ENTRIES).collect(Collectors.toList());
        try {
            Files.write(new File(CUSTOM_ADDRESSES_FILENAME).toPath(), collect, StandardCharsets.UTF_8);
        } catch ( IOException e ) {
            LOG.error("Something went wrong while writing the custom addresses: {}", e.getMessage());
        }
    }

    private static List<Pair<String, String>> loadCustomAddresses() {
        try {
            List<Pair<String, String>> addresses = new ArrayList<>();

            List<String> readAddresses = Files.readAllLines(new File(CUSTOM_ADDRESSES_FILENAME).toPath(), StandardCharsets.UTF_8);

            for ( String address : readAddresses ) {
                Pair<String, String> ipPort = extractIpAndPort(address);
                if (ipPort != null) {
                    addresses.add(ipPort);
                }
            }

            return addresses.stream().limit(MAX_ADDRESS_ENTRIES).collect(Collectors.toList());
        } catch ( IOException e ) {
            LOG.error("Something went wrong while reading the custom addresses: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    private static Pair<String, String> extractIpAndPort(String address) {
        String[] s = address.split(ADDRESS_DELIMITER);

        if (s.length == 1) {
            return new Pair<>(s[0], "");
        } else if (s.length == 2) {
            String sPort = s[1];

            if (validatePort(sPort)) {
                return new Pair<>(s[0], sPort);
            }
        }
        return null;
    }

    private static boolean validatePort(String sPort) {
        try {
            int port = Integer.valueOf(sPort);

            if ( (port >= 0) && (port <= 65535) ) {
                return true;
            }
        } catch ( NumberFormatException e ) {
            LOG.error("The given port is invalid: {}", sPort);
        }

        return false;
    }

    @Override
    public void registerListener(IOraListener<Robot> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void unregisterListener(IOraListener<Robot> listener) {
        this.listeners.remove(listener);
    }

    @Override
    public void fire(Robot object) {
        for ( IOraListener<Robot> listener : this.listeners ) {
            listener.update(object);
        }
    }

    private class MainViewListener implements IOraUiListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            LOG.info("User performed action {}", e.getActionCommand());
            switch ( e.getActionCommand() ) {
                case "close":
                    closeApplication();
                    break;
                case "about":
                    showAboutPopup();
                    break;
                case "customaddress":
                    MainController.this.mainView.toggleAdvancedOptions();
                    break;
                case "scan":
                    MainController.this.connector.interrupt();
                    setDiscover();
                    break;
                case "serial":
                    MainController.this.serialMonitorController.showSerialMonitor();
                    break;
                case "connect":
                    checkForValidCustomServerAddressAndUpdate();
                    MainController.this.connector.userPressConnectButton();
                    break;
                case "disconnect":
                    MainController.this.connector.userPressDisconnectButton();
                    break;
                default:
                    throw new UnsupportedOperationException("Action " + e.getActionCommand() + " is not implemented!");
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
            LOG.info("User closed main window");
            closeApplication();
        }

        // Sends event to all listeners waiting for the robot selection event when a robot was selected
        @Override
        public void valueChanged(ListSelectionEvent e) {
            LOG.debug("valueChanged: {}", e.getFirstIndex());
            JList<?> source = (JList<?>) e.getSource();
            source.clearSelection();
            fire(MainController.this.robotList.get(e.getFirstIndex()));
        }

        private void showAboutPopup() {
            OraPopup.showPopup(MainController.this.mainView, MainController.this.rb.getString("about"), MainController.this.rb.getString("aboutInfo"),
                new ImageIcon(new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/iais_logo.gif"))).getImage()
                    .getScaledInstance(100, 27, java.awt.Image.SCALE_AREA_AVERAGING)));
        }

        private void checkForValidCustomServerAddressAndUpdate() {
            LOG.debug("checkForValidCustomServerAddressAndUpdate");
            if ( MainController.this.mainView.isCustomAddressSelected() ) {
                Pair<String, String> address = MainController.this.mainView.getCustomAddress();
                String ip = address.getFirst();
                String port = address.getSecond();

                if ( ip.isEmpty() ) {
                    LOG.info("Invalid custom address (null or empty) - Using default address");
                    MainController.this.connector.resetToDefaultServerAddress();
                } else {
                    if ( port.isEmpty() ) {
                        LOG.info("Valid custom ip {}, using default ports", ip);
                        MainController.this.connector.updateCustomServerAddress(ip);
                    } else {
                        String formattedAddress = ip + ':' + port;
                        LOG.info("Valid custom address {}", formattedAddress);
                        MainController.this.connector.updateCustomServerAddress(formattedAddress);
                    }
                    addCustomAddress(address);
                }
            } else {
                MainController.this.connector.resetToDefaultServerAddress();
            }
        }

        private void closeApplication() {
            LOG.debug("closeApplication");
            saveCustomAddresses();
            if ( MainController.this.connected ) {
                String[] buttons = {
                    MainController.this.rb.getString("close"), MainController.this.rb.getString("cancel")
                };
                int n = OraPopup.showPopup(MainController.this.mainView, MainController.this.rb.getString("attention"), MainController.this.rb.getString("confirmCloseInfo"),
                    new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/Roberta.png"))),
                    buttons);
                if ( n == 0 ) {
                    if ( MainController.this.connector != null ) {
                        MainController.this.connector.close();
                    }
                    System.exit(0);
                }
            } else {
                System.exit(0);
            }
        }
    }
}
