package com.iafenvoy.elb.gui;

import com.iafenvoy.elb.config.ElbConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Objects;

public class PreLaunchWindow {
    private static final JDialog frame = new JDialog();
    private static boolean disposed = false;

    static {
        frame.setTitle(ElbConfig.getInstance().barTitle);
        frame.setResizable(false);
        frame.setSize(300, 110); // Increased height for second bar
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setLayout(new GridLayout(3, 1)); // Use grid layout for 3 components (RAM, Progress, Mods)
        frame.addKeyListener(new PreLaunchWindowKeyListener());
        
        if (ElbConfig.getInstance().logoPath != null && Files.exists(FileSystems.getDefault().getPath(ElbConfig.getInstance().logoPath), LinkOption.NOFOLLOW_LINKS))
            frame.setIconImage(new ImageIcon(ElbConfig.getInstance().logoPath).getImage());
        else {
            java.net.URL iconUrl = PreLaunchWindow.class.getResource("/minecraft_256x256.png");
            if (iconUrl != null) frame.setIconImage(new ImageIcon(iconUrl).getImage());
        }

        JProgressBar memoryBar = new JProgressBar();
        memoryBar.setStringPainted(true);
        memoryBar.setBackground(Color.DARK_GRAY);
        memoryBar.setForeground(new Color(0xAA0000)); // Darker red
        frame.add(memoryBar);

        JProgressBar modBar = new JProgressBar();
        modBar.setStringPainted(true);
        modBar.setBackground(Color.DARK_GRAY);
        modBar.setForeground(new Color(0x00AA00)); // Green for mods
        frame.add(modBar);

        JProgressBar statusProgress = new JProgressBar();
        statusProgress.setIndeterminate(true);
        statusProgress.setBackground(Color.DARK_GRAY);
        statusProgress.setForeground(new Color(0x0000AA)); // Blue
        statusProgress.setStringPainted(true);
        statusProgress.setString(ElbConfig.getInstance().barMessage);
        frame.add(statusProgress);

        new Thread(() -> {
            int totalMods = net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().size();
            modBar.setMaximum(totalMods);
            modBar.setValue(totalMods); // Since we are in PreLaunch, metadata is already there
            modBar.setString("Mods Detected: " + totalMods);

            while (!disposed) {
                long memMax = Runtime.getRuntime().maxMemory();
                long memUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long maxInMb = bytesToMb(memMax);
                long usedInMb = bytesToMb(memUsed);
                memoryBar.setMaximum((int) maxInMb);
                memoryBar.setValue((int) usedInMb);
                memoryBar.setString(String.format("RAM: %d/%d MB", usedInMb, maxInMb));
                
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    public static void display() {
        if (disposed) throw new IllegalStateException("Pre-launch window has been disposed!");
        frame.setVisible(true);
    }

    public static void remove() {
        if (disposed) return;
        frame.setVisible(false);
        frame.dispose();
        disposed = true;
    }

    public static class PreLaunchWindowKeyListener extends KeyAdapter {
        private static Tetris tetris = null;

        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == 't' && tetris == null) {
                tetris = new Tetris();
                tetris.setAlwaysOnTop(true);
                tetris.setVisible(true);
            }
        }
    }

    public static void main(String[] args) {
        display();
    }

    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }
}
