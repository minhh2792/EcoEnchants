package com.willfp.ecoenchants.util.internal.drops;

import com.willfp.ecoenchants.EcoEnchantsPlugin;
import com.willfp.ecoenchants.config.ConfigManager;
import com.willfp.ecoenchants.util.internal.DropQueue;
import com.willfp.ecoenchants.util.internal.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend implementation of {@link AbstractDropQueue}
 * {@link this#push()} adds to a map that creates a new {@link InternalDropQueue} at the end of every tick
 *
 * The drops are not instantly pushed when called, instead the map is iterated over at the end of every tick. This massively improves performance.
 */
public class FastCollatedDropQueue extends InternalDropQueue {
    private static boolean collate = false;
    private static final HashMap<Player, CollatedDrops> COLLATED_MAP = new HashMap<>();

    /**
     * Create {@link DropQueue} linked to player
     *
     * @param player The player
     */
    public FastCollatedDropQueue(Player player) {
        super(player);
    }

    @Override
    public void push() {
        CollatedDrops collatedDrops = COLLATED_MAP.containsKey(player) ? COLLATED_MAP.get(player).addDrops(items).setLocation(loc) : new CollatedDrops(items, loc, xp);
        COLLATED_MAP.put(player, collatedDrops);
    }

    public static boolean use() {
        return collate;
    }

    public static void update() {
        collate = ConfigManager.getConfig().getBool( "drops.collate");
    }

    static {
        Bukkit.getScheduler().runTaskTimer(EcoEnchantsPlugin.getInstance(), () -> {
            for (Map.Entry<Player, CollatedDrops> entry : COLLATED_MAP.entrySet()) {
                new InternalDropQueue(entry.getKey())
                        .setLocation(entry.getValue().getLocation())
                        .addItems(entry.getValue().getDrops())
                        .addXP(entry.getValue().getXp())
                        .push();
            }
            COLLATED_MAP.clear();
        }, 0, 1);

        update();
    }

    private static class CollatedDrops {
        private final List<ItemStack> drops;
        private Location location;
        private final int xp;

        private CollatedDrops(List<ItemStack> drops, Location location, int xp) {
            this.drops = drops;
            this.location = location;
            this.xp = xp;
        }

        public List<ItemStack> getDrops() {
            return drops;
        }

        public Location getLocation() {
            return location;
        }

        public int getXp() {
            return xp;
        }

        public CollatedDrops addDrops(List<ItemStack> toAdd) {
            drops.addAll(toAdd);
            return this;
        }

        public CollatedDrops setLocation(Location loc) {
            this.location = loc;
            return this;
        }

    }
}
