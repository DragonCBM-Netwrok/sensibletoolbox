package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.gui.GUIUtil;
import me.mrCookieSlime.sensibletoolbox.api.gui.ToggleButton;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.util.AreaLocation;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.api.util.StorageLocation;
import me.mrCookieSlime.sensibletoolbox.blocks.machines.BigStorageUnit;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.ItemRouterModule;
import me.mrCookieSlime.sensibletoolbox.util.UnicodeSymbol;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Wool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public abstract class AreaItemRouterModule extends ItemRouterModule {

    private static final String LIST_ITEM = ChatColor.LIGHT_PURPLE + UnicodeSymbol.CENTERED_POINT.toUnicode() + " " + ChatColor.AQUA;
    private static final ItemStack WHITE_BUTTON = GUIUtil.makeTexture(
            new Wool(DyeColor.WHITE), ChatColor.RESET.toString() + ChatColor.UNDERLINE + "Whitelist",
            "Module will only process", "items which match the filter."
    );
    private static final ItemStack BLACK_BUTTON = GUIUtil.makeTexture(
            new Wool(DyeColor.BLACK), ChatColor.RESET.toString() + ChatColor.UNDERLINE + "Blacklist",
            "Module will NOT process", "items which match the filter."
    );
    private static final ItemStack OFF_BUTTON = GUIUtil.makeTexture(
            STBUtil.makeColouredMaterial(Material.STAINED_GLASS, DyeColor.LIGHT_BLUE), ChatColor.RESET.toString() + ChatColor.UNDERLINE + "Termination OFF",
            "Subsequent modules in the", "Item Router will process items", "as normal."
    );
    private static final ItemStack ON_BUTTON = GUIUtil.makeTexture(
            new Wool(DyeColor.ORANGE), ChatColor.RESET.toString() + ChatColor.UNDERLINE + "Termination ON",
            "If this module processes an", "item, the Item Router will", "not process any more items", "on this tick."
    );
    public static final int FILTER_LABEL_SLOT = 0;
    public static final int DIRECTION_LABEL_SLOT = 5;
    private boolean terminator;
    private AreaLocation area;
    private HashMap<Material, List<StorageLocation>> storageList;

    /**
     * Run this module's action.
     *
     * @param loc the location of the module's owning item router
     * @return true if the module did some work on this tick
     */
    public abstract boolean execute(Location loc);


    public AreaItemRouterModule() {
        area = new AreaLocation();
        storageList = new HashMap<Material, List<StorageLocation>>();
    }

    public AreaItemRouterModule(ConfigurationSection conf) {
        super(conf);
        setTerminator(conf.getBoolean("terminator", false));

        area = new AreaLocation(conf);

        storageList = new HashMap<Material, List<StorageLocation>>();
        List<Location> tmpSave2 = (List<Location>) conf.getList("AreaList-GlobalL");
        for (Location li : tmpSave2) {
            StorageLocation nweW = new StorageLocation(li);
            this.AddStorage(nweW);
        }
    }

    private void AddStorage(StorageLocation IS) {

        Material SortingType = Material.AIR;

        if (IS.getItem() != null) {
            SortingType = IS.getItem().getType();
        }
        if (!storageList.containsKey(SortingType)) {
            storageList.put(SortingType, new ArrayList<StorageLocation>());
        }
        List<StorageLocation> tmpAdd = storageList.get(SortingType);
        tmpAdd.add(IS);
        storageList.put(SortingType, tmpAdd);
    }
    private void removeStorageFromAIR(Location IS) {

        Material SortingType = Material.AIR;
        if (!storageList.containsKey(SortingType)) {
            storageList.put(SortingType, new ArrayList<StorageLocation>());
        }
        List<StorageLocation> tmpRemove = storageList.get(SortingType);
        int toRemove = -1;
        for (int i = 0; i < tmpRemove.size();i++)
        {
         if (IS.equals(tmpRemove.get(i).getLocation()))
         {
             toRemove = i;
             break;
         }
        }
        if (toRemove > 0)
        {
            tmpRemove.remove(toRemove);
        }
        storageList.put(SortingType, tmpRemove);
    }
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        conf.set("terminator", isTerminator());

        conf = area.freeze(conf);

        List<Location> tmpSave1 = new ArrayList<Location>();
        for (Material mats : storageList.keySet()) {
            List<StorageLocation> tmpY = storageList.get(mats);
            for (StorageLocation SL2 : tmpY) {
                tmpSave1.add(SL2.getLocation());
            }
        }
        conf.set("AreaList-GlobalL", tmpSave1);
        return conf;
    }

    public BigStorageUnit getStorage(ItemStack item)
    {
        BigStorageUnit BSU = getStorage(item, item.getType());
        if (BSU == null)
        {
            BSU  = getStorage(item,  Material.AIR);
            if (BSU != null)
            {
                this.removeStorageFromAIR(BSU.getLocation());
                if (!BSU.isLocked()) {
                    BSU.setLocked(true);
                    ((ToggleButton) BSU.getGUI().getGadget(26)).setValue(true);
                }
                if (BSU.getStoredItemType() == null) {
                    BSU.setStoredItemType(item.clone());
                }
                StorageLocation nweW = new StorageLocation(BSU.getLocation());
                this.AddStorage(nweW);
            }
        }
        return  BSU;
    }
    public BigStorageUnit getStorage(ItemStack item, Material searchType) {
        if (!storageList.containsKey(searchType)) return null;
        List<StorageLocation> wheretoFind = storageList.get(searchType);
        for (StorageLocation SL : wheretoFind) {
            if (searchType == Material.AIR || SL.getItem().getDurability() == item.getDurability()) {
                if (searchType == Material.AIR || SensibleToolboxPlugin.isItemSimiliar(SL.getItem(), item)) {
                    BaseSTBBlock stb = SensibleToolbox.getBlockAt(SL.getLocation(), true);
                    if (stb instanceof BigStorageUnit) {
                        if (((BigStorageUnit) stb).getStoredItemType() != null) {
                            ItemStack itemBSU = ((BigStorageUnit) stb).getStoredItemType().clone();
                            if (itemBSU.getDurability() == item.getDurability()) {
                                if (SensibleToolboxPlugin.isItemSimiliar(itemBSU, item)) {
                                    return ((BigStorageUnit) stb);
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override
    public String[] getExtraLore() {

        return new String[0];
    }


    public boolean isTerminator() {
        return terminator;
    }

    public void setTerminator(boolean terminator) {
        this.terminator = terminator;
    }

    @Override
    public void onInteractItem(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // set module direction based on clicked block face
            area.setLocation1(event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "Location 1 set");
            event.getPlayer().setItemInHand(toItemStack(event.getPlayer().getItemInHand().getAmount()));
            event.setCancelled(true);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            area.setLocation2(event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "Location 2 set");
            event.getPlayer().setItemInHand(toItemStack(event.getPlayer().getItemInHand().getAmount()));
            event.setCancelled(true);
        }
        if (area.areLocationsset()) {
            if (area.getLocation1().distance(area.getLocation2()) > 300) {
                event.getPlayer().sendMessage(ChatColor.DARK_RED + "Area to big to scan!!");
                return;
            }
            storageList.clear();
            event.getPlayer().sendMessage(ChatColor.GREEN + "Scanning...");
            int Xmin = Math.min(area.getX1(), area.getX2());
            int Ymin = Math.min(area.getY1(), area.getY2());
            int Zmin = Math.min(area.getZ1(), area.getZ2());

            int Xmax = Math.max(area.getX1(), area.getX2());
            int Ymax = Math.max(area.getY1(), area.getY2());
            int Zmax = Math.max(area.getZ1(), area.getZ2());

            int count = 0;
            for (int x = Xmin; x <= Xmax; x++) {
                for (int y = Ymin; y <= Ymax; y++) {
                    for (int z = Zmin; z <= Zmax; z++) {
                        Location check = new Location(Bukkit.getWorld(area.getWorld()), x, y, z);
                        BaseSTBBlock stb = SensibleToolbox.getBlockAt(check, true);
                        if (stb instanceof BigStorageUnit) {
                            // if (((BigStorageUnit)stb).getStoredItemType() != null)
                            {
                                StorageLocation SL = new StorageLocation(check);
                                this.AddStorage(SL);
                                count++;
                            }
                        }
                    }
                }
            }
            event.getPlayer().sendMessage(ChatColor.GREEN + "Found: " + ChatColor.WHITE + count + ChatColor.GREEN + " HSU/BSU(s)");
            event.getPlayer().setItemInHand(toItemStack(event.getPlayer().getItemInHand().getAmount()));
            event.setCancelled(true);

        }


    }


    protected String[] makeDirectionalLore(String... lore) {
        String[] newLore = Arrays.copyOf(lore, lore.length + 3);
        newLore[lore.length] = "L-click Block: " + ChatColor.RESET + " Set point 1";
        newLore[lore.length + 1] = "R-click Block: " + ChatColor.RESET + " Set point 2";
        newLore[lore.length + 2] = "L/R-click Air: " + ChatColor.RESET + " Scans area";
        return newLore;
    }

    @Override
    public boolean onPlayerInventoryClick(HumanEntity player, int slot, ClickType click, ItemStack inSlot, ItemStack onCursor) {
        return true;
    }

    @Override
    public int onShiftClickInsert(HumanEntity player, int slot, ItemStack toInsert) {
        return 0;
    }

    @Override
    public boolean onShiftClickExtract(HumanEntity player, int slot, ItemStack toExtract) {
        return false;
    }

    @Override
    public boolean onClickOutside(HumanEntity player) {
        return false;
    }

}