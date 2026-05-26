package org.mclavo.example.notification.sender;

public final class SlackNotificationSender implements NotificationSender {
    @Override
    public void send(String message) {
        System.out.println("[SLACK] " + message);
    }

    @Override
    public String name() {
        return "SlackNotificationSender";
    }
}
