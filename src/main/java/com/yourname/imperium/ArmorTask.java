package com.yourname.imperium;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class ArmorTask extends BukkitRunnable {

    private final EnchantManager manager;

    public ArmorTask(EnchantManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkArmor(player);
            checkHand(player);
        }
    }

    private void checkArmor(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chest = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        // Speed (Boots)
        int speed = manager.getLevel(boots, EnchantKeys.SPEED);
        if (speed > 0) addEffect(player, PotionEffectType.SPEED, speed - 1);

        // Springs (Boots)
        int jump = manager.getLevel(boots, EnchantKeys.SPRINGS);
        if (jump > 0) addEffect(player, PotionEffectType.JUMP_BOOST, jump - 1);

        // Night Vision (Helmet)
        int vision = manager.getLevel(helmet, EnchantKeys.NIGHT_VISION);
        if (vision > 0) addEffect(player, PotionEffectType.NIGHT_VISION, 0);
        
        // Saturation (Helmet)
        int sat = manager.getLevel(helmet, EnchantKeys.SATURATION);
        if (sat > 0 && player.getFoodLevel() < 20) {
            // Slowly feed the player
            if (player.getTicksLived() % (100 / sat) == 0) { 
                player.setFoodLevel(player.getFoodLevel() + 1);
            }
        }

        // Obsidian Shield (Leggings - Fire Res)
        int shield = manager.getLevel(leggings, EnchantKeys.OBSIDIAN_SHIELD);
        if (shield > 0) addEffect(player, PotionEffectType.FIRE_RESISTANCE, 0);
    }

    private void checkHand(Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        
        // Haste (Dig Speed)
        int haste = manager.getLevel(tool, EnchantKeys.HASTE);
        if (haste > 0) addEffect(player, PotionEffectType.HASTE, haste - 1);
    }

    private void addEffect(Player p, PotionEffectType type, int amplifier) {
        // Apply for 40 ticks (2 seconds) so it doesn't flicker
        p.addPotionEffect(new PotionEffect(type, 40, amplifier, false, false, true));
    }
}