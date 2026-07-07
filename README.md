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

Current skeleton behavior:

1. The app validates that exactly one argument was provided.
2. It validates that the argument exists and is a regular file.
3. It reports that parser and audio playback are not implemented yet.

Parser, eSpeak NG invocation, and WAV playback will be added in later work orders.

---

## Important references

- eSpeak NG: https://github.com/espeak-ng/espeak-ng
- Java ProcessBuilder: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ProcessBuilder.html
- Java Sound playback: https://docs.oracle.com/javase/8/docs/technotes/guides/sound/programmer_guide/chapter4.html
- JUnit 5: https://docs.junit.org/current/user-guide/
- Codex AGENTS.md: https://developers.openai.com/codex/guides/agents-md
