package org.mclavo.example.notification;

import org.mclavo.annotation.Fuel;
import org.mclavo.annotation.Intake;
import org.mclavo.annotation.Jet;
import org.mclavo.example.notification.formatter.MessageFormatter;
import org.mclavo.example.notification.sender.NotificationSender;

@Jet
public final class AlertService {
    private final NotificationSender sender;
    private final MessageFormatter formatter;

    @Intake
    public AlertService(@Fuel("slack") NotificationSender sender, MessageFormatter formatter) {
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
