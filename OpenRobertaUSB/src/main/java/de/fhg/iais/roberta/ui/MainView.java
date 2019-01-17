package de.fhg.iais.roberta.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainView extends JFrame {
    private static final Logger LOG = LoggerFactory.getLogger(MainView.class);

    private static final long serialVersionUID = 1L;
    private static final int WIDTH = 310;
    private static final int HEIGHT = 500;
    private static final int ADVANCED_HEIGHT = 558;

    private static final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

    static {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch ( ClassNotFoundException | IllegalAccessException | UnsupportedLookAndFeelException | InstantiationException e ) {
            LOG.error("Error when setting up the look and feel: {}", e.getMessage());
        }
        UIManager.put("MenuBar.background", Color.white);
        UIManager.put("Menu.background", Color.white);
        UIManager.put("Menu.selectionBackground", Color.decode("#afca04"));
        UIManager.put("Menu.foreground", Color.decode("#333333"));
        UIManager.put("Menu.font", font.deriveFont(12.0f));
        UIManager.put("MenuItem.background", Color.white);
        UIManager.put("MenuItem.selectionBackground", Color.decode("#afca04"));
        UIManager.put("MenuItem.foreground", Color.decode("#333333"));
        UIManager.put("MenuItem.font", font.deriveFont(12.0f));
        UIManager.put("MenuItem.focus", Color.decode("#afca04"));
        UIManager.put("Panel.background", Color.white);
        UIManager.put("CheckBox.background", Color.white);
        UIManager.put("Separator.foreground", Color.decode("#dddddd"));
        UIManager.put("TextField.background", Color.white);
        UIManager.put("TextField.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("List.font", font);
    }

    // Menu
    private final JMenuBar menu = new JMenuBar();

    private final JMenu menuFile = new JMenu();

    private final JMenuItem menuItemClose = new JMenuItem();

    private final JMenu menuInfo = new JMenu();
    private final JMenuItem menuItemAbout = new JMenuItem();

    private final JMenu menuArduino = new JMenu();
    private final JMenuItem menuItemSerial = new JMenuItem();

    private final JLabel lblRobot = new JLabel();

    // Center panel
    private final JPanel pnlCenter = new JPanel();

    private final JSeparator sep = new JSeparator();

    private final JLabel lblSelection = new JLabel();
    private final JList<String> listRobots = new JList<>();

    private final JPanel pnlToken = new JPanel();
    private final JTextField txtFldToken = new JTextField();

    private final JPanel pnlMainGif = new JPanel();
    private final JLabel lblMainGif = new JLabel();

    private final JTextArea txtAreaInfo = new JTextArea();

    private final JPanel pnlButton = new JPanel();
    private final ORAToggleButton butConnect = new ORAToggleButton();
    private final ORAToggleButton butScan = new ORAToggleButton();
    private final ORAButton butClose = new ORAButton();

    // Custom panel
    private final JPanel pnlCustomInfo = new JPanel();
    private final JCheckBox checkCustom = new JCheckBox();
    private final JLabel lblCustom = new JLabel();

    private final JPanel pnlCustomHeading = new JPanel();
    private final JTextField txtFldCustomHeading = new JTextField();

    private final JPanel pnlCustomAddress = new JPanel();
    private final JLabel lblCustomIp = new JLabel();
    private final JTextField txtFldCustomIp = new JTextField();
    private final JLabel lblCustomPort = new JLabel();
    private final JTextField txtFldCustomPort = new JTextField();

    // Resources
    private final ResourceBundle messages;
    private ImageIcon icoTitle;
    private ImageIcon icoRobotNotDiscovered;
    private ImageIcon icoRobotConnected;
    private ImageIcon icoRobotDiscovered;
    private ImageIcon gifPlug;
    private ImageIcon gifConnect;
    private ImageIcon gifServer;
    private ImageIcon gifConnected;


    private boolean toggle = true;

    public MainView(ResourceBundle messages, MainViewListener listener) {
        this.messages = messages;

        this.createIcons();
        this.initGUI();
        this.setDiscover();

        this.setWindowListener(listener);
        this.setListSelectionListener(listener);
        this.setActionListener(listener);
    }

    private void initGeneralGUI() {
        // General
        this.setSize(WIDTH, HEIGHT);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);

        // Titlebar
        this.setIconImage(this.icoTitle.getImage());
        this.setTitle(this.messages.getString("title"));
    }

    private void initMenuGUI() {
        // General
        this.add(this.menu, BorderLayout.PAGE_START);
        this.menu.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // File
        this.menu.add(this.menuFile);
        this.menuFile.setText(this.messages.getString("file"));
        this.menuFile.add(this.menuItemClose);
        this.menuItemClose.setText(this.messages.getString("close"));
        this.menuItemClose.setActionCommand("close");

        // Info
        this.menu.add(this.menuInfo);
        this.menuInfo.setText(this.messages.getString("info"));
        this.menuInfo.add(this.menuItemAbout);
        this.menuItemAbout.setText(this.messages.getString("about"));
        this.menuItemAbout.setActionCommand("about");

        // Arduino
        this.menu.add(this.menuArduino);
        this.menuArduino.setText("Arduino");
        this.menuArduino.add(this.menuItemSerial);
        this.menuItemSerial.setText("Serial Monitor");
        this.menuItemSerial.setActionCommand("serial");

        this.menu.add(Box.createHorizontalGlue());

        // Icon
        this.menu.add(this.lblRobot);
        this.lblRobot.setIcon(this.icoRobotNotDiscovered);
    }

    private void initCenterPanelGUI() {
        // General
        this.add(this.pnlCenter, BorderLayout.CENTER);
        this.pnlCenter.setLayout(new BoxLayout(this.pnlCenter, BoxLayout.PAGE_AXIS));

        // Seperator
        this.pnlCenter.add(this.sep);
        this.pnlCenter.add(Box.createRigidArea(new Dimension(0,25)));

        // Robotlist
        this.pnlCenter.add(this.lblSelection);
        this.lblSelection.setText(this.messages.getString("listInfo"));
        this.lblSelection.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.lblSelection.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        this.lblSelection.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));

        this.pnlCenter.add(this.listRobots);
        this.listRobots.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        this.listRobots.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        // Token panel
        this.pnlCenter.add(this.pnlToken);

        this.pnlToken.add(this.txtFldToken);
        this.txtFldToken.setFont(font.deriveFont(18.0f));
        this.txtFldToken.setBorder(BorderFactory.createEmptyBorder());
        this.txtFldToken.setEditable(false);

        // Main gif panel
        this.pnlCenter.add(this.pnlMainGif);

        this.pnlMainGif.add(this.lblMainGif);

        this.pnlCenter.add(Box.createRigidArea(new Dimension(0,15)));

        // Info texts
        this.pnlCenter.add(this.txtAreaInfo);
        this.txtAreaInfo.setText(Locale.getDefault().getLanguage());
        this.txtAreaInfo.setLineWrap(true);
        this.txtAreaInfo.setWrapStyleWord(true);
        this.txtAreaInfo.setMargin(new Insets(8, 16, 8, 16));
        this.txtAreaInfo.setEditable(false);

        // Button panel
        this.pnlCenter.add(this.pnlButton);
        this.pnlButton.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        this.pnlButton.setLayout(new FlowLayout(FlowLayout.LEADING));

        this.pnlButton.add(this.butConnect);

        this.pnlButton.add(Box.createRigidArea(new Dimension(12,0)));
        this.pnlButton.add(this.butScan);
        this.butScan.setText(this.messages.getString("scan"));
        this.butScan.setActionCommand("scan");

        this.pnlButton.add(Box.createRigidArea(new Dimension(12,0)));
        this.pnlButton.add(this.butClose);
        this.butClose.setText(this.messages.getString("close"));
        this.butClose.setActionCommand("close");

        this.pnlCenter.add(Box.createRigidArea(new Dimension(0, 20)));
    }

    private void initCustomPanelGUI() {
        // Custom info panel
        this.pnlCenter.add(this.pnlCustomInfo);
        this.pnlCustomInfo.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        this.pnlCustomInfo.setLayout(new FlowLayout(FlowLayout.LEADING));

        this.pnlCustomInfo.add(this.checkCustom);
        this.checkCustom.setActionCommand("customaddress");

        this.pnlCustomInfo.add(this.lblCustom);
        this.lblCustom.setBorder(null);
        this.lblCustom.setText(this.messages.getString("checkCustomDesc"));

        // Custom heading panel
        this.pnlCenter.add(this.pnlCustomHeading);
        this.pnlCustomHeading.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        this.pnlCustomHeading.setLayout(new FlowLayout(FlowLayout.LEADING));

        this.pnlCustomHeading.add(this.txtFldCustomHeading);
        this.txtFldCustomHeading.setEditable(false);
        this.txtFldCustomHeading.setBorder(null);
        this.txtFldCustomHeading.setText(this.messages.getString("customDesc"));

        // Custom address panel
        this.pnlCenter.add(this.pnlCustomAddress);
        this.pnlCustomAddress.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        this.pnlCustomAddress.setLayout(new FlowLayout(FlowLayout.LEADING));

        this.pnlCustomAddress.add(this.lblCustomIp);
        this.lblCustomIp.setBorder(null);
        this.lblCustomIp.setText(this.messages.getString("ip"));

        this.pnlCustomAddress.add(Box.createVerticalGlue());
        this.pnlCustomAddress.add(this.txtFldCustomIp);
        this.txtFldCustomIp.setEditable(true);
        this.txtFldCustomIp.setColumns(10);

        this.pnlCustomAddress.add(this.lblCustomPort);
        this.lblCustomPort.setBorder(null);
        this.lblCustomPort.setText(this.messages.getString("port"));

        this.pnlCustomAddress.add(this.txtFldCustomPort);
        this.txtFldCustomPort.setEditable(true);
        this.txtFldCustomPort.setColumns(4);

        this.pnlCenter.add(Box.createRigidArea(new Dimension(0,15)));
    }

    private void initGUI() {
        this.initGeneralGUI();
        this.initMenuGUI();
        this.initCenterPanelGUI();
        this.initCustomPanelGUI();

        this.hideRobotList();
        this.hideCustom();
        this.hideArduinoMenu();
    }

    private void hideCustom() {
        this.pnlCustomHeading.setVisible(false);
        this.pnlCustomAddress.setVisible(false);
    }

    private void showCustom() {
        this.pnlCustomHeading.setVisible(true);
        this.pnlCustomAddress.setVisible(true);
    }

    private void setActionListener(ActionListener listener) {
        this.menuItemClose.addActionListener(listener);
        this.menuItemAbout.addActionListener(listener);
        this.menuItemSerial.addActionListener(listener);
        this.butConnect.addActionListener(listener);
        this.butScan.addActionListener(listener);
        this.butClose.addActionListener(listener);
        this.checkCustom.addActionListener(listener);
    }

    private void setWindowListener(WindowListener windowListener) {
        this.addWindowListener(windowListener);
    }

    private void setListSelectionListener(ListSelectionListener listener) {
        this.listRobots.addListSelectionListener(listener);
    }

    public void setWaitForConnect() {
        this.lblRobot.setIcon(this.icoRobotDiscovered);
        this.butConnect.setEnabled(true);
        this.butScan.setEnabled(true);
        this.txtAreaInfo.setText(this.messages.getString("connectInfo"));
        this.lblMainGif.setIcon(this.gifConnect);
    }

    public void setWaitExecution() {
        if ( this.toggle ) {
            this.lblRobot.setIcon(this.icoRobotConnected);
        } else {
            this.lblRobot.setIcon(this.icoRobotDiscovered);
        }
        this.toggle = !this.toggle;
    }

    public void setWaitForCmd() {
        this.butConnect.setText(this.messages.getString("disconnect"));
        this.butConnect.setEnabled(true);
        this.butConnect.setSelected(true);
        this.lblRobot.setIcon(this.icoRobotConnected);
        this.txtAreaInfo.setText(this.messages.getString("serverInfo"));
        this.lblMainGif.setIcon(this.gifConnected);
    }

    public void setDiscover() {
        this.txtFldToken.setText("");
        this.lblRobot.setIcon(this.icoRobotNotDiscovered);
        this.butConnect.setText(this.messages.getString("connect"));
        this.butConnect.setSelected(false);
        this.butConnect.setEnabled(false);
        this.butScan.setEnabled(false);
        this.butScan.setSelected(false);
        this.txtAreaInfo.setText(this.messages.getString("plugInInfo"));
        this.lblMainGif.setIcon(this.gifPlug);
        this.hideArduinoMenu();
    }

    public void setWaitForServer() {
        this.butConnect.setSelected(false);
        this.butConnect.setEnabled(false);
    }

    public void setNew(String token) {
        this.butScan.setEnabled(false);
        this.txtFldToken.setText(token);
        this.txtAreaInfo.setText(this.messages.getString("tokenInfo"));
        this.lblMainGif.setIcon(this.gifServer);
    }

    public void showRobotList(List<String> robotNames) {
        this.lblSelection.setVisible(true);
        this.listRobots.setVisible(true);
        this.listRobots.setListData(robotNames.toArray(new String[0]));
    }

    public void hideRobotList() {
        this.lblSelection.setVisible(false);
        this.listRobots.setVisible(false);
    }

    public void showArduinoMenu() {
        this.menuArduino.setVisible(true);
    }

    public void hideArduinoMenu() {
        this.menuArduino.setVisible(false);
    }

    public void setArduinoMenuText(String text) {
        this.menuArduino.setText(text);
    }

    public void showAdvancedOptions() {
        if ( this.checkCustom.isSelected() ) {
            this.setSize(new Dimension(WIDTH, ADVANCED_HEIGHT));
            this.setPreferredSize(new Dimension(WIDTH, ADVANCED_HEIGHT));
            this.showCustom();
            this.revalidate();
        } else {
            this.setSize(new Dimension(WIDTH, HEIGHT));
            this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            this.hideCustom();
            this.revalidate();
        }
    }

    public void setConnectButtonText(String text) {
        this.butConnect.setText(text);
    }

    public String getCustomIP() {
        return this.txtFldCustomIp.getText();
    }

    public String getCustomPort() {
        return this.txtFldCustomPort.getText();
    }

    public boolean isCustomAddressSelected() {
        return this.checkCustom.isSelected();
    }

    private void createIcons() {
        URL imgURL = getClass().getClassLoader().getResource("images/OR.png");
        if ( imgURL != null ) {
            this.icoTitle = new ImageIcon(imgURL);
        }
        imgURL = getClass().getClassLoader().getResource("images/Roberta_Menu_Icon_green.png");
        if ( imgURL != null ) {
            this.icoRobotConnected = new ImageIcon(imgURL);
        }
        imgURL = getClass().getClassLoader().getResource("images/Roberta_Menu_Icon_red.png");
        if ( imgURL != null ) {
            this.icoRobotDiscovered = new ImageIcon(imgURL);
        }
        imgURL = getClass().getClassLoader().getResource("images/Roberta_Menu_Icon_grey.png");
        if ( imgURL != null ) {
            this.icoRobotNotDiscovered = new ImageIcon(imgURL);
        }
        imgURL = getClass().getClassLoader().getResource("images/plug.gif");
        if ( imgURL != null ) {
            this.gifPlug = new ImageIcon(imgURL);
        }
        imgURL = getClass().getClassLoader().getResource("images/connect.gif");
        if ( imgURL != null ) {
            this.gifConnect = new ImageIcon(imgURL);
        }
        imgURL = getClass().getClassLoader().getResource("images/server.gif");
        if ( imgURL != null ) {
            this.gifServer = new ImageIcon(imgURL);
        }
        imgURL = getClass().getClassLoader().getResource("images/connected.gif");
        if ( imgURL != null ) {
            this.gifConnected = new ImageIcon(imgURL);
        }
    }
}
