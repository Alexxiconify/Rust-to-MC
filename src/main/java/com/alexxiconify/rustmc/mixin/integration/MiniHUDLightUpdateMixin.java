package com.alexxiconify.rustmc.mixin.integration;
import com.alexxiconify.rustmc.ModBridge;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
//
 //  Notifies MiniHUD and Lighty that server-side light data is stale.
 //  Reflection is cached once via ensureInit().
//noinspection MixinClassReference
@Mixin(ClientChunkManager.class)
public class MiniHUDLightUpdateMixin {
    @Unique
    private static MethodHandle minihudSetNeedsUpdate = null;
    @Unique
    private static Object minihudInstance = null;
    @Unique
    private static MethodHandle lightyInvalidate = null;
    @Unique
    private static boolean initDone = false;
    @Unique
    private static long lastUpdateTime = 0;
    @Unique
    private static void tryBindMiniHUD() {
        if (!ModBridge.MINIHUD) return;
        try {
            Class<?> cls = Class.forName("fi.dy.masa.minihud.renderer.OverlayRendererLightLevel");
            Object inst = cls.getDeclaredField("INSTANCE").get(null);
            if (inst != null) {
                Method method = cls.getMethod("setNeedsUpdate");
                minihudSetNeedsUpdate = MethodHandles.lookup().unreflect(method);
                minihudInstance = inst;
            }
        } catch (Exception | LinkageError ignored) { // MiniHUD absent or incompatible
        }
    }
    @Unique
    private static void tryBindLighty() {
        if (!ModBridge.LIGHTY) return;
        try {
            Class<?> compute = Class.forName("dev.schmarrn.lighty.core.Compute");
            Method method = probeMethod(compute );
            if (method != null) {
                lightyInvalidate = MethodHandles.lookup().unreflect(method);
            }
        } catch (Exception | LinkageError ignored) { // Lighty absent or incompatible
        }
    }
    //Returns the first method found by name, or null if none match. // /
    @Unique
    private static Method probeMethod( Class<?> cls ) {
        for (String name : new String[] { "requestRecompute" , "markDirty" , "clear" } ) {
            try {
                return cls.getMethod(name);
            } catch (NoSuchMethodException ignored) { // try next candidate
            }
        }
        return null;
    }
    @Unique
    private static void ensureInit() {
        if (initDone) return;
        initDone = true;
        tryBindMiniHUD();
        tryBindLighty();
    }
    @Inject(method = "onLightUpdate", at = @At("TAIL"))
    @SuppressWarnings("java:S2696") // Mixin @Inject has to be instance method while updating static dispatch timestamp.
    private void rustmcOnLightUpdate(LightType type, ChunkSectionPos pos, CallbackInfo ci) {
        ensureInit();
        // Use a fast time-based rate limit of min 16ms between dispatches
        // to avoid freezing the lighting thread during massive batch updates.
        long now = System.currentTimeMillis();
        if (now - lastUpdateTime < 16) return;
        lastUpdateTime = now;
        if (minihudSetNeedsUpdate != null) {
            try { minihudSetNeedsUpdate.invoke(minihudInstance); }
            catch (Throwable ignored) { // MiniHUD instance gone
            }
        }
        if (lightyInvalidate != null) {
            try { lightyInvalidate.invoke(); }
            catch (Throwable ignored) { // Lighty static call failed
            }
        }
    }
}