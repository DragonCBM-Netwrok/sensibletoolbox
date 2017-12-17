package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.mrCookieSlime.sensibletoolbox.SensibleToolboxPlugin;
import me.mrCookieSlime.sensibletoolbox.blocks.ItemRouter;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.BlankModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.DirectionalItemRouterModule;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.MaterialData;

import java.util.List;

public class KillerModule extends DirectionalItemRouterModule {
    private static final MaterialData md = makeDye(DyeColor.BLACK);
    private static final ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE, 1);

    public KillerModule() {

    }

    public KillerModule(ConfigurationSection conf) {
        super(conf);
    }

    private String getKey(Location loc)
    {
        if (loc == null)
        {
            return "";
        }
        return  loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }
    @Override
    public boolean execute(Location loc) {
        if (!loc.getChunk().isLoaded())
        {
            return true;
        }

        try {
            int Damage = 0;

            for (ItemRouter.ModuleAndAmount MaM : getItemRouter().modules) {
                if (MaM.module instanceof KillerModule) {
                    Damage = MaM.amount;
                }
            }
            if (Damage > 10) {

                Damage = 10;
            }


            EntityPlayer npc = SensibleToolboxPlugin.getInstance().npcs.get(loc.getWorld().getName());
            npc.setLocation(loc.getX(), loc.getY(), loc.getZ(), 0, 0);
            CraftPlayer opCr = npc.getBukkitEntity();
            List<Entity> nearEntity = npc.getBukkitEntity().getNearbyEntities(5,5,5);
            for (int i = 0; i < nearEntity.size(); i++) {
                if (nearEntity.get(i).getType() != EntityType.PLAYER) {
                    if (nearEntity.get(i) instanceof LivingEntity) {
                        if (!nearEntity.get(i).isDead()) {
                            if (nearEntity.get(i).getLocation().distance(loc) < 5) {
                                if (nearEntity.get(i).getTicksLived() > 10) {
                                    try {
                                        ((LivingEntity) nearEntity.get(i)).damage(4 * Damage, opCr);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
            //npc.getBukkitEntity().remove();

            return true;
        }
        catch (Exception e)
        {
            return true;
        }


    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Mob Killer";
    }

    @Override
    public String[] getLore() {
        return new String[] {
                "Fires Of Titan",
        		"Insert into an Item Router", 
        		"Hurts mobs with in 5 Blocks",
                "Doesn't hurt players",
                "Doesn't collect items",
                "Stack up to 10."
        };
    }

    @Override
    public Recipe getRecipe() {
        BlankModule bm = new BlankModule();
        registerCustomIngredients(bm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(Material.DIAMOND_SWORD);
        return recipe;
    }

    protected ItemStack getBreakerTool() {
        return pick;
    }
}
