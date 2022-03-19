package com.limitedchunks.mixin;

import com.limitedchunks.Utils.IChunkTicketManagerAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.server.world.ChunkTicketManager;
import net.minecraft.util.collection.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkTicketManager.class)
public abstract class ChunkTicketManagerAccessorMixin implements IChunkTicketManagerAccessor
{
    @Shadow
    @Final
    private Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> ticketsByPosition;

    @Shadow
    abstract void removeTicket(final long pos, final ChunkTicket<?> ticket);

    @Override
    public Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> getTicketsByPosition()
    {
        return ticketsByPosition;
    }

    @Override
    public void removeTicketAccessor(long pos, ChunkTicket<?> ticket)
    {
        removeTicket(pos, ticket);
    }
}
