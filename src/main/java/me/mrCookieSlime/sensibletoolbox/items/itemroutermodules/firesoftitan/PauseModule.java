package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.BlankModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.ItemRouterModule;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.MaterialData;

public class PauseModule extends ItemRouterModule {
    private static final MaterialData md = new MaterialData(Material.BLAZE_POWDER);

    public PauseModule() {
    }

    public PauseModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Pause Upgrade";
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "Fires Of Titan",
                "Insert into an Item Router",
                "Passive module; increases router speed:",
                "0 modules = 1 operation / 20 ticks",
                "1 = 1/30, 2 = 1/40, 3 = 1/50",
                "Any modules over 3 are ignored."
        };
    }

    @Override
    public Recipe getRecipe() {
        BlankModule bm = new BlankModule();
        registerCustomIngredients(bm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(Material.BLAZE_POWDER);
        recipe.addIngredient(Material.SOUL_SAND);
        return recipe;
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }
}
