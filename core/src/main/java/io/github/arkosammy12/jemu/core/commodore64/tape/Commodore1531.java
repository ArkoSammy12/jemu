package io.github.arkosammy12.jemu.core.commodore64.tape;

import io.github.arkosammy12.jemu.core.commodore64.Commodore64Controller;
import io.github.arkosammy12.jemu.core.commodore64.Commodore64Emulator;
import io.github.arkosammy12.jemu.core.exceptions.EmulatorException;
import io.github.arkosammy12.jemu.core.exceptions.ROMInitializationException;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class Commodore1531 {

    private static final String TAP_FILE_EXTENSION = "tap";
    private static final String T64_FILE_EXTENSION = "t64";

    private final Commodore64Emulator commodore64Emulator;
    private final AtomicReference<TapeImageEvent> pendingTapeImageEvent = new AtomicReference<>(null);

    @Nullable
    private TapeImage currentTapeImage;
    private boolean pulseOutput;
    private int pulseIndex;
    private int pulseLengthCountdown = -1;

    private Commodore64Controller.Datasette datasetteButton = Commodore64Controller.Datasette.STOP;

    public Commodore1531(Commodore64Emulator commodore64Emulator) {
        this.commodore64Emulator = commodore64Emulator;
    }

    public boolean getREAD() {
        return this.pulseOutput;
    }

    public boolean getSENSE() {
        // TODO: Also check for fast forward, rewind, or record when those are implemented
        return this.datasetteButton == Commodore64Controller.Datasette.PLAY;
    }

    public void updateTapeImage(@Nullable Path path) {
        if (path == null) {
            this.pendingTapeImageEvent.set(new EjectTapeEvent());
        } else {
            try {
                byte[] bytes = Files.readAllBytes(path);
                String fileExtension = FilenameUtils.getExtension(path.toString());
                this.pendingTapeImageEvent.set(new InsertTapeEvent(switch (fileExtension.toLowerCase()) {
                    case TAP_FILE_EXTENSION -> new TAPImage(bytes);
                    case T64_FILE_EXTENSION -> throw new ROMInitializationException("T64 tape images are not yet supported!");
                    default -> throw new ROMInitializationException("Unsupported tape image file extension %s!".formatted(fileExtension));
                }));
            } catch (Exception e) {
                throw new EmulatorException("Failed to initialize Commodore 64 tape image!", e);
            }
        }
    }

    public void pressButton(Commodore64Controller.Datasette datasetteButton) {
        this.datasetteButton = datasetteButton;
    }

    public void cycle() {
        TapeImageEvent tapeImageEvent = this.pendingTapeImageEvent.getAndSet(null);
        if (tapeImageEvent != null) {
            switch (tapeImageEvent) {
                case InsertTapeEvent(TapeImage tapeImage) -> {
                    this.currentTapeImage = tapeImage;
                    this.pulseLengthCountdown = this.currentTapeImage.getPulseLength(0);
                }
                case EjectTapeEvent _ -> {
                    this.currentTapeImage = null;
                    this.pulseLengthCountdown = -1;
                }
                default -> {}
            }
            this.pulseOutput = false;
            this.pulseIndex = 0;
            this.datasetteButton = Commodore64Controller.Datasette.STOP;
        }

        if (this.pulseOutput) {
            this.pulseOutput = false;
        }

        if (this.currentTapeImage != null && this.commodore64Emulator.getMOTOR()) {
            if (this.pulseLengthCountdown > 0) {
                this.pulseLengthCountdown--;
                if (this.pulseLengthCountdown <= 0) {
                    this.pulseIndex++;
                    this.pulseLengthCountdown = this.currentTapeImage.getPulseLength(this.pulseIndex);
                    this.pulseOutput = true;
                }
            }
        }
    }

    private sealed interface TapeImageEvent {}

    private record InsertTapeEvent(TapeImage tapeImage) implements TapeImageEvent {}

    private record EjectTapeEvent() implements TapeImageEvent {}

}