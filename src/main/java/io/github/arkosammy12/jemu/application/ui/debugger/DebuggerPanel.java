package io.github.arkosammy12.jemu.application.ui.debugger;

import io.github.arkosammy12.jemu.application.Jemu;
import io.github.arkosammy12.jemu.application.config.DataManager;
import io.github.arkosammy12.jemu.application.config.initializers.ApplicationInitializer;
import io.github.arkosammy12.jemu.application.config.initializers.EmulatorInitializer;
import io.github.arkosammy12.jemu.application.config.initializers.EmulatorInitializerConsumer;
import io.github.arkosammy12.jemu.backend.common.Emulator;
import io.github.arkosammy12.jemu.application.ui.MainWindow;
import io.github.arkosammy12.jemu.application.ui.util.DebuggerLabelTable;
import net.miginfocom.layout.AlignX;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;

import static io.github.arkosammy12.jemu.application.config.DataManager.tryOptional;

public class DebuggerPanel extends JPanel implements EmulatorInitializerConsumer {

    public static final String DEFAULT_TEXT_SECTION_NAME = "Current Quirks";
    public static final String DEFAULT_CPU_REGISTERS_SECTION_NAME = "Processor Registers";
    public static final String DEFAULT_GENERAL_PURPOSE_REGISTERS_SECTION_NAME = "General Purpose Registers";
    public static final String DEFAULT_STACK_SECTION_NAME = "Stack";

    @Nullable
    private DebuggerSchema debuggerSchema;

    private final List<DebuggerLabel<?>> textPanelLabels = new ArrayList<>();
    private final List<DebuggerLabel<?>> cpuRegisterLabels = new ArrayList<>();
    private final List<DebuggerLabel<?>> generalPurposeRegisterLabels = new ArrayList<>();
    private final List<DebuggerLabel<?>> stackLabels = new ArrayList<>();

    private final JTextArea textArea;

    private final JScrollPane textScrollPane;
    private final JScrollPane cpuRegistersScrollPane;
    private final JScrollPane generalPurposeRegistersScrollPane;
    private final JScrollPane stackScrollPane;

    private final JSplitPane firstSplit;
    private final JSplitPane secondSplit;
    private final JSplitPane thirdSplit;
    private final JSplitPane mainSplit;

    private final DebuggerLabelTable cpuRegistersTable;
    private final DebuggerLabelTable generalPurposeRegistersTable;
    private final DebuggerLabelTable stackTable;
    private final MemoryTable memoryTable;

    private final JCheckBox memoryFollowCheckBox;

