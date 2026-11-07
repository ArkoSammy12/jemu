package io.github.arkosammy12.jemu.frontend.util;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {

    public StatusBar() {
        super(new MigLayout(new LC().insets("1").fill(), new AC(), new AC().grow()));
    }

    public JLabel createLabel() {
        return this.createLabel(new CC().grow().push().gap("5"));
    }

    public JLabel createLabel(CC componentConstraints) {
        JLabel field = new JLabel();
        field.setBorder(null);
        field.setFocusable(false);
        field.setBackground(null);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        this.add(field, componentConstraints);
        return field;
    }

}
