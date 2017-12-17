package me.mrCookieSlime.sensibletoolbox.items.components;

import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBItem;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.material.MaterialData;

public class FishBait extends BaseSTBItem {
	
    private static final MaterialData md = new MaterialData(Material.ROTTEN_FLESH);

    public FishBait() {
    }

    public FishBait(ConfigurationSection conf) {
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Fish Bait";
    }

    @Override
    public String[] getLore() {
        return new String[]{"Used in a Fishing Net", "to catch Fish"};
    }

    @Override
    public Recipe getRecipe() {
        return null;
    }
}
