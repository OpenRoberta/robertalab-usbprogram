package de.fhg.iais.roberta.ui.main;

import de.fhg.iais.roberta.ui.OraButton;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

class HelpDialog extends JDialog {
    static final String CMD_SELECT_EV3 = "select_ev3";
    static final String CMD_SELECT_OTHER = "select_other";

    private final JPanel pnlGreet = new JPanel();
    private final JLabel lblGreet = new JLabel();

    private final JPanel pnlInfo = new JPanel();
    private final JTextArea txtAreaInfo = new JTextArea();

    private final JPanel pnlRobots = new JPanel();
    private final OraButton butEv3 = new OraButton();
    private final OraButton butOther = new OraButton();

    HelpDialog(Frame frame, ResourceBundle messages, ActionListener listener) {
        super(frame);
        // General
        this.setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        this.setResizable(false);

        this.add(this.pnlGreet);
        this.pnlGreet.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.pnlGreet.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        this.pnlGreet.add(this.lblGreet);
        this.lblGreet.setText(messages.getString("helpConnectionGreeting"));

        this.add(this.pnlInfo);
        this.pnlInfo.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.pnlInfo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        this.pnlInfo.add(this.txtAreaInfo);
        this.pnlInfo.setPreferredSize(new Dimension(300, 70));
        this.txtAreaInfo.setText(messages.getString("helpConnection"));
        this.txtAreaInfo.setLineWrap(true);
        this.txtAreaInfo.setWrapStyleWord(true);
        this.txtAreaInfo.setColumns(20);
        this.txtAreaInfo.setEditable(false);

        this.add(this.pnlRobots);
        this.pnlRobots.setLayout(new FlowLayout(FlowLayout.LEADING));
        this.pnlRobots.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        this.pnlRobots.add(this.butEv3);
        this.butEv3.setActionCommand(CMD_SELECT_EV3);
        this.butEv3.addActionListener(listener);
        this.butEv3.setText(messages.getString("ev3"));
        this.pnlRobots.add(this.butOther);
        this.butOther.setActionCommand(CMD_SELECT_OTHER);
        this.butOther.addActionListener(listener);
        this.butOther.setText(messages.getString("other"));

        setUndecorated(true);
        pack();
    }
}
