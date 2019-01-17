package de.fhg.iais.roberta.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SerialMonitorListener extends WindowAdapter implements ActionListener {
    private static final Logger LOG = LoggerFactory.getLogger(SerialMonitorListener.class);

    private final MainController mainController;

    public SerialMonitorListener(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        LOG.debug("ActionEvent {}", e.getActionCommand());
        if ( e.getActionCommand().equals("comboBoxChanged") ) {
            this.mainController.restartSerialLogging();
            this.mainController.clearSerialLog();
        } else if ( e.getActionCommand().equals("clear") ) {
            this.mainController.clearSerialLog();
        }
    }

@Override public void windowClosing(WindowEvent e) {
        LOG.debug("windowClosing");
        this.mainController.stopSerialLogging();
    }
}
