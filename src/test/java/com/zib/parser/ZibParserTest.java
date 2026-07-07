package com.zib.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class ZibParserTest {
    private final ZibParser parser = new ZibParser();

    @Test
    void parsesTheRequiredDemoFile() {
        ZibDocument document = parser.parse("\"Today is a beautiful day. The children are playing outside ${children_laughing.wav} on the playground.\"");

        assertTokens(document.tokens(), List.of(
                new ZibToken.Text("Today is a beautiful day. The children are playing outside "),
                new ZibToken.Sound("children_laughing.wav"),
                new ZibToken.Text(" on the playground.")));
    }

    @Test
    void parsesMultipleMarkers() {
        ZibDocument document = parser.parse("\"Hello ${a.wav} world ${b.wav} again.\"");

        assertTokens(document.tokens(), List.of(
                new ZibToken.Text("Hello "),
                new ZibToken.Sound("a.wav"),
                new ZibToken.Text(" world "),
                new ZibToken.Sound("b.wav"),
                new ZibToken.Text(" again.")));
    }

    @Test
    void parsesMarkerAtBeginning() {
        ZibDocument document = parser.parse("\"${start.wav} begins here\"");

        assertTokens(document.tokens(), List.of(
                new ZibToken.Sound("start.wav"),
                new ZibToken.Text(" begins here")));
    }

    @Test
    void parsesMarkerAtEnd() {
        ZibDocument document = parser.parse("\"ends here ${end.wav}\"");

        assertTokens(document.tokens(), List.of(
                new ZibToken.Text("ends here "),
                new ZibToken.Sound("end.wav")));
    }

    @Test
    void rejectsMissingOpeningQuote() {
        assertParseError("missing opening quote", "Hello ${a.wav}\"");
    }

    @Test
    void rejectsMissingClosingQuote() {
        assertParseError("missing closing quote", "\"Hello ${a.wav}");
    }

    @Test
    void rejectsTextOutsideTheQuotedBlock() {
        assertParseError("text outside the quoted block is not allowed", "\"Hello\" trailing");
    }

    @Test
    void rejectsInnerDoubleQuotes() {
        assertParseError("inner double quotes are not supported", "\"Hello \"quoted\" text\"");
    }

    @Test
    void rejectsEmptyMarker() {
        assertParseError("sound marker must not be empty", "\"Hello ${}\"");
    }

    @Test
    void rejectsMarkerWithoutWavExtension() {
        assertParseError("sound marker must reference a .wav file", "\"Hello ${sound}\"");
    }

    @Test
    void rejectsMp3Marker() {
        assertParseError("sound marker must reference a .wav file", "\"Hello ${sound.mp3}\"");
    }

    @Test
    void rejectsNestedMarker() {
        assertParseError("nested sound markers are not supported", "\"Hello ${outer${inner.wav}}\"");
    }

    @Test
    void preservesTextSpacingAroundMarkers() {
        ZibDocument document = parser.parse("\"A  ${one.wav}   B ${two.wav} C\"");

        assertTokens(document.tokens(), List.of(
                new ZibToken.Text("A  "),
                new ZibToken.Sound("one.wav"),
                new ZibToken.Text("   B "),
                new ZibToken.Sound("two.wav"),
                new ZibToken.Text(" C")));
    }

    private static void assertParseError(String expectedMessage, String source) {
        ZibParseException exception = assertThrows(ZibParseException.class, () -> new ZibParser().parse(source));

        assertEquals(expectedMessage, exception.getMessage());
    }

    private static void assertTokens(List<ZibToken> actual, List<ZibToken> expected) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            ZibToken expectedToken = expected.get(index);
            ZibToken actualToken = actual.get(index);

            assertInstanceOf(expectedToken.getClass(), actualToken);
            assertEquals(expectedToken, actualToken);
        }
    }
}
