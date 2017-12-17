package me.mrCookieSlime.sensibletoolbox.blocks.machines;

import me.mrCookieSlime.CSCoreLibPlugin.CSCoreLib;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.items.AutoFarmingMachine;
import me.mrCookieSlime.sensibletoolbox.items.GoldCombineHoe;
import me.mrCookieSlime.sensibletoolbox.items.components.MachineFrame;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InfernalFarm extends AutoFarmingMachine {
	
    private static final MaterialData md = new MaterialData(Material.NETHER_BRICK);
    private static final Map<Material, Material> crops = new HashMap<Material, Material>();
    private static final int radius = 5;
    
    static {
    	crops.put(Material.NETHER_WARTS, Material.NETHER_STALK);
    }
    
    private Set<Block> blocks;
    private Material buffer;

    public InfernalFarm() {
        blocks = new HashSet<Block>();
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQxM2Q2NzBlMjlhMTM0Y2U2ODZjNzhjOGU1NjM1M2VhZTc1YzI5YTc2MGI0ODMzN2IxNmMxZDVjMzY5NjFlIn19fQ==";
    }

    public InfernalFarm(ConfigurationSection conf) {
        super(conf);
        blocks = new HashSet<Block>();
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTQxM2Q2NzBlMjlhMTM0Y2U2ODZjNzhjOGU1NjM1M2VhZTc1YzI5YTc2MGI0ODMzN2IxNmMxZDVjMzY5NjFlIn19fQ==";
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Infernal Farm";
    }

    @Override
    public String[] getLore() {
        return new String[] {
                "Automatically harvests and replants",
                "Nether Warts",
                "in a " + radius + "x" + radius + " Radius 2 Blocks above the Machine"
        };
    }

    @Override
    public Recipe getRecipe() {
    	MachineFrame frame = new MachineFrame();
    	GoldCombineHoe hoe = new GoldCombineHoe();
    	registerCustomIngredients(frame, hoe);
        ShapedRecipe res = new ShapedRecipe(toItemStack());
        res.shape("NHN", "IFI", "RGR");
        res.setIngredient('R', Material.REDSTONE);
        res.setIngredient('G', Material.GOLD_INGOT);
        res.setIngredient('I', Material.IRON_INGOT);
        res.setIngredient('H', SensibleToolboxPlugin.getSTBItemMaterialData(hoe));
        res.setIngredient('F', SensibleToolboxPlugin.getSTBItemMaterialData(frame));
        res.setIngredient('N', Material.NETHER_BRICK_ITEM);
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

    @SuppressWarnings("deprecation")
	@Override
    public void onServerTick() {
    	if (!isJammed()) {
    		for (Block crop: blocks) {
        		if (crops.containsKey(crop.getType()) && crop.getData() >= 3) {
        			if (getCharge() >= getScuPerCycle()) setCharge(getCharge() - getScuPerCycle());
        			else break;
        			crop.setData((byte) 0);
        			crop.getWorld().playEffect(crop.getLocation(), Effect.STEP_SOUND, crop.getType());
            		setJammed(!output(crops.get(crop.getType())));
        			break;
        		}
        	}
    	}
    	else if (buffer != null) {
    		setJammed(!output(buffer));
    	}
    	
        super.onServerTick();
    }

	private boolean output(Material m) {
		for (int slot: getOutputSlots()) {
			ItemStack stack = getInventoryItem(slot);
			if (stack == null || (stack.getType() == m && stack.getAmount() < stack.getMaxStackSize())) {
				if (stack == null) stack = new ItemStack(m);
				int amount = (stack.getMaxStackSize() - stack.getAmount()) > 3 ? (CSCoreLib.randomizer().nextInt(2) + 1): (stack.getMaxStackSize() - stack.getAmount());
				setInventoryItem(slot, new CustomItem(stack, stack.getAmount() + amount));
				buffer = null;
				return true;
			}
		}
		return false;
	}
	
	@Override
    public double getScuPerCycle() {
        return 50.0;
    }
}
