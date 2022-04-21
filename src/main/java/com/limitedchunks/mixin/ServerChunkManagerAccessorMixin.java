package com.limitedchunks.mixin;

import com.limitedchunks.Utils.ITicketManagerGetter;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerChunkManager.class)
public class ServerChunkManagerAccessorMixin implements ITicketManagerGetter
{
    @Shadow
    @Final
    private ChunkTicketManager ticketManager;

    @Override
    public ChunkTicketManager getTicketManager()
    {
        return ticketManager;
    }
}
