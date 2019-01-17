package de.fhg.iais.roberta.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ResourceBundle;

public class SerialMonitorView extends JFrame {
    private final JTextArea textArea = new JTextArea();
    private final JScrollPane scrollPane = new JScrollPane(this.textArea,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    private final JPanel options = new JPanel();
    private final JComboBox<Integer> rateSelection = new JComboBox<>(
        new Integer[] { 1200, 2400, 4800, 9600, 19200, 38400, 57600, 74880, 115200 });
    private final JButton clearButton = new JButton();

    public SerialMonitorView(ResourceBundle messages, SerialMonitorListener listener) {
        // General
        this.setSize(700, 500);
        this.setLocationRelativeTo(null);
        this.addWindowListener(listener);

        // Titlebar
        this.setIconImage(new ImageIcon(getClass().getClassLoader().getResource("images/OR.png")).getImage());
        this.setTitle("Serial Monitor");

        this.add(this.scrollPane, BorderLayout.CENTER);
        this.scrollPane.setBorder(new EmptyBorder(4, 4, 4, 4));
        this.textArea.setRows(16);
        this.textArea.setColumns(40);
        this.textArea.setEditable(false);

        this.add(this.options, BorderLayout.PAGE_END);
        this.options.setBorder(new EmptyBorder(4, 4, 4, 4));
        this.options.setLayout(new FlowLayout(FlowLayout.TRAILING));
        this.options.add(Box.createHorizontalGlue());
        this.options.add(this.rateSelection);
        this.rateSelection.setMaximumSize(this.rateSelection.getMinimumSize());
        this.rateSelection.setSelectedIndex(3);
        this.rateSelection.addActionListener(listener);
        this.options.add(Box.createRigidArea(new Dimension(8, 0)));
        this.options.add(Box.createRigidArea(new Dimension(8, 0)));
        this.options.add(this.clearButton);
        this.clearButton.setText(messages.getString("clearOutput"));
        this.clearButton.setActionCommand("clear");
        this.clearButton.addActionListener(listener);
    }

    public int getSerialRate() {
        Object selected = this.rateSelection.getSelectedItem();
        return (selected != null) ? (int) selected : 0;
    }

    public void appendText(byte[] bytes) {
        this.textArea.append(new String(bytes));
        this.textArea.setCaretPosition(this.textArea.getDocument().getLength());
    }

    public void clearText() {
        this.textArea.setText("");
    }
}
