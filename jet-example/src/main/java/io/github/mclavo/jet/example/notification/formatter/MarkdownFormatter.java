package io.github.mclavo.jet.example.notification.formatter;

public final class MarkdownFormatter implements MessageFormatter {
    @Override
    public String format(String title, String body) {
        return "## " + title + "\n\n" + body;
    }

    @Override
    public String name() {
        return "MarkdownFormatter";
    }
}
