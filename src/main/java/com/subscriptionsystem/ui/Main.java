package com.subscriptionsystem.ui;

import javax.swing.*;
import java.awt.*;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Subscription Management System");

            JLabel title = new JLabel(
                    "Subscription Management System",
                    SwingConstants.CENTER
            );
            title.setFont(new Font("SansSerif", Font.BOLD, 24));

            JLabel message = new JLabel(
                    "Welcome! Your application is ready to grow.",
                    SwingConstants.CENTER
            );

            frame.setLayout(new BorderLayout(10, 10));
            frame.add(title, BorderLayout.NORTH);
            frame.add(message, BorderLayout.CENTER);

            frame.setSize(800, 500);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
