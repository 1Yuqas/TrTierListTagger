package one.yuqas;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import one.yuqas.command.TierCommand;
import one.yuqas.utils.TierConfigUtil;
import one.yuqas.utils.VersionChecker;

public class TrTierListTagger implements ModInitializer {

    @Override
    public void onInitialize() {
        TierConfigUtil.load();

        VersionChecker.check();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            TierCommand.register(dispatcher);
        });
    }
    public static String getId() {
        return "trtierlisttagger";
    }

}
