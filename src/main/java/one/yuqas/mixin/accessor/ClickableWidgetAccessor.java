package one.yuqas.mixin.accessor;

import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClickableWidget.class)
public interface ClickableWidgetAccessor {
    @Accessor("x")
    int getX();

    @Accessor("y")
    int getY();

    @Accessor("width")
    int getWidth();

    @Accessor("height") 
    int getHeight();
}