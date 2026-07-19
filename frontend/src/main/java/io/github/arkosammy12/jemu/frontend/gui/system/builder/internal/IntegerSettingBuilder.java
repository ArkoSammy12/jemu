package io.github.arkosammy12.jemu.frontend.gui.system.builder.internal;

import io.github.arkosammy12.jemu.frontend.events.Event;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.IntegerEmulationSetting;
import io.github.arkosammy12.jemu.frontend.gui.system.internal.settings.EmulationSettingElement;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class IntegerSettingBuilder extends SystemSettingElementBuilder {

    private final int startingValue;
    private final Function<? super Integer, ? extends Event> eventSupplier;

    @Nullable
    private final Integer minimumValue;

    @Nullable
    private final Integer maximumValue;

    IntegerSettingBuilder(String name, int startingValue, @Nullable Integer minimumValue, @Nullable Integer maximumValue, Function<? super Integer, ? extends Event> eventSupplier) {
        super(name);
        this.startingValue = startingValue;
        this.eventSupplier = eventSupplier;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;

        if (this.minimumValue != null && this.maximumValue != null && this.minimumValue > this.maximumValue) {
            throw new IllegalArgumentException("Minimum value %d for setting '%s' is greater than maximum value %d".formatted(this.minimumValue, name, this.maximumValue));
        }

        if (this.maximumValue != null && this.startingValue > this.maximumValue) {
            throw new IllegalArgumentException("Starting value %d for setting '%s' exceeds maximum value %d".formatted(this.startingValue, name, this.maximumValue));
        } else if (this.minimumValue != null && this.startingValue < this.minimumValue) {
            throw new IllegalArgumentException("Starting value %d for setting '%s' is below minimum value %d".formatted(this.startingValue, name, this.minimumValue));
        }

    }

    @Override
    protected EmulationSettingElement build() {
        return new IntegerEmulationSetting(this.name, this.startingValue, this.minimumValue, this.maximumValue, this.eventSupplier);
    }

}
