package com.iafenvoy.elb.gui;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import com.iafenvoy.elb.config.ElbConfig;
import net.fabricmc.loader.api.FabricLoader;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
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
        setTitle(config.barTitle);
        setUndecorated(true);
        setSize(400, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(45, 45, 45));

        if (config.logoPath != null && !config.logoPath.isEmpty()) {
            try {
                Path path = Paths.get(config.logoPath);
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
        modBar.setForeground(new Color(Integer.parseInt(config.messageBarColor)));
        panel.add(modBar);

        panel.add(Box.createVerticalStrut(10));

        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setForeground(new Color(Integer.parseInt(config.memoryBarColor)));
        panel.add(memoryBar);

        panel.add(Box.createVerticalStrut(10));

        messageLabel = new JLabel(config.barMessage);
        messageLabel.setForeground(Color.WHITE);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageLabel);

        add(panel, BorderLayout.CENTER);

        new Thread(() -> {
            try (java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
                java.lang.foreign.MemorySegment totalPtr = arena.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);
                java.lang.foreign.MemorySegment usedPtr = arena.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);

                while (!disposed) {
                    com.alexxiconify.rustmc.NativeBridge.getSystemMemory(totalPtr, usedPtr);
                    long maxInMb = totalPtr.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
                    long usedInMb = usedPtr.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);

                    if (maxInMb > 0) {
                        memoryBar.setMaximum((int) maxInMb);
                        memoryBar.setValue((int) usedInMb);
                        memoryBar.setString(String.format("RAM: %d/%d MB (System)", usedInMb, maxInMb));
                    } else {
                        long memMax = Runtime.getRuntime().maxMemory();
                        long memUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                        memoryBar.setMaximum((int) (memMax / 1024 / 1024));
                        memoryBar.setValue((int) (memUsed / 1024 / 1024));
                        memoryBar.setString(String.format("RAM: %d/%d MB (JVM)", memUsed / 1024 / 1024, memMax / 1024 / 1024));
                    }

                    try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            } catch (Exception e) {
                while (!disposed) {
                    long memMax = Runtime.getRuntime().maxMemory();
                    long memUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                    memoryBar.setMaximum((int) (memMax / 1024 / 1024));
                    memoryBar.setValue((int) (memUsed / 1024 / 1024));
                    memoryBar.setString(String.format("RAM: %d/%d MB (JVM)", memUsed / 1024 / 1024, memMax / 1024 / 1024));
                    try { Thread.sleep(100); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                }
            }
        }).start();
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
