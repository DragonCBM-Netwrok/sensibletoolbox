package me.mrCookieSlime.sensibletoolbox;

/*
    This file is part of SensibleToolbox

    SensibleToolbox is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SensibleToolbox is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SensibleToolbox.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.comphenix.protocol.ProtocolLibrary;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.mojang.authlib.GameProfile;
import com.onarandombox.MultiverseCore.MultiverseCore;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.desht.dhutils.*;
import me.desht.dhutils.commands.CommandManager;
import me.mrCookieSlime.CSCoreLibPlugin.PluginUtils;
import me.mrCookieSlime.CSCoreLibSetup.CSCoreLibLoader;
import me.mrCookieSlime.Slimefun.Setup.SlimefunManager;
import me.mrCookieSlime.sensibletoolbox.api.AccessControl;
import me.mrCookieSlime.sensibletoolbox.api.FriendManager;
import me.mrCookieSlime.sensibletoolbox.api.RedstoneBehaviour;
import me.mrCookieSlime.sensibletoolbox.api.gui.InventoryGUI;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBItem;
import me.mrCookieSlime.sensibletoolbox.api.recipes.RecipeUtil;
import me.mrCookieSlime.sensibletoolbox.api.util.BlockProtection;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.blocks.*;
import me.mrCookieSlime.sensibletoolbox.blocks.machines.*;
import me.mrCookieSlime.sensibletoolbox.commands.*;
import me.mrCookieSlime.sensibletoolbox.core.IDTracker;
import me.mrCookieSlime.sensibletoolbox.core.STBFriendManager;
import me.mrCookieSlime.sensibletoolbox.core.STBItemRegistry;
import me.mrCookieSlime.sensibletoolbox.core.enderstorage.EnderStorageManager;
import me.mrCookieSlime.sensibletoolbox.core.energy.EnergyNetManager;
import me.mrCookieSlime.sensibletoolbox.core.gui.STBInventoryGUI;
import me.mrCookieSlime.sensibletoolbox.core.storage.LocationManager;
import me.mrCookieSlime.sensibletoolbox.items.*;
import me.mrCookieSlime.sensibletoolbox.items.RecipeBook;
import me.mrCookieSlime.sensibletoolbox.items.components.*;
import me.mrCookieSlime.sensibletoolbox.items.energycells.FiftyKEnergyCell;
import me.mrCookieSlime.sensibletoolbox.items.energycells.HundredKEnergyCell;
import me.mrCookieSlime.sensibletoolbox.items.energycells.TenKEnergyCell;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.*;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan.*;
import me.mrCookieSlime.sensibletoolbox.items.machineupgrades.EjectorUpgrade;
import me.mrCookieSlime.sensibletoolbox.items.machineupgrades.RegulatorUpgrade;
import me.mrCookieSlime.sensibletoolbox.items.machineupgrades.SpeedUpgrade;
import me.mrCookieSlime.sensibletoolbox.items.machineupgrades.ThoroughnessUpgrade;
import me.mrCookieSlime.sensibletoolbox.listeners.*;
import me.mrCookieSlime.sensibletoolbox.slimefun.SlimefunBridge;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.minecraft.server.v1_12_R1.*;
import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SensibleToolboxPlugin extends JavaPlugin implements ConfigurationListener {

    private static SensibleToolboxPlugin instance = null;
    private final CommandManager cmds = new CommandManager(this);
    private ConfigurationManager configManager;
    private boolean protocolLibEnabled = false;
    private SoundMufflerListener soundMufflerListener;
    private PlayerUUIDTracker uuidTracker;
    private boolean inited = false;
    private boolean holographicDisplays = false;
    private BukkitTask energyTask = null;
    private LWC lwc = null;
    private EnderStorageManager enderStorageManager;
    private STBItemRegistry itemRegistry;
    private STBFriendManager friendManager;
    private EnergyNetManager enetManager;
    private WorldGuardPlugin worldGuardPlugin = null;
    private GriefPrevention  griefPrevention = null;
    private PreciousStones preciousStonesPlugin = null;
    private BlockProtection blockProtection;
    private ConfigCache configCache;
    private MultiverseCore multiverseCore = null;
    private IDTracker scuRelayIDTracker;
    public HashMap<String, EntityPlayer> npcs;

    public static SensibleToolboxPlugin getInstance() {
        return instance;
    }
    
    public void registerItems() {
        final String CONFIG_NODE = "items_enabled";
        final String PERMISSION_NODE = "stb";

        itemRegistry.registerItem(new AngelicBlock(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnderLeash(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new RedstoneClock(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new BlockUpdateDetector(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnderBag(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new WateringCan(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new MoistureChecker(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AdvancedMoistureChecker(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new WoodCombineHoe(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new IronCombineHoe(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new GoldCombineHoe(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new DiamondCombineHoe(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new TrashCan(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new PaintBrush(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new PaintRoller(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new PaintCan(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new Elevator(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new TapeMeasure(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new CircuitBoard(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SimpleCircuit(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new MultiBuilder(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new MachineFrame(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new Smelter(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new Masher(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new Sawmill(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new IronDust(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new GoldDust(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new ItemRouter(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new BlankModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new PullerModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new DropperModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SenderModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new DistributorModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AdvancedSenderModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new HyperSenderModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new ReceiverModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SorterModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new VacuumModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new BreakerModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new StackModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SpeedModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SFPullerModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SFSenderModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SuperSorterModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AreaSorterModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new PauseModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new RotationModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new FishingModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new KillerModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new CobblestoneModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new BHSUPullerModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new BHSUSenderModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new MoverModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new TenKEnergyCell(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new FiftyKEnergyCell(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new HundredKEnergyCell(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new TenKBatteryBox(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new FiftyKBatteryBox(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SpeedUpgrade(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EjectorUpgrade(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new RegulatorUpgrade(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new ThoroughnessUpgrade(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new HeatEngine(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new BasicSolarCell(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new DenseSolar(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new RecipeBook(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AdvancedRecipeBook(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new Multimeter(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new BigStorageUnit(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new HyperStorageUnit(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new Pump(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnderTuner(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnderBox(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new InfernalDust(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnergizedIronDust(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnergizedGoldDust(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnergizedIronIngot(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnergizedGoldIngot(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new ToughMachineFrame(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new QuartzDust(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SiliconWafer(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new IntegratedCircuit(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new LandMarker(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new PVCell(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AutoBuilder(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new UnlinkedSCURelay(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SCURelay(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SilkyBreakerModule(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new SubspaceTransponder(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new BioEngine(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new MagmaticEngine(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new EnergizedQuartz(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new ElectricalEnergizer(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new PowerMonitor(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new Fermenter(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new FishBait(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new FishingNet(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AutoFarm(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AutoForester(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AdvancedFarm(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new InfernalFarm(), this, CONFIG_NODE, PERMISSION_NODE);
        itemRegistry.registerItem(new AutoFarm2(), this, CONFIG_NODE, PERMISSION_NODE);

        
        if (isProtocolLibEnabled()) {
        	itemRegistry.registerItem(new SoundMuffler(), this, CONFIG_NODE, PERMISSION_NODE);
        }
        if (isHolographicDisplaysEnabled()) {
        	itemRegistry.registerItem(new HolographicMonitor(), this, CONFIG_NODE, PERMISSION_NODE);
        }
    }
    public static MaterialData getSTBItemMaterialData(BaseSTBItem block)
    {
        if (block.getSkullData() == null)
        {
            return block.getMaterialData();
        }
        else
        {
            return block.getSkullData().getData();
        }
    }
    public static boolean isItemSimiliar(ItemStack item1, ItemStack item2)
    {
        if (item1 == null && item2 == null)
        {
            return true;
        }
        if (item1 == null || item2 == null) {
            return false;
        }
        ItemStack item1clone = item1.clone();
        item1clone.setAmount(1);
        ItemStack item2clone = item2.clone();
        item2clone.setAmount(1);
        return SlimefunManager.isItemSimiliar(item1clone, item2clone, true);
    }
    @Override
    public void onEnable() {
        CSCoreLibLoader loader = new CSCoreLibLoader(this);
        if (loader.load()) {
        	instance = this;

            LogUtils.init(this);
            
            PluginUtils utils = new PluginUtils(this);
            utils.setupUpdater(79884, getFile());
            utils.setupMetrics();
            
            configManager = new ConfigurationManager(this, this);

            configCache = new ConfigCache(this);
            configCache.processConfig();

            MiscUtil.init(this);
            MiscUtil.setColouredConsole(getConfig().getBoolean("coloured_console"));

            LogUtils.setLogLevel(getConfig().getString("log_level", "INFO"));

            Debugger.getInstance().setPrefix("[STB] ");
            Debugger.getInstance().setLevel(getConfig().getInt("debug_level"));
            if (getConfig().getInt("debug_level") > 0) Debugger.getInstance().setTarget(getServer().getConsoleSender());

            // try to hook other plugins
            holographicDisplays = getServer().getPluginManager().isPluginEnabled("HolographicDisplays");
            setupProtocolLib();
            setupLWC();
            setupWorldGuard();
            setupPreciousStones();
            setupMultiverse();
            setupGriefPrevention();


            scuRelayIDTracker = new IDTracker(this, "scu_relay_id");

            blockProtection = new BlockProtection(this);

            STBInventoryGUI.buildStockTextures();

            itemRegistry = new STBItemRegistry();
            registerItems();

            friendManager = new STBFriendManager(this);
            enetManager = new EnergyNetManager(this);

            registerEventListeners();
            registerCommands();
            try {
                LocationManager.getManager().load();
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.severe(e.getMessage());
                setEnabled(false);
                return;
            }


            MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
            npcs = new HashMap<String, EntityPlayer>();
            for (World world: Bukkit.getWorlds())
            {
                WorldServer nmsWorld = ((CraftWorld)world).getHandle();
                EntityPlayer npc = new EntityPlayer(nmsServer, nmsWorld, new GameProfile(UUID.randomUUID(), "NPC_" + world.getName()), new PlayerInteractManager(nmsWorld));

                npc.setLocation(world.getSpawnLocation().getX(), world.getSpawnLocation().getY(), world.getSpawnLocation().getZ(), 0, 0);
                CraftPlayer opCr = npc.getBukkitEntity();
                opCr.setGameMode(GameMode.SURVIVAL);
                opCr.getPlayer().setGameMode(GameMode.SURVIVAL);
                opCr.getHandle().playerInteractManager.setGameMode(EnumGamemode.SURVIVAL);

                PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(opCr.getUniqueId());
                playerData.ignoreClaims = true;

                npcs.put(world.getName(), npc);
            };

            MessagePager.setPageCmd("/stb page [#|n|p]");
            MessagePager.setDefaultPageSize(getConfig().getInt("pager.lines", 0));

            // do all the recipe setup on a delayed task to ensure we pick up
            // custom recipes from any plugins that may have loaded after us
            Bukkit.getScheduler().runTask(this, new Runnable() {
                @Override
                public void run() {
                    RecipeUtil.findVanillaFurnaceMaterials();
                    RecipeUtil.setupRecipes();
                    RecipeBook.buildRecipes();
                }
            });

            Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    LocationManager.getManager().tick();
                }
            }, 1L, 1L);

            Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    getEnderStorageManager().tick();
                }
            }, 1L, 300L);

            Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    friendManager.save();
                }
            }, 60L, 300L);

            scheduleEnergyNetTicker();
            if (Bukkit.getPluginManager().isPluginEnabled("Slimefun")) SlimefunBridge.initiate();

            inited = true;
        }
    }

    public void onDisable() {
        if (!inited) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Any open inventory GUI's must be closed -
            // if they stay open after server reload, event dispatch will probably not work,
            // allowing fake items to be removed from them - not a good thing
            InventoryGUI gui = STBInventoryGUI.getOpenGUI(p);
            if (gui != null) {
                gui.hide(p);
                p.closeInventory();
            }
        }
        if (soundMufflerListener != null) soundMufflerListener.clear();
        LocationManager.getManager().save();
        LocationManager.getManager().shutdown();

        friendManager.save();

        Bukkit.getScheduler().cancelTasks(this);

        instance = null;
    }
    private void registerEventListeners() {
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new GeneralListener(this), this);
        pm.registerEvents(new FurnaceListener(this), this);
        pm.registerEvents(new MobListener(this), this);
        pm.registerEvents(new WorldListener(this), this);
        pm.registerEvents(new TrashCanListener(this), this);
        pm.registerEvents(new ElevatorListener(this), this);
        pm.registerEvents(new AnvilListener(this), this);
        uuidTracker = new PlayerUUIDTracker(this);
        pm.registerEvents(uuidTracker, this);
        if (isProtocolLibEnabled()) {
            soundMufflerListener = new SoundMufflerListener(this);
            soundMufflerListener.start();
        }
        enderStorageManager = new EnderStorageManager(this);
        pm.registerEvents(enderStorageManager, this);
    }

    private void setupGriefPrevention() {

        Plugin plugin = getServer().getPluginManager().getPlugin("GriefPrevention");

        if (plugin != null && plugin.isEnabled() && plugin instanceof GriefPrevention) {
            Debugger.getInstance().debug("Hooked GriefPrevention v" + plugin.getDescription().getVersion());
            griefPrevention = (GriefPrevention) plugin;
        }
    }
    private void setupWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (plugin != null && plugin.isEnabled() && plugin instanceof WorldGuardPlugin) {
            Debugger.getInstance().debug("Hooked WorldGuard v" + plugin.getDescription().getVersion());
            worldGuardPlugin = (WorldGuardPlugin) plugin;
        }
    }

    private void setupPreciousStones() {
        Plugin plugin = getServer().getPluginManager().getPlugin("PreciousStones");

        if (plugin != null && plugin.isEnabled() && plugin instanceof PreciousStones) {
            Debugger.getInstance().debug("Hooked PreciousStones v" + plugin.getDescription().getVersion());
            preciousStonesPlugin = (PreciousStones) plugin;
        }
    }

    private void setupProtocolLib() {
        //protocolManager =

        Plugin pLib = getServer().getPluginManager().getPlugin("ProtocolLib");
        if (pLib != null && pLib.isEnabled() && pLib instanceof ProtocolLibrary) {
            protocolLibEnabled = true;
            Debugger.getInstance().debug("Hooked ProtocolLib v" + pLib.getDescription().getVersion());
        }
        if (protocolLibEnabled) {
            if (getConfig().getBoolean("options.glowing_items"))ItemGlow.init(this);
        } 
        else {
            LogUtils.warning("ProtocolLib not detected - some functionality is reduced:");
            LogUtils.warning("  No glowing items, Reduced particle effects, Sound Muffler item disabled");
        }
    }

    private void setupMultiverse() {
        Plugin mvPlugin = getServer().getPluginManager().getPlugin("Multiverse-Core");
        if (mvPlugin != null && mvPlugin.isEnabled() && mvPlugin instanceof MultiverseCore) {
            multiverseCore = (MultiverseCore) mvPlugin;
            Debugger.getInstance().debug("Hooked Multiverse-Core v" + mvPlugin.getDescription().getVersion());
        }
    }

    private void setupLWC() {
        Plugin lwcPlugin = getServer().getPluginManager().getPlugin("LWC");
        if (lwcPlugin != null && lwcPlugin.isEnabled() && lwcPlugin instanceof LWCPlugin) {
            lwc = ((LWCPlugin) lwcPlugin).getLWC();
            Debugger.getInstance().debug("Hooked LWC v" + lwcPlugin.getDescription().getVersion());
        }
    }

    public boolean isProtocolLibEnabled() {
        return protocolLibEnabled;
    }

    public boolean isHolographicDisplaysEnabled() {
        return holographicDisplays;
    }

    private void registerCommands() {
        cmds.registerCommand(new SaveCommand());
        cmds.registerCommand(new RenameCommand());
        cmds.registerCommand(new GiveCommand());
        cmds.registerCommand(new ShowCommand());
        cmds.registerCommand(new ChargeCommand());
        cmds.registerCommand(new GetcfgCommand());
        cmds.registerCommand(new SetcfgCommand());
        cmds.registerCommand(new DebugCommand());
        cmds.registerCommand(new ParticleCommand());
        cmds.registerCommand(new SoundCommand());
        cmds.registerCommand(new RecipeCommand());
        cmds.registerCommand(new ExamineCommand());
        cmds.registerCommand(new RedrawCommand());
        cmds.registerCommand(new FriendCommand());
        cmds.registerCommand(new UnfriendCommand());
        cmds.registerCommand(new ValidateCommand());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            return cmds.dispatch(sender, command, label, args);
        } catch (DHUtilsException e) {
            MiscUtil.errorMessage(sender, e.getMessage());
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return cmds.onTabComplete(sender, command, label, args);
    }

    @Override
    public Object onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        if (key.equals("save_interval")) {
            DHValidate.isTrue((Integer) newVal > 0, "save_interval must be > 0");
        } else if (key.equals("energy.tick_rate")) {
            DHValidate.isTrue((Integer) newVal > 0, "energy.tick_rate must be > 0");
        } else if (key.startsWith("gui.texture.")) {
            STBUtil.parseMaterialSpec(newVal.toString());
        } else if (key.equals("inventory_protection")) {
            getEnumValue(newVal.toString().toUpperCase(), BlockProtection.InvProtectionType.class);
        } else if (key.equals("block_protection")) {
            getEnumValue(newVal.toString().toUpperCase(), BlockProtection.BlockProtectionType.class);
        } else if (key.equals("default_access")) {
            getEnumValue(newVal.toString().toUpperCase(), AccessControl.class);
        } else if (key.equals("default_redstone")) {
            getEnumValue(newVal.toString().toUpperCase(), RedstoneBehaviour.class);
        }
        return newVal;
    }

    @SuppressWarnings({ "unchecked" })
	private <T> T getEnumValue(String value, Class<T> c) {
        try {
            Method m = c.getMethod("valueOf", String.class);
            //noinspection unchecked
            return (T) m.invoke(null, value);
        } catch (Exception e) {
            if (!(e instanceof InvocationTargetException) || !(e.getCause() instanceof IllegalArgumentException)) {
                e.printStackTrace();
                LogUtils.severe(e.getMessage());
                throw new DHUtilsException(e.getMessage());
            } else {
                throw new DHUtilsException("Unknown value: " + value);
            }
        }
    }

    @Override
    public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        if (key.equals("debug_level")) {
            Debugger dbg = Debugger.getInstance();
            dbg.setLevel((Integer) newVal);
            if (dbg.getLevel() > 0) {
                dbg.setTarget(getServer().getConsoleSender());
            } else {
                dbg.setTarget(null);
            }
        } else if (key.equals("save_interval")) {
            LocationManager.getManager().setSaveInterval((Integer) newVal);
        } else if (key.equals("energy.tick_rate")) {
            scheduleEnergyNetTicker();
        } else if (key.startsWith("gui.texture.")) {
            STBInventoryGUI.buildStockTextures();
        } else if (key.equals("inventory_protection")) {
            blockProtection.setInvProtectionType(BlockProtection.InvProtectionType.valueOf(newVal.toString().toUpperCase()));
        } else if (key.equals("block_protection")) {
            blockProtection.setBlockProtectionType(BlockProtection.BlockProtectionType.valueOf(newVal.toString().toUpperCase()));
        } else if (key.equals("default_access")) {
            getConfigCache().setDefaultAccess(AccessControl.valueOf(newVal.toString().toUpperCase()));
        } else if (key.equals("default_redstone")) {
            getConfigCache().setDefaultRedstone(RedstoneBehaviour.valueOf(newVal.toString().toUpperCase()));
        } else if (key.equals("particle_effects")) {
            getConfigCache().setParticleLevel((Integer) newVal);
        } else if (key.equals("noisy_machines")) {
            getConfigCache().setNoisyMachines((Boolean) newVal);
        } else if (key.equals("creative_ender_access")) {
            getConfigCache().setCreativeEnderAccess((Boolean) newVal);
        }
    }

    private void scheduleEnergyNetTicker() {
        if (energyTask != null) energyTask.cancel();
        enetManager.setTickRate(getConfig().getLong("energy.tick_rate", EnergyNetManager.DEFAULT_TICK_RATE));
        energyTask = Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                enetManager.tick();
            }
        }, 1L, enetManager.getTickRate());
    }

    public ConfigurationManager getConfigManager() 			{			return configManager;														}
    public EnderStorageManager getEnderStorageManager() 	{			return enderStorageManager;													}
    public STBItemRegistry getItemRegistry() 				{			return itemRegistry;														}
    public FriendManager getFriendManager() 				{			return friendManager;														}
    public EnergyNetManager getEnergyNetManager() 			{			return enetManager;															}
    public boolean isWorldGuardAvailable() 					{			return worldGuardPlugin != null && worldGuardPlugin.isEnabled();			}
    public boolean isPreciousStonesAvailable() 				{			return preciousStonesPlugin != null && preciousStonesPlugin.isEnabled(); 	}
    public boolean isGriefPreventionAvailable() 				{			return griefPrevention != null && griefPrevention.isEnabled(); 	}
    public BlockProtection getBlockProtection() 			{			return blockProtection;														}
    public ConfigCache getConfigCache() 					{			return configCache;															}
    public MultiverseCore getMultiverseCore() 				{			return multiverseCore;														}
	public IDTracker getScuRelayIDTracker() 				{			return scuRelayIDTracker;													}
    public LWC getLWC()									 	{			return lwc;																	}
    public SoundMufflerListener getSoundMufflerListener() 	{			return soundMufflerListener;												}
    public PlayerUUIDTracker getUuidTracker() 				{			return uuidTracker;															}

	public boolean isGlowingEnabled() {
		return isProtocolLibEnabled() && getConfig().getBoolean("options.glowing_items");
	}
}
