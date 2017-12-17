package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.mrCookieSlime.Slimefun.SlimefunStartup;
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

import java.util.Random;

public class FishingModule extends DirectionalItemRouterModule {
    private static final MaterialData md = makeDye(DyeColor.YELLOW);

    public FishingModule() {
    }

    public FishingModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public boolean execute(Location loc) {
        try {
            int Speed = 0;
            for (ItemRouter.ModuleAndAmount MaM : getItemRouter().modules) {
                if (MaM.module instanceof FishingModule) {
                    Speed = MaM.amount;
                }
            }
            if (Speed > 7) {
                Speed = 7;
            }
            Material picker = Material.RAW_FISH;
            short valu = 0;
            if (SlimefunStartup.chance(100, 15)) valu = 1;
            if (SlimefunStartup.chance(100, 10)) valu = 2;
            if (SlimefunStartup.chance(100, 5)) valu = 3;
            Location water = loc.clone().add(0, -1, 0);
            ItemStack mainDrop = new ItemStack(picker, Speed, valu);

            ItemStack inBuffer = getItemRouter().getBufferItem();
            if (inBuffer == null || inBuffer.isSimilar(mainDrop) && inBuffer.getAmount() < inBuffer.getMaxStackSize()) {
                if (getFilter().shouldPass(mainDrop)) {
                    if (water.getBlock().getType() == Material.STATIONARY_WATER) {
                        Random tmpR = new Random(System.currentTimeMillis());
                        if (tmpR.nextInt(1000) >65 )
                        {
                            return false;
                        }
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
                        loc.getWorld().playEffect(loc, Effect.STEP_SOUND, Material.STATIONARY_WATER);
                        return true;
                    }
                }
            }
            return false;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Fishing";
    }

    @Override
    public String[] getLore() {
        return new String[] {
                "Fires Of Titan",
        		"Insert into an Item Router", 
        		"Must be over water.",
        		"adds fish into the item router",
                "Stack up to 7."
        };
    }

    @Override
    public Recipe getRecipe() {
        BlankModule bm = new BlankModule();
        registerCustomIngredients(bm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(Material.FISHING_ROD);
        return recipe;
    }
}
