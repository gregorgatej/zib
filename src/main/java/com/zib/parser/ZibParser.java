package com.zib.parser;

import java.util.ArrayList;
import java.util.List;

public final class ZibParser {
    public ZibDocument parse(String source) {
        if (source == null) {
            throw new ZibParseException("input is empty");
        }

        int firstContent = firstNonWhitespace(source);
        if (firstContent == -1) {
            throw new ZibParseException("missing opening quote");
        }

        if (source.charAt(firstContent) != '"') {
            throw new ZibParseException("missing opening quote");
        }

        int firstClosingQuote = source.indexOf('"', firstContent + 1);
        if (firstClosingQuote == -1) {
            throw new ZibParseException("missing closing quote");
        }

        int lastContent = lastNonWhitespace(source);
        if (lastContent != firstClosingQuote) {
            if (source.charAt(lastContent) == '"') {
                throw new ZibParseException("inner double quotes are not supported");
            }
            throw new ZibParseException("text outside the quoted block is not allowed");
        }

        String quotedBlock = source.substring(firstContent + 1, firstClosingQuote);
        return new ZibDocument(parseTokens(quotedBlock));
    }

    private static List<ZibToken> parseTokens(String quotedBlock) {
        List<ZibToken> tokens = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        int index = 0;
        while (index < quotedBlock.length()) {
            if (quotedBlock.startsWith("${", index)) {
                flushText(tokens, text);

                int markerEnd = quotedBlock.indexOf('}', index + 2);
                if (markerEnd == -1) {
                    throw new ZibParseException("missing closing brace for sound marker");
                }

                String marker = quotedBlock.substring(index + 2, markerEnd);
                validateMarker(marker);
                tokens.add(new ZibToken.Sound(marker));
                index = markerEnd + 1;
            } else {
                char next = quotedBlock.charAt(index);
                if (next == '"') {
                    throw new ZibParseException("inner double quotes are not supported");
                }
                text.append(next);
                index++;
            }
        }

        flushText(tokens, text);
        return tokens;
    }

    private static void validateMarker(String marker) {
        if (marker.isEmpty()) {
            throw new ZibParseException("sound marker must not be empty");
        }

        if (marker.contains("${")) {
            throw new ZibParseException("nested sound markers are not supported");
        }

        if (!marker.endsWith(".wav")) {
            throw new ZibParseException("sound marker must reference a .wav file");
        }
    }

    private static void flushText(List<ZibToken> tokens, StringBuilder text) {
        if (!text.isEmpty()) {
            tokens.add(new ZibToken.Text(text.toString()));
            text.setLength(0);
        }
    }

    private static int firstNonWhitespace(String source) {
        for (int index = 0; index < source.length(); index++) {
            if (!Character.isWhitespace(source.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private static int lastNonWhitespace(String source) {
        for (int index = source.length() - 1; index >= 0; index--) {
            if (!Character.isWhitespace(source.charAt(index))) {
                return index;
            }
        }
        return -1;
    }
}
