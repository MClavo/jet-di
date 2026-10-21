package io.github.mclavo.jet.example.notification.formatter;

public final class PlainTextFormatter implements MessageFormatter {
    @Override
    public String format(String title, String body) {
        return title + ": " + body;
    }

    @Override
    public String name() {
        return "PlainTextFormatter";
    }
}
