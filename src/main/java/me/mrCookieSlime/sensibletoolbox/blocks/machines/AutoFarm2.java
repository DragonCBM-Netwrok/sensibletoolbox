package me.mrCookieSlime.sensibletoolbox.blocks.machines;

import me.mrCookieSlime.CSCoreLibPlugin.CSCoreLib;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.items.AutoFarmingMachine;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.items.GoldCombineHoe;
import me.mrCookieSlime.sensibletoolbox.items.components.MachineFrame;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("deprecation")
public class AutoFarm2 extends AutoFarmingMachine {
	
    private static final MaterialData md = STBUtil.makeColouredMaterial(Material.STAINED_CLAY, DyeColor.BROWN);
    private static final Map<Material, MaterialData> crops = new HashMap<Material, MaterialData>();
    private static final int radius = 5;
    
    static {
    	crops.put(Material.COCOA, new MaterialData(Material.INK_SACK, (byte) 3));
    	crops.put(Material.SUGAR_CANE_BLOCK, new MaterialData(Material.SUGAR_CANE));
    	crops.put(Material.CACTUS, new MaterialData(Material.CACTUS));
    }
    
    private Set<Block> blocks;
    private MaterialData buffer;

    public AutoFarm2() {
		super();
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQ5NzRlYzE5ZDQwZWVmOTJiMmU0YWUzMmFjN2I4NjQ2ZjM0ODljNTA4ZjUwYjE2ZjM4MzUyOTZlMmFkYTIifX19";
		blocks = new HashSet<Block>();
    }

    public AutoFarm2(ConfigurationSection conf) {
        super(conf);
		skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQ5NzRlYzE5ZDQwZWVmOTJiMmU0YWUzMmFjN2I4NjQ2ZjM0ODljNTA4ZjUwYjE2ZjM4MzUyOTZlMmFkYTIifX19";
        blocks = new HashSet<Block>();
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Auto Farm MkII";
    }

    @Override
    public String[] getLore() {
        return new String[] {
                "Automatically harvests and replants",
                "Cocoa Beans/Sugar Cane/Cactus",
                "in a " + radius + "x" + radius + " Radius 2 Blocks above the Machine"
        };
    }

    @Override
    public Recipe getRecipe() {
    	MachineFrame frame = new MachineFrame();
    	GoldCombineHoe hoe = new GoldCombineHoe();
    	registerCustomIngredients(frame, hoe);
        ShapedRecipe res = new ShapedRecipe(toItemStack());
        res.shape("LHL", "IFI", "RGR");
        res.setIngredient('R', Material.REDSTONE);
        res.setIngredient('G', Material.GOLD_INGOT);
        res.setIngredient('I', Material.IRON_INGOT);
        res.setIngredient('L', Material.LOG);
		res.setIngredient('H', SensibleToolboxPlugin.getSTBItemMaterialData(hoe));
		res.setIngredient('F', SensibleToolboxPlugin.getSTBItemMaterialData(frame));
        return res;
    }
    
    @Override
    public void onBlockRegistered(Location location, boolean isPlacing) {
    	int i = radius / 2;
    	for (int x = -i; x <= i; x++) {
    		for (int z = -i; z <= i; z++) {
        		blocks.add(new Location(location.getWorld(), location.getBlockX() + x, location.getBlockY() + 2, location.getBlockZ() + z).getBlock());
        	}
    	}
    	
    	super.onBlockRegistered(location, isPlacing);
    }
    
	@Override
    public void onServerTick() {
    	if (!isJammed()) {
    		for (Block crop: blocks) {
        		if (crops.containsKey(crop.getType())) {
        			if (crop.getType() == Material.COCOA) {
        				if (crop.getData() >= 8) {
        					if (getCharge() >= getScuPerCycle()) setCharge(getCharge() - getScuPerCycle());
                			else break;
                			crop.setData((byte) (crop.getData() - 8));
                			crop.getWorld().playEffect(crop.getLocation(), Effect.STEP_SOUND, crop.getType());
                    		setJammed(!output(crops.get(crop.getType())));
                			break;
        				}
        			}
        			else {
        				Block block = crop.getRelative(BlockFace.UP);
        				if (crops.containsKey(block.getType()) && block.getType() != Material.COCOA) {
        					if (getCharge() >= getScuPerCycle()) setCharge(getCharge() - getScuPerCycle());
                			else break;
        					block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getType());
                    		setJammed(!output(crops.get(block.getType())));
        					block.setType(Material.AIR);
                			break;
        				}
        			}
        		}
        	}
    	}
    	else if (buffer != null) setJammed(!output(buffer));
    	
        super.onServerTick();
    }

	private boolean output(MaterialData m) {
		for (int slot: getOutputSlots()) {
			ItemStack stack = getInventoryItem(slot);
			if (stack == null || (stack.getType() == m.getItemType() && stack.getAmount() < stack.getMaxStackSize())) {
				if (stack == null) stack = m.toItemStack(1);
				int amount = m.getItemType() == Material.INK_SACK ? ((stack.getMaxStackSize() - stack.getAmount()) > 3 ? (CSCoreLib.randomizer().nextInt(2) + 1): (stack.getMaxStackSize() - stack.getAmount())): 1;
				setInventoryItem(slot, new CustomItem(stack, stack.getAmount() + amount));
				buffer = null;
				return true;
			}
		}
		return false;
	}
	
	@Override
    public double getScuPerCycle() {
        return 30.0;
    }
}
