package io.github.arkosammy12.jemu.frontend.ui.util;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Optional;

public class BooleanMenu extends JMenu {

    private volatile Boolean state = null;

    private final JRadioButtonMenuItem unspecifiedItem;
    private final JRadioButtonMenuItem enabledItem;
    private final JRadioButtonMenuItem disabledItem;

    public BooleanMenu(String name) {
        super(name);

        this.unspecifiedItem = new JRadioButtonMenuItem("Unspecified");
        this.enabledItem = new JRadioButtonMenuItem("Enabled");
        this.disabledItem = new JRadioButtonMenuItem("Disabled");

        unspecifiedItem.setSelected(true);

        unspecifiedItem.addActionListener(_ -> this.setState(null));
        unspecifiedItem.setMnemonic(KeyEvent.VK_U);

        enabledItem.addActionListener(_ -> this.setState(true));
        enabledItem.setMnemonic(KeyEvent.VK_E);

        disabledItem.addActionListener(_ -> this.setState(false));
        disabledItem.setMnemonic(KeyEvent.VK_D);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(this.unspecifiedItem);
        buttonGroup.add(this.enabledItem);
        buttonGroup.add(this.disabledItem);

        this.add(this.unspecifiedItem);
        this.add(this.enabledItem);
        this.add(this.disabledItem);
    }

    public void setState(Boolean val) {
        this.state = val;
        this.unspecifiedItem.setSelected(false);
        this.enabledItem.setSelected(false);
        this.disabledItem.setSelected(false);
        if (val == null) {
            this.unspecifiedItem.setSelected(true);
        } else if (val) {
            this.enabledItem.setSelected(true);
        } else {
            this.disabledItem.setSelected(true);
        }
    }

     public Optional<Boolean> getState() {
        return Optional.ofNullable(this.state);
     }

}
