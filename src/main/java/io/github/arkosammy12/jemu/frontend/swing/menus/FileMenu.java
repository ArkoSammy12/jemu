package io.github.arkosammy12.jemu.frontend.swing.menus;

import com.formdev.flatlaf.icons.FlatFileViewFileIcon;
import com.formdev.flatlaf.util.SystemFileChooser;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class FileMenu extends MenuBarMenu {

    @Nullable
    private volatile Path currentRomPath;

    @Nullable
    private Path currentDirectory;

    private final JMenu openRecentMenu;
    private final JMenuItem clearRecentsButton;
    private final CircularFifoQueue<Path> recentFilePaths = new CircularFifoQueue<>(10);

    public FileMenu(JFrame jFrame) {
        super();

        this.jMenu.setText("File");
        this.jMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(_ -> {
            SystemFileChooser chooser = new SystemFileChooser();
            // TODO: File filters
            if (this.currentDirectory != null) {
                chooser.setCurrentDirectory(this.currentDirectory.toFile());
            }
            if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this.jMenu)) == JFileChooser.APPROVE_OPTION) {
                Path selectedRomPath = chooser.getSelectedFile().toPath();
                this.currentDirectory = selectedRomPath.getParent();


            }
            openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK, true));
            openItem.setIcon(new FlatFileViewFileIcon());
            openItem.setToolTipText("Load binary ROM data from a file.");
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

        // this.mainWindow.setTitleSection(1, "No file selected");

        openRecentMenu.add(clearRecentsButton);
        this.jMenu.add(openItem);
        this.jMenu.add(openRecentMenu);
    }

    public Optional<Path> getSelectedRomPath() {
        return Optional.ofNullable(this.currentRomPath);
    }

    private void loadFile(Path filePath) {
        this.currentRomPath = filePath;
        //this.mainWindow.setTitleSection(1, filePath.getFileName().toString());
    }

    private void addRecentFilePath(Path filePath) {
        if (this.recentFilePaths.contains(filePath)) {
            return;
        }
        this.recentFilePaths.offer(filePath);
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
