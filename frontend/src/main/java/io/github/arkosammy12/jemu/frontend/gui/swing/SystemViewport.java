package io.github.arkosammy12.jemu.frontend.gui.swing;

import io.github.arkosammy12.jemu.frontend.config.settings.internal.VideoSize;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.InternalVideoSizeChangedEvent;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Supplier;

public class SystemViewport {

    private final MainWindow mainWindow;
    private final JPanel viewportPanel;

    @Nullable
    private SystemDisplayComponent systemDisplayComponent;
    private final SystemKeyListener systemKeyListener;

    private Dimension lastFitVideoSizeFrameDimension = null;

    public SystemViewport(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
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

        mainWindow.onEvent(InternalVideoSizeChangedEvent.class, internalVideoSizeChangedEvent -> {
            VideoSize newVideoSize = internalVideoSizeChangedEvent.videoSize();
            if (newVideoSize == null) {
                return;
            }
            if (this.systemDisplayComponent != null) {
                if (this.isMainWindowMaximized()) {
                    // Cancel the video size selection if there's a system running and the window is maximized
                    SwingUtilities.invokeLater(() -> mainWindow.pushEvent(new InternalVideoSizeChangedEvent(null)));
                } else {
                    this.resizeWindowToFitDisplay(newVideoSize);
                }
            }
        });

        mainWindow.getJFrame().addWindowStateListener(e -> {
            // Clear the video size selection if there's a system running and the window is maximized
            if (this.systemDisplayComponent != null && (e.getNewState() & Frame.MAXIMIZED_BOTH) != 0) {
                SwingUtilities.invokeLater(() -> this.mainWindow.pushEvent(new InternalVideoSizeChangedEvent(null)));
            }
        });

        mainWindow.getJFrame().addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                // Clear the video size selection if there's a system running and the window is resized away from the video size
                if (systemDisplayComponent != null && !mainWindow.getJFrame().getSize().equals(lastFitVideoSizeFrameDimension)) {
                    SwingUtilities.invokeLater(() -> mainWindow.pushEvent(new InternalVideoSizeChangedEvent(null)));
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

    public void setSystemDisplay(@Nullable Supplier<@NotNull SystemDisplayComponent> displaySupplier) {
        SwingUtilities.invokeLater(() -> {
            if (this.systemDisplayComponent != null) {
                this.viewportPanel.remove(this.systemDisplayComponent.getComponent());
                this.systemDisplayComponent = null;
            }

            if (displaySupplier != null) {
                this.systemDisplayComponent = displaySupplier.get();
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
                this.viewportPanel.add(component, "grow, push");
                SwingUtilities.invokeLater(() -> {
                    component.requestFocusInWindow();
                    if (this.isMainWindowMaximized()) {
                        SwingUtilities.invokeLater(() -> this.mainWindow.pushEvent(new InternalVideoSizeChangedEvent(null)));
                    } else {
                        this.resizeWindowToFitDisplay(this.mainWindow.getConfig().getInternalPreferenceSettings().getInternalVideoSettings().getVideoSize().orElse(null));
                    }
                });
            }

            this.viewportPanel.revalidate();
            this.viewportPanel.repaint();
        });
    }

    private void resizeWindowToFitDisplay(@Nullable VideoSize videoSize) {
        if (videoSize == null) {
            return;
        }
        Dimension newSize = this.getFrameDimensionsForVideoSize(videoSize);
        if (newSize != null) {
            this.mainWindow.getJFrame().setSize(newSize);
            this.lastFitVideoSizeFrameDimension = this.mainWindow.getJFrame().getSize();
        }
    }

    private boolean isMainWindowMaximized() {
        return (this.mainWindow.getJFrame().getExtendedState() & Frame.MAXIMIZED_BOTH) != 0;
    }

    @Nullable
    private Dimension getFrameDimensionsForVideoSize(@Nullable VideoSize videoSize) {
        if (videoSize == null) {
            return null;
        }
        if (this.systemDisplayComponent == null) {
            return null;
        }

        int displayWidth = systemDisplayComponent.getSystemDisplayWidth() * videoSize.getScaleFactor();
        int displayHeight = systemDisplayComponent.getSystemDisplayHeight() * videoSize.getScaleFactor();

        int horizontalPadding = Math.max(0, this.mainWindow.getJFrame().getWidth() - viewportPanel.getWidth());
        int verticalPadding = Math.max(0, this.mainWindow.getJFrame().getHeight() - viewportPanel.getHeight());

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

}
