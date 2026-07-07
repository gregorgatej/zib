package com.zib.validation;

import java.nio.file.Files;
import java.nio.file.Path;

import com.zib.parser.ZibDocument;
import com.zib.parser.ZibToken;

public final class ZibValidator {
    public void validate(ZibDocument document, Path zibFile) {
        Path zibDirectory = zibFile.toAbsolutePath().normalize().getParent();
        if (zibDirectory == null) {
            zibDirectory = Path.of(".").toAbsolutePath().normalize();
        }

        for (ZibToken token : document.tokens()) {
            if (token instanceof ZibToken.Sound sound) {
                validateSoundMarker(sound.filename(), zibDirectory);
            }
        }
    }

    private static void validateSoundMarker(String marker, Path zibDirectory) {
        if (!marker.endsWith(".wav")) {
            throw new ZibValidationException("Sound marker must reference a .wav file: " + marker);
        }

        Path markerPath = Path.of(marker);
        if (markerPath.isAbsolute()) {
            throw new ZibValidationException("Sound marker must not be an absolute path: " + marker);
        }

        if (marker.contains("..")) {
            throw new ZibValidationException("Sound marker must not contain parent traversal: " + marker);
        }

        if (marker.contains("/") || marker.contains("\\")) {
            throw new ZibValidationException("Sound marker must be a filename, not a path: " + marker);
        }

        Path soundFile = zibDirectory.resolve(marker).normalize();
        if (!soundFile.startsWith(zibDirectory)) {
            throw new ZibValidationException("Sound marker resolves outside the .zib directory: " + marker);
        }

        if (!Files.exists(soundFile)) {
            throw new ZibValidationException("Referenced sound file not found next to .zib file: " + marker);
        }

        if (!Files.isRegularFile(soundFile)) {
            throw new ZibValidationException("Referenced sound file is not a regular file: " + marker);
        }

        if (!Files.isReadable(soundFile)) {
            throw new ZibValidationException("Referenced sound file is not readable: " + marker);
        }
    }
}
