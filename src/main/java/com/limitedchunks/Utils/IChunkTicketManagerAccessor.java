package com.limitedchunks.Utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;

public interface IChunkTicketManagerAccessor
{
    Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> limitedchunks$getTicketsByPosition();

    void limitedchunks$removeTicketAccessor(long pos, Ticket<?> ticket);
}
