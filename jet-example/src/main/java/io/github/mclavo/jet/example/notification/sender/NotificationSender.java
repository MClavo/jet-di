package io.github.mclavo.jet.example.notification.sender;

public interface NotificationSender {
    void send(String message);

    String name();
}
