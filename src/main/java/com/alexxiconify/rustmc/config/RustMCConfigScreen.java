package com.alexxiconify.rustmc.config;

import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"java:S3776", "java:S3011", "java:S108"})
public class RustMCConfigScreen extends Screen {
    private final Screen parent;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 8;

    public RustMCConfigScreen(Screen parent) {
        super(Text.literal("Rust to MC Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.clearChildren();

        List<Field> validFields = new ArrayList<>();
        for (Field f : RustMCConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.getName().startsWith("mod") || f.getName().equals("nativeReady")) continue;
            validFields.add(f);
        }

        int maxPages = (int) Math.ceil((double) validFields.size() / ITEMS_PER_PAGE);
        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, validFields.size());

        int yOffset = 24;
        int startY = 40;

        for (int i = 0; startIdx + i < endIdx; i++) {
            Field field = validFields.get(startIdx + i);
            field.setAccessible(true);
            try {
                if (field.getType() == boolean.class) {
                    boolean val = field.getBoolean(RustMC.CONFIG);
                    String name = field.getName();
                    ButtonWidget btn = ButtonWidget.builder(Text.literal(name + ": " + (val ? "§aON" : "§cOFF")), button -> {
                        try {
                            boolean newVal = !field.getBoolean(RustMC.CONFIG);
                            field.set(RustMC.CONFIG, newVal);
                            button.setMessage(Text.literal(name + ": " + (newVal ? "§aON" : "§cOFF")));
                            RustMC.saveConfig();
                        } catch (Exception e) { /* ignore */ }
                    }).dimensions(this.width / 2 - 100, startY + (i * yOffset), 200, 20).build();
                    this.addDrawableChild(btn);
                } else if (field.getType() == int.class) {
                    int val = field.getInt(RustMC.CONFIG);
                    String name = field.getName();
                    ButtonWidget btn = ButtonWidget.builder(Text.literal(name + ": " + val), button -> {}).dimensions(this.width / 2 - 100, startY + (i * yOffset), 200, 20).build();
                    this.addDrawableChild(btn);
                    btn.active = false; // Disable int editing for now to keep UI simple
                }
            } catch (Exception e) { /* ignore */ }
        }

        int bottomY = this.height - 30;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), btn -> {
            if (page > 0) {
                page--;
                this.init();
            }
        }).dimensions(this.width / 2 - 100, bottomY, 48, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Exit"), btn -> this.close()).dimensions(this.width / 2 - 48, bottomY, 96, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), btn -> {
            if (page < maxPages - 1) {
                page++;
                this.init();
            }
        }).dimensions(this.width / 2 + 52, bottomY, 48, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
