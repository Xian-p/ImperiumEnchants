package com.yourname.imperium;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ImperiumEnchants extends JavaPlugin implements CommandExecutor {

    private EnchantManager enchantManager;

    @Override
    public void onEnable() {
        // 1. Initialize the Manager
        this.enchantManager = new EnchantManager(this);

        // 2. Register all Listeners (The logic for the enchants)
        // Part 1: Combat (Swords/Bows)
        Bukkit.getPluginManager().registerEvents(new CombatListener(enchantManager), this);
        // Part 2: Industrial (Mining/Tools/Armor Events)
        Bukkit.getPluginManager().registerEvents(new IndustrialListener(enchantManager), this);
        // Part 3: Survival (Anvils and Books)
        Bukkit.getPluginManager().registerEvents(new AnvilListener(enchantManager), this);
        Bukkit.getPluginManager().registerEvents(new BookListener(this, enchantManager), this);

        // 3. Start the Passive Effects Task (Runs every 1 second)
        // This handles Speed, Night Vision, Regeneration, etc.
        new ArmorTask(enchantManager).runTaskTimer(this, 20L, 20L);

        // 4. Register the Admin Command
        if (getCommand("giveenchant") != null) {
            getCommand("giveenchant").setExecutor(this);
        }

        getLogger().info("ImperiumEnchants (Full Version) has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ImperiumEnchants disabled.");
    }

    /**
     * Admin Command Handler
     * Usage: /giveenchant <id> <level> [book]
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        // Only players can use this command (because they have inventories)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        // Permission check
        if (!player.hasPermission("imperium.admin")) {
            player.sendMessage(Component.text("You do not have permission to use this.", NamedTextColor.RED));
            return true;
        }

        // Validate arguments
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /giveenchant <id> <level> [book]", NamedTextColor.RED));
            return true;
        }

        String key = args[0].toLowerCase();
        int level;

        // Validate Level is a number
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Level must be a valid number.", NamedTextColor.RED));
            return true;
        }

        // Cap level (optional safety)
        if (level < 1) level = 1;
        if (level > 10) level = 10;

        // MODE 1: Give Enchanted Book
        if (args.length > 2 && args[2].equalsIgnoreCase("book")) {
            ItemStack book = enchantManager.getEnchantBook(key, level);
            
            // Add to inventory or drop if full
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(book);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), book);
            }
            
            player.sendMessage(Component.text("Received Book: " + key + " " + level, NamedTextColor.GREEN));
            return true;
        }

        // MODE 2: Apply to Item in Hand
        ItemStack handItem = player.getInventory().getItemInMainHand();
        
        if (handItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("You must hold an item to enchant it!", NamedTextColor.RED));
            return true;
        }

        // Apply the enchant
        player.getInventory().setItemInMainHand(
            enchantManager.applyEnchant(handItem, key, level)
        );
        
        player.sendMessage(Component.text("Applied " + key + " " + level + " to your item.", NamedTextColor.GREEN));
        return true;
    }
}