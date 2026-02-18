package com.yourname.imperium;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.*;

public class IndustrialListener implements Listener {

    private final EnchantManager manager;

    public IndustrialListener(EnchantManager manager) {
        this.manager = manager;
    }

    // --- MINING LOGIC ---
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        Block block = event.getBlock();

        // 1. Vein Miner
        int veinLevel = manager.getLevel(tool, EnchantKeys.VEIN_MINER);
        if (veinLevel > 0 && player.isSneaking() && isOre(block.getType())) {
            // Cap at 64 blocks to prevent lag
            breakVein(block, tool, veinLevel * 10, new HashSet<>());
        }

        // 2. Telekinesis & AutoSmelt & Experience
        int tele = manager.getLevel(tool, EnchantKeys.TELEKINESIS);
        int smelt = manager.getLevel(tool, EnchantKeys.AUTOSMELT);
        int xp = manager.getLevel(tool, EnchantKeys.EXPERIENCE);

        // If we have custom drop logic, cancel vanilla drops and do it ourselves
        if (tele > 0 || smelt > 0 || xp > 0) {
            event.setDropItems(false); // Disable vanilla drops
            
            // Handle XP
            if (xp > 0) {
                event.setExpToDrop(event.getExpToDrop() * (1 + xp));
            }

            // Handle Items
            for (ItemStack drop : block.getDrops(tool)) {
                ItemStack finalDrop = drop;

                // Auto Smelt Logic
                if (smelt > 0) finalDrop = getSmelted(drop);

                // Telekinesis Logic
                if (tele > 0) {
                    HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(finalDrop);
                    // Drop whatever didn't fit
                    for (ItemStack item : leftOver.values()) {
                        player.getWorld().dropItemNaturally(block.getLocation(), item);
                    }
                } else {
                    // Just drop it on the ground (smelted)
                    player.getWorld().dropItemNaturally(block.getLocation(), finalDrop);
                }
            }
        }
    }

    // --- ARMOR EVENTS ---
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Jelly Legs (No Fall Damage)
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            ItemStack boots = player.getInventory().getBoots();
            if (manager.getLevel(boots, EnchantKeys.JELLY_LEGS) > 0) {
                event.setCancelled(true);
            }
        }
    }

    // --- UTILS ---

    private void breakVein(Block start, ItemStack tool, int limit, Set<Block> visited) {
        if (visited.size() >= limit) return;
        visited.add(start);
        
        // Break the block naturally (triggers events, but we ignore recursion via isSneaking check usually)
        start.breakNaturally(tool);

        // Check all 26 neighbors
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Block target = start.getRelative(x, y, z);
                    
                    if (target.getType() == start.getType() && !visited.contains(target)) {
                        breakVein(target, tool, limit, visited);
                    }
                }
            }
        }
    }

    private boolean isOre(Material mat) {
        String name = mat.name();
        return name.endsWith("_ORE") || name.endsWith("_DEBRIS") || name.endsWith("_LOG") || name.endsWith("_WOOD");
    }

    private ItemStack getSmelted(ItemStack source) {
        Iterator<Recipe> iter = Bukkit.recipeIterator();
        while (iter.hasNext()) {
            Recipe r = iter.next();
            if (r instanceof FurnaceRecipe fr) {
                // If the input matches our item
                if (fr.getInput().getType() == source.getType()) {
                    ItemStack result = fr.getResult();
                    result.setAmount(source.getAmount());
                    return result;
                }
            }
        }
        return source; // No recipe found
    }
}