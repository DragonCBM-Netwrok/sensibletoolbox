package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.mrCookieSlime.sensibletoolbox.blocks.ItemRouter;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.BlankModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.DirectionalItemRouterModule;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.MaterialData;

public class CobblestoneModule extends DirectionalItemRouterModule {
    private static final MaterialData md = makeDye(DyeColor.YELLOW);

    public CobblestoneModule() {
    }

    public CobblestoneModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public boolean execute(Location loc) {
        int Speed = 0;
        for (ItemRouter.ModuleAndAmount MaM: getItemRouter().modules)
        {
            if (MaM.module instanceof CobblestoneModule)
            {
                Speed = MaM.amount;
            }
        }
        if (Speed > 64)
        {
            Speed = 64;
        }
        ItemStack mainDrop = new ItemStack(Material.COBBLESTONE, Speed);
        ItemStack inBuffer = getItemRouter().getBufferItem();
        if (inBuffer == null || inBuffer.isSimilar(mainDrop) && inBuffer.getAmount() < inBuffer.getMaxStackSize()) {
            if (getFilter().shouldPass(mainDrop)) {
                if (inBuffer == null) {
                    getItemRouter().setBufferItem(mainDrop);
                } else {
                    int toAdd = Math.min(mainDrop.getAmount(), inBuffer.getMaxStackSize() - inBuffer.getAmount());
                    getItemRouter().setBufferAmount(inBuffer.getAmount() + toAdd);
                    if (toAdd < mainDrop.getAmount()) {
                        ItemStack stack = mainDrop.clone();
                        stack.setAmount(mainDrop.getAmount() - toAdd);
                    }
                }
                loc.getWorld().playEffect(loc, Effect.STEP_SOUND, Material.COBBLESTONE);
                return true;
            }
        }
        return false;
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Cobble Generator";
    }

    @Override
    public String[] getLore() {
        return new String[] {
                "Fires Of Titan",
        		"Insert into an Item Router", 
        		"Generates Cobblestone",
        		"into the item router",
                "Stack up to 64."
        };
    }

    @Override
    public Recipe getRecipe() {
        BlankModule bm = new BlankModule();
        registerCustomIngredients(bm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(Material.LAVA_BUCKET);
        recipe.addIngredient(Material.WATER_BUCKET);
        return recipe;
    }
}
