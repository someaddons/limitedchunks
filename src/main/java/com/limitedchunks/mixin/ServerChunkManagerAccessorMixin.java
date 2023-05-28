package com.limitedchunks.mixin;

import com.limitedchunks.Utils.ITicketManagerGetter;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerChunkCache.class)
public class ServerChunkManagerAccessorMixin implements ITicketManagerGetter
{
    @Shadow
    @Final
    private DistanceManager distanceManager;

    @Override
    public DistanceManager getTicketManager()
    {
        return distanceManager;
    }
}
