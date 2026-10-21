package io.github.mclavo.jet.example.notification.sender;

public final class ConsoleNotificationSender implements NotificationSender {
    @Override
    public void send(String message) {
        System.out.println("[CONSOLE] " + message);
    }

    @Override
    public String name() {
        return "ConsoleNotificationSender";
    }
}
