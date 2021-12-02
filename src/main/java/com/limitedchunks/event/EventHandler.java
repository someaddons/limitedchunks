package com.limitedchunks.event;

import com.limitedchunks.LimitedChunks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fmllegacy.server.ServerLifecycleHooks;
import net.minecraftforge.fmlserverevents.FMLServerAboutToStartEvent;

import java.util.*;

/**
 * Handler to catch server tick events
 */
public class EventHandler
{
    /**
     * Contains the positions that are/will be loaded by a player
     */
    static Map<ResourceKey<Level>, Long2ObjectOpenHashMap<UUID>> playerIDByPos = new HashMap<>();
    static Map<ResourceKey<Level>, HashMap<UUID, Set<Long>>>     posByPlayerID = new HashMap<>();

    // Pos to time to unload at
    static Map<ResourceKey<Level>, Queue<ChunkPosAndTime>> unloadQue = new HashMap<>();

    @SubscribeEvent
    public static void onWorldTick(final TickEvent.WorldTickEvent event)
    {
        if (event.world.isClientSide() || event.phase == TickEvent.Phase.START)
        {
            return;
        }

        final ServerLevel world = (ServerLevel) event.world;

        Queue<ChunkPosAndTime> queue = unloadQue.get(world.dimension());
        if (queue == null || queue.isEmpty())
        {
            return;
        }


        final ChunkPosAndTime current = queue.peek();
        if (current != null && current.time < world.getServer().getNextTickTime())
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
    private static void checkLoadedAndClear(final long pos, final ServerLevel world)
    {
        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = playerIDByPos.get(world.dimension());
        if (!(worldPositionsLoaded != null && worldPositionsLoaded.containsKey(pos)))
        {
            final SortedArraySet<Ticket<?>> ticketsE = world.getChunkSource().distanceManager.tickets.get(pos);
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
                    world.getChunkSource().distanceManager.removeTicket(pos, ticket);
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
        if (event.getWorld().isClientSide())
        {
            return;
        }

        final ServerLevel world = (ServerLevel) event.getWorld();
        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = playerIDByPos.get(world.dimension());
        final long pos = event.getChunk().getPos().toLong();
        if (worldPositionsLoaded != null && !worldPositionsLoaded.containsKey(pos))
        {
            // Que chunk to unload/check on later
            final Queue<ChunkPosAndTime> quedChunks = unloadQue.computeIfAbsent(world.dimension(), s -> new PriorityQueue<>());
            quedChunks.add(new ChunkPosAndTime(pos, world.getServer().getNextTickTime() + LimitedChunks.getConfig().getCommonConfig().chunkunloadnoplayer.get() * 1000 * 60));
        }
    }

    @SubscribeEvent
    public static void onChunkUnLoad(final ChunkEvent.Unload event)
    {
        if (event.getWorld().isClientSide())
        {
            return;
        }

        final ServerLevel world = (ServerLevel) event.getWorld();
        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = playerIDByPos.get(world.dimension());
        final HashMap<UUID, Set<Long>> posByPlayerIDMap = posByPlayerID.get(world.dimension());
        final long pos = event.getChunk().getPos().toLong();
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
        if (event.getEntity().level.isClientSide())
        {
            return;
        }

        final ServerLevel world = (ServerLevel) event.getPlayer().level;
        if (posByPlayerID.get(world.dimension()) == null)
        {
            return;
        }

        final Set<Long> chunksFromPlayer = posByPlayerID.get(world.dimension()).get(event.getPlayer().getUUID());
        if (chunksFromPlayer == null)
        {
            return;
        }

        final Long2ObjectOpenHashMap<UUID> worldPositionsLoaded = playerIDByPos.get(world.dimension());
        final Queue<ChunkPosAndTime> quedChunks = unloadQue.computeIfAbsent(world.dimension(), s -> new PriorityQueue<>());
        for (final long pos : chunksFromPlayer)
        {
            if (worldPositionsLoaded != null)
            {
                if (worldPositionsLoaded.remove(pos, event.getPlayer().getUUID()))
                {
                    quedChunks.add(new ChunkPosAndTime(pos,
                      world.getServer().getNextTickTime() + LimitedChunks.getConfig().getCommonConfig().chunkunloadnoplayer.get() * 1000 * 60));
                }
            }
        }

        posByPlayerID.get(world.dimension()).remove(event.getPlayer().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerEnter(final PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity().level.isClientSide() || !(event.getEntity() instanceof Player))
        {
            return;
        }

        final int viewDist = ServerLifecycleHooks.getCurrentServer().getPlayerList().getViewDistance();

        // Same chunk
        int playerX = event.getPlayer().chunkPosition().x;
        int playerZ = event.getPlayer().chunkPosition().z;

        int chunkXStart = playerX - viewDist;
        int chunkZStart = playerZ - viewDist;
        final int maxChunkX = playerX + viewDist;
        final int maxChunkZ = playerZ + viewDist;

        addChunksFromTo(chunkXStart, chunkZStart, maxChunkX, maxChunkZ, (ServerLevel) event.getEntity().level, event.getPlayer());
    }

    private final static Map<UUID, ChunkPos> playerPos = new HashMap<>();

    @SubscribeEvent
    public static void onChunkEnter(final TickEvent.PlayerTickEvent event)
    {
        if (event.player.level.isClientSide())
        {
            return;
        }

        if (event.player.level.getGameTime() % 100 != 0)
        {
            return;
        }

        ChunkPos oldChunk = playerPos.get(event.player.getUUID());
        if (oldChunk == null)
        {
            playerPos.put(event.player.getUUID(), event.player.chunkPosition());
            // TODO: Remove chunks outside the previous radius from watch, que to unload
            oldChunk = event.player.chunkPosition();
        }

        final boolean xChanged = oldChunk.x != event.player.getBlockX() >> 4;
        final boolean zChanged = oldChunk.z != event.player.getBlockZ() >> 4;

        if (!xChanged && !zChanged)
        {
            return;
        }

        final ChunkPos newChunk = event.player.chunkPosition();
        playerPos.put(event.player.getUUID(), newChunk);

        final int viewDist = ServerLifecycleHooks.getCurrentServer().getPlayerList().getViewDistance();
        // new chunk Bsp -> X: -16 -> X: -15 -> -15 + view - (-15--16)

        int xMaxViewDist = viewDist + (xChanged ? 1 : 0);
        int zMaxViewDist = viewDist + (zChanged ? 1 : 0);

        int chunkXStart = newChunk.x + (zChanged ? -viewDist : viewDist);
        int chunkZStart = newChunk.z + (xChanged ? -viewDist : viewDist);
        final int maxChunkX = newChunk.x + xMaxViewDist;
        final int maxChunkZ = newChunk.z + zMaxViewDist;

        addChunksFromTo(chunkXStart, chunkZStart, maxChunkX, maxChunkZ, (ServerLevel) event.player.level, event.player);
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
    private static void addChunksFromTo(final int xFrom, final int zFrom, final int xTo, final int zTo, final ServerLevel world, final Player playerEntity)
    {
        final Long2ObjectOpenHashMap<UUID> positionsToLoad = playerIDByPos.computeIfAbsent(world.dimension(), e -> new Long2ObjectOpenHashMap<>());
        final HashMap<UUID, Set<Long>> playerPositions = posByPlayerID.computeIfAbsent(world.dimension(), e -> new HashMap<>());

        for (int x = xFrom; x <= xTo; x++)
        {
            for (int z = zFrom; z <= zTo; z++)
            {
                final long currentPos = ChunkPos.asLong(x, z);
                positionsToLoad.put(currentPos, playerEntity.getUUID());
                playerPositions.computeIfAbsent(playerEntity.getUUID(), s -> new HashSet<>()).add(currentPos);
            }
        }
    }

    /**
     * Reset all values
     */
    @SubscribeEvent
    public static void onServerAboutToStart(final FMLServerAboutToStartEvent event)
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
