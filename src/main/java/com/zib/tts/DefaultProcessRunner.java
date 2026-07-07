package com.zib.tts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

final class DefaultProcessRunner implements ProcessRunner {
    @Override
    public ProcessResult run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, stderr);
    }
}
