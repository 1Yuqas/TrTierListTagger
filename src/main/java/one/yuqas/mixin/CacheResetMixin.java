package one.yuqas.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import one.yuqas.utils.APIUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientConnection.class)
public class CacheResetMixin {
    @Inject(method = "disconnect", at = @At("HEAD"))
    private void disconnect(Text reason, CallbackInfo ci) {
        APIUtils.clearCache();
    }
}
