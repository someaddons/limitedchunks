package com.limitedchunks.Utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.world.ChunkTicket;
import net.minecraft.util.collection.SortedArraySet;

public interface IChunkTicketManagerAccessor
{
    Long2ObjectOpenHashMap<SortedArraySet<ChunkTicket<?>>> getTicketsByPosition();

    void removeTicketAccessor(long pos, ChunkTicket<?> ticket);
}
