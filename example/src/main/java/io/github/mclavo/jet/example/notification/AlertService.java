package io.github.mclavo.jet.example.notification;

import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.github.mclavo.jet.example.notification.formatter.MessageFormatter;
import io.github.mclavo.jet.example.notification.sender.NotificationSender;

@Singleton
public final class AlertService {
    private final NotificationSender sender;
    private final MessageFormatter formatter;

    @Inject
    public AlertService(@Named("slack") NotificationSender sender, MessageFormatter formatter) {
        this.sender = sender;
        this.formatter = formatter;
    }

    public void sendDeploymentAlert() {
        System.out.println("AlertService resolved by JET");
        System.out.println("Formatter: " + formatter.name());
        System.out.println("Sender: " + sender.name());
        sender.send(formatter.format("Deployment completed", "JET example is running"));
    }
}
