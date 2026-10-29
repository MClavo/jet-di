package io.github.mclavo.jet.example.notification.config;

import jakarta.inject.Named;
import io.github.mclavo.jet.annotation.Factory;
import io.github.mclavo.jet.annotation.Primary;
import io.github.mclavo.jet.annotation.Bean;
import io.github.mclavo.jet.example.notification.formatter.JsonFormatter;
import io.github.mclavo.jet.example.notification.formatter.MarkdownFormatter;
import io.github.mclavo.jet.example.notification.formatter.MessageFormatter;
import io.github.mclavo.jet.example.notification.formatter.PlainTextFormatter;
import io.github.mclavo.jet.example.notification.sender.ConsoleNotificationSender;
import io.github.mclavo.jet.example.notification.sender.EmailNotificationSender;
import io.github.mclavo.jet.example.notification.sender.NotificationSender;
import io.github.mclavo.jet.example.notification.sender.SlackNotificationSender;

@Factory
public class NotificationHangar {
    @Bean
    @Named("email")
    public NotificationSender emailSender() {
        return new EmailNotificationSender();
    }

    @Bean
    @Named("slack")
    @Primary
    public NotificationSender slackSender() {
        return new SlackNotificationSender();
    }

    @Bean
    @Named("console")
    public NotificationSender consoleSender() {
        return new ConsoleNotificationSender();
    }

    @Bean
    @Primary
    public MessageFormatter plainTextFormatter() {
        return new PlainTextFormatter();
    }

    @Bean
    @Named("json")
    public MessageFormatter jsonFormatter() {
        return new JsonFormatter();
    }

    @Bean
    @Named("markdown")
    public MessageFormatter markdownFormatter() {
        return new MarkdownFormatter();
    }
}
