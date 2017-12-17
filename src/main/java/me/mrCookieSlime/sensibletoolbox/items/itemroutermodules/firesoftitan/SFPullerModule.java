package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.desht.dhutils.LogUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.UniversalBlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.DirectionalItemRouterModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.PullerModule;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;

public class SFPullerModule extends DirectionalItemRouterModule {
    private static final Dye md = makeDye(DyeColor.LIME);

    public SFPullerModule() {
    }

    public SFPullerModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Slimefun Puller";
    }

    @Override
    public String[] getLore() {
        return makeDirectionalLore("Fires Of Titan","Insert into an Item Router", "Pulls items from an adjacent slimefun inventory");
    }

    @Override
    public Recipe getRecipe() {
        PullerModule sm = new PullerModule();
        registerCustomIngredients(sm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(sm.getMaterialData());
        recipe.addIngredient(Material.STRING);
        return recipe;
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public boolean execute(Location loc) {

        ItemStack inBuffer = getItemRouter().getBufferItem();
        if (inBuffer != null && inBuffer.getAmount() >= inBuffer.getType().getMaxStackSize()) {
            return false;
        }
        Location targetLoc = getTargetLocation(loc);
        try {

            ItemStack update = withdraw(BlockStorage.getStorage(targetLoc.getWorld()),targetLoc.getBlock(), inBuffer);
            if (update != null) {
                if (inBuffer == null) {
                    getItemRouter().setBufferItem(update);
                } else {
                    update.setAmount(inBuffer.getAmount() + 1);
                    getItemRouter().setBufferItem(update);
                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            LogUtils.severe(e.getMessage());
        }

        return false;
    }
    public ItemStack withdraw(BlockStorage storage, Block target, ItemStack inBuffer ) {
        ItemStack C2 = null;
        if (inBuffer != null) {
            C2 = inBuffer.clone();
            C2.setAmount(1);
        }

        if (storage.hasUniversalInventory(target)) {;

            UniversalBlockMenu menu = storage.getUniversalInventory(target);
            for (int slot: menu.getPreset().getSlotsAccessedByItemTransport(menu, ItemTransportFlow.WITHDRAW, null)) {
                final ItemStack is = menu.getItemInSlot(slot);
                if (is != null ) {
                    int StackSize = Math.min(is.getAmount(), getItemRouter().getStackSize());
                    ItemStack C1 = null;

                    C1 = is.clone();
                    C1.setAmount(1);


                    if (inBuffer == null || SensibleToolboxPlugin.isItemSimiliar(C1, C2)) {
                        ItemStack out = is.clone();
                        out.setAmount(StackSize);
                        if (this.getFilter().shouldPass(out)) {
                            if (is.getAmount() > StackSize) {
                                menu.replaceExistingItem(slot, new CustomItem(is, is.getAmount() - StackSize));
                                return out;
                            } else {
                                menu.replaceExistingItem(slot, null);
                                return out;
                            }
                        }
                    }
                }

            }
        }
        else if (storage.hasInventory(target.getLocation())) {
            BlockMenu menu = BlockStorage.getInventory(target.getLocation());
            for (int slot: menu.getPreset().getSlotsAccessedByItemTransport(menu, ItemTransportFlow.WITHDRAW, null)) {
                final ItemStack is = menu.getItemInSlot(slot);
                if (is != null ) {
                    int StackSize = Math.min(is.getAmount(), getItemRouter().getStackSize());
                    ItemStack C1 = null;
                    C1 = is.clone();
                    C1.setAmount(1);

                    if (inBuffer == null || SensibleToolboxPlugin.isItemSimiliar(C1, C2)) {
                        ItemStack out = is.clone();
                        out.setAmount(StackSize);
                        if (this.getFilter().shouldPass(out)) {
                            if (is.getAmount() > StackSize) {
                                menu.replaceExistingItem(slot, new CustomItem(is, is.getAmount() - StackSize));
                                return out;
                            } else {
                                menu.replaceExistingItem(slot, null);
                                return out;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
