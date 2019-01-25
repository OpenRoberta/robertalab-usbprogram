package de.fhg.iais.roberta.ui.main;

import de.fhg.iais.roberta.connection.IConnector;
import de.fhg.iais.roberta.connection.IConnector.State;
import de.fhg.iais.roberta.ui.IController;
import de.fhg.iais.roberta.ui.OraPopup;
import de.fhg.iais.roberta.ui.deviceIdEditor.DeviceIdEditorController;
import de.fhg.iais.roberta.ui.serialMonitor.SerialMonitorController;
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
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

import static de.fhg.iais.roberta.ui.main.HelpDialog.CMD_SELECT_EV3;
import static de.fhg.iais.roberta.ui.main.HelpDialog.CMD_SELECT_OTHER;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_ABOUT;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_EXIT;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_CONNECT;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_CUSTOMADDRESS;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_DISCONNECT;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_HELP;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_ID_EDITOR;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_SCAN;
import static de.fhg.iais.roberta.ui.main.MainView.CMD_SERIAL;
import static de.fhg.iais.roberta.ui.main.MainView.IMAGES_PATH;

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
    private final DeviceIdEditorController deviceIdEditorController;

    private final HelpDialog helpDialog;

    public MainController(ResourceBundle rb) {
        MainViewListener mainViewListener = new MainViewListener();
        this.mainView = new MainView(rb, mainViewListener);
        this.mainView.setVisible(true);
        this.rb = rb;
        this.connected = false;

        this.customAddresses.addAll(loadCustomAddresses());
        this.mainView.setCustomAddresses(this.customAddresses);

        this.deviceIdEditorController = new DeviceIdEditorController(rb);

        this.helpDialog = new HelpDialog(this.mainView, rb, mainViewListener);
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

                if ( this.connector.getRobot() == Robot.ARDUINO ) {
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
        if ( connector.getRobot() == Robot.ARDUINO ) {
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

    public void showHelp() {
        this.helpDialog.setLocation(this.mainView.getRobotButtonLocation());
        this.helpDialog.setVisible(true);
    }

    public void toggleHelp() {
        this.helpDialog.setLocation(this.mainView.getRobotButtonLocation());
        this.helpDialog.setVisible(!this.helpDialog.isVisible());
    }

    private void setDiscover() {
        LOG.debug("setDiscover");
        this.connected = false;
        this.mainView.setDiscover();
    }

    private void showAttentionPopup(String key, String additionalInfo) {
        OraPopup.showPopup(this.mainView, this.rb.getString("attention"), this.rb.getString(key) + additionalInfo, null);
    }

    private static List<Pair<String, String>> loadCustomAddresses() {
        try {
            Collection<Pair<String, String>> addresses = new ArrayList<>();

            List<String> readAddresses = Files.readAllLines(new File(CUSTOM_ADDRESSES_FILENAME).toPath(), StandardCharsets.UTF_8);

            for ( String address : readAddresses ) {
                Pair<String, String> ipPort = extractIpAndPort(address);
                if ( ipPort != null ) {
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

        if ( s.length == 1 ) {
            return new Pair<>(s[0], "");
        } else if ( s.length == 2 ) {
            String sPort = s[1];

            if ( validatePort(sPort) ) {
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
                case CMD_EXIT:
                    closeApplication();
                    break;
                case CMD_ABOUT:
                    showAboutPopup();
                    break;
                case CMD_CUSTOMADDRESS:
                    MainController.this.mainView.toggleAdvancedOptions();
                    break;
                case CMD_SCAN:
                    MainController.this.connector.interrupt();
                    setDiscover();
                    break;
                case CMD_SERIAL:
                    MainController.this.serialMonitorController.showSerialMonitor();
                    break;
                case CMD_CONNECT:
                    checkForValidCustomServerAddressAndUpdate();
                    MainController.this.connector.userPressConnectButton();
                    break;
                case CMD_DISCONNECT:
                    MainController.this.connector.userPressDisconnectButton();
                    break;
                case CMD_HELP:
                    toggleHelp();
                    break;
                case CMD_ID_EDITOR:
                    MainController.this.deviceIdEditorController.showEditor();
                    MainController.this.connector.interrupt();
                    setDiscover();
                    break;
                case CMD_SELECT_EV3:
                    MainController.this.helpDialog.dispose();
                    try {
                        Desktop.getDesktop().browse(new URI(MainController.this.rb.getString("linkEv3UsbWiki")));
                    } catch ( IOException | URISyntaxException e1 ) {
                        LOG.error("Could not open browser: {}", e1.getMessage());
                    }
                    break;
                case CMD_SELECT_OTHER:
                    MainController.this.helpDialog.dispose();
                    MainController.this.deviceIdEditorController.showEditor();
                    MainController.this.connector.interrupt();
                    setDiscover();
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
            OraPopup.showPopup(MainController.this.mainView,
                MainController.this.rb.getString("about"),
                MainController.this.rb.getString("aboutInfo"),
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
                    LOG.warn("Invalid custom address - Using default address");
                    MainController.this.connector.resetToDefaultServerAddress();
                } else {
                    if ( port.isEmpty() ) {
                        LOG.info("Valid custom ip {}, using default ports", ip);
                        MainController.this.connector.updateCustomServerAddress(ip);
                        addCustomAddress(address);
                    } else {
                        if ( validatePort(port) ) {
                            String formattedAddress = ip + ':' + port;
                            LOG.info("Valid custom address {}", formattedAddress);
                            MainController.this.connector.updateCustomServerAddress(formattedAddress);
                            addCustomAddress(address);
                        } else {
                            LOG.warn("Invalid port {}", port);
                        }
                    }
                }
            } else {
                MainController.this.connector.resetToDefaultServerAddress();
            }
        }

        private void addCustomAddress(Pair<String, String> address) {
            MainController.this.customAddresses.addFirst(address);
            MainController.this.customAddresses =
                MainController.this.customAddresses.stream().distinct().limit(MAX_ADDRESS_ENTRIES).collect(Collectors.toCollection(ArrayDeque::new));
            MainController.this.mainView.setCustomAddresses(MainController.this.customAddresses);
        }

        private void saveCustomAddresses() {
            // space as delimiter, colon may be used in ipv6
            List<String>
                collect =
                MainController.this.customAddresses.stream()
                    .map(address -> address.getFirst() + ADDRESS_DELIMITER + address.getSecond())
                    .limit(MAX_ADDRESS_ENTRIES)
                    .collect(Collectors.toList());
            try {
                Files.write(new File(CUSTOM_ADDRESSES_FILENAME).toPath(), collect, StandardCharsets.UTF_8);
            } catch ( IOException e ) {
                LOG.error("Something went wrong while writing the custom addresses: {}", e.getMessage());
            }
        }

        private void closeApplication() {
            LOG.debug("closeApplication");
            saveCustomAddresses();
            if ( MainController.this.connected ) {
                String[] buttons = {
                    MainController.this.rb.getString("exit"), MainController.this.rb.getString("cancel")
                };
                int n = OraPopup.showPopup(MainController.this.mainView,
                    MainController.this.rb.getString("attention"),
                    MainController.this.rb.getString("confirmCloseInfo"),
                    new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource(IMAGES_PATH + "Roberta.png"))),
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
