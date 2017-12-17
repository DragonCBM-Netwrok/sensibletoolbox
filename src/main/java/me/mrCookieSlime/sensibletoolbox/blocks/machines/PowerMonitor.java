package me.mrCookieSlime.sensibletoolbox.blocks.machines;

import me.mrCookieSlime.CSCoreLibPlugin.general.Math.DoubleHandler;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.energy.EnergyNet;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.items.energycells.TenKEnergyCell;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

public class PowerMonitor extends BaseSTBBlock {
	
    private static final MaterialData md = STBUtil.makeColouredMaterial(Material.STAINED_GLASS, DyeColor.ORANGE);

    public PowerMonitor() {
		skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGVkYzFjNzc2ZDRhZWFmYjc1Y2I4YjkzOGFmODllMjA5MDJkODY4NGI3NDJjNmE4Y2M3Y2E5MjE5N2FiN2IifX19";
    }

    public PowerMonitor(ConfigurationSection conf) {
        super(conf);
		skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGVkYzFjNzc2ZDRhZWFmYjc1Y2I4YjkzOGFmODllMjA5MDJkODY4NGI3NDJjNmE4Y2M3Y2E5MjE5N2FiN2IifX19";
    }

	@Override
	public MaterialData getMaterialData() {
		return md;
	}

	@Override
	public String getItemName() {
		return "Power Monitor";
	}

	@Override
	public String[] getLore() {
		return new String[] {
				"Displays the Net Gain/Loss",
				"on attached Signs"
		};
	}

	@Override
	public Recipe getRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("GGG", "RCR", "GGG");
        TenKEnergyCell cell = new TenKEnergyCell();
        cell.setCharge(0.0);
        registerCustomIngredients(cell);
        recipe.setIngredient('G', Material.GLASS);
        recipe.setIngredient('C', STBUtil.makeWildCardMaterialData(cell));
        recipe.setIngredient('R', Material.REDSTONE);
        return recipe;
	}
	
	@Override
	public int getTickRate() {
        return 100;
    }
	
	@Override
	public void onServerTick() {
        updateAttachedLabelSigns();
		super.onServerTick();
    }
	
	@Override
    protected String[] getSignLabel(BlockFace face) {
        String[] label = super.getSignLabel(face);
        for (BlockFace f: STBUtil.mainHorizontalFaces) {
            EnergyNet net = SensibleToolbox.getEnergyNet(getRelativeLocation(f).getBlock());
            if (net != null) {
            	double stat = net.getSupply() - net.getDemand();
            	String prefix;
            	if (stat > 0) prefix = "�a�l+";
            	else prefix = "�4�l-";
            	label[2] = prefix + " �8" + DoubleHandler.getFancyDouble(Double.valueOf(String.valueOf(stat).replace("-", ""))) + " SCU/t";
            	break;
            }
        }
        return label;
    }
}