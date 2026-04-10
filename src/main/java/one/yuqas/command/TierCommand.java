package one.yuqas.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import one.yuqas.utils.ui.PlayerSearchScreen;
import one.yuqas.utils.ui.TierConfigScreen;

public class TierCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        Minecraft minecraft = Minecraft.getInstance();

        dispatcher.register(
                ClientCommands.literal("trtiertagger")
                        .executes(context -> {
                            minecraft.execute(() -> minecraft.setScreen(new TierConfigScreen(null)));
                            return 1;
                        })
                        .then(ClientCommands.argument("player", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    var connection = minecraft.getConnection();
                                    if (connection != null) {
                                        return SharedSuggestionProvider.suggest(
                                                connection.getListedOnlinePlayers().stream()
                                                        .map(entry -> entry.getProfile().name()),
                                                builder
                                        );
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String player = StringArgumentType.getString(context, "player");
                                    minecraft.execute(() ->
                                            minecraft.setScreen(new PlayerSearchScreen(
                                                    minecraft.screen,
                                                    player
                                            ))
                                    );
                                    return 1;
                                }))
        );
    }
}