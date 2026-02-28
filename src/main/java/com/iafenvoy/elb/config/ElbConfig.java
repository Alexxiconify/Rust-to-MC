package com.iafenvoy.elb.config;

import net.fabricmc.loader.api.FabricLoader;
import com.alexxiconify.rustmc.RustMC;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ElbConfig {
    private static ElbConfig instance = null;

    public String logoPath = null;
    public String barTitle = "Minecraft %version%";
    public String barMessage = "Minecraft is launching, please wait";
    public String memoryBarColor = "-65536";
    public String messageBarColor = "16711935";

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
