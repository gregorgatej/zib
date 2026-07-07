# ZIB POC Review

## Implemented behavior

- Builds as a Java 17 Maven project and packages `target/zib-0.1.0.jar`.
- Runs with:

  ```bash
  java -jar target/zib-0.1.0.jar examples/demo.zib
  ```

- Parses exactly one quoted `.zib` block with text and `${filename.wav}` markers.
- Rejects invalid syntax, unsafe marker filenames, missing referenced WAV files, MP3 markers, paths, absolute paths, and parent traversal.
- Checks `espeak-ng` availability through `ProcessBuilder` argument lists.
- Generates one temporary speech WAV per non-blank text segment in a `zib-` temp directory.
- Plays speech sequentially and starts WAV effects at marker positions while speech continues.
- Uses a single Java Sound output line for playback to avoid multiple-line `Clip` contention.
- Deletes temp files after successful playback and keeps them on failure with a reported temp path.

## Demo assets

`examples/demo.zib` contains:

```zib
"Today is a beautiful day. The children are playing outside ${children_laughing.wav} on the playground."
```

`examples/children_laughing.wav` is tracked next to the demo file.

## Verification commands

```bash
mvn test
mvn package
java -jar target/zib-0.1.0.jar examples/demo.zib
```

The manual smoke test requires `espeak-ng` and a working Linux audio output device.

## Remaining limitations

- No GUI or installer.
- No MP3 support.
- No final mixed audio export.
- No language or voice selection; the demo is English for the current default eSpeak voice.
- No cross-platform support beyond the Linux POC target.
- No integration-test Maven profile is configured yet.
