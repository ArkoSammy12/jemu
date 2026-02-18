package io.github.arkosammy12.jemu.frontend.ui;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.application.Main;
import io.github.arkosammy12.jemu.application.io.DataManager;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.application.io.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.frontend.ui.debugger.DebuggerPanel;
import io.github.arkosammy12.jemu.frontend.ui.util.ToggleableSplitPane;
import io.github.arkosammy12.jemu.frontend.ui.util.WindowTitleManager;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Closeable;

import static io.github.arkosammy12.jemu.application.io.DataManager.tryOptional;

public class MainWindow extends JFrame implements EmulatorInitializerConsumer, Closeable {

    public static final String DEFAULT_TITLE = "jemu " + Main.VERSION_STRING;

    private final WindowTitleManager windowTitleManager;
    private final ToggleableSplitPane mainSplitPane;
    private final LeftPanel leftPanel;
    private final SettingsBar settingsBar;
    private final StatusBar statusBar;

    private final CC infoBarConstraints;

    private Point lastUnmaximizedLocation = new Point(0, 0);
    private int lastUnmaximizedWidth = 0;
    private int lastUnmaximizedHeight = 0;

    public MainWindow(Jemu jemu) {
        super(DEFAULT_TITLE);

        this.windowTitleManager = new WindowTitleManager(this);
        this.setTitleSection(0, DEFAULT_TITLE);
        this.setBackground(Color.BLACK);
        this.getRootPane().putClientProperty("apple.awt.fullscreenable", true);

        MigLayout migLayout = new MigLayout(new LC().insets("0"), new AC(), new AC().gap("0"));
        this.setLayout(migLayout);
        this.setBackground(Color.BLACK);

        this.leftPanel = new LeftPanel(jemu, this);
        this.settingsBar = new SettingsBar(jemu, this);
        this.statusBar = new StatusBar(jemu);
        DebuggerPanel debuggerPanel = new DebuggerPanel(jemu, this);
        this.mainSplitPane = new ToggleableSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftPanel, debuggerPanel, 5, 0.5);

        this.infoBarConstraints = new CC().grow().pushX().dockSouth().height("28!");

        this.setJMenuBar(this.settingsBar);
        this.add(this.mainSplitPane, new CC().grow().push().wrap());
        this.add(this.statusBar, this.infoBarConstraints);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        this.requestFocusInWindow();
        this.setResizable(true);
        this.setPreferredSize(new Dimension((int) (screenSize.getWidth() / 1.5), (int) (screenSize.getHeight() / 1.5)));
        this.pack();
        this.setLocationRelativeTo(null);

        this.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                if ((getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                    lastUnmaximizedWidth = getWidth();
                    lastUnmaximizedHeight = getHeight();
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if ((getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                    lastUnmaximizedLocation = getLocation();
                }
            }

        });

        jemu.addShutdownListener(() -> {
            DataManager dataManager = jemu.getDataManager();
            dataManager.putPersistent("ui.main_window_width", String.valueOf(this.lastUnmaximizedWidth));
            dataManager.putPersistent("ui.main_window_height", String.valueOf(this.lastUnmaximizedHeight));
            dataManager.putPersistent("ui.main_window_x", String.valueOf(this.lastUnmaximizedLocation.x));
            dataManager.putPersistent("ui.main_window_y", String.valueOf(this.lastUnmaximizedLocation.y));
            dataManager.putPersistent("ui.main_window_extended_state", String.valueOf(this.getExtendedState()));
            dataManager.putPersistent("ui.main_split_divider_location", String.valueOf(this.mainSplitPane.getAbsoluteDividerLocation()));
        });
    }

    public SettingsBar getSettingsBar() {
        return this.settingsBar;
    }

    @Override
    public void accept(EmulatorInitializer initializer) {
        if (initializer instanceof DataManager dataManager) {
            dataManager.getPersistent("ui.main_window_width").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(width -> this.setSize(width, this.getHeight()));
            dataManager.getPersistent("ui.main_window_height").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(height -> this.setSize(this.getWidth(), height));
            dataManager.getPersistent("ui.main_window_x").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(x -> this.setLocation(x, this.getLocation().y));
            dataManager.getPersistent("ui.main_window_y").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(y -> this.setLocation(this.getLocation().x, y));
            dataManager.getPersistent("ui.main_window_extended_state").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(this::setExtendedState);
            dataManager.getPersistent("ui.main_split_divider_location").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(this.mainSplitPane::setAbsoluteDividerLocation);
        }
        for (Component child : this.getComponents()) {
            this.visit(child, initializer);
        }
    }

    private void visit(Component component, EmulatorInitializer emulatorInitializer) {
        if (component instanceof EmulatorInitializerConsumer consumer) {
            consumer.accept(emulatorInitializer);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (child instanceof Container c) {
                    this.visit(c, emulatorInitializer);
                }
            }
        }
    }

    public void setTitleSection(int index, String text) {
        this.windowTitleManager.setSection(index, text);
    }

    public void setDebuggerEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            this.mainSplitPane.toggleShowSplit(enabled);
            this.revalidate();
            this.repaint();
        });
    }

    public void setDisassemblerEnabled(boolean enabled) {
        this.leftPanel.setDisassemblerEnabled(enabled);
    }

    public void setInfoBarEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            this.statusBar.setVisible(enabled);
            this.infoBarConstraints.setHideMode(enabled ? 0 : 3);
            this.revalidate();
            this.repaint();
        });
    }

    public void showExceptionDialog(Throwable e) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                e.getClass().getSimpleName() + ": " + e.getMessage(),
                "Emulation has stopped unexpectedly!",
                JOptionPane.ERROR_MESSAGE
        ));
    }

    /*
    public void onBreakpoint() {
        this.settingsBar.onBreakpoint();
    }
     */

    @Override
    public void close() {
        SwingUtilities.invokeLater(this::dispose);
    }

    public static void fireVisibleRowsUpdated(JTable table, AbstractTableModel model) {
        Rectangle visibleRect = table.getVisibleRect();

        int firstRow = table.rowAtPoint(visibleRect.getLocation());
        int lastRow = table.rowAtPoint(new Point(visibleRect.x, visibleRect.y + visibleRect.height));

        if (firstRow < 0) {
            firstRow = 0;
        }
        if (lastRow < 0) {
            lastRow = table.getRowCount() - 1;
        }

        int modelFirstRow = table.convertRowIndexToModel(firstRow);
        int modelLastRow = table.convertRowIndexToModel(lastRow);

        model.fireTableRowsUpdated(modelFirstRow, modelLastRow);
    }

}
