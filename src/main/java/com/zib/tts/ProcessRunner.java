package com.zib.tts;

import java.io.IOException;
import java.util.List;

@FunctionalInterface
public interface ProcessRunner {
    ProcessResult run(List<String> command) throws IOException, InterruptedException;
}
