package io.github.arkosammy12.jemu.frontend.gui;

import io.github.arkosammy12.jemu.frontend.config.settings.internal.VideoSize;
import io.github.arkosammy12.jemu.frontend.events.core.AspectRatioSettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.FileLoadedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.ROMEjectedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.TriggerOpenFileEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.VideoSizeChangedEvent;
import net.miginfocom.layout.AlignX;
import net.miginfocom.layout.AlignY;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public class SystemViewport {

    private final MainWindow mainWindow;
    private final JFrame appFrame;
    private final IdleViewport idleViewport;
    private final JPanel viewportPanel;

    @Nullable
    private SystemDisplayComponent systemDisplayComponent;
    private final SystemKeyListener systemKeyListener;

    private Dimension lastFitVideoSizeFrameDimension = null;

    public SystemViewport(MainWindow mainWindow, JFrame appFrame) {
        this.mainWindow = mainWindow;
        this.appFrame = appFrame;
        this.idleViewport = new IdleViewport(mainWindow);
        MigLayout viewportPanelLayout = new MigLayout(new LC().insets("0"));
        this.viewportPanel = new JPanel(viewportPanelLayout);
        this.viewportPanel.setFocusable(true);
        this.viewportPanel.setBackground(Color.BLACK);
        this.viewportPanel.setPreferredSize(new Dimension(960, this.viewportPanel.getHeight()));
        this.viewportPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                SwingUtilities.invokeLater(viewportPanel::requestFocusInWindow);
            }

        });
        this.systemKeyListener = new SystemKeyListener();
        this.viewportPanel.addKeyListener(this.systemKeyListener);
        this.viewportPanel.add(this.idleViewport.getJPanel(), "grow, push");

        mainWindow.onEvent(VideoSizeChangedEvent.class, videoSizeChangedEvent -> {
            VideoSize newVideoSize = videoSizeChangedEvent.videoSize();
            if (newVideoSize == null) {
                return;
            }
            if (this.systemDisplayComponent != null) {
                if (this.isMainWindowMaximized()) {
                    // Cancel the video size selection if there's a system running and the window is maximized
                    SwingUtilities.invokeLater(() -> mainWindow.publishEvent(new VideoSizeChangedEvent(null)));
                } else {
                    this.resizeWindowToFitDisplay(newVideoSize);
                }
            }
        });

        mainWindow.onEvent(AspectRatioSettingChangedEvent.class, _ -> SwingUtilities.invokeLater(this::resizeWindowToDisplay));

        appFrame.addWindowStateListener(e -> {
            // Clear the video size selection if there's a system running and the window is maximized
            if (this.systemDisplayComponent != null && (e.getNewState() & Frame.MAXIMIZED_BOTH) != 0) {
                SwingUtilities.invokeLater(() -> this.mainWindow.publishEvent(new VideoSizeChangedEvent(null)));
            }
        });

        appFrame.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                // Clear the video size selection if there's a system running and the window is resized away from the video size
                if (systemDisplayComponent != null && !appFrame.getSize().equals(lastFitVideoSizeFrameDimension)) {
                    SwingUtilities.invokeLater(() -> mainWindow.publishEvent(new VideoSizeChangedEvent(null)));
                }
            }

        });

    }

    @ApiStatus.Internal
    JPanel getJPanel() {
        return this.viewportPanel;
    }

    public void setSystemKeyListener(@Nullable KeyListener keyListener) {
        SwingUtilities.invokeLater(() -> {
            this.systemKeyListener.setDelegate(keyListener);
            if (keyListener != null) {
                this.viewportPanel.requestFocusInWindow();
            }
        });
    }

    public void setSystemDisplay(@Nullable Supplier<? extends Optional<? extends SystemDisplayComponent>> displaySupplier) {
        SwingUtilities.invokeLater(() -> {
            if (this.systemDisplayComponent != null) {
                this.viewportPanel.remove(this.systemDisplayComponent.getComponent());
                this.systemDisplayComponent = null;
            }

            SystemDisplayComponent newSystemDisplayComponent = null;
            if (displaySupplier != null) {
                newSystemDisplayComponent = displaySupplier.get().orElse(null);
            }

            if (newSystemDisplayComponent != null) {
                this.systemDisplayComponent = newSystemDisplayComponent;
                Component component = this.systemDisplayComponent.getComponent();
                component.setFocusable(true);
                component.setBackground(Color.BLACK);
                component.setMinimumSize(new Dimension(0, 0));
                component.addKeyListener(this.systemKeyListener);
                component.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        SwingUtilities.invokeLater(component::requestFocusInWindow);
                    }
                });
                this.viewportPanel.remove(this.idleViewport.getJPanel());
                this.viewportPanel.add(component, "grow, push");
                SwingUtilities.invokeLater(() -> {
                    component.requestFocusInWindow();
                    if (this.isMainWindowMaximized()) {
                        SwingUtilities.invokeLater(() -> this.mainWindow.publishEvent(new VideoSizeChangedEvent(null)));
                    } else {
                        this.resizeWindowToDisplay();
                    }
                });
            } else {
                this.viewportPanel.add(this.idleViewport.getJPanel(), "grow, push");
            }

            this.viewportPanel.revalidate();
            this.viewportPanel.repaint();
        });
    }

    private void resizeWindowToDisplay() {
        this.resizeWindowToFitDisplay(this.mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().getVideoSize().orElse(null));
    }

    private void resizeWindowToFitDisplay(@Nullable VideoSize videoSize) {
        if (videoSize == null) {
            return;
        }
        Dimension newSize = this.getFrameDimensionsForVideoSize(videoSize);
        if (newSize != null) {
            this.appFrame.setSize(newSize);
            this.lastFitVideoSizeFrameDimension = this.appFrame.getSize();
        }
    }

    private boolean isMainWindowMaximized() {
        return (this.appFrame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
    }

    @Nullable
    private Dimension getFrameDimensionsForVideoSize(@Nullable VideoSize videoSize) {
        if (videoSize == null) {
            return null;
        }
        if (this.systemDisplayComponent == null) {
            return null;
        }

        int displayWidth = (int) Math.round(systemDisplayComponent.getSystemDisplayWidth() * videoSize.getScaleFactor() * systemDisplayComponent.getSystemAspectRatio());
        int displayHeight = systemDisplayComponent.getSystemDisplayHeight() * videoSize.getScaleFactor();

        int horizontalPadding = Math.max(0, this.appFrame.getWidth() - viewportPanel.getWidth());
        int verticalPadding = Math.max(0, this.appFrame.getHeight() - viewportPanel.getHeight());

        return new Dimension(displayWidth + horizontalPadding, displayHeight + verticalPadding);
    }

    private static class SystemKeyListener implements KeyListener {

        @Nullable
        private KeyListener delegate;

        private void setDelegate(@Nullable KeyListener keyListener) {
            this.delegate = keyListener;
        }

        @Override
        public void keyTyped(KeyEvent e) {
            if (this.delegate != null) {
                this.delegate.keyTyped(e);
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (this.delegate != null) {
                this.delegate.keyPressed(e);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (this.delegate != null) {
                this.delegate.keyReleased(e);
            }
        }

    }

    private static class IdleViewport {

        private static final String NO_ROM_FILE_SELECTED_TEXT = "No ROM file selected.";
        private static final String ROM_FILE_SELECTED_TEXT = "Selected ROM file: %s";

        private static final String IDLE_PROMPT_TEXT = "Double-click or drag and drop to select a ROM file...";

        private final JPanel jPanel;
        private final JLabel idleTextLabel;

        private IdleViewport(MainWindow mainWindow) {
            this.jPanel = new JPanel();
            MigLayout layout = new MigLayout();
            layout.setLayoutConstraints(new LC().fill());
            this.jPanel.setLayout(layout);

            this.idleTextLabel = new JLabel();
            this.jPanel.add(this.idleTextLabel, new CC().alignX(AlignX.CENTER).alignY(AlignY.CENTER));
            this.setSelectedRomFile(null);

            MouseListener mouseListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        mainWindow.publishEvent(new TriggerOpenFileEvent());
                    }
                }

            };

            this.idleTextLabel.addMouseListener(mouseListener);
            this.getJPanel().addMouseListener(mouseListener);

            mainWindow.onEvent(FileLoadedEvent.class, fileLoadedEvent -> this.setSelectedRomFile(fileLoadedEvent.loadedFilePath()));

            mainWindow.onEvent(ROMEjectedEvent.class, _ -> this.setSelectedRomFile(null));
        }

        private JPanel getJPanel() {
            return this.jPanel;
        }

        private void setSelectedRomFile(@Nullable Path path) {
            String topText = path == null ? NO_ROM_FILE_SELECTED_TEXT : ROM_FILE_SELECTED_TEXT.formatted(path.toString());
            this.idleTextLabel.setText("<html><center>%s</center><br><center>%s</center></html>".formatted(topText, IDLE_PROMPT_TEXT));
            this.jPanel.revalidate();
            this.jPanel.repaint();
        }

    }

}
