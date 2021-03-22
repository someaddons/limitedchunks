package com.limitedchunks.event;

import com.limitedchunks.LimitedChunks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Handler to catch server tick events
 */
public class EventHandler
{
    @SubscribeEvent
    public static void onWorldTick(final TickEvent.WorldTickEvent event)
    {
        if (event.world.isRemote || event.phase == TickEvent.Phase.START)
        {
            return;
        }

        final ServerWorld world = (ServerWorld) event.world;

        Queue<ChunkPosAndTime> queue = unloadQue.get(world.getDimensionKey());
        if (queue == null || queue.isEmpty())
        {
            return;
        }


        final ChunkPosAndTime current = queue.peek();
        if (current != null && current.time < world.getServer().getServerTime())
        {
            queue.poll();
            checkLoadedAndClear(current.pos, world);
        }
    }

    /**
     * Checks if a player is keeping the chunk active, unloads otherwise
     *
     * @param pos
     * @param world
     */
    private static void checkLoadedAndClear(final long pos, final ServerWorld world)
    {
        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = playerIDByPos.get(world.getDimensionKey());
        if (!(worldPositionsLoaded != null && worldPositionsLoaded.containsKey(pos)))
        {
            final SortedArraySet<Ticket<?>> ticketsE = world.getChunkProvider().ticketManager.tickets.get(pos);
            if (ticketsE == null)
            {
                return;
            }

            final ArrayList<Ticket<?>> tickets = new ArrayList<>(ticketsE);
            for (final Ticket<?> ticket : tickets)
            {
                if (ticket == null)
                {
                    continue;
                }

                if (!excludedTickets.contains(ticket.getType().toString()))
                {
                    if (LimitedChunks.getConfig().getCommonConfig().debugLog.get())
                    {
                        LimitedChunks.LOGGER.info("Unloading ticket:" + ticket.getType().toString() + " at chunkpos:" + new ChunkPos(pos));
                    }
                    world.getChunkProvider().ticketManager.release(pos, ticket);
                }
            }
        }
    }

    public static Set<String> excludedTickets = new HashSet<>();

    public static void initDefaultExcludes()
    {
        excludedTickets = new HashSet<>();
        excludedTickets.add(TicketType.POST_TELEPORT.toString());
        excludedTickets.add(TicketType.PLAYER.toString());
        excludedTickets.add(TicketType.START.toString());
        excludedTickets.add(TicketType.UNKNOWN.toString());
        excludedTickets.add(TicketType.PORTAL.toString());
    }

    @SubscribeEvent
    public static void onChunkLoad(final ChunkEvent.Load event)
    {
        if (event.getWorld().isRemote())
        {
            return;
        }

        final ServerWorld world = (ServerWorld) event.getWorld();
        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = playerIDByPos.get(world.getDimensionKey());
        final long pos = event.getChunk().getPos().asLong();
        if (worldPositionsLoaded != null && !worldPositionsLoaded.containsKey(pos))
        {
            // Que chunk to unload/check on later
            final Queue<ChunkPosAndTime> quedChunks = unloadQue.computeIfAbsent(world.getDimensionKey(), s -> new PriorityQueue<>());
            quedChunks.add(new ChunkPosAndTime(pos, world.getServer().getServerTime() + LimitedChunks.getConfig().getCommonConfig().chunkunloadnoplayer.get() * 1000 * 60));
        }
    }

