package de.fhg.iais.roberta.ui;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public class OraPopup extends JOptionPane {

    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 250;

    public static int showPopup(Component component, String title, String text, Icon icon, String[] txtButtons) {
        OraButton buttons[] = new OraButton[txtButtons.length];

        for ( int i = 0; i < txtButtons.length; i++ ) {
            OraButton oraButton = new OraButton();
            oraButton.setText(txtButtons[i]);
            oraButton.addActionListener(e -> {
                JOptionPane pane = (JOptionPane) ((JComponent) e.getSource()).getParent().getParent();
                pane.setValue(oraButton);
            });

            buttons[i] = oraButton;
        }
        UIManager.put("OptionPane.background", Color.white);
        UIManager.put("OptionPane.messageFont", new Font("Arial", Font.PLAIN, 14));
        UIManager.put("OptionPane.buttonFont", new Font("Arial", Font.PLAIN, 16));
        UIManager.put("Panel.background", Color.white);
        text = "<html><body><p style='width: " + WIDTH + "px;'>" + text + "</p></body></html>";
        text = text.replace("\n", "<br/>");

        return JOptionPane.showOptionDialog(component, text, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, icon, buttons, buttons[0]);
    }

    public static int showPopup(Component component, String title, String text, Icon icon) {
        if ( icon == null ) {
            icon = new ImageIcon(OraPopup.class.getClassLoader().getResource("images/warning-outline.png"));
        }
        return showPopup(component, title, text, icon, new String[] {
            "OK"
        });
    }
}
