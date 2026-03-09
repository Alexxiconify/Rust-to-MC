package com.alexxiconify.rustmc.compat;

import com.alexxiconify.rustmc.NativeBridge;
import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.network.ClientPlayerEntity;

public class XaeroGhostMapCompat {
    private static NativeImageBackedTexture ghostTexture = null;
    private static final Identifier GHOST_TEXTURE_ID = Identifier.of(RustMC.MOD_ID, "textures/gui/ghost_map.png");
    private static double lastUpdateX = -10000;
    private static double lastUpdateZ = -10000;

    private XaeroGhostMapCompat() {}

    public static Identifier getGhostTexture() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || !NativeBridge.isReady()) return null;
        
        // Prevent loading ghost map if both features are disabled via config
        if (RustMC.CONFIG.getGhostMapMode() == com.alexxiconify.rustmc.config.RustMCConfig.GhostMapMode.NONE) {
            return null;
        }

        double px = player.getX();
        double pz = player.getZ();

        if (Math.abs(px - lastUpdateX) > 8 || Math.abs(pz - lastUpdateZ) > 8 || ghostTexture == null) {
            updateTexture(px, pz);
            lastUpdateX = px;
            lastUpdateZ = pz;
        }

        return GHOST_TEXTURE_ID;
    }

    private static void updateTexture(double centerX, double centerZ) {
        int size = 256;
        double scale = 1.0;
        
        // Ensure noise is seeded with the configured custom seed in multiplayer scenarios
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && !mc.isInSingleplayer()) {
            net.minecraft.client.network.ServerInfo serverInfo = mc.getCurrentServerEntry();
            if (serverInfo != null) {
                applyServerSeed(serverInfo.address);
            }
        }
        
        int[] pixels = NativeBridge.generateGhostMap(centerX, centerZ, size, scale);

        if (ghostTexture == null) {
            // Trying constructor with name which might be required in this mapping version
            ghostTexture = new NativeImageBackedTexture("rustmc_ghost_map", size, size, false);
            MinecraftClient.getInstance().getTextureManager().registerTexture(GHOST_TEXTURE_ID, ghostTexture);
        }

        NativeImage image = ghostTexture.getImage();
        if (image != null) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int p = pixels[y * size + x];
                    int a = (p >> 24) & 0xFF;
                    int r = (p >> 16) & 0xFF;
                    int g = (p >> 8) & 0xFF;
                    int b = p & 0xFF;
                    image.setColor(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                }
            }
            ghostTexture.upload();
        }
    }

    private static void applyServerSeed(String ip) {
        String configSeeds = RustMC.CONFIG.getCustomGhostMapSeed();
        if (configSeeds == null || configSeeds.isEmpty()) return;
        
        RustMC.LOGGER.info("[Rust-MC] Ghost Map checking seed for server IP: '{}' against config: '{}'", ip, configSeeds);
        
        for (String entry : configSeeds.split(",")) {
            if (tryApplySeedEntry(entry, ip)) {
                break;
            }
        }
    }

    private static boolean tryApplySeedEntry(String entry, String ip) {
        int splitObj = entry.lastIndexOf('=');
        if (splitObj == -1) splitObj = entry.lastIndexOf(':');
        
        if (splitObj > 0 && splitObj < entry.length() - 1) {
            String serverPart = entry.substring(0, splitObj).trim();
            String seedPart = entry.substring(splitObj + 1).trim();
            
            if (isIpMatch(ip, serverPart)) {
                RustMC.LOGGER.info("[Rust-MC] Found custom Ghost Map seed match! Applying seed: {}", seedPart);
                applySeedValue(seedPart);
                return true;
            }
        }
        return false;
    }

    private static boolean isIpMatch(String ip, String serverPart) {
        String trimmedIp = ip.trim();
        return trimmedIp.equalsIgnoreCase(serverPart) 
            || trimmedIp.contains(serverPart) 
            || serverPart.contains(trimmedIp);
    }

    private static void applySeedValue(String seedPart) {
        NativeBridge.noiseReset();
        try {
            NativeBridge.noiseInit(Long.parseLong(seedPart));
        } catch (NumberFormatException e) {
            NativeBridge.noiseInit(seedPart.hashCode());
        }
    }
}
