package me.mrCookieSlime.sensibletoolbox.blocks.machines;

import me.mrCookieSlime.CSCoreLibPlugin.general.Block.Vein;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.items.AbstractProcessingMachine;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.items.components.MachineFrame;
import me.mrCookieSlime.sensibletoolbox.items.components.SimpleCircuit;
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

import java.util.ArrayList;
import java.util.List;

public class Pump extends AbstractProcessingMachine {
	
    private static final MaterialData md = STBUtil.makeColouredMaterial(Material.STAINED_CLAY, DyeColor.CYAN);
    private static final int PUMP_FILL_TIME = 40; // 40 ticks to fill a bucket
    private BlockFace pumpFace = BlockFace.DOWN;  // will be configurable later

    public Pump() {
        super();
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI1N2E0YzY4ZDFkMmM1ZGU5NzhlYTZkZTRkYjkxZWYzODdjYTZjMzc5NjZiYjhlN2M4ODI2ZjkzN2U2YzMifX19";
    }

    public Pump(ConfigurationSection conf) {
        super(conf);
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI1N2E0YzY4ZDFkMmM1ZGU5NzhlYTZkZTRkYjkxZWYzODdjYTZjMzc5NjZiYjhlN2M4ODI2ZjkzN2U2YzMifX19";
    }

    @Override
    public int getTickRate() {
        return 5;
    }

    @Override
    public int getInventoryGUISize() {
        return 45;
    }

    @Override
    public int getProgressItemSlot() {
        return 12;
    }

    @Override
    public int getProgressCounterSlot() {
        return 3;
    }

    @Override
    public ItemStack getProgressIcon() {
        return new ItemStack(Material.DIAMOND_BOOTS);
    }

    @Override
    public int[] getInputSlots() {
        return new int[]{10};
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{14};
    }

    @Override
    public int[] getUpgradeSlots() {
        return new int[]{41, 42, 43, 44};
    }

    @Override
    public int getUpgradeLabelSlot() {
        return 40;
    }

    @Override
    protected void playActiveParticleEffect() {
        getLocation().getWorld().playEffect(getLocation(), Effect.STEP_SOUND, getRelativeLocation(pumpFace).getBlock().getType());
    }

    @Override
    public boolean acceptsEnergy(BlockFace face) {
        return true;
    }

    @Override
    public boolean suppliesEnergy(BlockFace face) {
        return false;
    }

    @Override
    public int getEnergyCellSlot() {
        return 36;
    }

    @Override
    public int getChargeDirectionSlot() {
        return 37;
    }

    @Override
    public int getMaxCharge() {
        return 1000;
    }

    @Override
    public int getChargeRate() {
        return 20;
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Pump";
    }

    @Override
    public String[] getLore() {
        return new String[] {
            "Pumps liquids into a bucket"
        };
    }

    @Override
    public Recipe getRecipe() {
        SimpleCircuit sc = new SimpleCircuit();
        MachineFrame mf = new MachineFrame();
        registerCustomIngredients(sc, mf);
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("PB ", "SIS", "RGR");
        recipe.setIngredient('P', Material.PISTON_BASE);
        recipe.setIngredient('B', Material.BUCKET);
        recipe.setIngredient('S', SensibleToolboxPlugin.getSTBItemMaterialData(sc));
        recipe.setIngredient('I', SensibleToolboxPlugin.getSTBItemMaterialData(mf));
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        return recipe;
    }

    @Override
    public double getScuPerTick() {
        // 0.1 SCU to fill a bucket
        return 0.1 / PUMP_FILL_TIME;
    }

    @Override
    public void onServerTick() {
        int inputSlot = getInputSlots()[0];
        ItemStack stackIn = getInventoryItem(inputSlot);

        Block toPump = findNextBlockToPump();

        if (getProcessing() == null && stackIn != null && isRedstoneActive()) {
            // pull a bucket from the input stack into processing
            ItemStack toProcess = makeProcessingItem(toPump, stackIn.getType());
            setProcessing(toProcess);
            if (toProcess != null) {
                getProgressMeter().setMaxProgress(PUMP_FILL_TIME);
                setProgress(PUMP_FILL_TIME);
                stackIn.setAmount(stackIn.getAmount() - 1);
                setInventoryItem(inputSlot, stackIn);
            }
        }

        if (getProgress() > 0 && getCharge() > 0 && STBUtil.isLiquidSourceBlock(toPump)) {
            // currently processing....
            setProgress(getProgress() - getSpeedMultiplier() * getTickRate());
            setCharge(getCharge() - getPowerMultiplier() * getScuPerTick() * getTickRate());
            playActiveParticleEffect();
        }


        if (getProcessing() != null && getProgress() <= 0 && !isJammed()) {
            // done processing - try to move filled container into output
            ItemStack result = getProcessing();
            int slot = findOutputSlot(result);
            if (slot >= 0) {
                setInventoryItem(slot, result);
                setProcessing(null);
                update(false);
                replacePumpedBlock(toPump);
            } else {
                setJammed(true);
            }
        }

        handleAutoEjection();

        super.onServerTick();
    }

    private Block findNextBlockToPump() {
        switch (getRelativeLocation(pumpFace).getBlock().getType()) {
        case LAVA: case STATIONARY_LAVA:
        	List<Location> list = new ArrayList<Location>();
        	list.add(getRelativeLocation(pumpFace));
        	Vein.calculate(getRelativeLocation(pumpFace), getRelativeLocation(pumpFace), list, 128);
        	return list.get(list.size() - 1).getBlock();
        default:
            return getRelativeLocation(pumpFace).getBlock();
        }
    }

    private void replacePumpedBlock(Block block) {
        if (STBUtil.isInfiniteWaterSource(block)) {
            return;
        }
        switch (block.getType()) {
            case WATER: case STATIONARY_WATER:
                block.setType(Material.AIR);
                break;
            case LAVA: case STATIONARY_LAVA:
                block.setType(Material.STONE);
                break;
            default:
                break;
        }
    }

    private ItemStack makeProcessingItem(Block toPump, Material container) {
        if (!STBUtil.isLiquidSourceBlock(toPump)) {
            return null;
        }
        Material res;
        switch (container) {
            case BUCKET:
                switch (toPump.getType()) {
                    case LAVA: case STATIONARY_LAVA:
                        res = Material.LAVA_BUCKET;
                        break;
                    case WATER: case STATIONARY_WATER:
                        res = Material.WATER_BUCKET;
                        break;
                    default:
                        res = null;
                }
                break;
            case GLASS_BOTTLE:
                switch (toPump.getType()) {
                    case WATER: case STATIONARY_WATER:
                        res = Material.POTION;
                        break;
                    default:
                        res = null;
                }
                break;
            default:
                res = null;
        }

        return res == null ? null : new ItemStack(res);
    }

    @Override
    public boolean acceptsItemType(ItemStack stack) {
        return stack.getType() == Material.BUCKET || stack.getType() == Material.GLASS_BOTTLE;
    }
}
