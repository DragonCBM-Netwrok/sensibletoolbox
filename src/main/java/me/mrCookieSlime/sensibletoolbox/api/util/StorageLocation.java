package me.mrCookieSlime.sensibletoolbox.api.util;

import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.blocks.machines.BigStorageUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Created by Daniel on 4/23/2017.
 */
public class StorageLocation {
    private ItemStack item;
    private int x, y, z;
    private String world;
    public StorageLocation(Location targetLoc)
    {
        setLocation(targetLoc);
        item = null;
        BaseSTBBlock stb = SensibleToolbox.getBlockAt(targetLoc, true);
        if (stb instanceof BigStorageUnit) {
            if (((BigStorageUnit)stb).getStoredItemType() != null)
            {

                item = ((BigStorageUnit)stb).getStoredItemType().clone();

            }
        }
    }
    public void setLocation(Location targetLoc)
    {
        x = targetLoc.getBlockX();
        y = targetLoc.getBlockY();
        z = targetLoc.getBlockZ();
        world = targetLoc.getWorld().getName();
    }
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
    public Location getLocation()
    {
        return new Location(Bukkit.getWorld(this.world), x, y, z);
    }
    public int getZ() {
        return z;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }
}
