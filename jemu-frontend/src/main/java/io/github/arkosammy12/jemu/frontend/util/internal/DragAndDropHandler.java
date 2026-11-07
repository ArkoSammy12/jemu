package io.github.arkosammy12.jemu.frontend.util.internal;

import org.tinylog.Logger;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class DragAndDropHandler extends TransferHandler implements DropTargetListener {

    private final Consumer<Collection<Path>> fileDroppedCallback;

    public DragAndDropHandler(Consumer<Collection<Path>> fileDroppedCallback) {
        this.fileDroppedCallback = fileDroppedCallback;
    }

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
        List<File> files = null;
        try {
            files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        } catch (Exception e) {
            Logger.error("Failed to accept drag-and-drop file! {}", e);
        }
        if (files == null) {
            return false;
        } else {
            this.fileDroppedCallback.accept(files.stream().map(File::toPath).toList());
            return true;
        }
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {

    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent dtde) {
        if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.rejectDrop();
            return;
        }

        List<File> files = null;
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY);
            files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
            dtde.dropComplete(true);
        } catch (Exception e) {
            Logger.error("Failed to accept drag-and-drop file! {}", e);
            dtde.dropComplete(false);
        }

        if (files != null) {
            this.fileDroppedCallback.accept(files.stream().map(File::toPath).toList());
        }
    }

}
