package com.iafenvoy.elb.gui;

import com.alexxiconify.rustmc.RustMC;
import com.iafenvoy.elb.config.ElbConfig;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PreLaunchWindow extends JFrame {
    private static PreLaunchWindow instance;
    private final JProgressBar modBar;
    private final JProgressBar memoryBar;
    private final JLabel messageLabel;
    private boolean disposed = false;

    public PreLaunchWindow() {
        ElbConfig config = ElbConfig.getInstance();
        setTitle(config.getBarTitle());
        setUndecorated(true);
        setSize(400, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(45, 45, 45));

        if (config.getLogoPath() != null && !config.getLogoPath().isEmpty()) {
            try {
                Path path = Paths.get(config.getLogoPath());
                if (Files.exists(path)) {
                    ImageIcon icon = new ImageIcon(path.toString());
                    Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                    JLabel logoLabel = new JLabel(new ImageIcon(img));
                    logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    panel.add(logoLabel);
                }
            } catch (Exception e) {
                RustMC.LOGGER.error("Failed to load custom logo", e);
            }
        }

        modBar = new JProgressBar(0, 100);
        modBar.setStringPainted(true);
        modBar.setForeground(parseColor(config.getMessageBarColor(), new Color(255, 0, 255)));
        panel.add(modBar);

        panel.add(Box.createVerticalStrut(10));

        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setForeground(parseColor(config.getMemoryBarColor(), new Color(255, 0, 0)));
        panel.add(memoryBar);

        panel.add(Box.createVerticalStrut(10));

        messageLabel = new JLabel(config.getBarMessage());
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageLabel);

        add(panel, BorderLayout.CENTER);

        Thread.ofVirtual().start(() -> {
            while (!disposed) {
                try {
                    long memMax = Runtime.getRuntime().maxMemory();
                    long memTotal = Runtime.getRuntime().totalMemory();
                    long memFree = Runtime.getRuntime().freeMemory();
                    long memUsed = memTotal - memFree;
                    
                    int maxMb = (int)(memMax / 1024 / 1024);
                    int usedMb = (int)(memUsed / 1024 / 1024);

                    memoryBar.setMaximum(maxMb);
                    memoryBar.setValue(usedMb);
                    memoryBar.setString(String.format("RAM: %d/%d MB (JVM)", usedMb, maxMb));

                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Fallback or ignore minor UI update errors
                }
            }
        });
    }

    private Color parseColor(String colorStr, Color fallback) {
        try {
            return new Color(Integer.parseInt(colorStr));
        } catch (Exception e) {
            return fallback;
        }
    }

    public static void display() {
        if (instance == null) {
            instance = new PreLaunchWindow();
            instance.setVisible(true);
        }
    }

    public static void remove() {
        if (instance != null) {
            instance.disposed = true;
            instance.dispose();
            instance = null;
        }
    }

    public static void updateProgress(int progress, String message) {
        if (instance != null) {
            instance.modBar.setValue(progress);
            if (message != null) instance.messageLabel.setText(message);
        }
    }
}
