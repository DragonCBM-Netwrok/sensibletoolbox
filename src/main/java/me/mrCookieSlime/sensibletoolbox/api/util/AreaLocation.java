package me.mrCookieSlime.sensibletoolbox.api.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Created by Daniel on 4/23/2017.
 */
public class AreaLocation {
    boolean loc1set = false, loc2set = false;
    int x1 = 0, y1 = 0, z1 = 0;
    int x2 = 0, y2 = 0, z2 = 0;
    String world = "";
     public AreaLocation()
     {

     }
    public AreaLocation(ConfigurationSection conf) {
            x1 = conf.getInt("Area-x1");
            y1 = conf.getInt("Area-y1");
            z1 = conf.getInt("Area-z1");

            x2 = conf.getInt("Area-x2");
            y2 = conf.getInt("Area-y2");
            z2 = conf.getInt("Area-z2");

            world = conf.getString("Area-world");
            loc1set = conf.getBoolean("Area-loc1");
            loc2set = conf.getBoolean("Area-loc2");


    }
    public Location getLocation1()
    {
        return new Location(Bukkit.getWorld(world), x1, y1, z1);
    }
    public Location getLocation2()
    {
        return new Location(Bukkit.getWorld(world), x2, y2, z2);
    }
    public boolean areLocationsset() {
        if (loc1set && loc2set)
        {
            return true;
        }
        return false;
    }

    public YamlConfiguration freeze(YamlConfiguration conf ) {
        conf.set("Area-x1", x1);
        conf.set("Area-y1", y1);
        conf.set("Area-z1", z1);

        conf.set("Area-x2", x2);
        conf.set("Area-y2", y2);
        conf.set("Area-z2", z2);
        conf.set("Area-world", world);
        conf.set("Area-loc1", loc1set);
        conf.set("Area-loc2", loc2set);
        return conf;
    }
    public void setLocation1(Location one) {
        this.x1 = one.getBlockX();
        this.y1 = one.getBlockY();
        this.z1 = one.getBlockZ();
        this.world = one.getWorld().getName();
        loc1set = true;
    }
    public void setLocation2(Location two) {
        this.x2 = two.getBlockX();
        this.y2 = two.getBlockY();
        this.z2 = two.getBlockZ();
        loc2set = true;
    }

    public boolean isLoc1set() {
        return loc1set;
    }

    public boolean isLoc2set() {
        return loc2set;
    }

    public int getX1() {
        return x1;
    }
    public int getX2() {
        return x2;
    }

    public int getY1() {
        return y1;
    }
    public int getY2() {
        return y2;
    }

    public int getZ1() {
        return z1;
    }

    public int getZ2() {
        return z2;
    }
    public void setWorld(String world) {
        this.world = world;
    }

    public String getWorld() {
        return world;
    }
}
