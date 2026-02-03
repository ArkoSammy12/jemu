package io.github.arkosammy12.jemu.ui.debugger;

import io.github.arkosammy12.jemu.main.Jemu;
import io.github.arkosammy12.jemu.systems.Emulator;
import io.github.arkosammy12.jemu.systems.bus.Bus;
import io.github.arkosammy12.jemu.main.MainWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MemoryTable extends JTable {

    private static final int HIGHLIGHT_FLAG = 0x8000_0000;
    private static final int CHANGED_FLAG = 0x4000_0000;

    private static final int MAX_SHOWN_BYTES = 0xFFFFFF + 1;

    private static final int MEMORY_COLUMN_WIDTH = 26;
    private static final int ADDRESS_COLUMN_WIDTH = 72;
    private static final int ROW_HEIGHT = 15;

    private final Model model;

    private int @Nullable [] bytes;

    public MemoryTable(Jemu jemu) {
        super();
        this.model = new Model();
        this.setModel(model);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        this.setRowHeight(ROW_HEIGHT);
        this.getTableHeader().setReorderingAllowed(false);
        this.getTableHeader().setResizingAllowed(false);
        this.setFocusable(false);
        this.setRowSelectionAllowed(false);
        this.setColumnSelectionAllowed(false);
        this.setTableHeader(null);
        this.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                int newBytesPerRow = calculateBytesPerRow();
                if ((newBytesPerRow != model.getBytesPerRow())) {
                    model.setBytesPerRow(newBytesPerRow);
                    rebuildTable();
                }
            }

        });
        this.rebuildTable();

        jemu.addStateChangedListener((emulator, _, newState) -> {
            if (emulator == null || newState.isStopping()) {
                this.onStopping();
            } else if (newState.isResetting()) {
                this.onResetting(emulator);
            }
        });
        jemu.addFrameListener(this::onFrame);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return this.getPreferredSize().getWidth() <= this.getParent().getWidth();
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        Color baseColor = getBackground();
        if (row % 2 != 0) {
            baseColor = baseColor.darker();
        }
        c.setBackground(baseColor);
        if (column == 0) {
            c.setForeground(UIManager.getColor("Table.foreground"));
            return c;
        }

        int idx = row * this.model.bytesPerRow + (column - 1);
        if (this.bytes != null && idx >= 0 && idx < this.bytes.length) {
            c.setForeground((this.bytes[idx] & HIGHLIGHT_FLAG) != 0 ? Color.YELLOW : UIManager.getColor("Table.foreground"));
        }
        return c;
    }

    private void onResetting(@NotNull Emulator emulator) {
        Bus memory = emulator.getBus();
        SwingUtilities.invokeLater(() -> {
            this.model.memory = memory;
            this.bytes = new int[memory.getMemorySize()];
            this.rebuildTable();
        });
    }

    private void onStopping() {
        SwingUtilities.invokeLater(() -> {
            this.bytes = null;
            this.model.clear();
            this.rebuildTable();
            this.scrollToAddress(0);
        });
    }

    private void onFrame(@Nullable Emulator emulator) {
        if (emulator == null) {
            return;
        }
        Jemu.State state = emulator.getEmulatorSettings().getJemu().getState();
        boolean updateChangeHighlights = state.isRunning() || state.isStepping();
        SwingUtilities.invokeLater(() -> {
            if (!this.isShowing()) {
                return;
            }
            if (updateChangeHighlights && this.bytes != null) {
                JViewport vp = (JViewport) getParent();
                Rectangle view = vp.getViewRect();

                int firstRow = view.y / getRowHeight();
                int lastRow  = (view.y + view.height) / getRowHeight();

                int startIdx = firstRow * model.bytesPerRow;
                int endIdx = Math.min((lastRow + 1) * model.bytesPerRow, bytes.length);

                for (int i = startIdx; i < endIdx; i++) {
                    if ((bytes[i] & CHANGED_FLAG) != 0) {
                        bytes[i] |= HIGHLIGHT_FLAG;
                    } else {
                        bytes[i] &= ~HIGHLIGHT_FLAG;
                    }
                }
            }
            this.model.update();
        });
    }

    public int getCurrentMaximumAddress() {
        return this.model.memory == null ? MAX_SHOWN_BYTES - 1 : this.model.memory.getMaximumAddress();
    }

    public void scrollToAddress(int address) {
        if (!(this.getParent() instanceof JViewport viewport)) {
            return;
        }
        int targetY = (address / this.model.getBytesPerRow()) * this.getRowHeight();
        if (targetY < 0) {
            targetY = 0;
        }
        viewport.setViewPosition(new Point(viewport.getViewPosition().x, targetY));
    }

    private int calculateBytesPerRow() {
        int currentWidth = getSize().width;
        if (currentWidth > (ADDRESS_COLUMN_WIDTH + MEMORY_COLUMN_WIDTH * 32)) {
            return 32;
        } else if (currentWidth > (ADDRESS_COLUMN_WIDTH + MEMORY_COLUMN_WIDTH * 16)) {
            return 16;
        }
        return 8;
    }

    private void rebuildTable() {
        this.model.rebuildColumns();
        TableColumnModel colModel = this.getColumnModel();

        TableColumn addressColumn = colModel.getColumn(0);
        addressColumn.setPreferredWidth(ADDRESS_COLUMN_WIDTH);
        addressColumn.setMinWidth(ADDRESS_COLUMN_WIDTH);

        for (int i = 1; i < colModel.getColumnCount(); i++) {
            TableColumn col = colModel.getColumn(i);
            col.setPreferredWidth(MEMORY_COLUMN_WIDTH);
            col.setMinWidth(MEMORY_COLUMN_WIDTH);
        }

        this.revalidate();
        this.repaint();
    }

    private class Model extends DefaultTableModel {

        private Bus memory;

        private int bytesPerRow = 8;
        private int rowCount;

        public Model() {
            super();
            this.rowCount = (int) Math.ceil(MAX_SHOWN_BYTES / (double) this.bytesPerRow);
        }

        @Override
        public int getRowCount() {
            return rowCount;
        }

        @Override
        public int getColumnCount() {
            return this.bytesPerRow + 1;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        private void setBytesPerRow(int bytesPerRow) {
            this.bytesPerRow = bytesPerRow;
        }

        private int getBytesPerRow() {
            return this.bytesPerRow;
        }

        private void rebuildColumns() {
            this.updateRowCount();
            this.fireTableStructureChanged();
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return String.format("%06X", row * this.bytesPerRow);
            } else if (this.memory != null) {
                int idx = row * this.bytesPerRow + (col - 1);
                int currentByte = this.memory.getByte(idx);

                if (bytes != null && idx >= 0 && idx < bytes.length) {
                    int previousVal = bytes[idx];
                    int previousByte = previousVal & 0xFF;
                    int highlightFlag = previousVal & HIGHLIGHT_FLAG;
                    bytes[idx] = previousByte != currentByte ? currentByte | CHANGED_FLAG | highlightFlag : currentByte | highlightFlag;
                }

                return idx < this.memory.getMemorySize() ? String.format("%02X", currentByte) : "";
            } else {
                return "00";
            }
        }

        private void update() {
            MainWindow.fireVisibleRowsUpdated(MemoryTable.this, this);
        }

        private void clear() {
            this.memory = null;
        }

        private void updateRowCount() {
            if (this.memory == null) {
                this.rowCount = (int) Math.ceil(MAX_SHOWN_BYTES / (double) this.bytesPerRow);
            } else {
                this.rowCount = (int) Math.ceil(this.memory.getMemorySize() / (double) this.bytesPerRow);
            }
        }

    }

}
