package io.github.arkosammy12.jemu.frontend.util.settings.panel;

import com.formdev.flatlaf.util.SystemFileChooser;
import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.util.settings.UISetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PathUISetting<E extends Event & Supplier<Path>> extends UISetting<Path, E> implements PanelSetting {

    private final JLabel jLabel;
    private final JTextField jTextField = new JTextField();
    private final JButton selectPathButton = new JButton("Select...");
    private final ActionListener jTextFieldActionListener;

    public PathUISetting(MainWindow mainWindow, PathSelectionMode pathSelectionMode, @NotNull String name, @NotNull Path startingValue, @Nullable Class<E> eventClass, @Nullable Predicate<E> eventPredicate, @NotNull Function<? super Path, ? extends Event> eventSupplier) {
        super(mainWindow, name, startingValue, eventClass, eventPredicate, eventSupplier);
        this.jLabel = new JLabel(name);
        this.jTextFieldActionListener = _ -> this.onPathChanged(Paths.get(this.jTextField.getText()));
        this.jTextField.addActionListener(this.jTextFieldActionListener);
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
    }

    @Override
    public void setValue(Path value) {
        SwingUtilities.invokeLater(() -> {
            this.jTextField.removeActionListener(this.jTextFieldActionListener);
            this.jTextField.setText(value.toString());
            this.jTextField.addActionListener(this.jTextFieldActionListener);
        });
    }

    @Override
    public void addToJPanel(JPanel jPanel, Object... constraints) {
        jPanel.add(this.jLabel, constraints.length >= 1 ? constraints[0] : null);
        jPanel.add(this.jTextField, constraints.length >= 2 ? constraints[1] : null);
        jPanel.add(this.selectPathButton, constraints.length >= 3 ? constraints[2] : null);
    }

    private void onPathChanged(Path path) {
        this.mainWindow.publishEvent(this.eventSupplier.apply(path));
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
