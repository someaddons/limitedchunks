package com.limitedchunks.mixin;

import com.limitedchunks.event.EventHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerLeaveHookMixin
{
    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerRemove(final ServerPlayer player, final CallbackInfo ci)
    {
        EventHandler.onPlayerLeave(player);
    }
}
