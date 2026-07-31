package io.github.arkosammy12.jemu.frontend.gui.internal.menus.settings.menubar;

import io.github.arkosammy12.jemu.frontend.gui.MainWindow;
import io.github.arkosammy12.jemu.frontend.gui.MenuBarMenu;
import io.github.arkosammy12.jemu.frontend.gui.system.SystemDescriptor;

public class EmulationSettings extends MenuBarMenu {

    public EmulationSettings(MainWindow mainWindow) {
        this.getJMenu().setText("Emulation");
        for (SystemDescriptor systemDescriptor : mainWindow.getSystemCatalog().getSystemDescriptors()) {
            systemDescriptor.getSettingsMenuBarContents().ifPresent(contentsSupplier -> this.getJMenu().add(contentsSupplier.apply(mainWindow)));
        }
    }

}
