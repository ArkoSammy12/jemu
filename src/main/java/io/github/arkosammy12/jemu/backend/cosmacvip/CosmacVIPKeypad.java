package io.github.arkosammy12.jemu.backend.cosmacvip;

import io.github.arkosammy12.jemu.backend.common.SystemController;
import io.github.arkosammy12.jemu.backend.drivers.KeyMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CosmacVIPKeypad<E extends CosmacVipEmulator> extends SystemController<E> implements IODevice {

    private final List<KeyMapping> keyMappings = new ArrayList<>();

    private final boolean[] keys = new boolean[16];
    private int latchedKey = 0;

    public CosmacVIPKeypad(E emulator) {
        super(emulator);
        this.keyMappings.add(new CosmacVipKeypadMapping('1', 0x1));
        this.keyMappings.add(new CosmacVipKeypadMapping('2', 0x2));
        this.keyMappings.add(new CosmacVipKeypadMapping('3', 0x3));
        this.keyMappings.add(new CosmacVipKeypadMapping('4', 0xC));
        this.keyMappings.add(new CosmacVipKeypadMapping('q', 0x4));
        this.keyMappings.add(new CosmacVipKeypadMapping('w', 0x5));
        this.keyMappings.add(new CosmacVipKeypadMapping('e', 0x6));
        this.keyMappings.add(new CosmacVipKeypadMapping('r', 0xD));
        this.keyMappings.add(new CosmacVipKeypadMapping('a', 0x7));
        this.keyMappings.add(new CosmacVipKeypadMapping('s', 0x8));
        this.keyMappings.add(new CosmacVipKeypadMapping('d', 0x9));
        this.keyMappings.add(new CosmacVipKeypadMapping('f', 0xE));
        this.keyMappings.add(new CosmacVipKeypadMapping('z', 0xA));
        this.keyMappings.add(new CosmacVipKeypadMapping('x', 0x0));
        this.keyMappings.add(new CosmacVipKeypadMapping('c', 0xB));
        this.keyMappings.add(new CosmacVipKeypadMapping('v', 0xF));
    }

    @Override
    public Collection<KeyMapping> getKeyMappings() {
        return new ArrayList<>(this.keyMappings);
    }

    @Override
    public void cycle() {
        this.emulator.getCpu().setEF(2, this.keys[this.latchedKey]);
    }

    @Override
    public boolean isOutputPort(int port)  {
        return port == 2;
    }

    @Override
    public void onOutput(int port, int value) {
        this.latchedKey = value & 0xF;
    }

    private class CosmacVipKeypadMapping implements KeyMapping {

        private final int codePoint;

        private final Runnable keyPressedCallback;
        private final Runnable keyReleasedCallback;

        private CosmacVipKeypadMapping(int codePoint, int keypadKey) {
            this.codePoint = codePoint;
            this.keyPressedCallback = () -> keys[keypadKey] = true;
            this.keyReleasedCallback = () -> keys[keypadKey] = false;
        }

        @Override
        public int getDefaultCodePoint() {
            return codePoint;
        }

        @Override
        public Runnable getKeyPressedCallback() {
            return this.keyPressedCallback;
        }

        @Override
        public Runnable getKeyReleasedCallback() {
            return this.keyReleasedCallback;
        }

    }

    // TODO: Handle custom key mappings
    /*
    private static int getKeypadHexForKeyCode(KeyboardLayout layout, int keyCode) {
        return switch (layout) {
            case QWERTY -> switch (keyCode) {
                case KeyEvent.VK_X -> 0x0;
                case KeyEvent.VK_1 -> 0x1;
                case KeyEvent.VK_2 -> 0x2;
                case KeyEvent.VK_3 -> 0x3;
                case KeyEvent.VK_Q -> 0x4;
                case KeyEvent.VK_W -> 0x5;
                case KeyEvent.VK_E -> 0x6;
                case KeyEvent.VK_A -> 0x7;
                case KeyEvent.VK_S -> 0x8;
                case KeyEvent.VK_D -> 0x9;
                case KeyEvent.VK_Z -> 0xA;
                case KeyEvent.VK_C -> 0xB;
                case KeyEvent.VK_4 -> 0xC;
                case KeyEvent.VK_R -> 0xD;
                case KeyEvent.VK_F -> 0xE;
                case KeyEvent.VK_V -> 0xF;
                default -> -1;
            };
            case DVORAK -> switch (keyCode) {
                case KeyEvent.VK_Q -> 0x0;
                case KeyEvent.VK_1 -> 0x1;
                case KeyEvent.VK_2 -> 0x2;
                case KeyEvent.VK_3 -> 0x3;
                case KeyEvent.VK_QUOTE -> 0x4;
                case KeyEvent.VK_COMMA -> 0x5;
                case KeyEvent.VK_PERIOD -> 0x6;
                case KeyEvent.VK_A -> 0x7;
                case KeyEvent.VK_O -> 0x8;
                case KeyEvent.VK_E -> 0x9;
                case KeyEvent.VK_SEMICOLON -> 0xA;
                case KeyEvent.VK_J -> 0xB;
                case KeyEvent.VK_4 -> 0xC;
                case KeyEvent.VK_P -> 0xD;
                case KeyEvent.VK_U -> 0xE;
                case KeyEvent.VK_K -> 0xF;
                default -> -1;
            };
            case AZERTY -> switch (keyCode) {
                case KeyEvent.VK_X -> 0x0;
                case KeyEvent.VK_1 -> 0x1;
                case KeyEvent.VK_2 -> 0x2;
                case KeyEvent.VK_3 -> 0x3;
                case KeyEvent.VK_A -> 0x4;
                case KeyEvent.VK_Z -> 0x5;
                case KeyEvent.VK_E -> 0x6;
                case KeyEvent.VK_Q -> 0x7;
                case KeyEvent.VK_S -> 0x8;
                case KeyEvent.VK_D -> 0x9;
                case KeyEvent.VK_W -> 0xA;
                case KeyEvent.VK_C -> 0xB;
                case KeyEvent.VK_4 -> 0xC;
                case KeyEvent.VK_R -> 0xD;
                case KeyEvent.VK_F -> 0xE;
                case KeyEvent.VK_V -> 0xF;
                default -> -1;
            };
            case COLEMAK -> switch (keyCode) {
                case KeyEvent.VK_X -> 0x0;
                case KeyEvent.VK_1 -> 0x1;
                case KeyEvent.VK_2 -> 0x2;
                case KeyEvent.VK_3 -> 0x3;
                case KeyEvent.VK_Q -> 0x4;
                case KeyEvent.VK_W -> 0x5;
                case KeyEvent.VK_F -> 0x6;
                case KeyEvent.VK_A -> 0x7;
                case KeyEvent.VK_R -> 0x8;
                case KeyEvent.VK_S -> 0x9;
                case KeyEvent.VK_Z -> 0xA;
                case KeyEvent.VK_C -> 0xB;
                case KeyEvent.VK_4 -> 0xC;
                case KeyEvent.VK_P -> 0xD;
                case KeyEvent.VK_T -> 0xE;
                case KeyEvent.VK_V -> 0xF;
                default -> -1;
            };
        };
    }

     */

}
