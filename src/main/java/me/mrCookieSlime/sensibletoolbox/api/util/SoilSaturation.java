package me.mrCookieSlime.sensibletoolbox.api.util;

import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;

import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

/**
 * Manage the "saturation" level of a block for the purposes of STB
 * farming tools.
 */
public class SoilSaturation {
    private static final String LAST_WATERED = "STB_LastWatered";
    private static final String SATURATION = "STB_Saturation";

    /**
     * The maximum saturation level for a soil block before it turns to water.
     */
    public static final int MAX_SATURATION = 100;

    /**
     * Get the time that this block was last watered.  The time will have been
     * returned by {@link System#currentTimeMillis()}.
     *
     * @param b the block to test
     * @return the time that the block was last watered
     */
    public static long getLastWatered(Block b) {
        for (MetadataValue v : b.getMetadata(LAST_WATERED)) {
            if (v.getOwningPlugin() == SensibleToolboxPlugin.getInstance()) {
                return v.asLong();
            }
        }
        return 0;
    }

    /**
     * Update the time that the block was last watered.  The time will have been
     * returned by {@link System#currentTimeMillis()}.
     *
     * @param b the block to update
     * @param lastWatered the time that the block was last watered
     */
    public static void setLastWatered(Block b, long lastWatered) {
        b.setMetadata(LAST_WATERED, new FixedMetadataValue(SensibleToolboxPlugin.getInstance(), lastWatered));
    }

    /**
     * Get the current saturation level for the given block.
     *
     * @param b the block to test
     * @return the block's current saturation level
     */
    public static int getSaturationLevel(Block b) {
        for (MetadataValue v : b.getMetadata(SATURATION)) {
            if (v.getOwningPlugin() == SensibleToolboxPlugin.getInstance()) {
                return v.asInt();
            }
        }
        return 0;
    }

    /**
     * Set the block's current saturation level.
     *
     * @param b the block to update
     * @param saturationLevel the new saturation level
     */
    public static void setSaturationLevel(Block b, int saturationLevel) {
        b.setMetadata(SATURATION, new FixedMetadataValue(SensibleToolboxPlugin.getInstance(), saturationLevel));
    }

    /**
     * Clear all saturation & watering data for the given block.
     *
     * @param b the block to update
     */
    public static void clear(Block b) {
        b.removeMetadata(LAST_WATERED, SensibleToolboxPlugin.getInstance());
        b.removeMetadata(SATURATION, SensibleToolboxPlugin.getInstance());
    }
}
