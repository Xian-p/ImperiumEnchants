package com.yourname.imperium;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Random;

public class TableListener implements Listener {

    private final ImperiumEnchants plugin;
    private final EnchantManager manager;
    private final Random random = new Random();

    public TableListener(ImperiumEnchants plugin, EnchantManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        // Check Config requirements
        if (event.getExpLevelCost() < plugin.getConfig().getInt("min-table-cost")) return;

        double chance = plugin.getConfig().getDouble("table-chance");
        if (random.nextDouble() * 100 > chance) return;

        ItemStack item = event.getItem();

        // Try to find a valid enchant for this item type
        // We try 10 times to find a compatible one, if not, we give up (prevents infinite loops)
        for (int i = 0; i < 10; i++) {
            String randomKey = EnchantKeys.ALL_ENCHANTS[random.nextInt(EnchantKeys.ALL_ENCHANTS.length)];

            if (manager.canEnchant(item, randomKey)) {
                // Determine Level (1 or 2 mostly)
                int level = 1;
                if (random.nextBoolean()) level = 2;

                // Apply logic is tricky here because Event Item is a snapshot
                // We modify the item in the event directly
                manager.applyEnchant(item, randomKey, level);
                break;
            }
        }
    }
}