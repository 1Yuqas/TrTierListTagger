package one.yuqas.utils.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import one.yuqas.utils.VersionChecker;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.*;

public class UpdateScreen extends Screen {
    private final Screen parent;

    public UpdateScreen(Screen parent) {
        super(Component.literal("Update Available"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(Component.literal("Yeni Sürüm Yükle"), button -> {
            Util.getPlatform().openUri("https://modrinth.com/mod/tgKll25i");
        }).bounds(this.width / 2 - 105, this.height / 2 + 10, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Boşver"), button -> {
            this.minecraft.gui.setScreen(this.parent);
        }).bounds(this.width / 2 + 5, this.height / 2 + 10, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
//        this.extractBackground(context, mouseX, mouseY, delta);
        super.extractRenderState(context, mouseX, mouseY, delta);

        String current = FabricLoader.getInstance().getModContainer("trtierlisttagger").get().getMetadata().getVersion().getFriendlyString();

        context.centeredText(this.font, "TrTierListTagger Güncelleme!", this.width / 2, this.height / 2 - 50, new Color(0xFF5555).getRGB());
        context.centeredText(this.font, "Sizin sürümünüz: " + current, this.width / 2, this.height / 2 - 30, new Color (0xFFFFFF).getRGB());
        context.centeredText(this.font, "Yeni sürüm: " + VersionChecker.latestVersion, this.width / 2, this.height / 2 - 15, new Color(0x55FF55).getRGB());
    }
}
