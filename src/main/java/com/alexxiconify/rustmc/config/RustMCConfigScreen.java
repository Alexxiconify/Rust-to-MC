package com.alexxiconify.rustmc.config;

import com.alexxiconify.rustmc.RustMC;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
        addFieldButtons(validFields, startIdx, endIdx);
        addNavButtons(maxPages);
    }

    @SuppressWarnings("java:S3011")
    private void addFieldButtons(List<Field> fields, int startIdx, int endIdx) {
        int yOffset = 24;
        int startY = 40;
        for (int i = 0; startIdx + i < endIdx; i++) {
            Field field = fields.get(startIdx + i);
            field.setAccessible(true);
            try {
                addFieldButton(field, startY + (i * yOffset));
            } catch (Exception e) { /* ignore */ }
        }
    }

    @SuppressWarnings("java:S3011")
    private void addFieldButton(Field field, int y) throws ReflectiveOperationException {
        String name = field.getName();
        if (field.getType() == boolean.class) {
            boolean val = field.getBoolean(RustMC.CONFIG); // NOSONAR
            ButtonWidget btn = ButtonWidget.builder(Text.literal(name + ": " + (val ? "§aON" : "§cOFF")), button -> {
                try {
                    boolean newVal = !field.getBoolean(RustMC.CONFIG);
                    field.set(RustMC.CONFIG, newVal); // NOSONAR
                    button.setMessage(Text.literal(name + ": " + (newVal ? "§aON" : "§cOFF")));
                    RustMC.saveConfig();
                } catch (Exception e) { /* ignore */ }
            }).dimensions(this.width / 2 - 100, y, 200, 20).build();
            this.addDrawableChild(btn);
        } else if (field.getType() == int.class) {
            ButtonWidget btn = ButtonWidget.builder(Text.literal(name + ": " + field.getInt(RustMC.CONFIG)), button -> {})
                    .dimensions(this.width / 2 - 100, y, 200, 20).build();
            btn.active = false;
            this.addDrawableChild(btn);
        }
    }

    private void addNavButtons(int maxPages) {
        int bottomY = this.height - 30;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("< Prev"), btn -> {
            if (page > 0) { page--; this.init(); }
        }).dimensions(this.width / 2 - 100, bottomY, 48, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Exit"), btn -> this.close()).dimensions(this.width / 2 - 48, bottomY, 96, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Next >"), btn -> {
            if (page < maxPages - 1) { page++; this.init(); }
        }).dimensions(this.width / 2 + 52, bottomY, 48, 20).build());
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
