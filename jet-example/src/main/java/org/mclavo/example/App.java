package org.mclavo.example;

import org.mclavo.context.ControlTower;
import org.mclavo.context.JetContext;
import org.mclavo.example.notification.NotificationCenter;
import org.mclavo.example.notification.sender.NotificationSender;

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