    public DebuggerPanel(Jemu jemu, MainWindow mainWindow) {
        MigLayout migLayout = new MigLayout(new LC().insets("0"));
        super(migLayout);

        this.setFocusable(false);
        this.setPreferredSize(new Dimension(500, this.getHeight()));
        this.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                "Debugger",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.getFont().deriveFont(Font.BOLD)));

        this.textArea = new JTextArea();
        this.textArea.setEditable(false);
        this.textArea.setFocusable(false);
        this.textArea.setLineWrap(true);
        this.textArea.setWrapStyleWord(true);
        this.textArea.setKeymap(null);
        this.textArea.setOpaque(false);
        DefaultCaret caret = (DefaultCaret) this.textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        caret.setVisible(false);

        this.cpuRegistersTable = new DebuggerLabelTable(this.cpuRegisterLabels, 2);
        this.generalPurposeRegistersTable = new DebuggerLabelTable(this.generalPurposeRegisterLabels, 2, true);
        this.stackTable = new DebuggerLabelTable(this.stackLabels, 2, true);
        this.memoryTable = new MemoryTable(jemu);

        this.textScrollPane = new JScrollPane(textArea);
        this.textScrollPane.setPreferredSize(new Dimension(this.textScrollPane.getWidth(), 120));

        this.cpuRegistersScrollPane = new JScrollPane(this.cpuRegistersTable);
        this.cpuRegistersScrollPane.setPreferredSize(new Dimension(this.cpuRegistersScrollPane.getWidth(), 105));

        this.generalPurposeRegistersScrollPane = new JScrollPane(this.generalPurposeRegistersTable);
        this.generalPurposeRegistersScrollPane.setPreferredSize(new Dimension(this.generalPurposeRegistersScrollPane.getWidth(), 230));

        this.stackScrollPane = new JScrollPane(this.stackTable);
        this.stackScrollPane.setPreferredSize(new Dimension(this.stackScrollPane.getWidth(), 210));

        JScrollPane memoryScrollPane = new JScrollPane(memoryTable);

        this.setDefaultBorders();

        this.firstSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.textScrollPane, this.cpuRegistersScrollPane);
        firstSplit.setDividerSize(3);
        firstSplit.setResizeWeight(0.5);
        firstSplit.setContinuousLayout(true);

        this.secondSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, firstSplit, this.generalPurposeRegistersScrollPane);
        secondSplit.setDividerSize(3);
        secondSplit.setResizeWeight(0.5);
        secondSplit.setContinuousLayout(true);

        this.thirdSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, secondSplit, this.stackScrollPane);
        thirdSplit.setDividerSize(3);
        thirdSplit.setResizeWeight(0.5);
        thirdSplit.setContinuousLayout(true);

        MigLayout leftPanelLayout = new MigLayout(new LC().insets("0"));
        JPanel leftPanel = new JPanel(leftPanelLayout);
        leftPanel.add(thirdSplit, new CC().grow().push());
        leftPanel.setPreferredSize(new Dimension(150, leftPanel.getHeight()));

        MigLayout rightPanelLayout = new MigLayout(new LC().insets("0"));
        JPanel rightPanel = new JPanel(rightPanelLayout);
        rightPanel.setPreferredSize(new Dimension(200, rightPanel.getHeight()));
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                "Memory",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                memoryScrollPane.getFont().deriveFont(Font.BOLD)));

        JLabel goToAddressLabel = new JLabel("Go to address: ");
        JTextField goToAddressField = new JTextField();
        goToAddressField.addActionListener(_ -> {
            String text = goToAddressField.getText().trim();
            if (text.isEmpty()) {
                return;
            }
            int maximumAddress = this.memoryTable.getCurrentMaximumAddress();
            try {
                int address = Integer.decode(text);
                if (address >= 0 && address <= maximumAddress) {
                    this.memoryTable.scrollToAddress(address);
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException _) {
                JOptionPane.showMessageDialog(
                        mainWindow,
                        "The address must be valid integer between 0 and " + maximumAddress + " (0x" + Integer.toHexString(maximumAddress).toUpperCase() + ")",
                        "Invalid address value",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });
        goToAddressField.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(goToAddressField::selectAll);
            }

        });

        this.memoryFollowCheckBox = new JCheckBox("Follow");
        this.memoryFollowCheckBox.setFocusable(false);
        this.memoryFollowCheckBox.addChangeListener(_ -> goToAddressField.setEnabled(!this.memoryFollowCheckBox.isSelected()));

        rightPanel.add(this.memoryFollowCheckBox, new CC().growX().pushX().alignX(AlignX.CENTER));
        rightPanel.add(goToAddressLabel, new CC().split(2).alignX(AlignX.CENTER));
        rightPanel.add(goToAddressField, new CC().growX().pushX().alignX(AlignX.CENTER).wrap());
        rightPanel.add(memoryScrollPane, new CC().grow().push().spanX());

        this.mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        mainSplit.setDividerSize(5);
        mainSplit.setResizeWeight(0.5);
        mainSplit.setContinuousLayout(true);
        mainSplit.setOneTouchExpandable(true);

        this.add(mainSplit, new CC().grow().push().width("500"));

        jemu.addStateChangedListener((emulator, _, newState) -> {
            if (emulator == null || newState.isStopping()) {
                this.onStopping();
            } else if (newState.isResetting()) {
                this.onResetting(emulator);
            }
        });
        jemu.addFrameListener(this::onFrame);

        jemu.addShutdownListener(() -> {
            DataManager dataManager = jemu.getDataManager();
            dataManager.putPersistent(DataManager.DEBUGGER_FOLLOW, String.valueOf(this.memoryFollowCheckBox.isSelected()));
            dataManager.putPersistent("ui.debugger.debugger_memory_divider_location", String.valueOf(this.mainSplit.getDividerLocation()));
            dataManager.putPersistent("ui.debugger.debugger_divider_location_1", String.valueOf(this.firstSplit.getDividerLocation()));
            dataManager.putPersistent("ui.debugger.debugger_divider_location_2", String.valueOf(this.secondSplit.getDividerLocation()));
            dataManager.putPersistent("ui.debugger.debugger_divider_location_3", String.valueOf(this.thirdSplit.getDividerLocation()));
        });
    }

    private void onResetting(@NotNull Emulator emulator) {
        DebuggerSchema debuggerSchema = emulator.getDebuggerSchema();
        SwingUtilities.invokeLater(() -> {
            this.debuggerSchema = debuggerSchema;
            this.initializeDebuggerPanel();
        });
    }

    private void onStopping() {
        SwingUtilities.invokeLater(() -> {
            this.debuggerSchema = null;
            this.clear();
            this.textArea.setText("");
            this.setDefaultBorders();
            this.revalidate();
            this.repaint();
        });
    }

    private void onFrame(@Nullable Emulator emulator, Jemu.State state) {
        if (emulator == null) {
            return;
        }
        boolean updateChangeHighlights = state.isRunning() || state.isStepping();
        SwingUtilities.invokeLater(() -> {
            if (!this.isShowing()) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < textPanelLabels.size(); i++) {
                DebuggerLabel<?> label = textPanelLabels.get(i);
                label.update();
                sb.append(label.getText());
                if (i < textPanelLabels.size() - 1) {
                    sb.append('\n');
                }
            }
            this.textArea.setText(sb.toString());

            this.cpuRegisterLabels.forEach(DebuggerLabel::update);
            this.generalPurposeRegisterLabels.forEach(DebuggerLabel::update);
            this.stackLabels.forEach(DebuggerLabel::update);

            this.cpuRegistersTable.update(updateChangeHighlights);
            this.generalPurposeRegistersTable.update(updateChangeHighlights);
            this.stackTable.update(updateChangeHighlights);

            if (this.memoryFollowCheckBox.isSelected() && this.debuggerSchema != null) {
                this.debuggerSchema.getMemoryPointerSupplier().ifPresent(addr -> this.memoryTable.scrollToAddress(addr.get()));
            }
        });
    }

    private void initializeDebuggerPanel() {
        this.clear();

        if (this.debuggerSchema == null) {
            return;
        }

        this.textPanelLabels.addAll(this.debuggerSchema.getTextSectionLabels());
        this.cpuRegisterLabels.addAll(this.debuggerSchema.getCpuRegisterLabels());
        this.generalPurposeRegisterLabels.addAll(this.debuggerSchema.getGeneralPurposeRegisterLabels());
        this.stackLabels.addAll(this.debuggerSchema.getStackLabels());

        this.textPanelLabels.forEach(label -> label.setFont(label.getFont().deriveFont(Font.BOLD).deriveFont(15f)));
        this.cpuRegisterLabels.forEach(label -> label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15)));
        this.generalPurposeRegisterLabels.forEach(label -> label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15)));
        this.stackLabels.forEach(label -> label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15)));

        this.textScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                this.debuggerSchema.getTextSectionName(),
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.textScrollPane.getFont().deriveFont(Font.BOLD)));

        this.cpuRegistersScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                this.debuggerSchema.getCpuRegistersSectionName(),
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.cpuRegistersScrollPane.getFont().deriveFont(Font.BOLD)));

        this.generalPurposeRegistersScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                this.debuggerSchema.getGeneralPurposeRegistersSectionName(),
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.generalPurposeRegistersScrollPane.getFont().deriveFont(Font.BOLD)));

        this.stackScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                this.debuggerSchema.getStackSectionName(),
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.stackScrollPane.getFont().deriveFont(Font.BOLD)));

        this.revalidate();
        this.repaint();
    }

    private void clear() {
        this.textPanelLabels.clear();

        this.cpuRegisterLabels.clear();
        this.generalPurposeRegisterLabels.clear();
        this.stackLabels.clear();

        this.cpuRegistersTable.update(true);
        this.generalPurposeRegistersTable.update(true);
        this.stackTable.update(true);
    }

    private void setDefaultBorders() {
        this.textScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                DEFAULT_TEXT_SECTION_NAME,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.textScrollPane.getFont().deriveFont(Font.BOLD)));

        this.cpuRegistersScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                DEFAULT_CPU_REGISTERS_SECTION_NAME,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.cpuRegistersScrollPane.getFont().deriveFont(Font.BOLD)));

        this.generalPurposeRegistersScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                DEFAULT_GENERAL_PURPOSE_REGISTERS_SECTION_NAME,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.generalPurposeRegistersScrollPane.getFont().deriveFont(Font.BOLD)));

        this.stackScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2, true),
                DEFAULT_STACK_SECTION_NAME,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                this.stackScrollPane.getFont().deriveFont(Font.BOLD)));
    }

    @Override
    public void accept(EmulatorInitializer initializer) {
        if (initializer instanceof ApplicationInitializer applicationInitializer) {
            applicationInitializer.getDebuggerFollowing().ifPresent(this.memoryFollowCheckBox::setSelected);
        }
        if (initializer instanceof DataManager dataManager) {
            dataManager.getPersistent("ui.debugger.debugger_memory_divider_location").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(this.mainSplit::setDividerLocation);
            dataManager.getPersistent("ui.debugger.debugger_divider_location_1").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(this.firstSplit::setDividerLocation);
            dataManager.getPersistent("ui.debugger.debugger_divider_location_2").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(this.secondSplit::setDividerLocation);
            dataManager.getPersistent("ui.debugger.debugger_divider_location_3").flatMap(v -> tryOptional(() -> Integer.valueOf(v))).ifPresent(this.thirdSplit::setDividerLocation);
        }
    }
}