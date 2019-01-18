package de.fhg.iais.roberta.util;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public interface ORAUIListener extends WindowListener, ActionListener, ListSelectionListener {
    @Override
    default void actionPerformed(ActionEvent e) {}

    @Override
    default void windowOpened(WindowEvent e) {}

    @Override
    default void windowClosing(WindowEvent e) {}

    @Override
    default void windowClosed(WindowEvent e) {}

    @Override
    default void windowIconified(WindowEvent e) {}

    @Override
    default void windowDeiconified(WindowEvent e) {}

    @Override
    default void windowActivated(WindowEvent e) {}

    @Override
    default void windowDeactivated(WindowEvent e) {}

    @Override
    default void valueChanged(ListSelectionEvent e) {}
}
