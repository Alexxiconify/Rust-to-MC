package com.iafenvoy.elb.gui;
import com.iafenvoy.elb.config.ElbConfig;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Modern dark-themed pre-launch splash screen.
 * Custom-painted: rounded gradient bars, anti-aliased text, mod icon,
 * elapsed timer, and periodic memory updates without busy-waiting.
 */
public class PreLaunchWindow extends JFrame {
    private static PreLaunchWindow instance;
    private boolean disposed = false;
    private ScheduledExecutorService updater;
    private volatile int modProgress = 0;
    private volatile String statusMessage = "";
    private final long startTimeMs;
    private static final Color BG_DARK       = new Color(18, 18, 22);
    private static final Color BG_PANEL      = new Color(26, 26, 32);
    private static final Color BG_BORDER     = new Color(48, 48, 56);
    private static final Color BAR_TRACK     = new Color(38, 38, 46);
    private static final Color TEXT_PRIMARY   = new Color(215, 215, 225);
    private static final Color TEXT_DIM       = new Color(120, 120, 140);
    private static final Color TEXT_ACCENT    = new Color(70, 190, 255);
    private static final Color PROGRESS_LO   = new Color(40, 170, 95);
    private static final Color PROGRESS_HI   = new Color(25, 200, 255);
    private static final Color RAM_GREEN      = new Color(80, 165, 65);
    private static final Color RAM_YELLOW     = new Color(200, 175, 40);
    private static final Color RAM_RED        = new Color(210, 55, 55);
    private static final int WIN_W = 480;
    private static final int WIN_H = 230;
    private static final int BAR_H = 14, BAR_ARC = BAR_H;
    private static final int MARGIN = 24;
    private BufferedImage logoImage;
    public PreLaunchWindow() {
        ElbConfig config = ElbConfig.getInstance();
        startTimeMs = System.currentTimeMillis();
        setTitle(config.getBarTitle());
        setUndecorated(true);
        setSize(WIN_W, WIN_H);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);
        setBackground(BG_DARK);
        loadLogo(config);
        setContentPane(new SplashPanel());
        updater = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rustmc-elb-repaint");
            t.setDaemon(true);
            return t;
        });
        updater.scheduleAtFixedRate(() -> {
            if (!disposed) SwingUtilities.invokeLater(this::repaint);
        }, 100, 150, TimeUnit.MILLISECONDS);
    }
    private void loadLogo(ElbConfig config) {
        if (config.getLogoPath() != null && !config.getLogoPath().isEmpty()) {
            try {
                java.nio.file.Path p = java.nio.file.Paths.get(config.getLogoPath());
                if (java.nio.file.Files.exists(p)) { logoImage = ImageIO.read(p.toFile()); return; }
            } catch (Exception ignored) { }
        }
        // Try namespaced path first to avoid loading another mod's icon.png
        try (InputStream is = PreLaunchWindow.class.getResourceAsStream("/assets/rust-mc/icon.png")) {
            if (is != null) { logoImage = ImageIO.read(is); return; }
        } catch (Exception ignored) { }
        try (InputStream is = PreLaunchWindow.class.getResourceAsStream("/icon.png")) {
            if (is != null) logoImage = ImageIO.read(is);
        } catch (Exception ignored) { }
    }
    private class SplashPanel extends JPanel {
        SplashPanel() { setOpaque(true); setBackground(BG_DARK); }
        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth();
            int h = getHeight();
            g.setColor(BG_PANEL);
            g.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 18, 18));
            g.setColor(BG_BORDER);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 18, 18));
            int y = MARGIN;
            int logoSz = 38;
            int titleX = MARGIN;
            if (logoImage != null) {
                g.drawImage(logoImage, MARGIN, y - 2, logoSz, logoSz, null);
                titleX = MARGIN + logoSz + 12;
            }
            g.setFont(new Font("Segoe UI", Font.BOLD, 17));
            g.setColor(TEXT_PRIMARY);
            g.drawString("Rust to MC", titleX, y + 14);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g.setColor(TEXT_DIM);
            g.drawString(getModVersion(), titleX, y + 30);
            long elapsed = System.currentTimeMillis() - startTimeMs;
            String timeStr = String.format("%.1fs", elapsed / 1000.0);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.setColor(TEXT_DIM);
            int tw = g.getFontMetrics().stringWidth(timeStr);
            g.drawString(timeStr, w - MARGIN - tw, y + 14);
            y += logoSz + 18;
            int barX = MARGIN, barW = w - MARGIN * 2;
            drawBarLabel(g, "Loading", modProgress + "%", barX, barW, y);
            y += 15;
            drawGradientBar(g, barX, y, barW, BAR_H, modProgress / 100.0f, PROGRESS_LO, PROGRESS_HI);
            y += BAR_H + 14;
            long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long max  = Runtime.getRuntime().maxMemory();
            float ramR = max > 0 ? (float) used / max : 0;
            String ramPct = (int)(ramR * 100) + "%";
            String ramMB  = String.format("%,d / %,d MB", (int)(used >> 20), (int)(max >> 20));
            Color rLo = ramR < 0.55f ? RAM_GREEN : RAM_YELLOW;
            Color rHi = ramR < 0.55f ? RAM_YELLOW : RAM_RED;
            drawBarLabel(g, "Memory", ramPct, barX, barW, y);
            y += 15;
            drawGradientBar(g, barX, y, barW, BAR_H, ramR, rLo, rHi);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g.setColor(new Color(255, 255, 255, 170));
            int mw = g.getFontMetrics().stringWidth(ramMB);
            g.drawString(ramMB, barX + (barW - mw) / 2, y + BAR_H - 3);
            y += BAR_H + 16;
            g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g.setColor(TEXT_ACCENT);
            String msg = (statusMessage != null && !statusMessage.isEmpty()) ? statusMessage : "Initializing...";
            g.drawString(stageIcon(modProgress) + "  " + msg, barX, y);
        }
    }
    private static void drawBarLabel(Graphics2D g, String left, String right, int x, int barW, int y) {
        g.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g.setColor(TEXT_DIM);
        g.drawString(left, x, y);
        g.setColor(TEXT_PRIMARY);
        int rw = g.getFontMetrics().stringWidth(right);
        g.drawString(right, x + barW - rw, y);
    }
    private static void drawGradientBar(Graphics2D g, int x, int y, int w, int h,
                                         float ratio, Color lo, Color hi) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int fillW = Math.max(0, (int)(w * ratio));
        g.setColor(BAR_TRACK);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, BAR_ARC, BAR_ARC));
        if (fillW > 2) {
            Shape oldClip = g.getClip();
            g.setClip(new RoundRectangle2D.Float(x, y, fillW, h, BAR_ARC, BAR_ARC));
            g.setPaint(new GradientPaint(x, y, lo, x + w, y, hi));
            g.fill(new RoundRectangle2D.Float(x, y, w, h, BAR_ARC, BAR_ARC));
            g.setPaint(null);
            g.setColor(new Color(255, 255, 255, 28));
            g.fillRect(x, y + 1, fillW, h / 2 - 1);
            g.setClip(oldClip);
        }
    }
    private static String stageIcon(int progress) {
        if (progress >= 95) return "✨";
        if (progress >= 80) return "\uD83D\uDD0A";
        if (progress >= 60) return "\uD83D\uDCE6";
        if (progress >= 25) return "⚙\uFE0F";
        return "\uD83D\uDE80";
    }
    private static String getModVersion() {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("rust-mc")
                .map(c -> "v" + c.getMetadata().getVersion().getFriendlyString())
                .orElse("v1.0");
        } catch (Exception e) { return "v1.0"; }
    }
    public static void display() {
        SwingUtilities.invokeLater(() -> {
            if (instance == null) { instance = new PreLaunchWindow(); instance.setVisible(true); }
        });
    }
    public static void remove() {
        SwingUtilities.invokeLater(() -> {
            if (instance != null) {
                instance.disposed = true;
                if (instance.updater != null) instance.updater.shutdown();
                instance.dispose();
                instance = null;
            }
        });
    }
    public static void updateProgress(int progress, String message) {
        PreLaunchWindow win = instance;
        if (win == null) return;
        win.modProgress = Math.max(0, Math.min(100, progress));
        if (message != null) win.statusMessage = message;
    }
}