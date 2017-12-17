package me.mrCookieSlime.sensibletoolbox.core.energy;

import com.google.common.base.Joiner;
import me.desht.dhutils.Debugger;
import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.api.energy.ChargeableBlock;
import me.mrCookieSlime.sensibletoolbox.api.energy.EnergyNet;
import me.mrCookieSlime.sensibletoolbox.api.items.BaseSTBMachine;
import me.mrCookieSlime.sensibletoolbox.api.util.STBUtil;
import me.mrCookieSlime.sensibletoolbox.core.storage.LocationManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;

public class EnergyNetManager {
	
    public static final long DEFAULT_TICK_RATE = 10;

    private long tickRate = DEFAULT_TICK_RATE;

    private final Map<Integer, STBEnergyNet> allNets = new HashMap<Integer, STBEnergyNet>();
    private final SensibleToolboxPlugin plugin;

    public EnergyNetManager(SensibleToolboxPlugin plugin) {
        this.plugin = plugin;
    }

    public long getTickRate() {
        return tickRate;
    }

    public void setTickRate(long tickRate) {
        this.tickRate = tickRate;
    }

    /**
     * Get the energy net this block is in, if any.
     *
     * @param block the block to check
     * @return the block's energy net, or null if none
     */
    public STBEnergyNet getEnergyNet(Block block) {
        Integer netId = (Integer) STBUtil.getMetadataValue(block, STBEnergyNet.STB_ENET_ID);
        return netId == null ? null : allNets.get(netId);
    }

    /**
     * Given a cable which has just been placed, check what energy nets and machines, if any,
     * are adjacent to it, and act accordingly.
     *
     * @param cable the newly placed cable
     */
    public void onCablePlaced(Block cable) {
        Set<Integer> netIds = getAdjacentNets(cable);
        if (Debugger.getInstance().getLevel() > 1) {
            Debugger.getInstance().debug(2,
                    "new cable " + cable + " has " + netIds.size() + " adjacent nets [" + Joiner.on(",").join(netIds) + "]");
        }
        List<AdjacentMachine> adjacentMachines;
        switch (netIds.size()) {
            case 0:
                // not connected to any net, start a new one IFF there is one or more adjacent machines
                adjacentMachines = getAdjacentMachines(cable);
                if (!adjacentMachines.isEmpty()) {
                    STBEnergyNet newNet = STBEnergyNet.buildNet(cable, this);
                    allNets.put(newNet.getNetID(), newNet);
                    addConnectedCables(cable, newNet);
                }
                break;
            case 1:
                // connected to a single net; just add this cable to that net
                Integer[] id = netIds.toArray(new Integer[1]);
                STBEnergyNet net = allNets.get(id[0]);
                net.addCable(cable);
                // attach any adjacent machines
                adjacentMachines = getAdjacentMachines(cable);
                for (AdjacentMachine record : adjacentMachines) {
                    net.addMachine(record.getMachine(), record.getDirection().getOppositeFace());
                }
                // and any connected cable which isn't part of a net
                addConnectedCables(cable, net);
                break;
            default:
                // connected to more than one different net!
                // delete those nets, then re-scan and build a new single unified net
                for (int netId : netIds) {
                    deleteEnergyNet(netId);
                }
                STBEnergyNet newNet = STBEnergyNet.buildNet(cable, this);
                allNets.put(newNet.getNetID(), newNet);
        }
    }

