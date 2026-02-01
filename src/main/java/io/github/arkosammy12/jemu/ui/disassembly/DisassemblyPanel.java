package io.github.arkosammy12.jemu.ui.disassembly;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.config.DataManager;
import io.github.arkosammy12.jemu.config.initializers.ApplicationInitializer;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.config.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.main.MainWindow;
import net.miginfocom.layout.AlignX;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class DisassemblyPanel extends JPanel implements EmulatorInitializerConsumer {

    private final DisassemblerTable disassemblerTable;
    private final JCheckBox followCheckbox;

    public DisassemblyPanel(Jemu jemu, MainWindow mainWindow) {
        MigLayout migLayout = new MigLayout(new LC().insets("0"));
        super(migLayout);

        this.disassemblerTable = new DisassemblerTable(jemu);

        this.setFocusable(false);
        this.setPreferredSize(new Dimension(this.getWidth(), 100));
        this.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                "Disassembler",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.getFont().deriveFont(Font.BOLD)));

        JLabel goToAddressLabel = new JLabel("Go to address: ");
        JTextField goToAddressField = new JTextField();
        goToAddressField.addActionListener(_ -> {
            String text = goToAddressField.getText().trim();
            if (text.isEmpty()) {
                return;
            }
            int address;
            try {
                address = Integer.decode(text);
            } catch (NumberFormatException _) {
                JOptionPane.showMessageDialog(
                        mainWindow,
                        "The address must be valid integer!",
                        "Invalid address value",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            if (!this.disassemblerTable.isAddressVisible(address)) {
                JOptionPane.showMessageDialog(
                        mainWindow,
                        "No disassembly currently exists for the provided instruction address!",
                        "Invalid address value",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            this.disassemblerTable.scrollToAddress(address);
        });
        goToAddressField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(goToAddressField::selectAll);
            }

        });

        JButton clearBreakpointsButton = new JButton("Clear Breakpoints");
        clearBreakpointsButton.addActionListener(_ -> this.disassemblerTable.clearBreakpoints());

        this.followCheckbox = new JCheckBox("Follow");
        this.followCheckbox.setFocusable(false);
        this.followCheckbox.addChangeListener(_ -> goToAddressField.setEnabled(!this.followCheckbox.isSelected()));

        JScrollPane disassemblerScrollPane = new JScrollPane(this.disassemblerTable);

        this.add(this.followCheckbox, new CC().growX().pushX().alignX(AlignX.CENTER));
        this.add(goToAddressLabel, new CC().split(2).alignX(AlignX.CENTER));
        this.add(goToAddressField, new CC().growX().pushX().alignX(AlignX.CENTER));
        this.add(clearBreakpointsButton, new CC().growX().pushX().alignX(AlignX.CENTER).wrap());
        this.add(disassemblerScrollPane, new CC().grow().push().spanX());

        jemu.addFrameListener(_ -> this.onFrame());

        jemu.addShutdownListener(() -> {
            DataManager dataManager = jemu.getDataManager();
            dataManager.putPersistent(DataManager.DISASSEMBLER_FOLLOW, String.valueOf(this.followCheckbox.isSelected()));
        });

    }

    private void onFrame() {
        SwingUtilities.invokeLater(() -> {
            if (!this.isShowing()) {
                return;
            }
            if (this.followCheckbox.isSelected()) {
                this.disassemblerTable.scrollToCurrentAddress();
            }
        });
    }

    @Override
    public void accept(EmulatorInitializer initializer) {
        if (initializer instanceof ApplicationInitializer applicationInitializer) {
            applicationInitializer.getDisassemblerFollowing().ifPresent(this.followCheckbox::setSelected);
        }
    }

}
