package com.alexxiconify.rustmc;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.awt.Color;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
public class ElbConfig {
    private static ElbConfig instance = null;
    private String logoPath = "";
    private String barTitle = "Early Loading Bar %version%";
    private String barMessage = "Loading Minecraft %version%...";
    private String memoryBarColor = String.valueOf(Color.RED.getRGB());
    private String messageBarColor = String.valueOf(Color.MAGENTA.getRGB());
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    public String getBarTitle() { return barTitle; }
    public void setBarTitle(String barTitle) { this.barTitle = barTitle; }
    public String getBarMessage() { return barMessage; }
    public void setBarMessage(String barMessage) { this.barMessage = barMessage; }
    public String getMemoryBarColor() { return memoryBarColor; }
    public void setMemoryBarColor(String memoryBarColor) { this.memoryBarColor = memoryBarColor; }
    public String getMessageBarColor() { return messageBarColor; }
    public void setMessageBarColor(String messageBarColor) { this.messageBarColor = messageBarColor; }
    public static ElbConfig getInstance() {
        if (instance == null) {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("early-loading-bar.json");
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    instance = new Gson().fromJson(reader, ElbConfig.class);
                } catch (Exception e) {
                    RustMC.LOGGER.error("Failed to load ELB config", e);
                }
            }
            if (instance == null) instance = new ElbConfig();
            FabricLoader.getInstance().getModContainer("minecraft").ifPresent(container -> {
                String version = container.getMetadata().getVersion().getFriendlyString();
                instance.barTitle = instance.barTitle.replace("%version%", version);
                instance.barMessage = instance.barMessage.replace("%version%", version);
            });
        }
        return instance;
    }
    public void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("early-loading-bar.json");
        try (Writer writer = Files.newBufferedWriter(path)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
        } catch (Exception e) {
            RustMC.LOGGER.error("Failed to save ELB config", e);
        }
    }
}