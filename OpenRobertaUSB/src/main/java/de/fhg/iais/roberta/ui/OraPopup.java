package de.fhg.iais.roberta.ui;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import java.awt.Component;

import static de.fhg.iais.roberta.ui.main.MainView.IMAGES_PATH;

public class OraPopup extends JOptionPane {
    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 250;

    public static int showPopup(Component component, String title, String text, Icon icon, String[] txtButtons) {
        OraButton[] buttons = new OraButton[txtButtons.length];

        for ( int i = 0; i < txtButtons.length; i++ ) {
            OraButton oraButton = new OraButton();
            oraButton.setText(txtButtons[i]);
            oraButton.addActionListener(e -> {
                JOptionPane pane = (JOptionPane) ((Component) e.getSource()).getParent().getParent();
                pane.setValue(oraButton);
            });

            buttons[i] = oraButton;
        }
        String formattedText = "<html><body><p style='width: " + WIDTH + "px;'>" + text + "</p></body></html>";
        formattedText = formattedText.replace("\n", "<br/>");

        return JOptionPane.showOptionDialog(component, formattedText, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, icon, buttons, buttons[0]);
    }

    public static int showPopup(Component component, String title, String text, Icon icon) {
        Icon displayedIcon;
        if ( icon == null ) {
            displayedIcon = new ImageIcon(OraPopup.class.getClassLoader().getResource(IMAGES_PATH + "warning-outline.png"));
        } else {
            displayedIcon = icon;
        }
        return showPopup(component, title, text, displayedIcon, new String[] {
            "OK"
        });
    }
}
