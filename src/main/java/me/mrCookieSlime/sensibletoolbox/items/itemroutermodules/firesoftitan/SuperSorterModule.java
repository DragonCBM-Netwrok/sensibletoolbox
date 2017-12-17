package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.desht.dhutils.Debugger;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.gui.ToggleButton;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.util.VanillaInventoryUtils;
import me.mrCookieSlime.sensibletoolbox.blocks.machines.BigStorageUnit;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.BlankModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.DirectionalItemRouterModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.SorterModule;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;

public class SuperSorterModule extends DirectionalItemRouterModule {
    private static final Dye md = makeDye(DyeColor.PURPLE);

    public SuperSorterModule() {
    }

    public SuperSorterModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Super Sorter";
    }

    @Override
    public String[] getLore() {
        return makeDirectionalLore(
                "Insert into an Item Router",
                "Places items into inventory IF",
                "- inventory already contains that item",
                "Looks in directs up to 15 blocks."
        );
    }

    @Override
    public Recipe getRecipe() {
        BlankModule bm = new BlankModule();
        SorterModule sm = new SorterModule();
        registerCustomIngredients(bm);
        registerCustomIngredients(sm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(sm.getMaterialData());
        recipe.addIngredient(Material.MINECART);
        return recipe;
    }

    @Override
    public boolean execute(Location loc) {
        if (getItemRouter() != null && getItemRouter().getBufferItem() != null) {
            if (getFilter() != null && !getFilter().shouldPass(getItemRouter().getBufferItem())) {
                return false;
            }
            Debugger.getInstance().debug(2, "sorter in " + getItemRouter() + " has: " + getItemRouter().getBufferItem());
            Location targetLoc = loc.clone();
            BigStorageUnit BSUEmpty = null;
            for (int i = 0; i < 15 ;i++) {
                targetLoc = getTargetLocation(targetLoc);
                int nToInsert = getItemRouter().getStackSize();
                BaseSTBBlock stb = SensibleToolbox.getBlockAt(targetLoc, true);
                int nInserted = 0;
                if (stb instanceof BigStorageUnit) {
                    if (creativeModeBlocked(stb, loc)) {
                        getItemRouter().ejectBuffer(getItemRouter().getFacing());
                        return false;
                    }
                    BigStorageUnit BSU = ((BigStorageUnit) stb);
                    if (BSU.getStoredItemType() == null)
                    {
                        if (BSUEmpty == null)
                        {
                            BSUEmpty = BSU;
                        }
                    }
                    else {
                        ItemStack toInsert = getItemRouter().getBufferItem().clone();
                        toInsert.setAmount(Math.min(nToInsert, toInsert.getAmount()));
                        nInserted = BSU.insertItems(toInsert, getFacing().getOppositeFace(), true, getItemRouter().getOwner());
                        if (nInserted > 0) {
                            if (!BSU.isLocked())
                            {
                                BSU.setLocked(true);
                                ((ToggleButton)BSU.getGUI().getGadget(26)).setValue(true);
                            }
                            getItemRouter().reduceBuffer(nInserted);
                            return true;
                        }
                    }

                }

            }
            /*if (BSUEmpty != null) {
                int nToInsert = getItemRouter().getStackSize();
                ItemStack toInsert = getItemRouter().getBufferItem().clone();
                toInsert.setAmount(Math.min(nToInsert, toInsert.getAmount()));
                int nInserted = 0;
                nInserted = BSUEmpty.insertItems(toInsert, getFacing().getOppositeFace(), true, getItemRouter().getOwner());
                if (nInserted > 0) {
                    if (!BSUEmpty.isLocked())
                    {
                        BSUEmpty.setLocked(true);
                        ((ToggleButton)BSUEmpty.getGUI().getGadget(26)).setValue(true);
                    }
                    getItemRouter().reduceBuffer(nInserted);
                    return true;
                }
            }*/
        }
        return false;
    }

    private int vanillaSortInsertion(Block target, int amount, BlockFace side) {
        ItemStack buffer = getItemRouter().getBufferItem();
        int nInserted = VanillaInventoryUtils.vanillaInsertion(target, buffer, amount, side, true, getItemRouter().getOwner());
        if (nInserted > 0) {
            getItemRouter().setBufferItem(buffer.getAmount() == 0 ? null : buffer);
        }
        return nInserted;
    }

}
