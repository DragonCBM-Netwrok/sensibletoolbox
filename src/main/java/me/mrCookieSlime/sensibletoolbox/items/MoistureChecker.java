package me.mrCookieSlime.sensibletoolbox.items;

import java.util.ArrayList;
import java.util.List;

import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBItem;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.api.util.SoilSaturation;
import me.mrCookieSlime.sensibletoolbox.items.components.SimpleCircuit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

public class MoistureChecker extends BaseSTBItem {
    private static final MaterialData md = new MaterialData(Material.GHAST_TEAR);

    public MoistureChecker() {
        super();
    }

    public MoistureChecker(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Moisture Checker";
    }

    @Override
    public String[] getLore() {
        int r = getRadius() * 2 + 1;
        return new String[]{
                "Tests the saturation level", " of a " + r + "x" + r + " area of farmland.",
                "R-click: " + ChatColor.RESET + "use"
        };
    }

    @Override
    public Recipe getRecipe() {
        SimpleCircuit sc = new SimpleCircuit();
        registerCustomIngredients(sc);
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("S", "C", "I");
        recipe.setIngredient('S', Material.SIGN);
        recipe.setIngredient('C', sc.getMaterialData());
        recipe.setIngredient('I', Material.GOLD_SWORD);
        return recipe;
    }

    @Override
    public boolean hasGlow() {
        return true;
    }

    protected int getRadius() {
        return 1;
    }

    @Override
    public void onInteractItem(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = event.getClickedBlock();
            if (STBUtil.isCrop(b.getType())) {
                b = b.getRelative(BlockFace.DOWN);
            }
            final List<Location> l = new ArrayList<Location>();
            for (int i = -getRadius(); i <= getRadius(); i++) {
                for (int j = -getRadius(); j <= getRadius(); j++) {
                    Block b1 = b.getRelative(i, 0, j);
                    if (b1.getType() == Material.SOIL) {
                        l.add(b1.getLocation());
                    }
                }
            }
            if (!l.isEmpty()) {
                Bukkit.getScheduler().runTask(getProviderPlugin(), new Runnable() {
                    @SuppressWarnings("deprecation")
					@Override
                    public void run() {
                        for (Location loc : l) {
                            player.sendBlockChange(loc, Material.WOOL, getSaturationData(loc.getBlock()));
                        }
                    }
                });
                Bukkit.getScheduler().runTaskLater(getProviderPlugin(), new Runnable() {
                    @SuppressWarnings("deprecation")
					@Override
                    public void run() {
                        for (Location loc : l) {
                            player.sendBlockChange(loc, loc.getBlock().getType(), loc.getBlock().getData());
                        }
                    }
                }, 30L);
                event.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("deprecation")
	private byte getSaturationData(Block b) {
        long now = System.currentTimeMillis();
        long delta = (now - SoilSaturation.getLastWatered(b)) / 1000;
        int saturation = SoilSaturation.getSaturationLevel(b);
        saturation = Math.max(0, saturation - (int) delta);
        if (saturation < 10) {
            return DyeColor.YELLOW.getWoolData();
        } else if (saturation < 30) {
            return DyeColor.BROWN.getWoolData();
        } else if (saturation < 50) {
            return DyeColor.GREEN.getWoolData();
        } else if (saturation < 70) {
            return DyeColor.LIGHT_BLUE.getWoolData();
        } else if (saturation < 90) {
            return DyeColor.CYAN.getWoolData();
        } else {
            return DyeColor.BLUE.getWoolData();
        }
    }
}
