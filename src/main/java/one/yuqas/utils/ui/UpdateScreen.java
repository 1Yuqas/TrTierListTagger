package one.yuqas.utils.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import one.yuqas.utils.VersionChecker;
import net.fabricmc.loader.api.FabricLoader;

public class UpdateScreen extends Screen {
    private final Screen parent;

    public UpdateScreen(Screen parent) {
        super(Text.literal("Update Available"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Yeni Sürüm Yükle"), button -> {
            Util.getOperatingSystem().open("https://modrinth.com/mod/tgKll25i");
        }).dimensions(this.width / 2 - 105, this.height / 2 + 10, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Boşver"), button -> {
            this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 + 5, this.height / 2 + 10, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        String current = FabricLoader.getInstance().getModContainer("trtierlisttagger").get().getMetadata().getVersion().getFriendlyString();

        context.drawCenteredTextWithShadow(this.textRenderer, "TrTierListTagger Güncelleme!", this.width / 2, this.height / 2 - 50, 0xFF5555);
        context.drawCenteredTextWithShadow(this.textRenderer, "Sizin sürümünüz: " + current, this.width / 2, this.height / 2 - 30, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "Yeni sürüm: " + VersionChecker.latestVersion, this.width / 2, this.height / 2 - 15, 0x55FF55);
    }
}
