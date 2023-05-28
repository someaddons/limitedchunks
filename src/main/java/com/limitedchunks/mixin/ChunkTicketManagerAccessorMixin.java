package com.limitedchunks.mixin;

import com.limitedchunks.Utils.IChunkTicketManagerAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DistanceManager.class)
public abstract class ChunkTicketManagerAccessorMixin implements IChunkTicketManagerAccessor
{
    @Shadow
    @Final
    private Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets;

    @Shadow
    abstract void removeTicket(final long pos, final Ticket<?> ticket);

    @Override
    public Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> limitedchunks$getTicketsByPosition()
    {
        return tickets;
    }

    @Override
    public void limitedchunks$removeTicketAccessor(long pos, Ticket<?> ticket)
    {
        removeTicket(pos, ticket);
    }
}
