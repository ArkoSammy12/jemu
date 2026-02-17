package io.github.arkosammy12.jemu.application.ui.util;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class NumberOnlyTextField extends JTextField {

    public NumberOnlyTextField() {
        super();
        this.setDocument(new NumberOnlyDocument());
    }

    static class NumberOnlyDocument extends PlainDocument {

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            try {
                super.insertString(offs, String.valueOf(Integer.parseInt(str)), a);
            } catch (NumberFormatException _) {}
        }

    }

}
