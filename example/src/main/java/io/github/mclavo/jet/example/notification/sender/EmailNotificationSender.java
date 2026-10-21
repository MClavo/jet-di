package io.github.mclavo.jet.example.notification.sender;

public final class EmailNotificationSender implements NotificationSender {
    @Override
    public void send(String message) {
        System.out.println("[EMAIL] " + message);
    }

    @Override
    public String name() {
        return "EmailNotificationSender";
    }
}
