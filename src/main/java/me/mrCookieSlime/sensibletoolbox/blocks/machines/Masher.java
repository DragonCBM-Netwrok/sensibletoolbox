package me.mrCookieSlime.sensibletoolbox.blocks.machines;

import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.items.AbstractIOMachine;
import me.mrCookieSlime.sensibletoolbox.api.recipes.CustomRecipeManager;
import me.mrCookieSlime.sensibletoolbox.api.recipes.SimpleCustomRecipe;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.items.components.GoldDust;
import me.mrCookieSlime.sensibletoolbox.items.components.IronDust;
import me.mrCookieSlime.sensibletoolbox.items.components.MachineFrame;
import me.mrCookieSlime.sensibletoolbox.items.components.QuartzDust;
import me.mrCookieSlime.sensibletoolbox.items.components.SimpleCircuit;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

public class Masher extends AbstractIOMachine {
	
    private static final MaterialData md = STBUtil.makeColouredMaterial(Material.STAINED_CLAY, DyeColor.GREEN);

    public Masher() {
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI3NDNhOGJlYTc1MmQ2ZGFmNjcyZGVhMzY3ZTFhNWQzOThhZWNiZTBjM2M4NDgzYTkwZWM5YWM0NDEyYTQxIn19fQ==";
    }

    public Masher(ConfigurationSection conf) {
        super(conf);
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI3NDNhOGJlYTc1MmQ2ZGFmNjcyZGVhMzY3ZTFhNWQzOThhZWNiZTBjM2M4NDgzYTkwZWM5YWM0NDEyYTQxIn19fQ==";
    }

    @Override
    public void addCustomRecipes(CustomRecipeManager crm) {
        ItemStack whiteDye = STBUtil.makeColouredMaterial(Material.INK_SACK, DyeColor.WHITE).toItemStack(5);
        ItemStack lapis = STBUtil.makeColouredMaterial(Material.INK_SACK, DyeColor.BLUE).toItemStack(8);
        ItemStack greenDye = STBUtil.makeColouredMaterial(Material.INK_SACK, DyeColor.GREEN).toItemStack(1);
        QuartzDust qd = new QuartzDust();

        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.COBBLESTONE), new ItemStack(Material.SAND), 120));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.GRAVEL), new ItemStack(Material.SAND), 80));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.BONE), whiteDye, 40));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.BLAZE_ROD), new ItemStack(Material.BLAZE_POWDER, 4), 80));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.COAL_ORE), new ItemStack(Material.COAL, 2), 100));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.REDSTONE_ORE), new ItemStack(Material.REDSTONE, 6), 100));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.DIAMOND_ORE), new ItemStack(Material.DIAMOND, 2), 160));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.IRON_ORE), new IronDust().toItemStack(2), 120));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.IRON_INGOT), new IronDust().toItemStack(), 120));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.GOLD_ORE), new GoldDust().toItemStack(2), 80));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.GOLD_INGOT), new GoldDust().toItemStack(), 80));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.WOOL), new ItemStack(Material.STRING, 4), 60));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.GLOWSTONE), new ItemStack(Material.GLOWSTONE_DUST, 4), 60));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.LAPIS_ORE), lapis, 80));
//        crm.addCustomRecipe(new SimpleCustomRecipe(this, STBUtil.makeWildCardItemStack(Material.LEAVES), greenDye, 40));
//        crm.addCustomRecipe(new SimpleCustomRecipe(this, STBUtil.makeWildCardItemStack(Material.LEAVES_2), greenDye, 40));
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.LEAVES), greenDye, 40), true);
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.LEAVES_2), greenDye, 40), true);
        crm.addCustomRecipe(new SimpleCustomRecipe(this, new ItemStack(Material.QUARTZ), qd.toItemStack(), 120));
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "Masher";
    }

    @Override
    public String[] getLore() {
        return new String[]{"Grinds ores and other ", "resources into dusts"};
    }

    @Override
    public Recipe getRecipe() {
        SimpleCircuit sc = new SimpleCircuit();
        MachineFrame mf = new MachineFrame();
        registerCustomIngredients(sc, mf);
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("FFF", "SIS", "RGR");
        recipe.setIngredient('F', Material.FLINT);
        recipe.setIngredient('S', SensibleToolboxPlugin.getSTBItemMaterialData(sc));
        recipe.setIngredient('I', SensibleToolboxPlugin.getSTBItemMaterialData(mf));
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('G', Material.GOLD_INGOT);
        return recipe;
    }

    @Override
    public int[] getInputSlots() {
        return new int[]{10};
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{14, 15};
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
        return new ItemStack(Material.GOLD_PICKAXE);
    }

    @Override
    protected void onMachineStartup() {
        if (SensibleToolbox.getPluginInstance().getConfigCache().isNoisyMachines()) {
            getLocation().getWorld().playSound(getLocation(), Sound.ENTITY_SKELETON_HORSE_AMBIENT, 1.0f, 0.5f);
        }
    }
    
//    TODO: Fix particles
//    @Override
//    protected void playActiveParticleEffect() {
//        if (((SensibleToolboxPlugin) getProviderPlugin()).isProtocolLibEnabled() && getTicksLived() % 20 == 0) {
//            ParticleEffect.LARGE_SMOKE.play(getLocation().add(0.5, 1.0, 0.5), 0.2f, 1.0f, 0.2f, 0.001f, 5);
//        }
//    }
}
