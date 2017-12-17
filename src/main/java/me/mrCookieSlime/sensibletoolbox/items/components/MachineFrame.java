package me.mrCookieSlime.sensibletoolbox.items.components;

import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBItem;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

public class MachineFrame extends BaseSTBItem {
    private static final MaterialData md = new MaterialData(Material.IRON_BLOCK);

    public MachineFrame() {
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMWI5ZjNiMjk5OWM5ZWFhNWU2ODE1NDdiNTJlZWJjNWJmN2U5Zjg3YTdiMTFkOWM4ZDkxZDI2ZjkxNjEifX19";
    }

    public MachineFrame(ConfigurationSection conf) {
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWZmMWI5ZjNiMjk5OWM5ZWFhNWU2ODE1NDdiNTJlZWJjNWJmN2U5Zjg3YTdiMTFkOWM4ZDkxZDI2ZjkxNjEifX19";
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Machine Frame";
    }

    @Override
    public String[] getLore() {
        return new String[]{"Used in fabrication of", "various machines."};
    }

    @Override
    public Recipe getRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("IBI", "B B", "IBI");
        recipe.setIngredient('B', Material.IRON_FENCE);
        recipe.setIngredient('I', Material.IRON_INGOT);
        return recipe;
    }

}
