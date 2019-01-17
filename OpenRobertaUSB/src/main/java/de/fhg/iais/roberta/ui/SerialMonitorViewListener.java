package de.fhg.iais.roberta.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SerialMonitorViewListener extends WindowAdapter implements ActionListener {
    private static final Logger LOG = LoggerFactory.getLogger(SerialMonitorViewListener.class);

    private final SerialMonitorController serialMonitorController;

    public SerialMonitorViewListener(SerialMonitorController serialMonitorController) {
        this.serialMonitorController = serialMonitorController;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        LOG.debug("ActionEvent {}", actionEvent.getActionCommand());
        if ( actionEvent.getActionCommand().equals("comboBoxChanged") ) {
            this.serialMonitorController.restartSerialLogging();
            this.serialMonitorController.clearSerialLog();
        } else if ( actionEvent.getActionCommand().equals("clear") ) {
            this.serialMonitorController.clearSerialLog();
        }
    }

    @Override
    public void windowClosing(WindowEvent windowEvent) {
        LOG.debug("windowClosing");
        this.serialMonitorController.stopSerialLogging();
    }
}
