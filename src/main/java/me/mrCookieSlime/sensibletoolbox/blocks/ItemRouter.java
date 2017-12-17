package me.mrCookieSlime.sensibletoolbox.blocks;

import com.google.common.collect.Lists;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.STBInventoryHolder;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.gui.AccessControlGadget;
import me.mrCookieSlime.sensibletoolbox.api.gui.GUIUtil;
import me.mrCookieSlime.sensibletoolbox.api.gui.InventoryGUI;
import me.mrCookieSlime.sensibletoolbox.api.gui.RedstoneBehaviourGadget;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.util.BukkitSerialization;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.api.util.VanillaInventoryUtils;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.*;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan.AreaItemRouterModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan.PauseModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan.RotationModule;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

import java.awt.Color;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ItemRouter extends BaseSTBBlock implements STBInventoryHolder {
	
    private static final MaterialData texture = STBUtil.makeColouredMaterial(Material.STAINED_CLAY, DyeColor.BLUE);

    private static final int BUFFER_LABEL_SLOT = 12;
    private static final int BUFFER_ITEM_SLOT = 13;
    private static final int MODULE_LABEL_SLOT = 18;
    private static final int MOD_SLOT_START = 27;
    private static final int MOD_SLOT_END = 36;
    private static final int MOD_SLOT_COUNT = 9;

    public final List<ModuleAndAmount> modules = Lists.newArrayList();
    private ItemStack bufferItem;
    private int stackSize;
    private int tickRate;
    private boolean needToProcessModules = false;
    private boolean needToScanBufferSlot = false;
    private final List<BlockFace> neighbours = new ArrayList<BlockFace>();
    private boolean updateNeeded = false;
    private ReceiverModule receiver = null;
    public boolean dontEject = false;
    private ItemStack lastInserted = null;

    public ItemRouter() {
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTVlOGNjOTliYjQyZGRhMmFhZmJmZjQ1Nzc1Njc3NmIyOGM4ZTM0ZWUyNDVjYzU1M2QyNjk0ZTZiMDRiNzIifX19";
        bufferItem = null;
        setStackSize(1);
        setTickRate(20);
    }

    public ItemRouter(ConfigurationSection conf) {
        super(conf);
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTVlOGNjOTliYjQyZGRhMmFhZmJmZjQ1Nzc1Njc3NmIyOGM4ZTM0ZWUyNDVjYzU1M2QyNjk0ZTZiMDRiNzIifX19";
        setStackSize(1);
        setTickRate(20);
        if (conf.contains("modules")) {
            loadOldStyleModules(conf);
            updateNeeded = true;
        } else loadNewStyleModules(conf);
        try {
            if (conf.contains("buffer")) {
                Inventory inv = BukkitSerialization.fromBase64(conf.getString("buffer"));
                setBufferItem(inv.getItem(0));
            }
        } catch (IOException e) {
            LogUtils.warning(this + ": can't restore buffer item: " + e.getMessage());
        }
    }

    private void loadNewStyleModules(ConfigurationSection conf) {
        try {
            String enc = conf.getString("moduleList");
            if (enc != null && !enc.isEmpty()) {
                Inventory inv = BukkitSerialization.fromBase64(conf.getString("moduleList"));
                processModules(inv, 0);
            }
        } catch (IOException e) {
            LogUtils.warning(this + ": can't restore module list: " + e.getMessage());
        }
    }

    private void loadOldStyleModules(ConfigurationSection conf) {
        for (String l : conf.getStringList("modules")) {
            String[] f = l.split("::", 2);
            try {
                YamlConfiguration modConf = new YamlConfiguration();
                if (f.length > 1) modConf.loadFromString(f[1]);
                ItemRouterModule mod = (ItemRouterModule) SensibleToolbox.getItemRegistry().getItemById(f[0], modConf);
                insertModule(mod, modConf.getInt("amount"));
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.severe(e.getMessage());
                LogUtils.warning("can't restore saved module " + f[0] + " for " + this + ": " + e.getMessage());
            }
        }
    }

    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();

        if (getGUI() != null) {
            Inventory modInv = Bukkit.createInventory(null, 9);
            for (int i = 0; i < 9; i++) {
                modInv.setItem(i, getGUI().getItem(MOD_SLOT_START + i));
            }
            conf.set("moduleList", BukkitSerialization.toBase64(modInv));
        }
        else conf.set("moduleList", "");

        Inventory bufferInv = Bukkit.createInventory(null, 9);
        bufferInv.setItem(0, getBufferItem());
        conf.set("buffer", BukkitSerialization.toBase64(bufferInv, 1));
        return conf;
    }

    @Override
    public MaterialData getMaterialData() {
        return texture;
    }

    @Override
    public String getItemName() {
        return "Item Router";
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "Routes items.  Insert one or",
                "more Routing Modules to activate.",
                "R-click block:" + ChatColor.RESET + " configure router"
        };
    }

    @Override
    public String[] getExtraLore() {
        if (modules.isEmpty()) return new String[0];
        else {
            List<String> lore = Lists.newArrayListWithCapacity(modules.size());
            for (ModuleAndAmount e : modules) {
                String s = e.module.getDisplaySuffix() == null ? "" : ": " + e.module.getDisplaySuffix();
                lore.add(ChatColor.GREEN + e.module.getItemName() + s);
            }
            return lore.toArray(new String[modules.size()]);
        }
    }

    @Override
    public Recipe getRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(toItemStack(4));
        recipe.shape("RFR", "FLF", "RFR");
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('F', Material.IRON_FENCE);
        recipe.setIngredient('L', Material.LEVER);
        return recipe;
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setStackSize(int stackSize) {
        this.stackSize = Math.min(stackSize, 64);
    }

    public void setTickRate(int tickRate) {
        this.tickRate = Math.max(tickRate, 5);
    }

    @Override
    public int getTickRate() {
        return tickRate;
    }

    public ReceiverModule getReceiver() {
        return receiver;
    }

    @Override
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            updateBufferIndicator(true);
            getGUI().show(event.getPlayer());
            event.setCancelled(true);
        } 
        else if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().isSneaking() && getBufferItem() != null) {
            if (hasAccessRights(event.getPlayer())) ejectBuffer(event.getBlockFace());
            event.setCancelled(true);
        } 
        else super.onInteractBlock(event);
    }

    public void ejectBuffer(BlockFace face) {
        Block b = getLocation().getBlock().getRelative(face);
        b.getWorld().dropItemNaturally(b.getLocation(), getBufferItem());
        setBufferItem(null);
        update(false);
        b.getWorld().playSound(b.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
    }

    @Override
    protected InventoryGUI createGUI() {
        InventoryGUI gui = GUIUtil.createGUI(this, 36, ChatColor.DARK_RED + getItemName());

        gui.addLabel("Item Buffer", BUFFER_LABEL_SLOT, null, "Items can be extracted", "here, but not inserted.");
        gui.setSlotType(BUFFER_ITEM_SLOT, InventoryGUI.SlotType.ITEM);
        gui.setItem(BUFFER_ITEM_SLOT, getBufferItem());

        gui.addGadget(new RedstoneBehaviourGadget(gui, 8));
        gui.addGadget(new AccessControlGadget(gui, 17));

        for (int slot = MOD_SLOT_START; slot < MOD_SLOT_END; slot++) {
            gui.setSlotType(slot, InventoryGUI.SlotType.ITEM);
        }
        gui.addLabel("Item Router Modules", MODULE_LABEL_SLOT, null,
                "Insert one or more modules below",
                "When the router ticks, modules",
                "are executed in order, from left",
                "to right.");
        int slot = MOD_SLOT_START;
        for (ModuleAndAmount e : modules) {
            gui.getInventory().setItem(slot++, e.module.toItemStack(e.amount));
        }

        return gui;
    }

    @Override
    public void onBlockRegistered(Location loc, boolean isPlacing) {
        Bukkit.getScheduler().runTask(getProviderPlugin(), new Runnable() {
            @Override
            public void run() {
                findNeighbourInventories();
            }
        });
        
        if (updateNeeded) {
            update(false);
            updateNeeded = false;
        }
        super.onBlockRegistered(loc, isPlacing);
    }

    @Override
    public void onBlockUnregistered(Location loc) {
        // eject any items in the buffer and/or module slots
        if (!dontEject) {
            getGUI().ejectItems(BUFFER_ITEM_SLOT);
            setBufferItem(null);
            for (int modSlot = MOD_SLOT_START; modSlot < MOD_SLOT_END; modSlot++) {
                getGUI().ejectItems(modSlot);
            }

            clearModules();
        }

        super.onBlockUnregistered(loc);
    }

    @Override
    public void onServerTick() {
        boolean didSomeWork = false;
        if (needToProcessModules) {
            processModules(getGUI().getInventory(), MOD_SLOT_START);
            needToProcessModules = false;
        }
        if (needToScanBufferSlot) {
            bufferItem = getGUI().getItem(BUFFER_ITEM_SLOT);
            update(false);
            needToScanBufferSlot = false;
        }
        if (isRedstoneActive()) {
            Location loc = getLocation();
            for (ModuleAndAmount e : modules) {
                if (e.module instanceof AreaItemRouterModule) {
                    AreaItemRouterModule dmod = (AreaItemRouterModule) e.module;
                    if (dmod.execute(loc.clone())) {
                        didSomeWork = true;
                        if (dmod.isTerminator()) {
                            break;
                        }
                    }
                }
                if (e.module instanceof DirectionalItemRouterModule) {
                    DirectionalItemRouterModule dmod = (DirectionalItemRouterModule) e.module;
                    if (dmod.execute(loc.clone())) {
                        didSomeWork = true;
                        if (dmod.isTerminator()) {
                            break;
                        }
                    }
                }

            }
            if (didSomeWork) {
                update(false);
                playParticles(new Color(0, 0, 255));
            }
        }


        super.onServerTick();
    }

    @Override
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Bukkit.getScheduler().runTask(getProviderPlugin(), new Runnable() {
            @Override
            public void run() {
                findNeighbourInventories();
            }
        });
    }

    private void findNeighbourInventories() {
        neighbours.clear();
        Location loc = getLocation();
        if (loc == null) return;
        Block b = loc.getBlock();
        for (BlockFace face : STBUtil.directFaces) {
            Block b1 = b.getRelative(face);
            BaseSTBBlock stb = SensibleToolbox.getBlockAt(b1.getLocation());
            if (stb instanceof STBInventoryHolder) neighbours.add(face);
            else if (VanillaInventoryUtils.isVanillaInventory(b1)) neighbours.add(face);
        }
    }

    public void playParticles(Color color) {
//    	try {
//        	Location l = getLocation().add(0.6, 1, 0.3);
//			ParticleEffect.REDSTONE.displayColoredParticle(l, color);
//			l = getLocation().add(1.6, 1, 0.1);
//			ParticleEffect.REDSTONE.displayColoredParticle(l, color);
//			l = getLocation().add(0.6, 0.5, -0.2);
//			ParticleEffect.REDSTONE.displayColoredParticle(l, color);
//			l = getLocation().add(0.4, 0.8, 0.6);
//			ParticleEffect.REDSTONE.displayColoredParticle(l, color);
//			l = getLocation().add(0.3, 0.6, 1.6);
//			ParticleEffect.REDSTONE.displayColoredParticle(l, color);
//			l = getLocation().add(-0.2, 0.3, 0.6);
//			ParticleEffect.REDSTONE.displayColoredParticle(l, color);
//			l = getLocation().add(1.6, 0.7, 0.3);
//			ParticleEffect.REDSTONE.displayColoredParticle(l, color);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
    }

    public void clearModules() {
        modules.clear();
        setStackSize(1);
        setTickRate(20);
        receiver = null;
    }

    private void insertModule(ItemRouterModule module, int count) {
        module.setItemRouter(this);

        if (module instanceof DirectionalItemRouterModule) {
            DirectionalItemRouterModule dm = (DirectionalItemRouterModule) module;
            if (dm.getFacing() == null) {
                dm.setFacingDirection(BlockFace.SELF);
            }
        } else if (module instanceof StackModule) {
            setStackSize(getStackSize() * (int) Math.pow(2, count));
        } else if (module instanceof SpeedModule) {
            setTickRate(getTickRate() - 5 * count);
        } else if (module instanceof PauseModule) {
            setTickRate(getTickRate() + 7 * count);
        } else if (module instanceof ReceiverModule) {
            receiver = (ReceiverModule) module;
        }
        modules.add(new ModuleAndAmount(module, count));
    }

    public ItemStack getBufferItem() {
        return bufferItem == null ? null : bufferItem.clone();
    }

    private void updateBufferIndicator(boolean force) {
        if (getGUI() != null && (getGUI().getViewers().size() > 0 || force)) {
            getGUI().getInventory().setItem(BUFFER_ITEM_SLOT, bufferItem);
        }
    }

    public void setBufferItem(ItemStack bufferItem) {
        this.bufferItem = bufferItem;
        if (bufferItem != null && (bufferItem.getAmount() == 0 || bufferItem.getType() == Material.AIR)) {
            this.bufferItem = null;
        }
        updateBufferIndicator(false);
    }

    public void setBufferAmount(int newAmount) {
        if (newAmount == bufferItem.getAmount()) {
            return;
        }
        if (newAmount <= 0) {
            setBufferItem(null);
        } else {
            bufferItem.setAmount(newAmount);
            updateBufferIndicator(false);
        }
    }

    public int reduceBuffer(int amount) {
        if (bufferItem != null && amount > 0) {
            amount = Math.min(amount, bufferItem.getAmount());
            setBufferAmount(bufferItem.getAmount() - amount);
            return amount;
        } else {
            return 0;
        }
    }
    public RotationModule getRoutertionAmount() {
        try {
            for (ModuleAndAmount e : modules) {
                if (e.module instanceof RotationModule) {
                    //noinspection unchecked
                    return (RotationModule)e.module;
                }
            }
            return null;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    public int insertItems(ItemStack item, BlockFace face, boolean sorting, UUID uuid) {
        if (!hasAccessRights(uuid)) return 0;
        int amountToAccept = 0;
        // item routers don't care about sorters - they will take items from them happily
        RotationModule RMod = this.getRoutertionAmount();
        if (RMod != null)
        {
            if (RMod.getFilter().listFiltered().size() == 0) {
                if (SensibleToolboxPlugin.isItemSimiliar(lastInserted, item)) {
                    return 0;
                }
            }
            else
            {
                List<ItemStack> listF = RMod.getFilter().listFiltered();
                for(int i=0;i < listF.size(); i++)
                {
                    if (SensibleToolboxPlugin.isItemSimiliar(lastInserted, listF.get(i)))
                    {
                        ItemStack nextItem = null;
                        if (i + 1 < listF.size())
                        {
                            if (listF.get(i + 1) != null) {
                                nextItem = listF.get(i + 1);
                            }
                            else
                            {
                                nextItem = listF.get(0);
                            }
                        }
                        else
                        {
                            nextItem = listF.get(0);
                        }
                        if (!SensibleToolboxPlugin.isItemSimiliar(nextItem, item))
                        {
                            return 0;
                        }
                    }
                }
            }

        }

        if (bufferItem == null) {
            setBufferItem(item.clone());
            amountToAccept = item.getAmount();
        } 
        else if (item.isSimilar(bufferItem)) {
            int nInserted = Math.min(item.getAmount(), item.getType().getMaxStackSize() - bufferItem.getAmount());
            setBufferAmount(bufferItem.getAmount() + nInserted);
            amountToAccept = nInserted;
        }

        if (amountToAccept > 0)
        {
            lastInserted = item.clone();
        }
        return amountToAccept;
    }

    @Override
    public ItemStack extractItems(BlockFace face, ItemStack receiver, int amount, UUID uuid) {
        if (!hasAccessRights(uuid) || bufferItem == null) return null;
        else if (receiver == null) {
            ItemStack returned = bufferItem.clone();
            int nExtracted = Math.min(amount, bufferItem.getAmount());
            returned.setAmount(nExtracted);
            setBufferAmount(bufferItem.getAmount() - nExtracted);
            return returned;
        } 
        else if (receiver.isSimilar(bufferItem)) {
            int nExtracted = Math.min(amount, bufferItem.getAmount());
            nExtracted = Math.min(nExtracted, receiver.getMaxStackSize() - receiver.getAmount());
            receiver.setAmount(receiver.getAmount() + nExtracted);
            setBufferAmount(bufferItem.getAmount() - nExtracted);
            return receiver;
        } 
        else return null;
    }

    @Override
    public Inventory showOutputItems(UUID uuid) {
        if (hasAccessRights(uuid)) {
            Inventory inv = Bukkit.createInventory(this, 9);
            inv.setItem(0, getBufferItem());
            return inv;
        } 
        else return null;
    }

    @Override
    public void updateOutputItems(UUID uuid, Inventory inventory) {
        if (hasAccessRights(uuid)) setBufferItem(inventory.getItem(0));
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    @Override
    public boolean onSlotClick(HumanEntity player, int slot, ClickType click, ItemStack inSlot, ItemStack onCursor) {
        if (slot == BUFFER_ITEM_SLOT) {
            return false;
            //if (inSlot == null || onCursor.getType() != Material.AIR) return false;
            //needToScanBufferSlot = true;
            //return true;
        } 
        else if (slot >= MOD_SLOT_START && slot < MOD_SLOT_END) {
            if (onCursor.getType() == Material.AIR || SensibleToolbox.getItemRegistry().isSTBItem(onCursor, ItemRouterModule.class)) {
                needToProcessModules = true;
                return true;
            } 
            else return false;
        } 
        else return false;
    }

    @Override
    public boolean onPlayerInventoryClick(HumanEntity player, int slot, ClickType click, ItemStack inSlot, ItemStack onCursor) {
        return true;
    }

    @Override
    public int onShiftClickInsert(HumanEntity player, int slot, ItemStack toInsert) {
        if (!SensibleToolbox.getItemRegistry().isSTBItem(toInsert, ItemRouterModule.class)) return 0;
        int nInserted = 0;
        for (int modSlot = MOD_SLOT_START; modSlot < MOD_SLOT_END; modSlot++) {
            ItemStack mod = getGUI().getInventory().getItem(modSlot);
            if (mod == null) {
                getGUI().getInventory().setItem(modSlot, toInsert);
                nInserted = toInsert.getAmount();
            } 
            else if (mod.isSimilar(toInsert)) {
                nInserted = mod.getType().getMaxStackSize() - mod.getAmount();
                nInserted = Math.min(toInsert.getAmount(), nInserted);
                mod.setAmount(mod.getAmount() + nInserted);
                getGUI().getInventory().setItem(modSlot, mod);
            }
            if (nInserted > 0) break;
        }
        if (nInserted > 0) needToProcessModules = true;
        return nInserted;
    }

    @Override
    public boolean onShiftClickExtract(HumanEntity player, int slot, ItemStack toExtract) {
        if (slot == BUFFER_ITEM_SLOT && getBufferItem() != null) {
            //needToScanBufferSlot = true;
            return false;
        } 
        else if (slot >= MOD_SLOT_START && slot < MOD_SLOT_END) {
            needToProcessModules = true;
            return true;
        } 
        else return false;
    }

    @Override
    public boolean onClickOutside(HumanEntity player) {
        return false;
    }

    @Override
    public void onGUIClosed(HumanEntity player) {
        // no action needed here
    }

    private void processModules(Inventory inv, int baseSlot) {
        clearModules();

        Map<ItemStack, Integer> mods = new LinkedHashMap<ItemStack, Integer>();
        for (int i = 0; i < MOD_SLOT_COUNT; i++) {
            ItemStack stack = inv.getItem(baseSlot + i);
            if (stack != null) {
                if (!mods.containsKey(stack)) mods.put(stack, stack.getAmount());
                else mods.put(stack, mods.get(stack) + stack.getAmount());
            }
        }

        for (Map.Entry<ItemStack, Integer> entry : mods.entrySet()) {
            ItemRouterModule mod = SensibleToolbox.getItemRegistry().fromItemStack(entry.getKey(), ItemRouterModule.class);
            if (mod != null) insertModule(mod, entry.getValue());
        }

        Debugger.getInstance().debug("re-processed modules for " + this + " tick-rate=" + getTickRate() + " stack-size=" + getStackSize());
        if (getTicksLived() > 20) update(false);


    }

    public List<BlockFace> getNeighbours() {
        return neighbours;
    }

    public class ModuleAndAmount {
        public  final ItemRouterModule module;
        public final int amount;

        private ModuleAndAmount(ItemRouterModule module, int amount) {
            this.module = module;
            this.amount = amount;
        }
        public ModuleAndAmount clone()
        {
            return new ItemRouter.ModuleAndAmount(module, amount);
        }
    }
}
