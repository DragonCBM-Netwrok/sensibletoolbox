package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.desht.dhutils.Debugger;
import me.mrCookieSlime.sensibletoolbox.api.gui.ToggleButton;
import me.mrCookieSlime.sensibletoolbox.blocks.machines.BigStorageUnit;
import me.mrCookieSlime.sensibletoolbox.items.LandMarker;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.HyperSenderModule;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;

public class AreaSorterModule extends AreaItemRouterModule {
    private static final Dye md = makeDye(DyeColor.PURPLE);

    public AreaSorterModule() {
    }

    public AreaSorterModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Area Sorter";
    }

    @Override
    public String[] getLore() {
        return makeDirectionalLore(
                "Insert into an Item Router",
                "Places items into inventory IF",
                "- inventory already contains that item"
        );
    }

    @Override
    public Recipe getRecipe() {
        HyperSenderModule bm = new HyperSenderModule();
        LandMarker sm = new LandMarker();
        registerCustomIngredients(bm);
        registerCustomIngredients(sm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(sm.getMaterialData());
        recipe.addIngredient(Material.SPIDER_EYE);
        recipe.addIngredient(Material.ARROW);
        return recipe;
    }

    @Override
    public boolean execute(Location loc) {

        if (getItemRouter() != null && getItemRouter().getBufferItem() != null) {
            Debugger.getInstance().debug(2, "sorter in " + getItemRouter() + " has: " + getItemRouter().getBufferItem());
            Location targetLoc = loc.clone();
            int nToInsert = getItemRouter().getStackSize();
            ItemStack toInsert = getItemRouter().getBufferItem().clone();
            BigStorageUnit BSU = getStorage(toInsert);

            if (BSU != null) {
                if (BSU.getStoredItemType() != null) {
                    toInsert.setAmount(Math.min(nToInsert, toInsert.getAmount()));
                    int nInserted = BSU.insertItems(toInsert, BlockFace.SELF, true, getItemRouter().getOwner());
                    if (nInserted > 0) {
                        if (!BSU.isLocked()) {
                            BSU.setLocked(true);
                            ((ToggleButton) BSU.getGUI().getGadget(26)).setValue(true);
                        }
                        getItemRouter().reduceBuffer(nInserted);
                        return true;
                    }
                }
            }

        }
        return false;
    }


}