    @SubscribeEvent
    public static void onChunkUnLoad(final ChunkEvent.Unload event)
    {
        if (event.getWorld().isRemote())
        {
            return;
        }

        final ServerWorld world = (ServerWorld) event.getWorld();
        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = playerIDByPos.get(world.getDimensionKey());
        final HashMap<UUID, Set<Long>> posByPlayerIDMap = posByPlayerID.get(world.getDimensionKey());
        final long pos = event.getChunk().getPos().asLong();
        if (worldPositionsLoaded != null && worldPositionsLoaded.containsKey(pos))
        {
            final UUID player = worldPositionsLoaded.remove(pos);
            // Clear keep alive values of players, to not have to much data
            if (player != null)
            {
                // Issue: there might be multiple players having the chunk saved here -> doesnt matter much as long as they're removed on logoff
                Set<Long> positions = posByPlayerIDMap.get(player);
                if (positions != null)
                {
                    positions.remove(pos);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(final PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity().world.isRemote())
        {
            return;
        }

        final ServerWorld world = (ServerWorld) event.getPlayer().world;
        if (posByPlayerID.get(world.getDimensionKey()) == null)
        {
            return;
        }

        final Set<Long> chunksFromPlayer = posByPlayerID.get(world.getDimensionKey()).get(event.getPlayer().getUniqueID());
        if (chunksFromPlayer == null)
        {
            return;
        }

        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = playerIDByPos.get(world.getDimensionKey());
        final Queue<ChunkPosAndTime> quedChunks = unloadQue.computeIfAbsent(world.getDimensionKey(), s -> new PriorityQueue<>());
        for (final long pos : chunksFromPlayer)
        {
            if (worldPositionsLoaded != null)
            {
                if (worldPositionsLoaded.remove(pos, event.getPlayer().getUniqueID()))
                {
                    quedChunks.add(new ChunkPosAndTime(pos, world.getServer().getServerTime() + LimitedChunks.getConfig().getCommonConfig().chunkunloadnoplayer.get() * 1000 * 60));
                }
            }
        }

        posByPlayerID.get(world.getDimensionKey()).remove(event.getPlayer().getUniqueID());
    }

    /**
     * Contains the positions that are/will be loaded by a player
     */
    static Map<RegistryKey<World>, Long2ObjectOpenHashMap<UUID>> playerIDByPos = new HashMap<>();
    static Map<RegistryKey<World>, HashMap<UUID, Set<Long>>>     posByPlayerID = new HashMap<>();

    // Pos to time to unload at
    static Map<RegistryKey<World>, Queue<ChunkPosAndTime>> unloadQue = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerEnter(final PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity().world.isRemote() || !(event.getEntity() instanceof PlayerEntity))
        {
            return;
        }

        final int viewDist = ServerLifecycleHooks.getCurrentServer().getPlayerList().getViewDistance();

        // Same chunk
        int chunkXStart = event.getPlayer().chunkCoordX - viewDist;
        int chunkZStart = event.getPlayer().chunkCoordZ - viewDist;
        final int maxChunkX = event.getPlayer().chunkCoordX + viewDist;
        final int maxChunkZ = event.getPlayer().chunkCoordZ + viewDist;

        addChunksFromTo(chunkXStart, chunkZStart, maxChunkX, maxChunkZ, (ServerWorld) event.getEntity().world, event.getPlayer());
    }

    @SubscribeEvent
    public static void onChunkEnter(final PlayerEvent.EnteringChunk event)
    {
        if (event.getEntity().world.isRemote() || !(event.getEntity() instanceof PlayerEntity))
        {
            return;
        }

        final boolean xChanged = event.getNewChunkX() != event.getOldChunkX();
        final boolean zChanged = event.getNewChunkZ() != event.getOldChunkZ();

        final int viewDist = ServerLifecycleHooks.getCurrentServer().getPlayerList().getViewDistance();
        // new chunk Bsp -> X: -16 -> X: -15 -> -15 + view - (-15--16)

        int xMaxViewDist = viewDist + (xChanged ? 1 : 0);
        int zMaxViewDist = viewDist + (zChanged ? 1 : 0);

        int chunkXStart = event.getNewChunkX() + (zChanged ? -viewDist : viewDist);
        int chunkZStart = event.getNewChunkZ() + (xChanged ? -viewDist : viewDist);
        final int maxChunkX = event.getNewChunkX() + xMaxViewDist;
        final int maxChunkZ = event.getNewChunkZ() + zMaxViewDist;

        addChunksFromTo(chunkXStart, chunkZStart, maxChunkX, maxChunkZ, (ServerWorld) event.getEntity().world, (PlayerEntity) event.getEntity());
    }

    /**
     * Adds chunk positions in the given box
     *
     * @param xFrom
     * @param zFrom
     * @param xTo
     * @param zTo
     * @param world
     * @param playerEntity
     */
    private static void addChunksFromTo(final int xFrom, final int zFrom, final int xTo, final int zTo, final ServerWorld world, final PlayerEntity playerEntity)
    {
        final Long2ObjectOpenHashMap<UUID> positionsToLoad = playerIDByPos.computeIfAbsent(world.getDimensionKey(), e -> new Long2ObjectOpenHashMap<>());
        final HashMap<UUID, Set<Long>> playerPositions = posByPlayerID.computeIfAbsent(world.getDimensionKey(), e -> new HashMap<>());

        for (int x = xFrom; x <= xTo; x++)
        {
            for (int z = zFrom; z <= zTo; z++)
            {
                final long currentPos = ChunkPos.asLong(x, z);
                positionsToLoad.put(currentPos, playerEntity.getUniqueID());
                playerPositions.computeIfAbsent(playerEntity.getUniqueID(), s -> new HashSet<>()).add(currentPos);
            }
        }
    }

    /**
     * Reset all values
     */
    @SubscribeEvent
    public static void onServerAboutToStart(@NotNull final FMLServerAboutToStartEvent event)
    {
        playerIDByPos = new HashMap<>();
        posByPlayerID = new HashMap<>();
        unloadQue = new HashMap<>();
    }

    /**
     * Data holder class
     */
    private static class ChunkPosAndTime implements Comparable<ChunkPosAndTime>
    {
        final long pos;
        final long time;

        ChunkPosAndTime(final long pos, final long time)
        {
            this.pos = pos;
            this.time = time;
        }

        @Override
        public int compareTo(final ChunkPosAndTime o)
        {
            return (int) (time - o.time);
        }
    }
}
