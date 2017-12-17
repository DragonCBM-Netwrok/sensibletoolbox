package me.mrCookieSlime.sensibletoolbox.blocks;

import me.mrCookieSlime.CSCoreLibPlugin.general.World.CustomSkull;
import me.mrCookieSlime.sensibletoolbox.api.RedstoneBehaviour;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.gui.*;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBItem;
import me.mrCookieSlime.sensibletoolbox.api.util.BlockProtection;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.material.MaterialData;

import java.awt.Color;

public class RedstoneClock extends BaseSTBBlock {
	
    private static final MaterialData inactive_texture = STBUtil.makeColouredMaterial(Material.STAINED_CLAY, DyeColor.RED);
    private static final MaterialData active_texture = new MaterialData(Material.REDSTONE_BLOCK);
    private static String active_skulltexture = null;
    private static String inactive_skulltexture = null;
    private int interval;
    private int onDuration;
    private boolean active = false;
    private boolean deleteing = false;

    public RedstoneClock() {
        interval = 20;
        onDuration = 5;

        active_skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI3OGZhNWRlZmU3MmRlYmNkOWM3NmFiOWY0ZTExNDI1MDQ3OWJiOWI0NGY0Mjg4N2JiZjZmNzM4NjEyYiJ9fX0=";

        inactive_skulltexture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI3OGZhNWRlZmU3MmRlYmNkOWM3NmFiOWY0ZTExNDI1MDQ3OWJiOWI0NGY0Mjg4N2JiZjZmNzM4NjEyYiJ9fX0=";

    }

    public RedstoneClock(ConfigurationSection conf) {
        super(conf);
        setInterval(conf.contains("interval") ? conf.getInt("interval") : conf.getInt("frequency"));
        setOnDuration(conf.getInt("onDuration"));
        active = conf.getBoolean("active", false);

    }

    @Override
    protected InventoryGUI createGUI() {
        InventoryGUI gui = GUIUtil.createGUI(this, 9, ChatColor.DARK_RED + getItemName());
        gui.addGadget(new NumericGadget(gui, 0, "Pulse Interval", new IntRange(1, Integer.MAX_VALUE), getInterval(), 10, 1, new NumericGadget.NumericListener() {
            @Override
            public boolean run(int newValue) {
                if (newValue > getOnDuration()) {
                    setInterval(newValue);
                    return true;
                } else {
                    return false;
                }
            }
        }));
        gui.addGadget(new NumericGadget(gui, 1, "Pulse Duration", new IntRange(1, Integer.MAX_VALUE), getOnDuration(), 10, 1, new NumericGadget.NumericListener() {
            @Override
            public boolean run(int newValue) {
                if (newValue < getInterval()) {
                    setOnDuration(newValue);
                    return true;
                } 
                else return false;
            }
        }));
        gui.addGadget(new RedstoneBehaviourGadget(gui, 8));
        gui.addGadget(new AccessControlGadget(gui, 7));
        return gui;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
        update(false);
    }

    public int getOnDuration() {
        return onDuration;
    }

    public void setOnDuration(int onDuration) {
        this.onDuration = onDuration;
        update(false);
    }
    @Override
    public String getSkullOwner() {
        return active ? active_skulltexture : inactive_skulltexture;
    }
    @Override
    public ItemStack getSkullData() {
        try {
            return active ? CustomSkull.getItem(active_skulltexture) : CustomSkull.getItem(inactive_skulltexture);
        }
        catch (Exception e)
        {
            return getMaterialData().toItemStack(1);
        }
    }

    @Override
    public YamlConfiguration freeze() {
        YamlConfiguration conf = super.freeze();
        conf.set("interval", interval);
        conf.set("onDuration", onDuration);
        conf.set("active", active);
        return conf;
    }


    @Override
    public MaterialData getMaterialData() {
        return active ? active_texture : inactive_texture;
    }

    @Override
    public String getItemName() {
        return "Redstone Clock";
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "Clock-in-a-block",
                "Emits a redstone signal with",
                "configurable interval & duration",
                "R-click block: " + ChatColor.RESET + " configure clock"
        };
    }

    @Override
    public Recipe getRecipe() {
        ShapedRecipe res = new ShapedRecipe(toItemStack());
        res.shape("RSR", "STS", "RSR");
        res.setIngredient('R', Material.REDSTONE);
        res.setIngredient('S', Material.STONE);
        res.setIngredient('T', Material.REDSTONE_TORCH_ON);
        return res;
    }

    @Override
    public String[] getExtraLore() {
        String l = BaseSTBItem.LORE_COLOR + "Interval: " + ChatColor.GOLD + getInterval() +
                LORE_COLOR + "t, Duration: " + ChatColor.GOLD + getOnDuration() + LORE_COLOR + "t";
        return new String[]{l};
    }

    @Override
    public int getTickRate() {
        return 1;
    }

    @Override
    public void onServerTick() {
        if (deleteing == false) {
            Location loc = getLocation();
            Block b = loc.getBlock();
            long time = getTicksLived();
            if (time % getInterval() == 0 && isRedstoneActive()) {
                // power up
                active = true;
                repaint(b);
                b.setTypeIdAndData(getMaterialData().getItemTypeId(), getMaterialData().getData(), true);
            } else if (time % getInterval() == getOnDuration()) {
                // power down
                active = false;
                repaint(b);
            }

            if (time % 50 == 10) playParticles(new Color(255, 0, 0));
            super.onServerTick();
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

	@Override
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !event.getPlayer().isSneaking()) {
            getGUI().show(event.getPlayer());
        }
        super.onInteractBlock(event);
    }

    @Override
    public void onBlockUnregistered(Location location) {
        // ensure the non-active form of the item is always dropped
        deleteing = true;
        active = false;
        super.onBlockUnregistered(location);
    }

    @Override
    public boolean supportsRedstoneBehaviour(RedstoneBehaviour behaviour) {
        return behaviour != RedstoneBehaviour.PULSED;
    }

    @Override
    public void onBlockDamage(BlockDamageEvent event) {
        // the angelic block has just been hit by a player - insta-break it
        Player p = event.getPlayer();
        Block b = event.getBlock();
        if (SensibleToolbox.getBlockProtection().playerCanBuild(p, b, BlockProtection.Operation.BREAK)) {
            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, b.getType());
            breakBlock(false);
            STBUtil.giveItems(p, toItemStack());
        }
        event.setCancelled(true);
    }
}
