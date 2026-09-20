package io.github.arkosammy12.jemu.app.system.nes;

import io.github.arkosammy12.jemu.app.Jemu;
import io.github.arkosammy12.jemu.app.system.SystemRegistry;
import io.github.arkosammy12.jemu.app.system.SystemAdapter;
import io.github.arkosammy12.jemu.app.system.SystemManager;
import io.github.arkosammy12.jemu.core.nes.NESController;
import io.github.arkosammy12.jemu.core.nes.ines.CartridgeInfo;
import io.github.arkosammy12.jemu.frontend.util.KeyAction;
import org.tinylog.Logger;
import tools.jackson.dataformat.xml.XmlMapper;

import javax.sound.sampled.LineUnavailableException;
import java.util.*;

public class NESManager extends SystemManager {

    private final Map<String, NES20Database.Game> databaseMap;

    public NESManager(Jemu jemu, SystemRegistry systemRegistry) {
        super(jemu, systemRegistry);

        Map<String, NES20Database.Game> map = new HashMap<>();

        dbInit: try {
            byte[] bytes = loadFromResources(this.getClass(), "/system/nes/nes20db/nes20db.xml");
            if (bytes == null) {
                Logger.error("NES database file not found!");
                break dbInit;
            }
            NES20Database nes20Database = new XmlMapper().readValue(bytes, NES20Database.class);
            for (NES20Database.Game game : nes20Database.getGames()) {
                map.put(game.getRom().getSha1().toLowerCase(), game);
            }
        } catch (Exception e) {
            Logger.error(e, "Failed to load NES 2.0 database!");
        }
        this.databaseMap = Map.copyOf(map);

        this.keyActionMap.put(KeyAction.W, NESController.Actions.JOY1_UP);
        this.keyActionMap.put(KeyAction.S, NESController.Actions.JOY1_DOWN);
        this.keyActionMap.put(KeyAction.A, NESController.Actions.JOY1_LEFT);
        this.keyActionMap.put(KeyAction.D, NESController.Actions.JOY1_RIGHT);
        this.keyActionMap.put(KeyAction.ENTER, NESController.Actions.JOY1_START);
        this.keyActionMap.put(KeyAction.BACK_SPACE, NESController.Actions.JOY1_SELECT);
        this.keyActionMap.put(KeyAction.J, NESController.Actions.JOY1_A);
        this.keyActionMap.put(KeyAction.K, NESController.Actions.JOY1_B);
    }

    @Override
    public String getName() {
        return "Nintendo Entertainment System";
    }

    @Override
    public String getId() {
        return "nes";
    }

    @Override
    public Collection<String> getFileExtensions() {
        return List.of("nes");
    }

    @Override
    public SystemAdapter createSystem(boolean detectedAutomatically) throws LineUnavailableException {
        return new NESAdapter(this.jemu, this);
    }

    @Override
    public boolean manages(SystemAdapter systemAdapter) {
        return systemAdapter instanceof NESAdapter;
    }

    Optional<CartridgeInfo> findDatabaseEntryFromNesFile(byte[] nesFile, int totalRomSize, boolean hasByteTrainer) {
        try {
            if (totalRomSize < 0) {
                throw new IllegalArgumentException("Total rom size cannot be negative!");
            }

            byte[] trustedSizeHeadersHashData = new byte[totalRomSize + (hasByteTrainer ? 512 : 0)];
            if (trustedSizeHeadersHashData.length <= nesFile.length - 16) {
                System.arraycopy(nesFile, 16, trustedSizeHeadersHashData, 0, trustedSizeHeadersHashData.length);
                String trustedSizeHeadersHash = SystemManager.getSha1Hash(trustedSizeHeadersHashData);
                NES20Database.Game trustedSizeHeaderEntry = this.databaseMap.get(trustedSizeHeadersHash);
                if (trustedSizeHeaderEntry != null) {
                    return Optional.of(trustedSizeHeaderEntry);
                }
            }

            if (nesFile.length >= 16) {
                byte[] allDataMinusHeader = new byte[nesFile.length - 16];
                System.arraycopy(nesFile, 16, allDataMinusHeader, 0, allDataMinusHeader.length);
                String allDataMinusHeaderHash = SystemManager.getSha1Hash(allDataMinusHeader);
                return Optional.ofNullable(this.databaseMap.get(allDataMinusHeaderHash));
            }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
