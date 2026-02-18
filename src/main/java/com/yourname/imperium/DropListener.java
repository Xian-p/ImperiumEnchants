package com.yourname.imperium;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import java.util.Random;

public class DropListener implements Listener {
    private final ImperiumEnchants plugin;
    private final EnchantManager manager;
    private final Random r = new Random();

    public DropListener(ImperiumEnchants plugin, EnchantManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!plugin.getConfig().getBoolean("allow-passive-mobs") && !(e.getEntity() instanceof org.bukkit.entity.Monster)) return;
        if (plugin.getConfig().getBoolean("player-kill-only") && e.getEntity().getKiller() == null) return;

        if (r.nextDouble() * 100 < plugin.getConfig().getDouble("drop-chance")) {
            String key = EnchantKeys.ALL_ENCHANTS[r.nextInt(EnchantKeys.ALL_ENCHANTS.length)];
            e.getDrops().add(manager.getEnchantBook(key, 1));
        }
    }
}