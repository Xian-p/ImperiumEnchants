package com.yourname.imperium;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;

public class AnvilListener implements Listener {

    private final EnchantManager manager;

    public AnvilListener(EnchantManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack left = event.getInventory().getItem(0);
        ItemStack right = event.getInventory().getItem(1);

        // Vanilla anvil logic handles the result generation, 
        // but it often wipes custom NBT or doesn't combine it.
        // We need to modify the Result item.
        ItemStack result = event.getResult();

        if (left == null || right == null || result == null) return;
        if (left.getType() == Material.AIR || right.getType() == Material.AIR) return;

        // We work on a clone of the result to add our custom data
        ItemStack finalResult = result.clone();
        ItemMeta resultMeta = finalResult.getItemMeta();
        PersistentDataContainer resultContainer = resultMeta.getPersistentDataContainer();

        boolean changed = false;

        // Loop through all known keys to see if we need to combine them
        // (In a real plugin, you'd have a list of all keys. Here we hardcode a few for the example or reflect)
        // Ideally, loop through ALL keys defined in EnchantKeys.
        // For this example, let's assume we check the Left Item's PDC keys.
        
        // Use a Set of keys found on the Left or Right item
        // Note: This requires us to know the keys. 
        // For simplicity, we just check if both items have the SAME custom enchant.

        String[] allKeys = {
            EnchantKeys.VAMPIRISM, EnchantKeys.THUNDERLORD, EnchantKeys.EXPLOSIVE, 
            EnchantKeys.SPEED, EnchantKeys.VEIN_MINER, EnchantKeys.AUTOSMELT
            // ... add all your keys here ...
        };

        for (String key : allKeys) {
            int leftLevel = manager.getLevel(left, key);
            int rightLevel = manager.getLevel(right, key);

            if (leftLevel > 0 || rightLevel > 0) {
                int newLevel = leftLevel;

                // Combine logic: 
                // Level 1 + Level 1 = Level 2
                // Level 2 + Level 1 = Level 2
                if (rightLevel == leftLevel) {
                    newLevel = leftLevel + 1;
                } else {
                    newLevel = Math.max(leftLevel, rightLevel);
                }
                
                // Cap max level (e.g., 5)
                if (newLevel > 5) newLevel = 5;

                // Apply to result
                manager.applyEnchant(finalResult, key, newLevel);
                changed = true;
            }
        }

        if (changed) {
            event.setResult(finalResult);
        }
    }
}