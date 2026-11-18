package one.yuqas;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import one.yuqas.command.TierCommand;
import one.yuqas.utils.TelemetryManager;
import one.yuqas.utils.TierConfigUtil;
import one.yuqas.utils.VersionChecker;
import one.yuqas.utils.ui.UpdateScreen;

public class TrTierListTagger implements ModInitializer, ClientModInitializer {
    private boolean hasShownUpdate = false;
    private static boolean initialized = false;

    @Override
    public void onInitialize() {
        initMod();
    }

    @Override
    public void onInitializeClient() {
        initMod();
    }

    private synchronized void initMod() {
        if (initialized) return;
        initialized = true;

        TierConfigUtil.load();
        TelemetryManager.start();

        new Thread(VersionChecker::check).start();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (VersionChecker.updateAvailable && !hasShownUpdate && client.currentScreen instanceof TitleScreen) {
                client.setScreen(new UpdateScreen(client.currentScreen));
                hasShownUpdate = true;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            TierCommand.register(dispatcher);
        });
    }

    public static String getId() {
        return "trtierlisttagger";
    }
}
