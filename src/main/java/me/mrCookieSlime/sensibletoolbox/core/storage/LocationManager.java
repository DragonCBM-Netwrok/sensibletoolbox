package me.mrCookieSlime.sensibletoolbox.core.storage;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.SensibleToolbox;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBBlock;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBItem;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.material.Sign;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LocationManager {
    private static LocationManager instance;

    private final Set<String> deferredBlocks = new HashSet<String>();
    private final PreparedStatement queryStmt;
    private PreparedStatement queryDelete;
    private final PreparedStatement queryTypeStmt;
    private long lastSave;
    private int saveInterval;  // ms
    private long totalTicks;
    private long totalTime;
    private final DBStorage dbStorage;
    private final Thread updaterTask;
    private static final BlockAccess blockAccess = new BlockAccess();

    // tracks those blocks (on a per-world basis) which need to do something on a server tick
    private final Map<UUID, Set<BaseSTBBlock>> allTickers = new HashMap<UUID, Set<BaseSTBBlock>>();
    // indexes all loaded blocks by world and (frozen) location
    private final Map<UUID, Map<String, BaseSTBBlock>> blockIndex = new HashMap<UUID, Map<String, BaseSTBBlock>>();
    // tracks the pending updates by (frozen) location since the last save was done
    private final Map<String, UpdateRecord> pendingUpdates = new HashMap<String, UpdateRecord>();
    // a blocking queue is used to pass actual updates over to the DB writer thread
    private final BlockingQueue<UpdateRecord> updateQueue = new LinkedBlockingQueue<UpdateRecord>();

    private LocationManager(SensibleToolboxPlugin plugin) throws SQLException {
        saveInterval = plugin.getConfig().getInt("save_interval", 30) * 1000;
        lastSave = System.currentTimeMillis();
        try {
            dbStorage = new DBStorage();
            dbStorage.getConnection().setAutoCommit(false);
            queryStmt = dbStorage.getConnection().prepareStatement("SELECT * FROM " + DBStorage.makeTableName("blocks") + " WHERE world_id = ?");
            queryTypeStmt = dbStorage.getConnection().prepareStatement("SELECT * FROM " + DBStorage.makeTableName("blocks") + " WHERE world_id = ? and type = ?");
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Unable to initialise DB storage: " + e.getMessage());
        }
        updaterTask = new Thread(new DBUpdaterTask(this));
        updaterTask.start();
    }

    public static synchronized LocationManager getManager() {
        if (instance == null) {
            try {
                instance = new LocationManager(SensibleToolboxPlugin.getInstance());
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        }
        return instance;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    DBStorage getDbStorage() {
        return dbStorage;
    }

    public void addTicker(BaseSTBBlock stb) {
        Location loc = stb.getLocation();
        World w = loc.getWorld();
        Set<BaseSTBBlock> tickerSet = allTickers.get(w.getUID());
        if (tickerSet == null) {
            tickerSet = Sets.newHashSet();
            allTickers.put(w.getUID(), tickerSet);
        }
        tickerSet.add(stb);
        Debugger.getInstance().debug(2, "Added ticking block " + stb);
    }

    private Map<String, BaseSTBBlock> getWorldIndex(World w) {
        Map<String,BaseSTBBlock> index = blockIndex.get(w.getUID());
        if (index == null) {
            index = Maps.newHashMap();
            blockIndex.put(w.getUID(), index);
        }
        return index;
    }

    public void registerLocation(Location loc, BaseSTBBlock stb, boolean isPlacing) {
        try {
            BaseSTBBlock stb2 = get(loc);
            if (stb2 != null) {
                LogUtils.warning("Attempt to register duplicate STB block " + stb + " @ " + loc + " - existing block " + stb2);
                return;
            }

            stb.setLocation(blockAccess, loc);

            String locStr = MiscUtil.formatLocation(loc);
            getWorldIndex(loc.getWorld()).put(locStr, stb);
            stb.preRegister(blockAccess, loc, isPlacing);

            if (isPlacing) {
                addPendingDBOperation(loc, locStr, UpdateRecord.Operation.INSERT);
            }

            if (stb.getTickRate() > 0) {
                addTicker(stb);
            }

            Debugger.getInstance().debug("Registered " + stb + " @ " + loc);
        }
        catch (Exception e)
        {
            deleteRow(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            LogUtils.warning("Found and Skipped 5");
        }
    }

    public void updateLocation(Location loc) {
        addPendingDBOperation(loc, MiscUtil.formatLocation(loc), UpdateRecord.Operation.UPDATE);
    }

    public void unregisterLocation(Location loc, BaseSTBBlock stb) {
        if (stb != null) {
            stb.onBlockUnregistered(loc);
            String locStr = MiscUtil.formatLocation(loc);
            addPendingDBOperation(loc, locStr, UpdateRecord.Operation.DELETE);
            getWorldIndex(loc.getWorld()).remove(locStr);
            Debugger.getInstance().debug("Unregistered " + stb + " @ " + loc);
        } else {
            LogUtils.warning("Attempt to unregister non-existent STB block @ " + loc);
        }
    }

    /**
     * Move an existing STB block to a new location.  Note that this method doesn't do any
     * redrawing of blocks.
     *
     * @param oldLoc the STB block's old location
     * @param newLoc the STB block's new location
     */
    public void moveBlock(BaseSTBBlock stb, Location oldLoc, Location newLoc) {

        // TODO: translate multi-block structures

        String locStr = MiscUtil.formatLocation(oldLoc);
        addPendingDBOperation(oldLoc, locStr, UpdateRecord.Operation.DELETE);
        getWorldIndex(oldLoc.getWorld()).remove(locStr);

        stb.moveTo(blockAccess, oldLoc, newLoc);

        locStr = MiscUtil.formatLocation(newLoc);
        addPendingDBOperation(newLoc, locStr, UpdateRecord.Operation.INSERT);
        getWorldIndex(newLoc.getWorld()).put(locStr, stb);

        Debugger.getInstance().debug("moved " + stb + " from " + oldLoc + " to " + newLoc);
    }

    private void addPendingDBOperation(Location loc, String locStr, UpdateRecord.Operation op) {
        UpdateRecord existingRec = pendingUpdates.get(locStr);
        switch (op) {
            case INSERT:
                if (existingRec == null) {
                    // brand new insertion
                    pendingUpdates.put(locStr, new UpdateRecord(UpdateRecord.Operation.INSERT, loc));
                } else if (existingRec.getOp() == UpdateRecord.Operation.DELETE) {
                    // re-inserting where a block was just deleted
                    pendingUpdates.put(locStr, new UpdateRecord(UpdateRecord.Operation.UPDATE, loc));
                }
                break;
            case UPDATE:
                if (existingRec == null || existingRec.getOp() != UpdateRecord.Operation.INSERT) {
                    pendingUpdates.put(locStr, new UpdateRecord(UpdateRecord.Operation.UPDATE, loc));
                }
                break;
            case DELETE:
                if (existingRec != null && existingRec.getOp() == UpdateRecord.Operation.INSERT) {
                    // remove a recent insertion
                    pendingUpdates.remove(locStr);
                } else {
                    pendingUpdates.put(locStr, new UpdateRecord(UpdateRecord.Operation.DELETE, loc));
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected operation: " + op);
        }
    }


    /**
     * Get the STB block at the given location.
     *
     * @param loc the location to check at
     * @return the STB block at the given location, or null if no matching item
     */
    public BaseSTBBlock get(Location loc) {
        return get(loc, false);
    }

    /**
     * Get the STB block at the given location, or if the location contains a
     * sign, possibly at the location the sign is attached to.
     *
     * @param loc the location to check at
     * @param checkSigns if true, and the location contains a sign, check at
     *                   the location that the sign is attached to
     * @return the STB block at the given location, or null if no matching item
     */
    public BaseSTBBlock get(Location loc, boolean checkSigns) {
        Block b = loc.getBlock();
        if (checkSigns && (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST)) {
            Sign sign = (Sign) b.getState().getData();
            b = b.getRelative(sign.getAttachedFace());
        }
        BaseSTBBlock stb = (BaseSTBBlock) STBUtil.getMetadataValue(b, BaseSTBBlock.STB_BLOCK);
        if (stb != null) {
            return stb;
        } else {
            // perhaps it's part of a multi-block structure
            return (BaseSTBBlock) STBUtil.getMetadataValue(b, BaseSTBBlock.STB_MULTI_BLOCK);
        }
    }

    /**
     * Get the STB block of the given type at the given location.
     *
     * @param loc  the location to check at
     * @param type the type of STB block required
     * @param <T>  a subclass of BaseSTBBlock
     * @return the STB block at the given location, or null if no matching item
     */
    public <T extends BaseSTBBlock> T get(Location loc, Class<T> type) {
        return get(loc, type, false);
    }

    /**
     * Get the STB block of the given type at the given location.
     *
     * @param loc  the location to check at
     * @param type the type of STB block required
     * @param <T>  a subclass of BaseSTBBlock
     * @param checkSigns if true, and the location contains a sign, check at
     *                   the location that the sign is attached to
     * @return the STB block at the given location, or null if no matching item
     */
    @SuppressWarnings("unchecked")
	public <T extends BaseSTBBlock> T get(Location loc, Class<T> type, boolean checkSigns) {
        BaseSTBBlock stbBlock = get(loc, checkSigns);
        if (stbBlock != null && type.isAssignableFrom(stbBlock.getClass())) {
            //noinspection unchecked
            return (T) stbBlock;
        } else {
            return null;
        }
    }

    /**
     * Get all the STB blocks in the given chunk
     *
     * @param chunk the chunk to check
     * @return an array of STB block objects
     */
    public List<BaseSTBBlock> get(Chunk chunk) {
        List<BaseSTBBlock> res = new ArrayList<BaseSTBBlock>();
        for (BaseSTBBlock stb : listBlocks(chunk.getWorld(), false)) {
            PersistableLocation pLoc = stb.getPersistableLocation();
            if ((int) pLoc.getX() >> 4 == chunk.getX() && (int) pLoc.getZ() >> 4 == chunk.getZ()) {
                res.add(stb);
            }
        }
        return res;
    }

    public void tick() {
        long now = System.nanoTime();
        for (World w : Bukkit.getWorlds()) {
            Set<BaseSTBBlock> tickerSet = allTickers.get(w.getUID());
            if (tickerSet != null) {
                Iterator<BaseSTBBlock> iter = tickerSet.iterator();
                while (iter.hasNext()) {
                    BaseSTBBlock stb = iter.next();
                    if (stb.isPendingRemoval()) {
                        Debugger.getInstance().debug("Removing block " + stb + " from tickers list");
                        iter.remove();
                    } else {
                        PersistableLocation pLoc = stb.getPersistableLocation();
                        int x = (int) pLoc.getX(), z = (int) pLoc.getZ();
                        if (w.isChunkLoaded(x >> 4, z >> 4)) {
                            stb.tick();
                            if (stb.getTicksLived() % stb.getTickRate() == 0) {
                                stb.onServerTick();
                            }
                        }
                    }
                }
            }
        }
        totalTicks++;
        totalTime += System.nanoTime() - now;
//		System.out.println("tickers took " + (System.nanoTime() - now) + " ns");
        if (System.currentTimeMillis() - lastSave > saveInterval) {
            save();
        }
    }

    public void save() {
        // send any pending updates over to the DB updater thread via a BlockingQueue
        if (!pendingUpdates.isEmpty()) {
            // TODO: may want to do this over a few ticks to reduce the risk of lag spikes
            for (UpdateRecord rec : pendingUpdates.values()) {
                if (rec != null) {
                    if (rec.getLocation() != null) {
                        BaseSTBBlock stb = get(rec.getLocation());
                        if (stb == null && rec.getOp() != UpdateRecord.Operation.DELETE) {
                            LogUtils.severe("STB block @ " + rec.getLocation() + " is null, but should not be!");
                            continue;
                        }
                        if (stb != null) {
                            rec.setType(stb.getItemTypeID());
                            rec.setData(stb.freeze().saveToString());
                        }
                        updateQueue.add(rec);
                    }
                }
            }
            updateQueue.add(UpdateRecord.commitRecord());
            pendingUpdates.clear();
        }
        lastSave = System.currentTimeMillis();
    }
    public int  getRSSize(World world, String wantedType ) {
        try {
            if (wantedType == null) {
                queryDelete = dbStorage.getConnection().prepareStatement("SELECT COUNT(*) FROM " + DBStorage.makeTableName("blocks") + " WHERE world_id = ?");
                queryDelete.setString(1, world.getUID().toString());
            }
            else
            {
                queryDelete = dbStorage.getConnection().prepareStatement("SELECT COUNT(*) FROM " + DBStorage.makeTableName("blocks") + " WHERE world_id = ? and type = ?");
                queryDelete.setString(1, world.getUID().toString());
                queryDelete.setString(2, wantedType);
            }
            ResultSet rs = queryDelete.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
        catch (Exception e) {
            return  -1;
        }
    }
    public void deleteRow(World world, int x, int y, int z) {
        try {
            //LogUtils.severe("Delete Record: Start");
            queryDelete = dbStorage.getConnection().prepareStatement("DELETE FROM " + DBStorage.makeTableName("blocks") + " WHERE world_id = ? and x = ? and y = ? and z = ?");
            queryDelete.setString(1, world.getUID().toString());
            queryDelete.setInt(2, x);
            queryDelete.setInt(3, y);
            queryDelete.setInt(4, z);
            int number = queryDelete.executeUpdate();
            //LogUtils.severe("Delete Record: End with " +number);
        }
        catch (Exception e) {
            LogUtils.severe("Delete Record. ERROR");
            e.printStackTrace();
            LogUtils.severe(e.getMessage());
        }
    }
    public boolean checkLocation(Location loc)
    {
        try {
            Block b = loc.getBlock();
            return  true;
        }
        catch (Exception e)
        {
            deleteRow(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            System.out.println("[SensibleToolbox]: Found and Skipped 4");
            return false;
        }
    }
    private static void printProgress(long startTime, long total, long current) {
        long eta = current == 0 ? 0 :
                (total - current) * (System.currentTimeMillis() - startTime) / current;

        String etaHms = current == 0 ? "N/A" :
                String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                        TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

        StringBuilder string = new StringBuilder(140);
        int percent = (int) (current * 100 / total);
        string
                .append('\r')
                .append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
                .append(String.format(" %d%% [", percent))
                .append(String.join("", Collections.nCopies(percent, "=")))
                .append('>')
                .append(String.join("", Collections.nCopies(100 - percent, " ")))
                .append(']')
                .append(String.join("", Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
                .append(String.format(" %d/%d, ETA: %s", current, total, etaHms));

        System.out.print(string);
    }
    public void loadFromDatabase(World world, String wantedType) throws SQLException {
        ResultSet rs;
        if (wantedType == null) {
            queryStmt.setString(1, world.getUID().toString());
            rs = queryStmt.executeQuery();
        } else {
            queryTypeStmt.setString(1, world.getUID().toString());
            queryTypeStmt.setString(2, wantedType);
            rs = queryTypeStmt.executeQuery();
        }
        HashMap<Location, Boolean> protectList = new HashMap<Location, Boolean>();
        for (int i = 0; i<20;i++)
        {
            String ListSplit = (String) SensibleToolboxPlugin.getInstance().getConfig().get("corrupted_block_" +i);
            if (ListSplit== null || ListSplit.equals(""))
            {
                break;
            }
            String[] Splitted = ListSplit.split(",");
            if (Splitted.length > 3) {
                Location tmp = new Location(Bukkit.getWorld(Splitted[0]), Integer.parseInt(Splitted[1]), Integer.parseInt(Splitted[2]), Integer.parseInt(Splitted[3]));
                protectList.put(tmp.clone(), false);
            }
            SensibleToolboxPlugin.getInstance().getConfig().set("corrupted_block_" + i,  null);
            SensibleToolboxPlugin.getInstance().getConfigManager().getPlugin().saveConfig();
        }

        if (protectList.size() > 0) {
            SensibleToolboxPlugin.getInstance().getConfig().set("corrupted_block_list", protectList);
            SensibleToolboxPlugin.getInstance().getConfigManager().getPlugin().saveConfig();
        }

        protectList = getProtectList();

        int HowMany = 0;
        for (Location e: protectList.keySet())
        {
            if (e.getWorld().getName().equals(world.getName()))
            {
                HowMany++;
            }
        }
        System.out.println("[SensibleToolbox]: " + world.getName() +  " is protected for " + HowMany + "/" + protectList.size() + " bad blocks!");
        int sizeRS = getRSSize(world,wantedType);
        System.out.println("[SensibleToolbox]: Loading: " + sizeRS + " "+ "");
        long totalbeforetime = System.currentTimeMillis();
        while (rs.next()) {


            String type = rs.getString(5);
            if (deferredBlocks.contains(type) && !type.equals(wantedType)) {
                continue;
            }
            int x = rs.getInt(2);
            int y = rs.getInt(3);
            int z = rs.getInt(4);
            Boolean Skip = false;
            for (Location e: protectList.keySet())
            {
                if (world.getName().equals(e.getWorld().getName()) && e.getBlockX() == x && e.getBlockY() == y && e.getBlockZ() == z)
                {
                    protectList.put(e, true);
                    System.out.println("[SensibleToolbox]: Found and Skipped");
                    Skip = true;
                    break;
                }
            }
            if (Skip == true)
            {
                deleteRow(world, x, y, z);
                continue;
            }
            //LogUtils.warning(data);
            long globalpass =  System.currentTimeMillis();;
            long beforetime = System.currentTimeMillis();
            long aftertime = System.currentTimeMillis();

            String data = rs.getString(6);
            long count = 0;
            try {
                beforetime = System.currentTimeMillis();
                YamlConfiguration conf = new YamlConfiguration();
                if (data.length() > 100000)
                {
                    deleteRow(world, x, y, z);
                    System.out.println("[SensibleToolbox]: Found and Skipped 6");
                    continue;
                }
                conf.loadFromString(data);
                if (data == null || conf == null)
                {
                    deleteRow(world, x, y, z);
                    System.out.println("[SensibleToolbox]: Found and Skipped 2");
                    continue;
                }

                BaseSTBItem stbItem = SensibleToolbox.getItemRegistry().getItemById(type, conf);


                if (stbItem != null) {

                    Location loc = new Location(world, x, y, z);
                    if (!checkLocation(loc))
                    {
                        continue;
                    }
                    if (stbItem instanceof BaseSTBBlock) {
                        if (((BaseSTBBlock) stbItem).getOwner() != null) {
                            Long lastplayed = Bukkit.getOfflinePlayer(((BaseSTBBlock) stbItem).getOwner()).getLastPlayed();
                            Long OffLine = System.currentTimeMillis() - lastplayed;
                            int days = (int) (OffLine / (1000 * 60 * 60 * 24));
                            if (days > 90)
                            {
                                deleteRow(world, x, y, z);
                                System.out.println("[SensibleToolbox]: Outofdate: " + Bukkit.getOfflinePlayer(((BaseSTBBlock) stbItem).getOwner()).getName() + " days: " + days);
                                continue;
                            }
                        }


                        registerLocation(loc, (BaseSTBBlock) stbItem, false);
                    } else {
                        LogUtils.severe("STB item " + type + " @ " + loc + " is not a block!");
                    }
                } else {
                    // defer it - should hopefully be registered by another plugin later
                    Debugger.getInstance().debug("deferring load for unrecognised block type '" + type + "'");
                    deferBlockLoad(type);
                }
                aftertime = System.currentTimeMillis();
                long passed = aftertime - beforetime;
                if (passed > 100)
                {
                    LogUtils.severe("Length: " + data.length() + " Millsecond: " + passed + " " + world.getName() + "," + x + "," + y  + "," +  z);
                }
                if (passed > 1000)
                {
                    LogUtils.severe((passed/1000) + " Second: " + world.getName() + "," + x + "," + y  + "," +  z);
                    protectList.put(new Location(world, x, y, z).clone(), true);
                }
                count++;
                if (System.currentTimeMillis() - globalpass > 10000)
                {
                    globalpass= System.currentTimeMillis();
                    System.out.println("[SensibleToolbox]: [SensibleToolbox](" + count + "): Still Loading...");
                }
            } catch (Exception e) {
                e.printStackTrace();
                LogUtils.severe(e.getMessage());
                LogUtils.severe(String.format("Can't load STB block at %s,%d,%d,%d: %s", world.getName(), x, y, z, e.getMessage()));
                deleteRow(world, x, y, z);
                System.out.println("[SensibleToolbox]: Found and Skipped 3");
            }
        }
        long totalPassed = System.currentTimeMillis() - totalbeforetime;


        String time = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(totalPassed),
                TimeUnit.MILLISECONDS.toSeconds(totalPassed) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalPassed))
        );

        System.out.println("[SensibleToolbox]: World: " + world.getName() + " done in: " + time + " with " + sizeRS + " Blocks loaded");
        List<Location> tmpRemover = new ArrayList<Location>();

        for (Location e: protectList.keySet()) {
            if (world.getName().equals(e.getWorld().getName()))
            {
                tmpRemover.add(e);
            }
        }
        for (Location e: tmpRemover) {
            protectList.remove(e);
        }
        SensibleToolboxPlugin.getInstance().getConfig().set("corrupted_block_list", protectList);
        SensibleToolboxPlugin.getInstance().getConfigManager().getPlugin().saveConfig();


        }

    private HashMap<Location, Boolean> getProtectList() {
        HashMap<Location, Boolean> protectList;
        try {
            protectList = (HashMap<Location, Boolean>) SensibleToolboxPlugin.getInstance().getConfig().get("corrupted_block_list");
        }
        catch (Exception e)
        {
            protectList = new HashMap<Location, Boolean>();
        }
        if (protectList == null)
        {
            protectList = new HashMap<Location, Boolean>();
        }
        return protectList;
    }

    public void load() throws SQLException {
        for (World w : Bukkit.getWorlds()) {
            loadFromDatabase(w, null);
        }
    }

    private void deferBlockLoad(String typeID) {
        deferredBlocks.add(typeID);
    }

    /**
     * Load all blocks for the given block type.  Called when a block is registered after the
     * initial DB load is done.
     *
     * @param type the block type
     * @throws SQLException if there is a problem loading from the DB
     */
    public void loadDeferredBlocks(String type) throws SQLException {
        if (deferredBlocks.contains(type)) {
            for (World world : Bukkit.getWorlds()) {
                loadFromDatabase(world, type);
            }
            deferredBlocks.remove(type);
        }
    }

    /**
     * The given world has just become unloaded..
     *
     * @param world the world that has been unloaded
     */
    public void worldUnloaded(World world) {
        save();

        Map<String, BaseSTBBlock> map = blockIndex.get(world.getUID());
        if (map != null) {
            map.clear();
            blockIndex.remove(world.getUID());
        }
    }

    /**
     * The given world has just become loaded.
     *
     * @param world the world that has been loaded
     */
    public void worldLoaded(World world) {
        if (!blockIndex.containsKey(world.getUID())) {
            try {
                loadFromDatabase(world, null);
            } catch (SQLException e) {
                e.printStackTrace();
                LogUtils.severe(e.getMessage());
                LogUtils.severe("can't load STB data for world " + world.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Get a list of all STB blocks for the given world.
     *
     * @param world  the world to query
     * @param sorted if true, the array is sorted by block type
     * @return an array of STB block objects
     */
    public List<BaseSTBBlock> listBlocks(World world, boolean sorted) {
        Collection<BaseSTBBlock> list = getWorldIndex(world).values();
        return sorted ? MiscUtil.asSortedList(list) : Lists.newArrayList(list);
    }

    /**
     * Get the average time in nanoseconds that the plugin has spent ticking tickable blocks
     * since the plugin started up.
     *
     * @return the average time spent ticking blocks
     */
    public long getAverageTimePerTick() {
        return totalTime / totalTicks;
    }

    /**
     * Set the save interval; any changes will be written to the persisted DB this often.
     *
     * @param saveInterval the save interval, in seconds.
     */
    public void setSaveInterval(int saveInterval) {
        this.saveInterval = saveInterval * 1000;
    }

    /**
     * Shut down the location manager after ensuring all pending changes are written to the DB,
     * and the DB thread has exited.  This may block the main thread for a short time, but should only
     * be called when the plugin is being disabled.
     */
    public void shutdown() {
        updateQueue.add(UpdateRecord.finishingRecord());
        try {
            // 5 seconds is hopefully enough for the DB thread to finish its work
            updaterTask.join(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            LogUtils.severe(e.getMessage());
        }
        try {
            dbStorage.getConnection().close();
        } catch (SQLException e) {
            e.printStackTrace();
            LogUtils.severe(e.getMessage());
        }
    }

    UpdateRecord getUpdateRecord() throws InterruptedException {
        return updateQueue.take();
    }

    public static class BlockAccess {
        // this is a little naughty, but it lets us call public methods
        // in BaseSTBBlock which we don't want everyone else to call
        private BlockAccess() { }
    }
}
