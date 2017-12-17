package me.mrCookieSlime.sensibletoolbox.blocks;

import me.mrCookieSlime.CSCoreLibPlugin.general.World.CustomSkull;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

import me.desht.dhutils.Debugger;
import me.mrCookieSlime.sensibletoolbox.api.RedstoneBehaviour;
import me.mrCookieSlime.sensibletoolbox.api.gui.AccessControlGadget;
import me.mrCookieSlime.sensibletoolbox.api.gui.GUIUtil;
import me.mrCookieSlime.sensibletoolbox.api.gui.InventoryGUI;
import me.mrCookieSlime.sensibletoolbox.api.gui.NumericGadget;
import me.mrCookieSlime.sensibletoolbox.api.gui.RedstoneBehaviourGadget;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;

public class BlockUpdateDetector extends BaseSTBBlock {
    private static final MaterialData md = STBUtil.makeColouredMaterial(Material.STAINED_CLAY, DyeColor.PURPLE);
    private static final MaterialData md2 = new MaterialData(Material.REDSTONE_BLOCK);
    private String skulltexture2;
    private long lastPulse;
    private int duration;
    private int quiet;
    private boolean active = false;

    public BlockUpdateDetector() {
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTJjMmY2ZmE3ZWM1MzA0MzVjNDMxNTcyOTM4YjlmZWI5NTljNDIyOThlNTU1NDM0MDI2M2M2NTI3MSJ9fX0=";
        skulltexture2 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTJjMmY2ZmE3ZWM1MzA0MzVjNDMxNTcyOTM4YjlmZWI5NTljNDIyOThlNTU1NDM0MDI2M2M2NTI3MSJ9fX0=";
        quiet = 1;
        duration = 2;
    }

    public BlockUpdateDetector(ConfigurationSection conf) {
        super(conf);
        setDuration(conf.getInt("duration"));
        setQuiet(conf.getInt("quiet"));
        skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTJjMmY2ZmE3ZWM1MzA0MzVjNDMxNTcyOTM4YjlmZWI5NTljNDIyOThlNTU1NDM0MDI2M2M2NTI3MSJ9fX0=";
        skulltexture2 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTJjMmY2ZmE3ZWM1MzA0MzVjNDMxNTcyOTM4YjlmZWI5NTljNDIyOThlNTU1NDM0MDI2M2M2NTI3MSJ9fX0=";
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        conf.set("duration", getDuration());
        conf.set("quiet", getQuiet());
        return conf;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
        update(false);
    }

    public int getQuiet() {
        return quiet;
    }

    public void setQuiet(int quiet) {
        this.quiet = quiet;
        update(false);
    }

    @Override
    public MaterialData getMaterialData() {
        return active ? md2 : md;
    }
    @Override
    public String getSkullOwner() {
        return active ? skulltexture : skulltexture2;
    }
    @Override
    public ItemStack getSkullData() {
        try {
            return active ? CustomSkull.getItem(skulltexture) : CustomSkull.getItem(skulltexture2);
        }
        catch (Exception e)
        {
            return getMaterialData().toItemStack(1);
        }
    }
    @Override
    public String getItemName() {
        return "Block Update Detector";
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "Emits a redstone pulse when",
                " an adjacent block updates",
                "R-click block: " + ChatColor.RESET + "configure BUD"
        };
    }

    @Override
    public String[] getExtraLore() {
        return new String[]{
                "Pulse duration: " + ChatColor.GOLD + getDuration() + " ticks",
                "Sleep time after pulse: " + ChatColor.GOLD + getQuiet() + " ticks",
        };
    }

    @Override
    public Recipe getRecipe() {
        ShapedRecipe res = new ShapedRecipe(toItemStack());
        res.shape("SRS", "SPS", "STS");
        res.setIngredient('S', Material.STONE);
        res.setIngredient('P', Material.PISTON_STICKY_BASE);
        res.setIngredient('R', Material.REDSTONE);
        res.setIngredient('T', Material.REDSTONE_TORCH_ON);
        return res;
    }

    @Override
    public void onBlockPhysics(BlockPhysicsEvent event) {
        final Block b = event.getBlock();
        long timeNow = getLocation().getWorld().getFullTime();
        Debugger.getInstance().debug(this + ": BUD physics: time=" + timeNow + ", lastPulse=" + lastPulse + ", duration=" + getDuration());
        if (timeNow - lastPulse > getDuration() + getQuiet() && isRedstoneActive()) {
            // emit a signal for one or more ticks
            lastPulse = timeNow;
            active = true;
            repaint(b);
            b.setTypeIdAndData(getMaterialData().getItemTypeId(), getMaterialData().getData(), true);
            Bukkit.getScheduler().runTaskLater(getProviderPlugin(), new Runnable() {
                @Override
                public void run() {
                    active = false;
                    repaint(b);
                }
            }, duration);
        }
    }

    @Override
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            getGUI().show(event.getPlayer());
            event.setCancelled(true);
        }
        super.onInteractBlock(event);
    }

    @Override
    protected InventoryGUI createGUI() {
        InventoryGUI gui = GUIUtil.createGUI(this, 9, ChatColor.DARK_PURPLE + getItemName());
        gui.addGadget(new NumericGadget(gui, 1, "Pulse Duration", new IntRange(1, Integer.MAX_VALUE), getDuration(), 10, 1, new NumericGadget.NumericListener() {
            @Override
            public boolean run(int newValue) {
                setDuration(newValue);
                return true;
            }
        }));
        gui.addGadget(new NumericGadget(gui, 0, "Sleep Time after Pulse", new IntRange(0, Integer.MAX_VALUE), getQuiet(), 10, 1, new NumericGadget.NumericListener() {
            @Override
            public boolean run(int newValue) {
                setQuiet(newValue);
                return true;
            }
        }));
        gui.addGadget(new RedstoneBehaviourGadget(gui, 8));
        gui.addGadget(new AccessControlGadget(gui, 7));
        return gui;
    }

    @Override
    public void onBlockUnregistered(Location location) {
        // ensure the non-active form of the item is always dropped
        active = false;
        super.onBlockUnregistered(location);
    }

    @Override
    public boolean supportsRedstoneBehaviour(RedstoneBehaviour behaviour) {
        return behaviour != RedstoneBehaviour.PULSED;
    }
}
