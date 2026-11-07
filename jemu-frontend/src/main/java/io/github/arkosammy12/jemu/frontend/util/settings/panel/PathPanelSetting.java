package io.github.arkosammy12.jemu.frontend.util.settings.panel;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.util.EventPublisher;
import io.github.arkosammy12.jemu.frontend.util.settings.PathUISetting;
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

public class PathPanelSetting<E extends Event & Supplier<@Nullable Path>> extends PathUISetting<E> implements PanelSetting {

    private final JLabel jLabel;
    private final JTextField jTextField = new JTextField();
    private final JButton selectPathButton = new JButton("Select...");
    private final JButton clearPathButton = new JButton("Clear");
    private final ActionListener jTextFieldActionListener;

    public PathPanelSetting(EventPublisher eventPublisher, PathSelectionMode pathSelectionMode, @NotNull String name, @Nullable Path startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super @Nullable Path, ? extends Event> eventSupplier) {
        super(eventPublisher, pathSelectionMode, name, eventClass, eventPredicate, eventSupplier);
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
            Path path = this.showFileDialog(SwingUtilities.getWindowAncestor(this.selectPathButton));
            this.setValue(path);
            this.onPathChanged(path);
        });
        this.clearPathButton.addActionListener(_ -> {
            this.setValue(null);
            this.onPathChanged(null);
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
        jPanel.add(this.clearPathButton, constraints.length >= 4 ? constraints[3] : null);
    }

    public void setEnabled(boolean value) {
        SwingUtilities.invokeLater(() -> {
            this.jTextField.setEnabled(value);
            this.selectPathButton.setEnabled(value);
            this.clearPathButton.setEnabled(value);
        });
    }

    private void onPathChanged(@Nullable Path path) {
        this.eventPublisher.publishEvent(this.eventSupplier.apply(path));
    }

}
