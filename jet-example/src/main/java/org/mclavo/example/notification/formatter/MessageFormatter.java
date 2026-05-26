package org.mclavo.example.notification.formatter;

public interface MessageFormatter {
    String format(String title, String body);

    String name();
}
