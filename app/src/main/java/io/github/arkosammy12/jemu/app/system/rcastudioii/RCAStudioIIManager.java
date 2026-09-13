package io.github.arkosammy12.jemu.app.system.rcastudioii;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.studioii.RCAStudioIIKeypad;
import io.github.arkosammy12.jemu.frontend.util.KeyAction;

import javax.sound.sampled.LineUnavailableException;
import java.util.Collection;
import java.util.List;

public class RCAStudioIIManager extends SystemManager {

    public RCAStudioIIManager(Jemu jemu, SystemRegistry systemRegistry) {
        super(jemu, systemRegistry);

        this.keyActionMap.put(KeyAction.NUM_1, RCAStudioIIKeypad.Actions.KEYPADA_1);
        this.keyActionMap.put(KeyAction.NUM_2, RCAStudioIIKeypad.Actions.KEYPADA_2);
        this.keyActionMap.put(KeyAction.NUM_3, RCAStudioIIKeypad.Actions.KEYPADA_3);
        this.keyActionMap.put(KeyAction.NUM_4, RCAStudioIIKeypad.Actions.KEYPADA_4);
        this.keyActionMap.put(KeyAction.NUM_5, RCAStudioIIKeypad.Actions.KEYPADA_5);
        this.keyActionMap.put(KeyAction.NUM_6, RCAStudioIIKeypad.Actions.KEYPADA_6);
        this.keyActionMap.put(KeyAction.NUM_7, RCAStudioIIKeypad.Actions.KEYPADA_7);
        this.keyActionMap.put(KeyAction.NUM_8, RCAStudioIIKeypad.Actions.KEYPADA_8);
        this.keyActionMap.put(KeyAction.NUM_9, RCAStudioIIKeypad.Actions.KEYPADA_9);
        this.keyActionMap.put(KeyAction.NUM_0, RCAStudioIIKeypad.Actions.KEYPADA_0);

        this.keyActionMap.put(KeyAction.NUM_7, RCAStudioIIKeypad.Actions.KEYPADB_1);
        this.keyActionMap.put(KeyAction.NUM_8, RCAStudioIIKeypad.Actions.KEYPADB_2);
        this.keyActionMap.put(KeyAction.NUM_9, RCAStudioIIKeypad.Actions.KEYPADB_3);
        this.keyActionMap.put(KeyAction.U, RCAStudioIIKeypad.Actions.KEYPADB_4);
        this.keyActionMap.put(KeyAction.I, RCAStudioIIKeypad.Actions.KEYPADB_5);
        this.keyActionMap.put(KeyAction.O, RCAStudioIIKeypad.Actions.KEYPADB_6);
        this.keyActionMap.put(KeyAction.J, RCAStudioIIKeypad.Actions.KEYPADB_7);
        this.keyActionMap.put(KeyAction.K, RCAStudioIIKeypad.Actions.KEYPADB_8);
        this.keyActionMap.put(KeyAction.L, RCAStudioIIKeypad.Actions.KEYPADB_9);
        this.keyActionMap.put(KeyAction.COMMA, RCAStudioIIKeypad.Actions.KEYPADB_0);

        this.keyActionMap.put(KeyAction.NUMPAD_1, RCAStudioIIKeypad.Actions.KEYPADB_1);
        this.keyActionMap.put(KeyAction.NUMPAD_2, RCAStudioIIKeypad.Actions.KEYPADB_2);
        this.keyActionMap.put(KeyAction.NUMPAD_3, RCAStudioIIKeypad.Actions.KEYPADB_3);
        this.keyActionMap.put(KeyAction.NUMPAD_4, RCAStudioIIKeypad.Actions.KEYPADB_4);
        this.keyActionMap.put(KeyAction.NUMPAD_5, RCAStudioIIKeypad.Actions.KEYPADB_5);
        this.keyActionMap.put(KeyAction.NUMPAD_6, RCAStudioIIKeypad.Actions.KEYPADB_6);
        this.keyActionMap.put(KeyAction.NUMPAD_7, RCAStudioIIKeypad.Actions.KEYPADB_7);
        this.keyActionMap.put(KeyAction.NUMPAD_8, RCAStudioIIKeypad.Actions.KEYPADB_8);
        this.keyActionMap.put(KeyAction.NUMPAD_9, RCAStudioIIKeypad.Actions.KEYPADB_9);
        this.keyActionMap.put(KeyAction.NUMPAD_0, RCAStudioIIKeypad.Actions.KEYPADB_0);
    }

    @Override
    public String getName() {
        return "RCA Studio II";
    }

    @Override
    public String getId() {
        return "rca-studioii";
    }

    @Override
    public Collection<String> getFileExtensions() {
        return List.of("st2");
    }

    @Override
    public SystemAdapter createSystem(boolean detectedAutomatically) throws LineUnavailableException {
        return new RCAStudioIIAdapter(this.jemu, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof RCAStudioIIAdapter;
    }

}
