package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.MiscUtil;
import me.mrCookieSlime.sensibletoolbox.api.STBInventoryHolder;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.blocks.machines.BigStorageUnit;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.DirectionalItemRouterModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.HyperSenderModule;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;

public class BHSUSenderModule extends DirectionalItemRouterModule {
    private static final Dye md = makeDye(DyeColor.BLUE);
    private Location linkedLoc;

    public BHSUSenderModule() {
    }

    public BHSUSenderModule(ConfigurationSection conf) {
        super(conf);
        if (conf.contains("linkedLoc")) {
            try {
                linkedLoc = MiscUtil.parseLocation(conf.getString("linkedLoc"));
            } catch (IllegalArgumentException e) {
                linkedLoc = null;
            }
        }
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        if (linkedLoc != null) {
            conf.set("linkedLoc", MiscUtil.formatLocation(linkedLoc));
        }
        return conf;
    }
    @Override
    public String getItemName() {
        return "I.R. Mod: (H)BSUSender";
    }

    @Override
    public String[] getLore() {
        return makeDirectionalLore("Fires Of Titan","Insert into an Item Router", "Sends items to linked BSU or HSU inventory");
    }

    @Override
    public Recipe getRecipe() {
        HyperSenderModule bm = new HyperSenderModule();
        registerCustomIngredients(bm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(Material.PISTON_BASE);
        return recipe;
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public void onInteractItem(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            // try to link up with a receiver module
            BigStorageUnit rtr = SensibleToolbox.getBlockAt(event.getClickedBlock().getLocation(), BigStorageUnit.class, true);
            if (rtr != null) {
                System.out.println("Linking");
                linkToRouter(rtr);
                event.getPlayer().setItemInHand(toItemStack(event.getPlayer().getItemInHand().getAmount()));
            } else {
                STBUtil.complain(event.getPlayer());
            }
            event.setCancelled(true);
        } else if (event.getPlayer().isSneaking() && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
            linkToRouter(null);
            event.getPlayer().setItemInHand(toItemStack(event.getPlayer().getItemInHand().getAmount()));
            event.setCancelled(true);
        } else if (event.getPlayer().getItemInHand().getAmount() == 1 &&
                (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            super.onInteractItem(event);
        }
    }

    public void linkToRouter(BigStorageUnit rtr) {
        linkedLoc = rtr == null ? null : rtr.getLocation();
    }

    @Override
    public String getDisplaySuffix() {
        return linkedLoc == null ? "[Not Linked]" : "[" + MiscUtil.formatLocation(linkedLoc) + "]";
    }
    @Override
    public boolean execute(Location loc) {
        if (linkedLoc !=null) {
            if (getItemRouter() != null && getItemRouter().getBufferItem() != null) {
                if (getFilter() != null && !getFilter().shouldPass(getItemRouter().getBufferItem())) return false;
                Debugger.getInstance().debug(2, "sender in " + getItemRouter() + " has: " + getItemRouter().getBufferItem());
                Block b = loc.getBlock();
                int nToInsert = getItemRouter().getStackSize();
                BaseSTBBlock stb = SensibleToolbox.getBlockAt(linkedLoc, true);
                if (stb instanceof STBInventoryHolder) {
                    if (creativeModeBlocked(stb, loc)) {
                        getItemRouter().ejectBuffer(getItemRouter().getFacing());
                        return false;
                    }
                    ItemStack toInsert = getItemRouter().getBufferItem().clone();
                    toInsert.setAmount(Math.min(nToInsert, toInsert.getAmount()));
                    int nInserted = ((STBInventoryHolder) stb).insertItems(toInsert, getFacing().getOppositeFace(), false, getItemRouter().getOwner());
                    getItemRouter().reduceBuffer(nInserted);
                    return nInserted > 0;
                }
            }
            return false;
        }
        return false;
    }

}
