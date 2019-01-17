package de.fhg.iais.roberta.ui;

import de.fhg.iais.roberta.usb.USBProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainViewListener extends WindowAdapter implements ActionListener, ListSelectionListener {
    private static final Logger LOG = LoggerFactory.getLogger(MainViewListener.class);

    private final MainController mainController;

    MainViewListener(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        AbstractButton button = (AbstractButton) actionEvent.getSource();
        if ( button.getActionCommand().equals("close") ) {
            LOG.debug("User close");
            this.mainController.closeApplication();
        } else if ( button.getActionCommand().equals("about") ) {
            LOG.debug("User about");
            this.mainController.showAboutPopup();
        } else if ( button.getActionCommand().equals("customaddress") ) {
            LOG.debug("User custom address");
            this.mainController.showAdvancedOptions();
        } else if ( button.getActionCommand().equals("scan") ) {
            LOG.debug("User scan");
            USBProgram.stopConnector();
            this.mainController.setDiscover();
        } else if ( button.getActionCommand().equals("serial")) {
            LOG.debug("User serial");
            this.mainController.showSerialMonitor();
        } else {
            if ( button.isSelected() ) {
                LOG.debug("User connect");
                if ( this.mainController.getConnector() != null ) { //TODO
                    this.mainController.checkForValidCustomServerAddressAndUpdate();
                    this.mainController.getConnector().userPressConnectButton();
                }
            } else {
                LOG.debug("User disconnect");
                if ( this.mainController.getConnector() != null ) {
                    this.mainController.getConnector().userPressDisconnectButton();
                }
            }
        }
    }

    @Override
    public void windowClosing(WindowEvent windowEvent) {
        LOG.debug("User close");
        this.mainController.closeApplication();
    }

    @Override
    public void valueChanged(ListSelectionEvent listSelectionEvent) {
        this.mainController.setSelectedRobot(listSelectionEvent.getFirstIndex());
    }
}
