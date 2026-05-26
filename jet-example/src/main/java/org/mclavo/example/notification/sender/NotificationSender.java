package org.mclavo.example.notification.sender;

public interface NotificationSender {
    void send(String message);

    String name();
}
