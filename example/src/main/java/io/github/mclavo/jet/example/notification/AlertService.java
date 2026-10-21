package io.github.mclavo.jet.example.notification;

import io.github.mclavo.jet.annotation.Fuel;
import io.github.mclavo.jet.annotation.Intake;
import io.github.mclavo.jet.annotation.Jet;
import io.github.mclavo.jet.example.notification.formatter.MessageFormatter;
import io.github.mclavo.jet.example.notification.sender.NotificationSender;

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
