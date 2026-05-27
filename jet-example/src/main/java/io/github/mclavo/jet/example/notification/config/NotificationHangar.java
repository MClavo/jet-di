package io.github.mclavo.jet.example.notification.config;

import io.github.mclavo.jet.annotation.Fuel;
import io.github.mclavo.jet.annotation.Hangar;
import io.github.mclavo.jet.annotation.Maverick;
import io.github.mclavo.jet.annotation.Part;
import io.github.mclavo.jet.example.notification.formatter.JsonFormatter;
import io.github.mclavo.jet.example.notification.formatter.MarkdownFormatter;
import io.github.mclavo.jet.example.notification.formatter.MessageFormatter;
import io.github.mclavo.jet.example.notification.formatter.PlainTextFormatter;
import io.github.mclavo.jet.example.notification.sender.ConsoleNotificationSender;
import io.github.mclavo.jet.example.notification.sender.EmailNotificationSender;
import io.github.mclavo.jet.example.notification.sender.NotificationSender;
import io.github.mclavo.jet.example.notification.sender.SlackNotificationSender;

@Hangar
public class NotificationHangar {
    @Part
    @Fuel("email")
    public NotificationSender emailSender() {
        return new EmailNotificationSender();
    }

    @Part
    @Fuel("slack")
    @Maverick
    public NotificationSender slackSender() {
        return new SlackNotificationSender();
    }

    @Part
    @Fuel("console")
    public NotificationSender consoleSender() {
        return new ConsoleNotificationSender();
    }

    @Part
    @Maverick
    public MessageFormatter plainTextFormatter() {
        return new PlainTextFormatter();
    }

    @Part
    @Fuel("json")
    public MessageFormatter jsonFormatter() {
        return new JsonFormatter();
    }

    @Part
    @Fuel("markdown")
    public MessageFormatter markdownFormatter() {
        return new MarkdownFormatter();
    }
}
