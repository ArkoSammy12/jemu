package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.panel;

import io.github.arkosammy12.jemu.frontend.events.BaseEvent;
import io.github.arkosammy12.jemu.frontend.events.SettingChangedEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.ApplySettingsWindowChanges;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.CloseSettingsWindowEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.OpenDataDirectoryEvent;
import io.github.arkosammy12.jemu.frontend.events.internal.ui.OpenSettingsWindowEvent;
import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;
import net.miginfocom.layout.CC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SettingsWindow extends JFrame {

    public SettingsWindow(MainWindow mainWindow, State eventPublisher) {
        super("Settings");
        this.getContentPane().setLayout(new MigLayout("insets 0, gap 0", "[grow]", "[grow][]"));

        JPanel contentPanel = new JPanel(new MigLayout());

        JTabbedPane jTabbedPane = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);

        jTabbedPane.add(new AudioSettingsPanel(mainWindow, eventPublisher), "Audio");
        jTabbedPane.add(new VideoSettingsPanel(mainWindow, eventPublisher), "Video");

        for (SystemDescriptor systemDescriptor : mainWindow.getSystemCatalog().getSystemDescriptors()) {
            systemDescriptor.getSettingsWindowContents().ifPresent(contentsSupplier -> jTabbedPane.add(contentsSupplier.apply(eventPublisher), systemDescriptor.getName()));
        }

        contentPanel.add(jTabbedPane);

        this.getContentPane().add(contentPanel, new CC().growX().push().wrap());

        JPanel buttonPanel = new JPanel(new MigLayout("insets 6"));
        buttonPanel.setBackground(buttonPanel.getBackground().darker());
        this.getContentPane().add(buttonPanel, new CC().growX());

        JButton openDataDirectoryButton = new JButton("Open data directory...");
        openDataDirectoryButton.addActionListener(_ -> eventPublisher.publishEvent(new OpenDataDirectoryEvent()));
        buttonPanel.add(openDataDirectoryButton, new CC().split(4).pushX());

        /*
        JButton resetDefaultsButton = new JButton("Reset to default");
        resetDefaultsButton.addActionListener(_ -> {});
        buttonPanel.add(resetDefaultsButton);
         */

        JButton okButton = new JButton("Ok");
        okButton.addActionListener(_ -> eventPublisher.publishEvent(new CloseSettingsWindowEvent(CloseSettingsWindowEvent.Type.SAVE_CHANGES)));
        buttonPanel.add(okButton, new CC().gapLeft("push"));

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(_ -> eventPublisher.publishEvent(new CloseSettingsWindowEvent(CloseSettingsWindowEvent.Type.DISCARD_CHANGES)));
        buttonPanel.add(cancelButton);

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(_ -> eventPublisher.publishEvent(new ApplySettingsWindowChanges()));
        buttonPanel.add(applyButton);
    }

    public static final class State implements io.github.arkosammy12.jemu.frontend.util.EventPublisher {

        private final MainWindow mainWindow;
        private final JFrame appFrame;
        private final Queue<SettingChangedEvent> pendingSettingChanges = new ConcurrentLinkedQueue<>();

        @Nullable
        private SettingsWindow settingsWindow;

        @Nullable
        private Rectangle windowSettingsBounds;

        public State(MainWindow mainWindow, JFrame appFrame) {
            this.mainWindow = mainWindow;
            this.appFrame = appFrame;

            mainWindow.onEvent(OpenSettingsWindowEvent.class, _ -> this.openSettingsWindow());
            mainWindow.onEvent(CloseSettingsWindowEvent.class, closeSettingsWindowEvent -> {
                switch (closeSettingsWindowEvent.type()) {
                    case SAVE_CHANGES -> this.drain();
                    case DISCARD_CHANGES -> this.flush();
                }
            });
            mainWindow.onEvent(ApplySettingsWindowChanges.class, _ -> this.commitSettingEvents());
        }

        private void openSettingsWindow() {
            this.settingsWindow = new SettingsWindow(this.mainWindow, this);
            this.settingsWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            this.settingsWindow.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    super.windowClosing(e);
                    SettingsWindow settingsWindow = State.this.settingsWindow;
                    if (settingsWindow != null) {
                        if (State.this.hasPendingEvents()) {
                            switch (JOptionPane.showConfirmDialog(settingsWindow, "Save changes?", State.this.mainWindow.getTitle(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                                case JOptionPane.YES_OPTION -> State.this.drain();
                                case JOptionPane.NO_OPTION -> State.this.flush();
                                default -> {}
                            }
                        } else {
                            State.this.disposeWindow();
                        }

                    }
                }

            });

            JFrame appFrame = State.this.appFrame;
            if (appFrame != null) {
                Image appFrameIconImage = appFrame.getIconImage();
                if (appFrameIconImage != null) {
                    this.settingsWindow.setIconImage(appFrameIconImage);
                }
            }

            if (this.windowSettingsBounds != null) {
                this.settingsWindow.setBounds(this.windowSettingsBounds);
            } else {
                this.settingsWindow.pack();
                this.settingsWindow.setLocationRelativeTo(appFrame);
            }
            this.settingsWindow.setVisible(true);
        }

        @Override
        public void publishEvent(BaseEvent event) {
            if (event instanceof SettingChangedEvent settingChangedEvent) {
                this.pendingSettingChanges.removeIf(existing -> existing.isOf(settingChangedEvent));
                this.pendingSettingChanges.offer(settingChangedEvent);
            } else {
                this.mainWindow.publishEvent(event);
            }
        }

        private void flush() {
            this.pendingSettingChanges.clear();
            this.disposeWindow();
        }

        private void drain() {
            this.commitSettingEvents();
            this.disposeWindow();
        }

        private void commitSettingEvents() {
            while (!this.pendingSettingChanges.isEmpty()) {
                this.mainWindow.publishEvent(this.pendingSettingChanges.poll());
            }
        }

        private boolean hasPendingEvents() {
            return !this.pendingSettingChanges.isEmpty();
        }

        public void disposeWindow() {
            this.pendingSettingChanges.clear();
            SettingsWindow settingsWindow = this.settingsWindow;
            if (settingsWindow != null) {
                this.windowSettingsBounds = settingsWindow.getBounds();
                settingsWindow.dispose();
            }
            this.settingsWindow = null;
        }

    }

}
