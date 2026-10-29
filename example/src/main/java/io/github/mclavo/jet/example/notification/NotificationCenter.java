package io.github.mclavo.jet.example.notification;

import jakarta.inject.Named;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import io.github.mclavo.jet.example.notification.formatter.MessageFormatter;
import io.github.mclavo.jet.example.notification.sender.NotificationSender;

@Singleton
public final class NotificationCenter {

    private final AlertService alertService;
    private final NotificationSender defaultSender;
    private final MessageFormatter jsonFormatter;
    private final MessageFormatter markdownFormatter;

    @Inject
    public NotificationCenter(
            AlertService alertService,
            NotificationSender defaultSender,
            @Named("json") MessageFormatter jsonFormatter,
            @Named("markdown") MessageFormatter markdownFormatter
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
        System.out.println("--- Default sender lookup through @Primary ---");
        defaultSender.send("This sender was injected into NotificationCenter through @Primary");

        System.out.println();
        System.out.println("--- Qualified JSON formatter injection ---");
        System.out.println(jsonFormatter.format("Container ready", "Message formatter injected with @Named(\"json\")"));

        System.out.println();
        System.out.println("--- Qualified Markdown formatter injection ---");
        System.out.println(markdownFormatter.format("Container ready", "Message formatter injected with @Named(\"markdown\")"));
    }
}
