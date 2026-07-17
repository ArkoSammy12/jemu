package io.github.arkosammy12.jemu.frontend.gui.internal.menus;

import com.formdev.flatlaf.icons.FlatFileViewFileIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import io.github.arkosammy12.jemu.frontend.config.SystemDescriptor;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.FileLoadedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.ROMEjectedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.TriggerOpenFileEvent;
import io.github.arkosammy12.jemu.frontend.gui.swing.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.swing.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.swing.managers.FileManager;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class FileMenu extends MenuBarMenu implements FileManager {

    private static final int RECENT_FILES_SIZE = 10;

    private final MainWindow mainWindow;

    @Nullable
    private volatile Path currentRomPath;

    private final JMenu openRecentMenu;
    private final JMenuItem clearRecentsButton;
    private final JMenuItem ejectRomButton;
    private final CircularFifoQueue<Path> recentFilePaths = new CircularFifoQueue<>(RECENT_FILES_SIZE);

    public FileMenu(MainWindow mainWindow, JFrame jFrame) {
        super();

        this.mainWindow = mainWindow;

        this.jMenu.setText("File");
        this.jMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK, true));
        openItem.setIcon(new FlatFileViewFileIcon());
        openItem.setToolTipText("Load binary ROM data from a file.");
        openItem.addActionListener(_ -> {
            SystemFileChooser chooser = new SystemFileChooser();
            Collection<String> fileExtensions = mainWindow.getSystemDescriptors().stream().map(SystemDescriptor::getFileExtensions).flatMap(Collection::stream).toList();
            if (!fileExtensions.isEmpty()) {
                chooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("ROMs", fileExtensions.toArray(String[]::new)));
            }
            this.mainWindow.getConfig().getState().getFileState().getCurrentDirectoryPath().ifPresent(path -> chooser.setCurrentDirectory(path.toFile()));
            if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this.jMenu)) == JFileChooser.APPROVE_OPTION) {
                Path selectedRomPath = chooser.getSelectedFile().toPath();
                this.loadFile(selectedRomPath);
                this.addRecentFilePath(selectedRomPath);
                this.mainWindow.getConfig().getState().getFileState().setCurrentDirectoryPath(selectedRomPath.getParent());
            }
        });

        jFrame.setTransferHandler(new TransferHandler() {

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!this.canImport(support)) {
                    return false;
                }
                try {
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    Path filePath = files.getFirst().toPath();
                    loadFile(filePath);
                    addRecentFilePath(filePath);
                    return true;
                } catch (Exception e) {
                    Logger.error("Failed to accept drag-and-drop file! {}", e);
                    return false;
                }
            }

        });

        this.openRecentMenu = new JMenu("Open Recent");

        this.clearRecentsButton = new JMenuItem("Clear all recents");
        clearRecentsButton.setEnabled(false);
        clearRecentsButton.addActionListener(_ -> {
            this.recentFilePaths.clear();
            this.rebuildOpenRecentMenu();
        });

        this.ejectRomButton = new JMenuItem("Eject ROM");
        this.ejectRomButton.setEnabled(false);
        this.ejectRomButton.addActionListener(_ -> {
            this.currentRomPath = null;
            this.ejectRomButton.setEnabled(false);
            mainWindow.publishEvent(new ROMEjectedEvent());
        });

        JRadioButtonMenuItem resetOnROMFileSelect = new JRadioButtonMenuItem("Reset on ROM file select");
        resetOnROMFileSelect.addActionListener(_ -> this.mainWindow.getConfig().getInternalPreferenceSettings().getInternalFileSettings().setResetOnRomFileSelect(resetOnROMFileSelect.isSelected()));

        JMenuItem exitButton = new JMenuItem("Exit");
        exitButton.addActionListener(_ -> jFrame.dispatchEvent(new WindowEvent(jFrame, WindowEvent.WINDOW_CLOSING)));

        openRecentMenu.add(clearRecentsButton);
        this.jMenu.add(openItem);
        this.jMenu.add(openRecentMenu);
        this.jMenu.add(ejectRomButton);
        this.jMenu.addSeparator();
        this.jMenu.add(resetOnROMFileSelect);
        this.jMenu.addSeparator();
        this.jMenu.add(exitButton);

        this.mainWindow.getConfig().getState().getFileState().getRecentFilePaths().forEach(this::addRecentFilePath);

        resetOnROMFileSelect.setSelected(this.mainWindow.getConfig().getInternalPreferenceSettings().getInternalFileSettings().getResetOnROMFileSelect());

        mainWindow.onEvent(TriggerOpenFileEvent.class, _ -> openItem.doClick());
    }

    @Override
    public Optional<Path> getSelectedRomPath() {
        return Optional.ofNullable(this.currentRomPath);
    }

    @Override
    public void loadFile(@NotNull Path filePath) {
        SwingUtilities.invokeLater(() -> {
            this.currentRomPath = filePath;
            this.ejectRomButton.setEnabled(true);
            this.mainWindow.publishEvent(new FileLoadedEvent(filePath));
        });
    }

    private void addRecentFilePath(Path filePath) {
        if (this.recentFilePaths.contains(filePath)) {
            return;
        }
        this.recentFilePaths.offer(filePath);
        this.mainWindow.getConfig().getState().getFileState().setRecentFilePaths(this.recentFilePaths);
        this.rebuildOpenRecentMenu();
    }

    private void rebuildOpenRecentMenu() {
        this.openRecentMenu.removeAll();
        for (Path recentFilePath : this.recentFilePaths.stream().toList().reversed()) {
            JMenuItem recentFileItem = new JMenuItem(recentFilePath.getFileName().toString());
            recentFileItem.setToolTipText(recentFilePath.toString());
            recentFileItem.addActionListener(_ -> this.loadFile(recentFilePath));
            this.openRecentMenu.add(recentFileItem);
        }
        if (!this.recentFilePaths.isEmpty()) {
            this.openRecentMenu.addSeparator();
            this.clearRecentsButton.setEnabled(true);
        } else {
            this.clearRecentsButton.setEnabled(false);
        }
        this.openRecentMenu.add(this.clearRecentsButton);
        this.openRecentMenu.revalidate();
        this.openRecentMenu.repaint();
    }

}
