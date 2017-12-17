package me.mrCookieSlime.sensibletoolbox.blocks.machines;

import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.items.AbstractIOMachine;
import me.mrCookieSlime.sensibletoolbox.api.recipes.CustomRecipeManager;
import me.mrCookieSlime.sensibletoolbox.api.recipes.SimpleCustomRecipe;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.items.components.MachineFrame;
import me.mrCookieSlime.sensibletoolbox.items.components.SimpleCircuit;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.TreeSpecies;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

public class Sawmill extends AbstractIOMachine {
	
    private static final MaterialData md = STBUtil.makeColouredMaterial(Material.STAINED_CLAY, DyeColor.BROWN);

    public Sawmill() {
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ3NDI4MzlkMzQxMjVkMTI4ZGY0ZTIzZWI2Mzc0OTgyYWM2N2Q3MmNiZjYzOTI1ZWQ2OTFlOTNjMjRlZmMzNyJ9fX0=";
    }

    public Sawmill(ConfigurationSection conf) {
        super(conf);
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ3NDI4MzlkMzQxMjVkMTI4ZGY0ZTIzZWI2Mzc0OTgyYWM2N2Q3MmNiZjYzOTI1ZWQ2OTFlOTNjMjRlZmMzNyJ9fX0=";
    }

    @Override
    public void addCustomRecipes(CustomRecipeManager crm) {
        for (TreeSpecies species : TreeSpecies.values()) {
            crm.addCustomRecipe(new SimpleCustomRecipe(this, STBUtil.makeLog(species).toItemStack(1), STBUtil.makePlank(species).toItemStack(6), 60));
        }
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.WOOD_DOOR), new ItemStack(Material.WOOD, 6), 40));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.TRAP_DOOR), new ItemStack(Material.WOOD, 3), 40));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.WOOD_PLATE), new ItemStack(Material.WOOD, 2), 40));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.WOOD_BUTTON), new ItemStack(Material.WOOD, 1), 40));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.WORKBENCH), new ItemStack(Material.WOOD, 4), 40));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.SIGN), new ItemStack(Material.WOOD, 2), 40));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.CHEST), new ItemStack(Material.WOOD, 8), 40));
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
        return new ItemStack(Material.GOLD_AXE);
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
    protected void onMachineStartup() {
        if (SensibleToolbox.getPluginInstance().getConfigCache().isNoisyMachines()) {
            getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_HORSE_STEP_WOOD, 1.0f, 0.5f);
        }
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Sawmill";
    }

    @Override
    public String[] getLore() {
        return new String[]{"Processes wooden items"};
    }

    @Override
    public Recipe getRecipe() {
        SimpleCircuit sc = new SimpleCircuit();
        MachineFrame mf = new MachineFrame();
        registerCustomIngredients(sc, mf);
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("WAW", "IFI", "RGR");
        recipe.setIngredient('W', Material.WOOD);
        recipe.setIngredient('A', Material.IRON_AXE);
        recipe.setIngredient('I', SensibleToolboxPlugin.getSTBItemMaterialData(sc));
        recipe.setIngredient('F', SensibleToolboxPlugin.getSTBItemMaterialData(mf));
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        return recipe;
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
    public int getInventoryGUISize() {
        return 45;
    }

    @Override
    public int getMaxCharge() {
        return 1000;
    }

    @Override
    public int getChargeRate() {
        return 20;
    }
}
