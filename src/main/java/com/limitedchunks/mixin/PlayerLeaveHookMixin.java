package com.limitedchunks.mixin;

import com.limitedchunks.event.EventHandler;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class PlayerLeaveHookMixin
{
    @Inject(method = "remove", at = @At("HEAD"))
    private void hashCode(final ServerPlayerEntity player, final CallbackInfo ci)
    {
        EventHandler.onPlayerLeave(player);
    }
}
