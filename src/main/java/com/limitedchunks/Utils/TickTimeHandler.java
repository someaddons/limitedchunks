package com.limitedchunks.Utils;

import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class TickTimeHandler
{
    private int meanTickSum   = 50;
    private int meanTickCount = 1;
    private int tickTimer     = 0;

    public static int serverTickTimerInterval = 100;

    private static TickTimeHandler instance = new TickTimeHandler();

    public static TickTimeHandler getInstance()
    {
        return instance;
    }

    private TickTimeHandler()
    {
        // Intentionally empty
    }

    /**
     * Averages values each 20 ticks
     */
    public void onServerTick()
    {
        tickTimer++;

        if (tickTimer % 20 == 0)
        {
            meanTickSum += average(ServerLifecycleHooks.getCurrentServer().tickTimeArray) * 1.0E-6D;
            meanTickCount++;

            if (tickTimer >= serverTickTimerInterval)
            {
                serverMeanTickTime = meanTickSum / meanTickCount;
                tickTimer = 0;
                meanTickCount = 0;
                meanTickSum = 0;

            }
        }
    }

    /**
     * Averages the arrays values.
     *
     * @param values
     * @return
     */
    private static long average(long[] values)
    {
        if (values == null || values.length == 0)
        {
            return 0L;
        }

        long sum = 0L;
        for (long v : values)
        {
            sum += v;
        }
        return sum / values.length;
    }

    private int serverMeanTickTime = 50;

    /**
     * Returns the mean tick time
     *
     * @return avg tick time
     */
    public int getMeanTickTime()
    {
        return serverMeanTickTime;
    }
}
