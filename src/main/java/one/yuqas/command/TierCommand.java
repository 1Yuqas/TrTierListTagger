package one.yuqas.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import one.yuqas.utils.ui.PlayerSearchScreen;
import one.yuqas.utils.ui.TierConfigScreen;

public class TierCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        MinecraftClient client = MinecraftClient.getInstance();

        dispatcher.register(
                ClientCommandManager.literal("trtiertagger")
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
                                    String player = StringArgumentType.getString(context, "player");
                                    client.send(() ->
                                            client.setScreen(new PlayerSearchScreen(
                                                    client.currentScreen,
                                                    player
                                            ))
                                    );
                                    return 1;
                                }))

        );
    }
}