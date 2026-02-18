package com.yourname.imperium;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class BookListener implements Listener {

    private final ImperiumEnchants plugin;
    private final EnchantManager manager;

    public BookListener(ImperiumEnchants plugin, EnchantManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Check if cursor holds a book and clicked item is equipment
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();

        if (cursor == null || cursor.getType() != Material.ENCHANTED_BOOK) return;
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Try to find our custom key on the book
        // We stored it as "stored_<enchant_key>" in EnchantManager
        String foundKey = null;
        int level = 0;

        // This is a bit manual. In a pro plugin, you'd loop through registry.
        // For now, let's scan our known keys.
        String[] allKeys = {
            EnchantKeys.VAMPIRISM, EnchantKeys.THUNDERLORD, EnchantKeys.EXPLOSIVE, 
            EnchantKeys.SPEED, EnchantKeys.VEIN_MINER, EnchantKeys.AUTOSMELT,
            EnchantKeys.TELEKINESIS, EnchantKeys.NIGHT_VISION
        };

        for (String key : allKeys) {
            NamespacedKey dataKey = new NamespacedKey(plugin, "stored_" + key);
            if (cursor.getItemMeta().getPersistentDataContainer().has(dataKey, PersistentDataType.INTEGER)) {
                foundKey = key;
                level = cursor.getItemMeta().getPersistentDataContainer().get(dataKey, PersistentDataType.INTEGER);
                break;
            }
        }

        if (foundKey != null) {
            // Validate compatibility
            if (!manager.canEnchant(clicked, foundKey)) {
                return; // Let vanilla behavior happen (swap items)
            }

            // Apply Enchant
            event.setCancelled(true); // Stop the swap
            
            // Check if item already has higher level
            int currentLevel = manager.getLevel(clicked, foundKey);
            if (currentLevel >= level) {
                event.getWhoClicked().sendMessage(Component.text("Item already has a better enchantment!", NamedTextColor.RED));
                return;
            }

            // Apply new level
            manager.applyEnchant(clicked, foundKey, level);
            
            // Consume Book
            cursor.setAmount(cursor.getAmount() - 1);
            event.setCursor(cursor);

            // Effects
            Player player = (Player) event.getWhoClicked();
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            player.sendMessage(Component.text("Applied " + foundKey + " successfully!", NamedTextColor.GREEN));
        }
    }
}