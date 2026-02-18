package io.github.arkosammy12.jemu.frontend.ui.menus;

import com.formdev.flatlaf.icons.*;
import com.formdev.flatlaf.util.SystemFileChooser;
import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.application.adapters.SystemAdapter;
import io.github.arkosammy12.jemu.application.io.DataManager;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.application.io.initializers.ApplicationInitializer;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.frontend.ui.MainWindow;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.tinylog.Logger;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class FileMenu extends JMenu implements EmulatorInitializerConsumer {

    private static final String[] FILE_EXTENSIONS = {"bin"};

    private final MainWindow mainWindow;
    private final AtomicReference<Path> romPath = new AtomicReference<>(null);
    private final AtomicReference<byte[]> rawRom = new AtomicReference<>(null);
    private Path currentDirectory;

    private final JMenu openRecentMenu;
    private final JMenuItem clearRecentButton;
    private final CircularFifoQueue<Path> recentFilePaths = new CircularFifoQueue<>(10);

    public FileMenu(Jemu jemu, MainWindow mainWindow) {
        super("File");
        this.mainWindow = mainWindow;

        this.setMnemonic(KeyEvent.VK_F);

        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(_ -> {
            SystemFileChooser chooser = new SystemFileChooser();
            chooser.setFileFilter(new SystemFileChooser.FileNameExtensionFilter("ROMs", FILE_EXTENSIONS));
            if (this.currentDirectory != null) {
                chooser.setCurrentDirectory(this.currentDirectory.toFile());
            }
            if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
                Path selectedFilePath =  chooser.getSelectedFile().toPath();
                this.currentDirectory = selectedFilePath.getParent();
                loadFile(selectedFilePath);
                addRecentFilePath(selectedFilePath);
            }
        });
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK, true));
        openItem.setIcon(new FlatFileViewFileIcon());
        openItem.setToolTipText("Load binary ROM data from a file.");

        mainWindow.setTransferHandler(new TransferHandler() {

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
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

        this.clearRecentButton = new JMenuItem("Clear all recents");
        this.clearRecentButton.setEnabled(false);
        this.clearRecentButton.addActionListener(_ -> {
            this.recentFilePaths.clear();
            this.rebuildOpenRecentMenu();
        });

        this.mainWindow.setTitleSection(1, "No file selected");

        this.openRecentMenu.add(this.clearRecentButton);
        this.add(openItem);
        this.add(openRecentMenu);

        jemu.addShutdownListener(() -> {
            DataManager dataManager = jemu.getDataManager();
            dataManager.putPersistent(DataManager.RECENT_FILES, this.recentFilePaths.stream().map(Path::toString).toArray(String[]::new));
            if (this.currentDirectory != null) {
                dataManager.putPersistent(DataManager.CURRENT_DIRECTORY, this.currentDirectory.toString());
            }
        });
    }

    public Optional<Path> getRomPath() {
        return Optional.ofNullable(this.romPath.get());
    }

    public Optional<byte[]> getRawRom() {
        byte[] val = this.rawRom.get();
        if (val == null) {
            return Optional.empty();
        }
        return Optional.of(Arrays.copyOf(val, val.length));
    }

    @Override
    public void accept(EmulatorInitializer initializer) {
        initializer.getRawRom().ifPresent(rawRom -> this.rawRom.set(Arrays.copyOf(rawRom, rawRom.length)));
        initializer.getRomPath().map(Path::toAbsolutePath).ifPresent(this::loadFile);
        if (initializer instanceof ApplicationInitializer applicationInitializer) {
            applicationInitializer.getRecentFiles().ifPresent(recentFiles -> {
                this.recentFilePaths.clear();
                recentFiles.forEach(file -> {
                    if (this.recentFilePaths.contains(file)) {
                        return;
                    }
                    this.recentFilePaths.offer(file);
                });
                this.rebuildOpenRecentMenu();
            });
            applicationInitializer.getCurrentDirectory().ifPresent(dir -> {
                if (!dir.isBlank()) {
                    this.currentDirectory = Path.of(dir);
                }
            });
        }
    }

    private void loadFile(Path filePath) {
        this.romPath.set(filePath);
        this.rawRom.set(SystemAdapter.readRawRom(filePath));
        this.mainWindow.setTitleSection(1, filePath.getFileName().toString());
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
            this.clearRecentButton.setEnabled(true);
        } else {
            this.clearRecentButton.setEnabled(false);
        }
        this.openRecentMenu.add(this.clearRecentButton);
        this.openRecentMenu.revalidate();
        this.openRecentMenu.repaint();
    }

}
