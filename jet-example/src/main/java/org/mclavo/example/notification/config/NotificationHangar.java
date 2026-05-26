package org.mclavo.example.notification.config;

import org.mclavo.annotation.Fuel;
import org.mclavo.annotation.Hangar;
import org.mclavo.annotation.Maverick;
import org.mclavo.annotation.Part;
import org.mclavo.example.notification.formatter.JsonFormatter;
import org.mclavo.example.notification.formatter.MarkdownFormatter;
import org.mclavo.example.notification.formatter.MessageFormatter;
import org.mclavo.example.notification.formatter.PlainTextFormatter;
import org.mclavo.example.notification.sender.ConsoleNotificationSender;
import org.mclavo.example.notification.sender.EmailNotificationSender;
import org.mclavo.example.notification.sender.NotificationSender;
import org.mclavo.example.notification.sender.SlackNotificationSender;

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
