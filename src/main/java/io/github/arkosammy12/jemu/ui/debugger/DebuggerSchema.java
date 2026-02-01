package io.github.arkosammy12.jemu.ui.debugger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.github.arkosammy12.jemu.ui.debugger.DebuggerPanel.*;

public class DebuggerSchema {

    private final List<TextEntry<?>> textSectionEntries = new ArrayList<>();
    private final List<TextEntry<?>> cpuRegisterEntries = new ArrayList<>();
    private final List<TextEntry<?>> generalPurposeRegisterEntries = new ArrayList<>();
    private final List<TextEntry<?>> stackEntries = new ArrayList<>();
    private Supplier<Integer> memoryPointerSupplier;

    private String textSectionName = DEFAULT_TEXT_SECTION_NAME;
    private String cpuRegistersSectionName = DEFAULT_CPU_REGISTERS_SECTION_NAME;
    private String generalPurposeRegistersSectionName = DEFAULT_GENERAL_PURPOSE_REGISTERS_SECTION_NAME;
    private String stackSectionName = DEFAULT_STACK_SECTION_NAME;

    public void setMemoryPointerSupplier(Supplier<Integer> memoryPointerSupplier) {
        this.memoryPointerSupplier = memoryPointerSupplier;
    }

    public void setTextSectionName(String name) {
        this.textSectionName = name;
    }

    public void clearTextSectionEntries() {
        this.textSectionEntries.clear();
    }

    public void setCpuRegistersSectionName(String name) {
        this.cpuRegistersSectionName = name;
    }

    public void clearCpuRegistersSectionName() {
        this.cpuRegisterEntries.clear();
    }

    public void setGeneralPurposeRegistersSectionName(String name) {
        this.generalPurposeRegistersSectionName = name;
    }

    public void clearGeneralPurposeRegistersSectionName() {
        this.generalPurposeRegisterEntries.clear();
    }

    public void setStackSectionName(String name) {
        this.stackSectionName = name;
    }

    public void clearStackSectionEntries() {
        this.stackEntries.clear();
    }

    public String getTextSectionName() {
        return this.textSectionName;
    }

    public String getCpuRegistersSectionName() {
        return this.cpuRegistersSectionName;
    }

    public String getGeneralPurposeRegistersSectionName() {
        return this.generalPurposeRegistersSectionName;
    }

    public String getStackSectionName() {
        return this.stackSectionName;
    }

    public <T> TextEntry<T> createTextEntry() {
        TextEntry<T> entry = new TextEntry<>();
        this.textSectionEntries.add(entry);
        return entry;
    }

    public <T> TextEntry<T> createCpuRegisterEntry() {
        TextEntry<T> entry = new TextEntry<>();
        this.cpuRegisterEntries.add(entry);
        return entry;
    }

    public <T> TextEntry<T> createGeneralPurposeRegisterEntry() {
        TextEntry<T> entry = new TextEntry<>();
        this.generalPurposeRegisterEntries.add(entry);
        return entry;
    }

    public <T> TextEntry<T> createStackEntry() {
        TextEntry<T> entry = new TextEntry<>();
        this.stackEntries.add(entry);
        return entry;
    }

    List<DebuggerLabel<?>> getTextSectionLabels() {
        return this.textSectionEntries.stream().map(TextEntry::getDebuggerLabel).collect(Collectors.toList());
    }

    List<DebuggerLabel<?>> getCpuRegisterLabels() {
        return this.cpuRegisterEntries.stream().map(TextEntry::getDebuggerLabel).collect(Collectors.toList());
    }

    List<DebuggerLabel<?>> getGeneralPurposeRegisterLabels() {
        return this.generalPurposeRegisterEntries.stream().map(TextEntry::getDebuggerLabel).collect(Collectors.toList());
    }

    List<DebuggerLabel<?>> getStackLabels() {
        return this.stackEntries.stream().map(TextEntry::getDebuggerLabel).collect(Collectors.toList());
    }

    Optional<Supplier<Integer>> getMemoryPointerSupplier() {
        return Optional.ofNullable(this.memoryPointerSupplier);
    }

    public static class TextEntry<T> {

        private String name = null;
        private Supplier<T> stateUpdater;
        private Function<T, String> toStringFunction;

        public TextEntry<T> withName(String name) {
            this.name = name;
            return this;
        }

        public TextEntry<T> withStateUpdater(Supplier<T> stateUpdater) {
            this.stateUpdater = stateUpdater;
            return this;
        }

        public TextEntry<T> withToStringFunction(Function<T, String> toStringFunction) {
            this.toStringFunction = toStringFunction;
            return this;
        }

        public Optional<String> getName() {
            return Optional.ofNullable(this.name);
        }

        public Optional<Supplier<T>> getStateUpdater() {
            return Optional.ofNullable(this.stateUpdater);
        }

        public Optional<Function<T, String>> getToStringFunction() {
            return Optional.ofNullable(this.toStringFunction);
        }

        private DebuggerLabel<T> getDebuggerLabel() {
            return new DebuggerLabel<>(this);
        }

    }

}
