package com.yourname.imperium;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class EnchantManager {

    private final ImperiumEnchants plugin;

    public EnchantManager(ImperiumEnchants plugin) {
        this.plugin = plugin;
    }

    /**
     * Applies an enchantment to an item (Sword, Pickaxe, Armor, etc.)
     */
    public ItemStack applyEnchant(ItemStack item, String enchantKey, int level) {
        if (item == null || item.getType() == Material.AIR) return item;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 1. Store the level in the item's Persistent Data Container (PDC)
        NamespacedKey key = new NamespacedKey(plugin, enchantKey);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);

        // 2. Update the visual Lore so the player can see it
        updateLore(meta, enchantKey, level);
        
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Gets the level of an enchantment on an item. Returns 0 if not present.
     */
    public int getLevel(ItemStack item, String enchantKey) {
        if (item == null || item.getItemMeta() == null) return 0;
        
        NamespacedKey key = new NamespacedKey(plugin, enchantKey);
        return item.getItemMeta().getPersistentDataContainer()
                .getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    /**
     * Removes an enchantment from an item completely.
     */
    public void removeEnchant(ItemStack item, String enchantKey) {
        if (item == null || item.getItemMeta() == null) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, enchantKey);
        
        if (meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
            meta.getPersistentDataContainer().remove(key);
            
            // We also need to remove the Lore line
            List<Component> lore = meta.lore();
            if (lore != null) {
                String nameToRemove = formatName(enchantKey);
                lore.removeIf(component -> {
                    // Convert component to plain text to check if it matches
                     String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
                     return plain.startsWith(nameToRemove);
                });
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
    }

    /**
     * Creates a custom "Enchanted Book" item that can be dragged onto tools.
     */
    public ItemStack getEnchantBook(String enchantKey, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        
        // We use a different key prefix "stored_" so the book doesn't act like a weapon itself
        NamespacedKey dataKey = new NamespacedKey(plugin, "stored_" + enchantKey);
        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.INTEGER, level);
        
        // Set Display Name
        String niceName = formatName(enchantKey);
        meta.displayName(Component.text("Imperium Book: " + niceName + " " + toRoman(level))
            .color(NamedTextColor.GOLD)
            .decoration(TextDecoration.ITALIC, false));
            
        // Add Explanatory Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Drag and drop onto an item to apply.").color(NamedTextColor.GRAY));
        lore.add(Component.text("Enchant: " + niceName).color(NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        
        book.setItemMeta(meta);
        return book;
    }

    /**
     * Checks if a specific item type is allowed to hold a specific enchantment.
     */
    public boolean canEnchant(ItemStack item, String key) {
        String type = item.getType().name();
        
        boolean isSword = type.endsWith("_SWORD");
        boolean isAxe = type.endsWith("_AXE");
        boolean isBow = type.contains("BOW") || type.contains("CROSSBOW");
        boolean isTool = type.endsWith("_PICKAXE") || type.endsWith("_SHOVEL") || type.endsWith("_HOE") || isAxe;
        boolean isBoots = type.endsWith("_BOOTS");
        boolean isHelmet = type.endsWith("_HELMET");
        boolean isLeggings = type.endsWith("_LEGGINGS");

        return switch (key) {
            // Combat - Melee
            case EnchantKeys.VAMPIRISM, EnchantKeys.THUNDERLORD, EnchantKeys.VENOM, 
                 EnchantKeys.WITHER, EnchantKeys.FROZEN, EnchantKeys.BLIND, 
                 EnchantKeys.CONFUSION, EnchantKeys.DOUBLE_STRIKE, EnchantKeys.EXECUTE, 
                 EnchantKeys.BACKSTAB, EnchantKeys.INQUISITOR, EnchantKeys.GIANT_SLAYER 
                 -> isSword || isAxe;

            // Combat - Ranged
            case EnchantKeys.EXPLOSIVE, EnchantKeys.SNIPER, EnchantKeys.BLAZE 
                 -> isBow;

            // Industrial - Tools
            case EnchantKeys.VEIN_MINER, EnchantKeys.AUTOSMELT, EnchantKeys.TELEKINESIS, 
                 EnchantKeys.EXPERIENCE, EnchantKeys.HASTE 
                 -> isTool;

            // Armor
            case EnchantKeys.SPEED, EnchantKeys.SPRINGS, EnchantKeys.JELLY_LEGS -> isBoots;
            case EnchantKeys.NIGHT_VISION, EnchantKeys.SATURATION -> isHelmet;
            case EnchantKeys.OBSIDIAN_SHIELD -> isLeggings;
            
            default -> true; // If we forgot to define it, allow it by default
        };
    }

    // --- INTERNAL HELPERS ---

    private void updateLore(ItemMeta meta, String enchantKey, int level) {
        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();

        String formattedName = formatName(enchantKey);

        // Remove old level of THIS specific enchant to avoid duplicates
        // e.g., if it says "Explosive I", and we act "Explosive II", remove the old line.
        lore.removeIf(component -> {
             String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
             return plain.startsWith(formattedName);
        });

        // Create the new line
        Component display = Component.text(formattedName + " " + toRoman(level))
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);

        lore.add(display);
        meta.lore(lore);
    }

    private String formatName(String key) {
        // "giantslayer" -> "Giantslayer"
        // You can add spaces here if you want, e.g. .replace("_", " ")
        String name = key.replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private String toRoman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(level);
        };
    }
}