    public void onCableRemoved(Block cable) {
        STBEnergyNet thisNet = getEnergyNet(cable);
        if (thisNet == null) {
            // cable with no net at all?
            return;
        }

        Debugger.getInstance().debug(2, "removing cable " + cable + " from enet #" + thisNet.getNetID());

        // scan this cable's neighbours to see what it was attached to
        final List<Block> attachedCables = new ArrayList<Block>();
        final List<BaseSTBMachine> attachedMachines = new ArrayList<BaseSTBMachine>();
        for (BlockFace face : STBUtil.directFaces) {
            Block b = cable.getRelative(face);
            if (STBUtil.isCable(b)) {
                attachedCables.add(b);
            } else {
                BaseSTBMachine machine = LocationManager.getManager().get(b.getLocation(), BaseSTBMachine.class);
                if (machine != null) {
                    attachedMachines.add(machine);
                }
            }
        }

        if (attachedCables.size() == 1 && attachedMachines.isEmpty()) {
            // simple case; cable attached to only one other cable - no need to delete the net
            thisNet.removeCable(cable);
        } else {
            // delete the energy net for the removed cable; this will also detach any machines
            deleteEnergyNet(thisNet.getNetID());
            if (attachedCables.size() > 0) {
                // need a delayed task here, since the block for the cable being removed isn't
                // actually updated to air yet...
                final EnergyNetManager mgr = this;
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        // rebuild energy nets for the deleted cable's neighbours
                        for (Block b : attachedCables) {
                            // those neighbours could have another path to each other
                            EnergyNet net1 = getEnergyNet(b);
                            if (net1 == null) {
                                STBEnergyNet newNet1 = STBEnergyNet.buildNet(b, mgr);
                                allNets.put(newNet1.getNetID(), newNet1);
                            }
                        }
                    }
                });
            }
        }
    }

    public void onMachinePlaced(ChargeableBlock machine) {
        Block b = machine.getLocation().getBlock();
        // scan adjacent blocks for cables
        for (BlockFace face : STBUtil.directFaces) {
            Block cable = b.getRelative(face);
            if (STBUtil.isCable(cable)) {
                STBEnergyNet net = getEnergyNet(cable);
                if (net == null) {
                    // cable with no net - create one!
                    STBEnergyNet newNet = STBEnergyNet.buildNet(cable, this);
                    newNet.addMachine(machine, face);
                    allNets.put(newNet.getNetID(), newNet);
                } else {
                    // cable on a net - add machine to it
                    net.addMachine(machine, face);
                }
            }
        }
    }

    public void onMachineRemoved(ChargeableBlock machine) {
        for (EnergyNet net : machine.getAttachedEnergyNets()) {
            ((STBEnergyNet) net).removeMachine(machine);
        }
    }

    /**
     * Recursively scan for any cable which is not currently part of an energy net,
     * and add it to the given net.
     *
     * @param start block to scan from
     * @param net   net to add cabling to
     */
    private void addConnectedCables(Block start, STBEnergyNet net) {
        for (BlockFace face : STBUtil.directFaces) {
            Block b = start.getRelative(face);
            if (STBUtil.isCable(b)) {
                EnergyNet net2 = getEnergyNet(b);
                if (net2 == null) {
                    net.addCable(b);
                    addConnectedCables(b, net);
                }
            }
        }
    }

    private static List<AdjacentMachine> getAdjacentMachines(Block cable) {
        final List<AdjacentMachine> attachedMachines = new ArrayList<AdjacentMachine>();
        for (BlockFace face : STBUtil.directFaces) {
            Block b = cable.getRelative(face);
            BaseSTBMachine machine = LocationManager.getManager().get(b.getLocation(), BaseSTBMachine.class);
            if (machine != null) {
                attachedMachines.add(new AdjacentMachine(machine, face));
            }
        }
        return attachedMachines;
    }

    /**
     * Get all the energy nets that the given block is attached to.
     *
     * @param startBlock the block to check
     * @return set of up to 6 integers, representing energy net IDs
     */
    private Set<Integer> getAdjacentNets(Block startBlock) {
        Set<Integer> res = new HashSet<Integer>();
        for (BlockFace face : STBUtil.directFaces) {
            EnergyNet net = getEnergyNet(startBlock.getRelative(face));
            if (net != null) {
                res.add(net.getNetID());
            }
        }
        return res;
    }

    private void deleteEnergyNet(int netID) {
        STBEnergyNet enet = allNets.get(netID);
        if (enet != null) {
            enet.shutdown();
            allNets.remove(netID);
        }
    }

    public void tick() {
        for (STBEnergyNet net : allNets.values()) {
            net.tick();
        }
    }
}
