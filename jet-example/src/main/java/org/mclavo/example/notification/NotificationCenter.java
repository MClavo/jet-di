package org.mclavo.example.notification;

import org.mclavo.annotation.Fuel;
import org.mclavo.annotation.Intake;
import org.mclavo.annotation.Jet;
import org.mclavo.example.notification.formatter.MessageFormatter;
import org.mclavo.example.notification.sender.NotificationSender;

@Jet
public final class NotificationCenter {

    private final AlertService alertService;
    private final NotificationSender defaultSender;
    private final MessageFormatter jsonFormatter;
    private final MessageFormatter markdownFormatter;

    @Intake
    public NotificationCenter(
            AlertService alertService,
            NotificationSender defaultSender,
            @Fuel("json") MessageFormatter jsonFormatter,
            @Fuel("markdown") MessageFormatter markdownFormatter
    ) {
        this.alertService = alertService;
        this.defaultSender = defaultSender;
        this.jsonFormatter = jsonFormatter;
        this.markdownFormatter = markdownFormatter;
    }

    public void runDemo() {
        System.out.println("--- Multi-level dependency graph ---");
        System.out.println("NotificationCenter resolved by JET");
        System.out.println("AlertService was injected into NotificationCenter");
        alertService.sendDeploymentAlert();

        System.out.println();
        System.out.println("--- Default sender lookup through @Maverick ---");
        defaultSender.send("This sender was injected into NotificationCenter through @Maverick");

        System.out.println();
        System.out.println("--- Qualified JSON formatter injection ---");
        System.out.println(jsonFormatter.format("Container ready", "Message formatter injected with @Fuel(\"json\")"));

        System.out.println();
        System.out.println("--- Qualified Markdown formatter injection ---");
        System.out.println(markdownFormatter.format("Container ready", "Message formatter injected with @Fuel(\"markdown\")"));
    }
}
