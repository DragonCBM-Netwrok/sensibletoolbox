package me.mrCookieSlime.sensibletoolbox.blocks.machines;

import me.mrCookieSlime.CSCoreLibPlugin.general.Math.DoubleHandler;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.energy.EnergyNet;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;

public class HolographicMonitor extends BaseSTBBlock {

    private static final MaterialData md = STBUtil.makeColouredMaterial(Material.STAINED_GLASS, DyeColor.LIGHT_BLUE);
    private Hologram hologram;

    public HolographicMonitor() {
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Q1OTkwOTQ2OWM5ZmI1NjhhYWQ1OWRjZGRlMjkyZTM3N2JkYjdhM2E1ODZjZTliMTJjOWFjYjc2MDRkNDQifX19";
    }

    public HolographicMonitor(ConfigurationSection conf) {
        super(conf);
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Q1OTkwOTQ2OWM5ZmI1NjhhYWQ1OWRjZGRlMjkyZTM3N2JkYjdhM2E1ODZjZTliMTJjOWFjYjc2MDRkNDQifX19";
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Holographic Monitor";
    }

    @Override
    public String[] getLore() {
        return new String[] {
                "Displays the Net Gain/Loss",
                "using Holograms"
        };
    }

    @Override
    public Recipe getRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("GGG", "LPL", "GGG");
        PowerMonitor monitor = new PowerMonitor();
        registerCustomIngredients(monitor);
        recipe.setIngredient('G', Material.GLASS);
        recipe.setIngredient('P', SensibleToolboxPlugin.getSTBItemMaterialData(monitor));
        recipe.setIngredient('L', STBUtil.makeColouredMaterial(Material.INK_SACK, DyeColor.BLUE));
        return recipe;
    }

    @Override
    public int getTickRate() {
        return 120;
    }

    @Override
    public void onServerTick() {
        super.onServerTick();
        if (hologram == null) return;
        this.hologram.clearLines();

        for (BlockFace f: STBUtil.mainHorizontalFaces) {
            EnergyNet net = SensibleToolbox.getEnergyNet(getRelativeLocation(f).getBlock());
            if (net != null) {
                double stat = net.getSupply() - net.getDemand();
                String prefix;
                if (stat > 0) prefix = "" +ChatColor.DARK_GREEN + ChatColor.BOLD + "+";
                else prefix =  "" +ChatColor.DARK_RED + ChatColor.BOLD + "-";
                this.hologram.appendTextLine(prefix + " " + ChatColor.GRAY + DoubleHandler.getFancyDouble(Double.valueOf(String.valueOf(stat).replace("-", ""))) + " SCU/t");
                break;
            }
        }
    }

    @Override
    public void onBlockRegistered(Location location, boolean isPlacing) {
        super.onBlockRegistered(location, isPlacing);

        onServerTick();
        this.hologram = HologramsAPI.createHologram(SensibleToolboxPlugin.getInstance(), getLocation().add(0.5, 1.4, 0.5));
    }

    @Override
    public void onBlockUnregistered(Location location) {
        super.onBlockUnregistered(location);

        this.hologram.delete();
    }
}