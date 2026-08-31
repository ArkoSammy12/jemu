package io.github.arkosammy12.jemu.frontend.util.settings.panel;

import com.formdev.flatlaf.util.SystemFileChooser;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.UISetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PathUISetting<E extends Event & Supplier<@Nullable Path>> extends UISetting<@Nullable Path, E> implements PanelSetting {

    private final JLabel jLabel;
    private final JTextField jTextField = new JTextField();
    private final JButton selectPathButton = new JButton("Select...");
    private final ActionListener jTextFieldActionListener;

    public PathUISetting(EventPublisher eventPublisher, PathSelectionMode pathSelectionMode, @NotNull String name, @Nullable Path startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super @Nullable Path, ? extends Event> eventSupplier) {
        super(eventPublisher, name, eventClass, eventPredicate, eventSupplier);
        this.jLabel = new JLabel(name);
        Runnable textFieldCommitRunnable = () -> {
            String text = this.jTextField.getText().trim();
            this.onPathChanged(text.isBlank() ? null : Paths.get(text));
        };
        this.jTextFieldActionListener = _ -> textFieldCommitRunnable.run();
        this.jTextField.addActionListener(this.jTextFieldActionListener);
        this.jTextField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                textFieldCommitRunnable.run();
            }

        });
        this.jTextField.setColumns(40);
        this.jTextField.setMinimumSize(this.jTextField.getPreferredSize());
        this.jTextField.setMaximumSize(null);
        this.selectPathButton.addActionListener(_ -> {
            SystemFileChooser fileChooser = new SystemFileChooser();
            fileChooser.setFileSelectionMode(pathSelectionMode.getFileSelectionMode());
            fileChooser.setDialogTitle(pathSelectionMode.getDialogTitle());
            if (fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(this.selectPathButton)) == JFileChooser.APPROVE_OPTION) {
                Path selectedPath = fileChooser.getSelectedFile().toPath();
                this.onPathChanged(selectedPath);
                this.setValue(selectedPath);
            }
        });
        this.setValue(startingValue);
    }

    @Override
    public void setValue(@Nullable Path value) {
        SwingUtilities.invokeLater(() -> {
            this.jTextField.removeActionListener(this.jTextFieldActionListener);
            this.jTextField.setText(value == null ? "" : value.toString());
            this.jTextField.addActionListener(this.jTextFieldActionListener);
        });
    }

    @Override
    public void addToJPanel(JPanel jPanel, Object... constraints) {
        jPanel.add(this.jLabel, constraints.length >= 1 ? constraints[0] : null);
        jPanel.add(this.jTextField, constraints.length >= 2 ? constraints[1] : null);
        jPanel.add(this.selectPathButton, constraints.length >= 3 ? constraints[2] : null);
    }

    public JTextField getJTextField() {
        return this.jTextField;
    }

    public JButton getSelectPathButton() {
        return this.selectPathButton;
    }

    private void onPathChanged(@Nullable Path path) {
        this.eventPublisher.publishEvent(this.eventSupplier.apply(path));
    }

    public enum PathSelectionMode {
        FILES_ONLY(JFileChooser.FILES_ONLY, "Select a File"),
        DIRECTORIES_ONLY(JFileChooser.DIRECTORIES_ONLY, "Select a Directory"),
        FILES_AND_DIRECTORIES(JFileChooser.FILES_AND_DIRECTORIES, "Select a File or Directory")
        ;

        private final int fileSelectionMode;
        private final String dialogTitle;

        PathSelectionMode(int fileSelectionMode, String dialogTitle) {
            this.fileSelectionMode = fileSelectionMode;
            this.dialogTitle = dialogTitle;
        }

        private int getFileSelectionMode() {
            return this.fileSelectionMode;
        }

        private String getDialogTitle() {
            return this.dialogTitle;
        }

    }

}
