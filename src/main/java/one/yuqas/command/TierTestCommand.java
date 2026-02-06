package one.yuqas.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import one.yuqas.utils.enums.TierType;
import one.yuqas.utils.ui.TierConfigScreen;

public class TierTestCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        MinecraftClient client = MinecraftClient.getInstance();

        dispatcher.register(
                ClientCommandManager.literal("tiertagger")
                        .executes(context -> {
                            client.send(() -> client.setScreen(new TierConfigScreen(null)));
                            return 1;
                        })
                        .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
                                    if (networkHandler != null) {
                                        return net.minecraft.command.CommandSource.suggestMatching(
                                                networkHandler.getPlayerList().stream()
                                                        .map(entry -> entry.getProfile().name()),
                                                builder
                                        );
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
//                                    client.send(() -> client.setScreen(new OpenPlayerInfoGUI()));  ekky burayı yapcan
                                    return 1;
                                }))

        );
    }
}