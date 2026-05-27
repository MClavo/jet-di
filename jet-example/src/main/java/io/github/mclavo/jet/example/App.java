package io.github.mclavo.jet.example;

import io.github.mclavo.jet.context.ControlTower;
import io.github.mclavo.jet.context.JetContext;
import io.github.mclavo.jet.example.notification.NotificationCenter;
import io.github.mclavo.jet.example.notification.sender.NotificationSender;

public class App {
    public static void main(String[] args) {
        System.out.println("JET Notification Center");
        System.out.println();

        JetContext context = ControlTower.run(App.class);

        NotificationCenter notificationCenter = context.provide(NotificationCenter.class);
        notificationCenter.runDemo();

        System.out.println("\n--- Direct qualified lookup ---");
        NotificationSender emailSender = context.provide(NotificationSender.class, "email");
        emailSender.send("This sender was resolved directly with @Fuel(\"email\")");
    }
}
