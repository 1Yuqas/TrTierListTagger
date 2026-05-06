package one.yuqas;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import one.yuqas.command.TierCommand;
import one.yuqas.utils.TierConfigUtil;
import one.yuqas.utils.VersionChecker;
import one.yuqas.utils.ui.UpdateScreen;

public class TrTierListTagger implements ModInitializer {
    private boolean hasShownUpdate = false;

    @Override
    public void onInitialize() {
        TierConfigUtil.load();

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
