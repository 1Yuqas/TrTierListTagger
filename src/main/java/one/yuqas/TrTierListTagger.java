package one.yuqas;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import one.yuqas.command.TierCommand;
import one.yuqas.utils.TierConfig;

public class TrTierListTagger implements ModInitializer {

    @Override
    public void onInitialize() {
        TierConfig.load();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            TierCommand.register(dispatcher);
        });
    }
    public static String getId() {
        return "trtierlisttagger";
    }

}
