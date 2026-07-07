# ZIB — zvok in beseda

ZIB is a Java 17 proof-of-concept application that reads a `.zib` file, generates speech with eSpeak NG, and starts WAV sound effects at marker positions while speech continues.

This repository is intended to be built incrementally with a coding agent such as Codex. Read `AGENTS.md` first.

---

## POC scope

Supported:

- Linux.
- Java 17.
- Maven.
- `.zib` file input.
- eSpeak NG TTS.
- WAV sound effects.
- Background sound effects triggered by markers.

Not supported in POC:

- GUI.
- MP3.
- audio export/mixing.
- bundled installer.
- terminal text input.
- LLM-based generation.

---

## Required demo syntax

`examples/demo.zib`:

```zib
"Today is a beautiful day. The children are playing outside ${children_laughing.wav} on the playground."
```

`children_laughing.wav` must be in the same directory as `demo.zib`.

---

## Install prerequisites

Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven espeak-ng
```

Arch:

```bash
sudo pacman -S jdk17-openjdk maven espeak-ng
```

Verify:

```bash
java -version
mvn -version
espeak-ng --version
```

---

## Build

```bash
mvn clean package
```

The executable JAR is written to:

```text
target/zib-0.1.0.jar
```

---

## Test

```bash
mvn test
```

Optional integration tests, if configured:

```bash
mvn verify -Pintegration-tests
```

---

## Run

```bash
java -jar target/zib-0.1.0.jar examples/demo.zib
```

Expected behavior:

1. The app validates the CLI argument and `.zib` file.
2. It parses the quoted `.zib` block and sound marker.
3. It verifies `children_laughing.wav` exists next to `demo.zib`.
4. It checks `espeak-ng` is available.
5. It generates temporary speech WAV files under a `zib-` temp directory.
6. It plays speech sequentially and starts the WAV effect at the marker while speech continues.
7. It exits after final speech and deletes temp files on success.

On failure, errors start with `ERROR:`. If failure happens after temp file creation, generated temp files are retained and the temp directory path is printed.

Common setup errors:

```text
ERROR: espeak-ng was not found on PATH. Install it first, for example: sudo apt install espeak-ng
ERROR: Referenced sound file not found next to .zib file: children_laughing.wav
```

---

## Important references

- eSpeak NG: https://github.com/espeak-ng/espeak-ng
- Java ProcessBuilder: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ProcessBuilder.html
- Java Sound playback: https://docs.oracle.com/javase/8/docs/technotes/guides/sound/programmer_guide/chapter4.html
- JUnit 5: https://docs.junit.org/current/user-guide/
- Codex AGENTS.md: https://developers.openai.com/codex/guides/agents-md
