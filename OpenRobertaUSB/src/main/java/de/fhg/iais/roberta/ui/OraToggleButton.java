package de.fhg.iais.roberta.ui;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

class OraToggleButton extends JToggleButton {
    private static final long serialVersionUID = 1L;

    OraToggleButton() {
        this.setBackground(Color.decode("#afca04"));
        this.setFont(new Font("Arial", Font.PLAIN, 16));
        this.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        this.setForeground(Color.white);
        this.setRolloverEnabled(true);
        UIManager.put("ToggleButton.select", Color.decode("#afca04"));
        SwingUtilities.updateComponentTreeUI(this);
        this.getModel().addChangeListener(e -> {
            ButtonModel b = (ButtonModel) e.getSource();
            if ( b.isRollover() ) {
                setBackground(Color.decode("#b7d032"));
            } else {
                setBackground(Color.decode("#afca04"));
            }
        });
    }
}
