package one.yuqas.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import one.yuqas.utils.APIUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class CacheResetMixin {

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(Component reason, CallbackInfo ci) {
        APIUtils.clearCache();
    }
}