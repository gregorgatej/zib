package com.zib.parser;

import java.util.List;

public record ZibDocument(List<ZibToken> tokens) {
    public ZibDocument {
        tokens = List.copyOf(tokens);
    }
}
