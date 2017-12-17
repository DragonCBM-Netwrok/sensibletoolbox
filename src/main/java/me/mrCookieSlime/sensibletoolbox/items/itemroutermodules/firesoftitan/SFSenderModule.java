package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.desht.dhutils.LogUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.Item.CustomItem;
import me.mrCookieSlime.Slimefun.Setup.SlimefunManager;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.UniversalBlockMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.DirectionalItemRouterModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.SenderModule;
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

public class SFSenderModule extends DirectionalItemRouterModule {
    private static final Dye md = makeDye(DyeColor.BLUE);

    public SFSenderModule() {
    }

    public SFSenderModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Slimefun Sender";
    }

    @Override
    public String[] getLore() {
        return makeDirectionalLore("Fires Of Titan","Insert into an Item Router", "Pulls items from an adjacent slimefun inventory");
    }

    @Override
    public Recipe getRecipe() {;
        SenderModule sm = new SenderModule();
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
        if (getItemRouter() != null && getItemRouter().getBufferItem() != null) {
            if (getFilter() != null && !getFilter().shouldPass(getItemRouter().getBufferItem())) return false;


            Location targetLoc = getTargetLocation(loc);
            try {

                ItemStack tmps = inBuffer.clone();
                int StackSize = Math.min(inBuffer.getAmount(), getItemRouter().getStackSize());
                tmps.setAmount(StackSize);
                int amountSent = insert(BlockStorage.getStorage(loc.getWorld()), targetLoc.getBlock(), tmps);
                if (amountSent > 0) {
                    inBuffer.setAmount(inBuffer.getAmount() - amountSent);
                    if (inBuffer.getAmount() <= 0) {
                        inBuffer = null;
                    }
                    getItemRouter().setBufferItem(inBuffer);
                }


            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.severe(e.getMessage());
            }
        }
        return false;
    }
    public int insert(BlockStorage storage, Block target, ItemStack stack) {
        if (storage.hasUniversalInventory(target)) {
            UniversalBlockMenu menu = storage.getUniversalInventory(target);
            for (int slot: menu.getPreset().getSlotsAccessedByItemTransport(menu, ItemTransportFlow.INSERT, stack)) {
                ItemStack is = menu.getItemInSlot(slot) == null ? null: menu.getItemInSlot(slot).clone();
                if (is == null) {
                    menu.replaceExistingItem(slot, stack.clone());
                    return stack.getAmount();
                }
                else if (SlimefunManager.isItemSimiliar(new CustomItem(is, 1), new CustomItem(stack, 1), true, SlimefunManager.DataType.ALWAYS) && is.getAmount() < is.getType().getMaxStackSize()) {
                    int amount = is.getAmount() + stack.getAmount();

                    if (amount > is.getType().getMaxStackSize()) {;
                        int amount2 = is.getType().getMaxStackSize() - is.getAmount();
                        if (amount2 < 1) {
                            continue;
                        }
                        else
                        {
                            is.setAmount(is.getType().getMaxStackSize());
                            menu.replaceExistingItem(slot, is);
                            return amount2;
                        }
                    }
                    else {
                        is.setAmount(amount);
                    }
                    menu.replaceExistingItem(slot, is);
                    return stack.getAmount();
                }
            }
        }
        else if (storage.hasInventory(target.getLocation())) {
            BlockMenu menu = BlockStorage.getInventory(target.getLocation());
            for (int slot: menu.getPreset().getSlotsAccessedByItemTransport(menu, ItemTransportFlow.INSERT, stack)) {
                ItemStack is = menu.getItemInSlot(slot) == null ? null: menu.getItemInSlot(slot).clone();
                if (is == null) {
                    menu.replaceExistingItem(slot, stack.clone());
                    return stack.getAmount();
                }
                else if (SlimefunManager.isItemSimiliar(new CustomItem(is, 1), new CustomItem(stack, 1), true, SlimefunManager.DataType.ALWAYS) && is.getAmount() < is.getType().getMaxStackSize()) {
                    int amount = is.getAmount() + stack.getAmount();
                    if (amount > is.getType().getMaxStackSize()) {;
                        int amount2 = is.getType().getMaxStackSize() - is.getAmount();
                        if (amount2 < 1) {
                            continue;
                        }
                        else
                        {
                            is.setAmount(is.getType().getMaxStackSize());
                            menu.replaceExistingItem(slot, is);
                            return amount2;
                        }
                    }
                    else {
                        is.setAmount(amount);
                    }
                    menu.replaceExistingItem(slot, is);
                    return stack.getAmount();
                }
            }
        }
        return 0;
    }
}
