package com.zib;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.zib.audio.AudioPlayer;
import com.zib.audio.JavaSoundAudioPlayer;
import com.zib.error.ZibException;
import com.zib.parser.ZibDocument;
import com.zib.parser.ZibParseException;
import com.zib.parser.ZibParser;
import com.zib.runtime.PlaybackEvent;
import com.zib.runtime.PlaybackOrchestrator;
import com.zib.runtime.TempDirectoryManager;
import com.zib.tts.EspeakNgService;
import com.zib.tts.TtsSegmentGenerator;
import com.zib.tts.TtsService;
import com.zib.validation.ZibValidationException;
import com.zib.validation.ZibValidator;

public final class ZibApp {
    private ZibApp() {
    }

    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        TtsService ttsService = new EspeakNgService();
        return run(args, out, err, ttsService, new TempDirectoryManager(), new JavaSoundAudioPlayer());
    }

    static int run(String[] args, PrintStream out, PrintStream err, TtsService ttsService, TempDirectoryManager tempDirectoryManager, AudioPlayer audioPlayer) {
        ValidationResult validation = CliArguments.validate(args);
        if (!validation.isValid()) {
            err.println("ERROR: " + validation.errorMessage().orElse("invalid command line arguments"));
            return 1;
        }

        Path inputFile = validation.inputFile().orElseThrow();
        Path tempDirectory = null;
        try {
            String source = Files.readString(inputFile);
            ZibDocument document = new ZibParser().parse(source);
            new ZibValidator().validate(document, inputFile);
            ttsService.checkAvailability();

            tempDirectory = tempDirectoryManager.createTempDirectory();
            List<PlaybackEvent> playbackEvents = new TtsSegmentGenerator(ttsService).generate(document, inputFile, tempDirectory);
            new PlaybackOrchestrator(audioPlayer).play(playbackEvents);
            tempDirectoryManager.deleteAfterSuccess(tempDirectory);

            out.println("Input accepted: " + inputFile);
            out.println("Parser and sound marker validation completed.");
            out.println("Generated and played events: " + playbackEvents.size());
            return 0;
        } catch (ZibParseException | ZibValidationException exception) {
            err.println("ERROR: " + exception.getMessage());
            return 1;
        } catch (ZibException exception) {
            if (tempDirectory != null) {
                tempDirectoryManager.keepAfterFailure(tempDirectory);
                err.println("ERROR: " + exception.getMessage() + ". Temporary files kept at: " + tempDirectory);
            } else {
                err.println("ERROR: " + exception.getMessage());
            }
            return 1;
        } catch (IOException exception) {
            err.println("ERROR: failed to read file: " + inputFile);
            return 1;
        }
    }
}
