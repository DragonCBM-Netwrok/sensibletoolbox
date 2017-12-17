package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.BlankModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.DirectionalItemRouterModule;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;

import java.util.ArrayList;
import java.util.List;

public class MoverModule extends DirectionalItemRouterModule {
    private static final Dye md = makeDye(DyeColor.WHITE);


    private List<String> moved = new ArrayList<String>();
    public MoverModule() {
    }

    public MoverModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Mover";
    }

    @Override
    public String[] getLore() {
        return makeDirectionalLore("Fires Of Titan","Insert into an Item Router", "Mover I.R. in the set direction.");
    }

    @Override
    public Recipe getRecipe() {
        BlankModule bm = new BlankModule();
        registerCustomIngredients(bm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(Material.PISTON_BASE);
        recipe.addIngredient(Material.MINECART);
        return recipe;
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public boolean execute(Location loc) {
        /*if (moved.size() > 2)
        {
            moved.remove(0);
        }
        Location targetLoc = getTargetLocation(loc);
        if (getItemRouter().isPendingRemoval()) return false;

        List<HumanEntity> viewers = getItemRouter().getGUI().getViewers();
        if (viewers != null)
        {
            if (viewers.size() > 0)
            {
                return false;
            }
        }

        if (targetLoc.getBlock().getType() == Material.AIR) {
            try {
                Bukkit.getScheduler().scheduleSyncDelayedTask(SlimefunStartup.instance, new Runnable() {
                    @Override
                    public void run() {
                        List<HumanEntity> viewers = getItemRouter().getGUI().getViewers();
                        if (viewers != null)
                        {
                            if (viewers.size() > 0)
                            {
                                return;
                            }
                        }
                        if (getItemRouter().isPendingRemoval()) return;
                        if (moved.contains(loc.toString())) return;
                        moved.add(loc.toString());
                        UUID tmpU = getItemRouter().getOwner();
                        BlockFace faceTmp = getFacing();
                        ItemRouter itTemp = new ItemRouter();

                        for (ItemRouter.ModuleAndAmount maa: getItemRouter().modules)
                        {
                            ItemRouter.ModuleAndAmount tempClone = maa.clone();
                            itTemp.modules.add(tempClone);
                        }
                    //    getItemRouter().modules.clear();
                        if (getItemRouter().getBufferItem() != null) {
                            itTemp.setBufferItem(getItemRouter().getBufferItem().clone());
                        }
                        itTemp.setStackSize(getItemRouter().getStackSize());
                        itTemp.setTickRate(getItemRouter().getTickRate());
                        getItemRouter().dontEject = true;
                        getItemRouter().breakBlock(false);
                        for (ItemRouter.ModuleAndAmount maa: itTemp.modules)
                        {
                            maa.module.setItemRouter(itTemp);
                        }

                        Block Changing = targetLoc.getBlock();
                        Changing.setType(Material.SKULL);
                        itTemp.placeBlock(Changing, tmpU, faceTmp, targetLoc);
                        itTemp.repaint(Changing);
                    }
                }, 5);


            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.severe(e.getMessage());
            }
        }
*/
        return true;
    }

}
