package com.zib.parser;

public sealed interface ZibToken permits ZibToken.Text, ZibToken.Sound {
    record Text(String value) implements ZibToken {
    }

    record Sound(String filename) implements ZibToken {
    }
}
