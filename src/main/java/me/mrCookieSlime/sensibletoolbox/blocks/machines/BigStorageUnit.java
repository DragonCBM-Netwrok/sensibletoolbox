package me.mrCookieSlime.sensibletoolbox.blocks.machines;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.mrCookieSlime.CSCoreLibPlugin.CSCoreLib;
import me.mrCookieSlime.CSCoreLibPlugin.general.String.StringUtils;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.gui.GUIUtil;
import me.mrCookieSlime.sensibletoolbox.api.gui.InventoryGUI;
import me.mrCookieSlime.sensibletoolbox.api.gui.ToggleButton;
import me.mrCookieSlime.sensibletoolbox.api.items.AbstractProcessingMachine;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBItem;
import me.mrCookieSlime.sensibletoolbox.api.util.BukkitSerialization;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.attributes.AttributeStorage;
import me.mrCookieSlime.sensibletoolbox.attributes.NbtFactory;
import me.mrCookieSlime.sensibletoolbox.core.STBItemRegistry;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BigStorageUnit extends AbstractProcessingMachine {

    private static final ItemStack LOCKED_BUTTON = GUIUtil.makeTexture(new MaterialData(Material.EYE_OF_ENDER), ChatColor.UNDERLINE + "Locked", "Unit will remember its", "stored item, even when", "emptied");
    private static final ItemStack UNLOCKED_BUTTON = GUIUtil.makeTexture(new MaterialData(Material.ENDER_PEARL), ChatColor.UNDERLINE + "Unlocked", "Unit will forget its stored", "item when emptied");
    private static final MaterialData md = STBUtil.makeLog(TreeSpecies.DARK_OAK);
    private static final String STB_LAST_BSU_INSERT = "STB_Last_BSU_Insert";
    private static final long DOUBLE_CLICK_TIME = 250L;
    private ItemStack stored;
    private int storageAmount;
    private int outputAmount;
    private int maxCapacity;
    private final String signLabel[] = new String[4];
    private int oldTotalAmount = -1;
    private boolean locked;
    private Hologram hologram;
    private long hologramTimer =0;
    private boolean hologramItem = false;
    private String LastName = "";
    public BigStorageUnit() {
        super();
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODkzNzVkODA1MjkxYmFjN2QwNDVjNWMwNDQ4YWI3ZjU1OTlmYTZkOWE4Y2FlYWU0YmQyNTFkNTg2ZTJjOCJ9fX0=";
        locked = false;
        setStoredItemType(null);
        oldTotalAmount = storageAmount = outputAmount = 0;

    }

    public BigStorageUnit(ConfigurationSection conf) {
        super(conf);
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODkzNzVkODA1MjkxYmFjN2QwNDVjNWMwNDQ4YWI3ZjU1OTlmYTZkOWE4Y2FlYWU0YmQyNTFkNTg2ZTJjOCJ9fX0=";
        try {
            Inventory inv = BukkitSerialization.fromBase64(conf.getString("stored"));
            setStoredItemType(inv.getItem(0));
        } catch (IOException e) {
            e.printStackTrace();
            LogUtils.severe(e.getMessage());
        }
        setStorageAmount(conf.getInt("amount"));
        locked = conf.getBoolean("locked", false);
        oldTotalAmount = getStorageAmount();
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        Inventory inv = Bukkit.createInventory(null, 9);
        inv.setItem(0, stored);
        conf.set("stored", BukkitSerialization.toBase64(inv, 1));
        conf.set("amount", storageAmount);
        conf.set("locked", locked);
        return conf;
    }

    public void setStorageAmount(int storageAmount) {
        this.storageAmount = Math.max(0, storageAmount);
    }

    public int getStorageAmount() {
        return storageAmount;
    }

    public int getOutputAmount() {
        return outputAmount;
    }

    public void setOutputAmount(int outputAmount) {
        this.outputAmount = outputAmount;
    }

    public int getTotalAmount() {
        return getStorageAmount() + getOutputAmount();
    }

    public ItemStack getStoredItemType() {
        return stored;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        updateSignQuantityLine();
        if (getTotalAmount() == 0 && !isLocked()) {
            setStoredItemType(null);
        }
        updateAttachedLabelSigns();
    }

    public void setStoredItemType(ItemStack stored) {
        Debugger.getInstance().debug(this + " set stored item = " + stored);
        if (stored != null) {
            this.stored = stored.clone();
            this.stored.setAmount(1);
        } else if (!isLocked()) {
            this.stored = null;
        }
        maxCapacity = getStackCapacity() * (this.stored == null ? 64 : this.stored.getMaxStackSize());
        updateSignItemLines();
    }

    private void updateSignQuantityLine() {
        if (isLocked()) signLabel[1] = ChatColor.DARK_RED + Integer.toString(getTotalAmount());
        else signLabel[1] = getTotalAmount() > 0 ? Integer.toString(getTotalAmount()) : "";
    }

    private void updateSignItemLines() {
        if (this.stored != null) {
            String[] lines = WordUtils.wrap(StringUtils.formatItemName(this.stored, false), 15).split("\\n");
            signLabel[2] = lines[0];
            String pfx = lines[0].startsWith("\u00a7") ? lines[0].substring(0, 2) : "";
            if (lines.length > 1) signLabel[3] = pfx + lines[1];
        } else {
            signLabel[2] = ChatColor.ITALIC + "Empty";
            signLabel[3] = "";
        }
    }

    @Override
    public int[] getInputSlots() {
        return new int[]{10};
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{14};
    }

    @Override
    public int[] getUpgradeSlots() {
        // no upgrades at this time (maybe in future)
        return new int[0];
    }

    @Override
    public int getUpgradeLabelSlot() {
        return -1;
    }

    @Override
    public int getInventoryGUISize() {
        return 36;
    }

    @Override
    protected void playActiveParticleEffect() {
        // nothing
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "BSU";
    }

    @Override
    public String[] getLore() {
        return new String[]{"Big Storage Unit", "Stores up to " + getStackCapacity() + " stacks", "of a single item type"};
    }

    @Override
    public String[] getExtraLore() {
        if (isLocked() && getStoredItemType() != null) {
            return new String[]{ChatColor.WHITE + "Locked: " + ChatColor.YELLOW + StringUtils.formatItemName(getStoredItemType(), false)};
        } else {
            return new String[0];
        }
    }

    public List<String> getTrasferLore(HyperStorageUnit toTrasfer) {
        String[] lore = toTrasfer.getLore();
        String[] blore2 = super.getExtraLore();
        if (getTotalAmount() > 0) {
            String[] lore2 = Arrays.copyOf(blore2, blore2.length + 1);
            lore2[lore2.length - 1] = ChatColor.WHITE + "Stored: " + ChatColor.YELLOW + getTotalAmount() + " " + StringUtils.formatItemName(getStoredItemType(), false);
            blore2 = lore2.clone();
        }
        List<String> res = new ArrayList<String>(lore.length + blore2.length + 1);
        res.add(STBItemRegistry.LORE_PREFIX + getProviderPlugin().getName() + " (STB) item");

        for (String l : lore) {
            res.add(LORE_COLOR + l);
        }
        for (String l : blore2) {
            res.add(LORE_COLOR + l);
        }
        return res;
    }

    @Override
    public Recipe getRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("LSL", "L L", "LLL");
        recipe.setIngredient('L', STBUtil.makeWildCardMaterialData(Material.LOG));
        recipe.setIngredient('S', STBUtil.makeWildCardMaterialData(Material.WOOD_STEP));
        return recipe;
    }

    @Override
    public Recipe[] getExtraRecipes() {
        ShapedRecipe recipe = new ShapedRecipe(toItemStack());
        recipe.shape("LSL", "L L", "LLL");
        recipe.setIngredient('L', STBUtil.makeWildCardMaterialData(Material.LOG_2));
        recipe.setIngredient('S', STBUtil.makeWildCardMaterialData(Material.WOOD_STEP));
        return new Recipe[]{recipe};
    }

    @Override
    public String getCraftingNotes() {
        return "Any type of log or slab may be used";
    }

    @Override
    public boolean acceptsEnergy(BlockFace face) {
        return false;
    }

    @Override
    public boolean suppliesEnergy(BlockFace face) {
        return false;
    }

    @Override
    public int getMaxCharge() {
        return 0;
    }

    @Override
    public int getChargeRate() {
        return 0;
    }

    public int getStackCapacity() {
        return 128;
    }

    @Override
    public int getTickRate() {
        return 5;
    }

    @Override
    protected InventoryGUI createGUI() {
        InventoryGUI gui = super.createGUI();

        gui.addGadget(new ToggleButton(gui, 26, isLocked(), LOCKED_BUTTON, UNLOCKED_BUTTON, new ToggleButton.ToggleListener() {
            @Override
            public boolean run(boolean newValue) {
                setLocked(newValue);
                return true;
            }
        }));
        return gui;
    }

    @Override
    public void onServerTick() {

        // 1. move items from input to storage
        int inputSlot = getInputSlots()[0];
        ItemStack stackIn = getInventoryItem(inputSlot);
        if (stackIn != null && (stored == null || stackIn.isSimilar(stored) && !isFull())) {
            double chargeNeeded = getChargePerOperation(stackIn.getAmount());
            if (getCharge() >= chargeNeeded) {
                if (stored == null) {
                    setStoredItemType(stackIn);
                }
                int toPull = Math.min(stackIn.getAmount(), maxCapacity - getStorageAmount());
                setStorageAmount(getStorageAmount() + toPull);
                stackIn.setAmount(stackIn.getAmount() - toPull);
                setInventoryItem(inputSlot, stackIn);
                setCharge(getCharge() - chargeNeeded);
                if (stackIn.getAmount() == 0) {
                    // workaround to avoid leaving ghost items in the input slot
                    STBUtil.forceInventoryRefresh(getInventory());
                }
            }
        }

        ItemStack stackOut = getOutputItem();
        int newAmount = stackOut == null ? 0 : stackOut.getAmount();
        if (getOutputAmount() != newAmount) {
            setOutputAmount(newAmount);
        }

        // 2. top up the output stack from storage
        if (stored != null) {
            int toPush = Math.min(getStorageAmount(), stored.getMaxStackSize() - getOutputAmount());
            if (toPush > 0) {
                if (stackOut == null) {
                    stackOut = stored.clone();
                    stackOut.setAmount(toPush);
                } else {
                    stackOut.setAmount(stackOut.getAmount() + toPush);
                }
                setOutputItem(stackOut);
                setOutputAmount(stackOut.getAmount());
                setStorageAmount(getStorageAmount() - toPush);
            }
        }

        // 3. perform any necessary updates if storage has changed
        if (getTotalAmount() != oldTotalAmount) {
            updateSignQuantityLine();
            if (getTotalAmount() == 0) {
                setStoredItemType(null);
            }
            Debugger.getInstance().debug(2, this + " amount changed! " + oldTotalAmount + " -> " + getTotalAmount());
            getProgressMeter().setMaxProgress(maxCapacity);
            setProcessing(stored);
            setProgress(maxCapacity - getStorageAmount());
            update(false);
            updateAttachedLabelSigns();
            oldTotalAmount = getTotalAmount();
        }

        boolean HGoff = true;
        Player player = Bukkit.getPlayer(getOwner());
        if (player != null) {
            if (player.getLocation().getWorld().getName().equals(getLocation().getWorld().getName())) {
                if (player.getLocation().distance(getLocation()) < 7) {
                    HGoff = false;
                    if (hologram != null) {
                        setHGLine();
                    } else {
                        reloadHD();
                    }
                }
            }
        }
        if (HGoff)
        {
            if (this.hologram != null)
            {
                this.hologram.clearLines();
            }
            this.hologram = null;
        }
        super.onServerTick();
    }

    private void setHGLine() {
        String lines = ChatColor.GRAY +"Empty";
        if (getOutputItem() != null)
        {
            lines = StringUtils.formatItemName(getOutputItem(), false);
        }
        else if (getStoredItemType() != null)
        {
            lines = StringUtils.formatItemName(getStoredItemType(), false);
        }
        if (lines.length() > 7) {
            String What = ChatColor.getLastColors(lines);
            lines = ChatColor.stripColor(lines);
            String[] CheapFix = lines.split(" ");
            if (CheapFix.length > 1) {
                lines = What + "";
                for (String letter : CheapFix) {
                    lines = lines + letter.substring(0, 1) + ".";
                }
            } else {
                lines = CheapFix[0].substring(0, 5) + "";
            }
        }
        if (!this.LastName.equals(lines)) {
            this.LastName = lines;
            hologramItem = false;
        }


        if (hologramItem == false) {
            this.hologram.clearLines();
            hologramItem = true;
            this.hologram.appendTextLine(lines);
        }

        Player player = Bukkit.getPlayer(getOwner());
        if (player != null) {
            hologram.getVisibilityManager().showTo(player);
        }

    }

    protected void setOutputItem(ItemStack stackOut) {
        setInventoryItem(getOutputSlots()[0], stackOut);
    }
    @Override
    public void repaint(Block block) {
        super.repaint(block);
        reloadHD();
    }

    private void reloadHD() {
        if (this.hologram == null) {
            this.hologram = HologramsAPI.createHologram(SensibleToolboxPlugin.getInstance(), getLocation().add(0.5, 0.9, 0.5));
            this.hologram.clearLines();
            setHGLine();
            hologramItem = false;
        }
    }

    @Override
    public void onBlockUnregistered(Location location) {
        try {
            if (getProcessing() != null && dropsItemsOnBreak()) {
                // dump contents on floor (could make a big mess)
                Location current = getLocation();
                storageAmount = Math.min(4096, storageAmount);  // max 64 stacks will be dropped
                while (storageAmount > 0) {
                    ItemStack stack = stored.clone();
                    stack.setAmount(Math.min(storageAmount, stored.getMaxStackSize()));
                    current.getWorld().dropItemNaturally(current, stack);
                    storageAmount -= stored.getMaxStackSize();
                }
                setStoredItemType(null);
                setStorageAmount(0);
            }
            this.hologram.clearLines();
            super.onBlockUnregistered(location);
        }
        catch (Exception e)
        {

        }
    }

    @Override
    public void onBlockRegistered(Location location, boolean isPlacing) {
        getProgressMeter().setMaxProgress(maxCapacity);
        setProcessing(stored);
        setProgress(maxCapacity - storageAmount);
        ItemStack output = getOutputItem();
        outputAmount = output == null ? 0 : output.getAmount();
        oldTotalAmount += outputAmount;
        super.onBlockRegistered(location, isPlacing);

        reloadHD();
    }

    @Override
    public void onInteractBlock(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        BaseSTBItem stb = SensibleToolbox.getItemRegistry().fromItemStack(inHand);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && stb instanceof HyperStorageUnit) {
            if (((HyperStorageUnit)stb).getTotalAmount() == 0) {
                List<String> Lore = getTrasferLore((HyperStorageUnit) stb);
                ItemMeta meta = inHand.getItemMeta();
                meta.setLore(Lore);
                inHand.setItemMeta(meta);

                // any serialized data from the object goes in the ItemStack attributes
                YamlConfiguration conf = freeze();
                if (!isStackable()) {
                    // add a (hopefully) unique hidden field to ensure the item can't stack
                    conf.set("*nostack", System.nanoTime() ^ CSCoreLib.randomizer().nextLong());
                }
                conf.set("*TYPE", ((HyperStorageUnit) stb).getItemTypeID());
                AttributeStorage storage = AttributeStorage.newTarget(inHand, STBItemRegistry.STB_ATTRIBUTE_ID);
                String data = conf.saveToString();
                Debugger.getInstance().debug(3, "serialize " + this + " to itemstack:\n" + data);
                storage.setData(data);

                // needs MC 1.8
                inHand = NbtFactory.getCraftItemStack(storage.getTarget());
                NbtFactory.NbtCompound compound = NbtFactory.fromItemTag(inHand);
                compound.putPath("HideFlags", 63);

                inHand = storage.getTarget().clone();


                player.getInventory().setItemInMainHand(inHand.clone());
                setOutputAmount(0);
                setStorageAmount(0);
                setOutputItem(null);

            }
                event.setCancelled(true);


        }else if (event.getAction() == Action.LEFT_CLICK_BLOCK && getStoredItemType() != null &&
                hasAccessRights(player) && hasOKItem(player)) {
            // try to extract items from the output stack
            int wanted = player.isSneaking() ? 1 : getStoredItemType().getMaxStackSize();
            int nExtracted = Math.min(wanted, getOutputAmount());
            if (nExtracted > 0) {
                Location loc = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0.5, 0.5);
                ItemStack stack = getStoredItemType().clone();
                stack.setAmount(nExtracted);
                loc.getWorld().dropItem(loc, stack);
                setOutputAmount(getOutputAmount() - nExtracted);
                stack.setAmount(getOutputAmount());
                setOutputItem(stack);
            }
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !player.isSneaking() &&
                hasAccessRights(player)) {
            Long lastInsert = (Long) STBUtil.getMetadataValue(player, STB_LAST_BSU_INSERT);
            long now = System.currentTimeMillis();
            if (inHand.getType() == Material.AIR && lastInsert != null && now - lastInsert < DOUBLE_CLICK_TIME) {
                rightClickFullInsert(player);
                event.setCancelled(true);
            } else if (inHand.isSimilar(getStoredItemType())) {
                rightClickInsert(player, player.getInventory().getHeldItemSlot());
                event.setCancelled(true);
            } else {
                super.onInteractBlock(event);
            }
        } else {
            super.onInteractBlock(event);
        }
    }

    private boolean hasOKItem(Player player) {
        switch (player.getItemInHand().getType()) {
            case SIGN:
            case WOOD_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case DIAMOND_AXE:
            case WOOD_PICKAXE:
            case STONE_PICKAXE:
            case IRON_PICKAXE:
            case DIAMOND_PICKAXE:
                return false;
            default:
                return true;
        }
    }

    @SuppressWarnings("deprecation")
    private void rightClickFullInsert(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack != null && stack.isSimilar(getStoredItemType()) && rightClickInsert(player, slot) == 0) {
                break;
            }
        }
        player.updateInventory();
    }

    private int rightClickInsert(Player player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        int toInsert = Math.min(stack.getAmount(), maxCapacity - getStorageAmount());
        if (toInsert == 0) {
            STBUtil.complain(player, getItemName() + " is full.");
            return 0;
        }
        double chargeNeeded = getChargePerOperation(toInsert);
        if (getCharge() >= chargeNeeded) {
            setStorageAmount(getStorageAmount() + toInsert);
            if (getStoredItemType() == null) {
                setStoredItemType(stack);
            }
            stack.setAmount(stack.getAmount() - toInsert);
            player.getInventory().setItem(slot, stack.getAmount() == 0 ? null : stack);
            setCharge(getCharge() - chargeNeeded);
            player.setMetadata(STB_LAST_BSU_INSERT, new FixedMetadataValue(getProviderPlugin(), System.currentTimeMillis()));
            return toInsert;
        } else {
            STBUtil.complain(player, getItemName() + " has insufficient charge to accept items.");
            return 0;
        }
    }

    protected boolean dropsItemsOnBreak() {
        return true;
    }

    @Override
    public int getProgressItemSlot() {
        return 12;
    }

    @Override
    public int getProgressCounterSlot() {
        return 3;
    }

    @Override
    public ItemStack getProgressIcon() {
        return new ItemStack(Material.DIAMOND_CHESTPLATE);
    }

    public boolean isFull() {
        return stored != null && storageAmount >= getStackCapacity() * stored.getMaxStackSize();
    }

    @Override
    public String getProgressMessage() {
        return ChatColor.YELLOW + "In Storage: " + getStorageAmount() + "/" + maxCapacity;
    }

    @Override
    public String[] getProgressLore() {
        return new String[] { "Total: " + (getStorageAmount() + getOutputAmount()) };
    }

    @Override
    public boolean acceptsItemType(ItemStack stack) {
        return stored == null || stored.isSimilar(stack);
    }

    @Override
    protected String[] getSignLabel(BlockFace face) {
        String[] label = super.getSignLabel(face);
        System.arraycopy(signLabel, 1, label, 1, 3);
        return label;
    }

    public ItemStack getOutputItem() {
        return getInventoryItem(getOutputSlots()[0]);
    }

    @Override
    public int insertItems(ItemStack item, BlockFace face, boolean sorting, UUID uuid) {
        if (!hasAccessRights(uuid)) {
            return 0;
        }
        double chargeNeeded = getChargePerOperation(item.getAmount());
        if (!isRedstoneActive() || getCharge() < chargeNeeded) {
            return 0;
        } else if (stored == null) {
            setStoredItemType(item);
            setStorageAmount(item.getAmount());
            setCharge(getCharge() - chargeNeeded);
            return item.getAmount();
        } else if (item.isSimilar(stored)) {
            int toInsert = Math.min(item.getAmount(), maxCapacity - getStorageAmount());
            setStorageAmount(getStorageAmount() + toInsert);
            setCharge(getCharge() - chargeNeeded);
            return toInsert;
        } else {
            return 0;
        }
    }

    @Override
    public ItemStack extractItems(BlockFace face, ItemStack receiver, int amount, UUID uuid) {
        if (!hasAccessRights(uuid)) {
            return null;
        }
        double chargeNeeded = getChargePerOperation(amount);
        if (!isRedstoneActive() || getStorageAmount() == 0 && getOutputAmount() == 0 || getCharge() < chargeNeeded) {
            return null;
        }

        if (receiver != null) {
            amount = Math.min(amount, receiver.getMaxStackSize() - receiver.getAmount());
            if (getStorageAmount() > 0 && !receiver.isSimilar(getStoredItemType())) {
                return null;
            }
            if (amount > getStorageAmount() && getOutputAmount() > 0 && !receiver.isSimilar(getOutputItem())) {
                return null;
            }
        }

        int fromStorage = Math.min(getStorageAmount(), amount);
        fromStorage = Math.min(fromStorage, getStoredItemType().getMaxStackSize());
        if (fromStorage > 0) {
            amount -= fromStorage;
            setStorageAmount(getStorageAmount() - fromStorage);
        }
        int fromOutput = 0;
        if (amount > 0) {
            fromOutput = Math.min(getOutputAmount(), amount);
            if (fromOutput > 0) {
                setOutputAmount(getOutputAmount() - fromOutput);
                ItemStack output = getOutputItem();
                output.setAmount(getOutputAmount());
                setOutputItem(output.getAmount() > 0 ? output : null);
            }
        }

        ItemStack tmpStored = getStoredItemType();
        if (getTotalAmount() == 0) {
            setStoredItemType(null);
        }

        setCharge(getCharge() - chargeNeeded);

        if (receiver == null) {
            ItemStack returned = tmpStored.clone();
            returned.setAmount(fromStorage + fromOutput);
            return returned;
        } else {
            receiver.setAmount(receiver.getAmount() + fromStorage + fromOutput);
            return receiver;
        }
    }

    @Override
    public Inventory showOutputItems(UUID uuid) {
        if (hasAccessRights(uuid)) {
            Inventory inv = Bukkit.createInventory(this, 9);
            inv.setItem(0, getOutputItem());
            return inv;
        } else {
            return null;
        }
    }

    @Override
    public void updateOutputItems(UUID uuid, Inventory inventory) {
        if (hasAccessRights(uuid)) {
            setOutputItem(inventory.getItem(0));
            setOutputAmount(getOutputItem() == null ? 0 : getOutputItem().getAmount());
        }
    }

    /**
     * Return the SCU cost for processing some items; either inserting or
     * extracting them.
     *
     * @param nItems the number of items to check for
     * @return the SCU cost
     */
    public double getChargePerOperation(int nItems) {
        return 0.0;
    }
}
