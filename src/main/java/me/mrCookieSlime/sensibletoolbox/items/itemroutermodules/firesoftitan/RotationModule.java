package me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.firesoftitan;

import me.desht.dhutils.Debugger;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.BlankModule;
import me.mrCookieSlime.sensibletoolbox.items.itemroutermodules.DirectionalItemRouterModule;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;

import java.util.UUID;

public class RotationModule extends DirectionalItemRouterModule {
    private static final Dye md = makeDye(DyeColor.ORANGE);

    @Override
    public boolean execute(Location loc) {
        return false;
    }

    public RotationModule() {
    }

    public RotationModule(ConfigurationSection conf) {
        super(conf);
    }

    @Override
    public String getItemName() {
        return "I.R. Mod: Rotation";
    }

    @Override
    public String[] getLore() {
        return new String[]{
                "Fires Of Titan",
                "Insert into an Item Router",
                "Passive module; Item Router will ",
                "be forced to rotate items,",
                "it will no take the same item twice."
        };
    }

    @Override
    public Recipe getRecipe() {
        BlankModule bm = new BlankModule();
        registerCustomIngredients(bm);
        ShapelessRecipe recipe = new ShapelessRecipe(toItemStack());
        recipe.addIngredient(bm.getMaterialData());
        recipe.addIngredient(Material.REDSTONE_COMPARATOR);
        return recipe;
    }

    @Override
    public MaterialData getMaterialData() {
        return md;
    }

    public int receiveItem(ItemStack item, UUID senderUUID) {
        int received = getItemRouter().insertItems(item, BlockFace.SELF, false, senderUUID);
        if (received > 0) {
            Debugger.getInstance().debug(2, "receiver in " + getItemRouter() + " received " + received + " of " + item +
                    ", now has " + getItemRouter().getBufferItem());
        }
        return received;
    }
}
