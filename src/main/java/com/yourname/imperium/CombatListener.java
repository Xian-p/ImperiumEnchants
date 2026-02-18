package com.yourname.imperium;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class CombatListener implements Listener {

    private final EnchantManager manager;
    private final Random random = new Random();

    public CombatListener(EnchantManager manager) {
        this.manager = manager;
    }

    // --- MELEE LOGIC ---
    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon.getType() == Material.AIR) return;

        // 1. Venom (Poison)
        int venom = manager.getLevel(weapon, EnchantKeys.VENOM);
        if (venom > 0 && chance(20)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60 * venom, 0));
        }

        // 2. Frozen (Slowness)
        int frozen = manager.getLevel(weapon, EnchantKeys.FROZEN);
        if (frozen > 0 && chance(25)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40 * frozen, 1));
            victim.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 5);
        }

        // 3. Blind (Blindness)
        int blind = manager.getLevel(weapon, EnchantKeys.BLIND);
        if (blind > 0 && chance(15)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40 * blind, 0));
        }

        // 4. Confusion (Nausea)
        int confusion = manager.getLevel(weapon, EnchantKeys.CONFUSION);
        if (confusion > 0 && chance(15)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0));
        }

        // 5. Wither
        int wither = manager.getLevel(weapon, EnchantKeys.WITHER);
        if (wither > 0 && chance(20)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60 * wither, 1));
        }

        // 6. Vampirism (Heal Self)
        int vamp = manager.getLevel(weapon, EnchantKeys.VAMPIRISM);
        if (vamp > 0 && chance(30)) {
            double maxHP = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
            attacker.setHealth(Math.min(maxHP, attacker.getHealth() + (vamp * 1.0)));
        }

        // 7. Execute (More damage if victim is under 20% HP)
        int execute = manager.getLevel(weapon, EnchantKeys.EXECUTE);
        if (execute > 0) {
            double maxHP = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
            if (victim.getHealth() < (maxHP * 0.2)) {
                event.setDamage(event.getDamage() * (1 + (execute * 0.5))); // +50% dmg per level
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1f, 2f);
            }
        }

        // 8. Backstab (More damage from behind)
        int backstab = manager.getLevel(weapon, EnchantKeys.BACKSTAB);
        if (backstab > 0) {
            if (attacker.getLocation().getDirection().dot(victim.getLocation().getDirection()) > 0) {
                event.setDamage(event.getDamage() * (1 + (backstab * 0.2))); // +20% dmg per level
                attacker.getWorld().spawnParticle(org.bukkit.Particle.CRIT, victim.getLocation().add(0,1,0), 10);
            }
        }

        // 9. Giant Slayer (More damage to bosses)
        int giant = manager.getLevel(weapon, EnchantKeys.GIANT_SLAYER);
        if (giant > 0) {
            if (victim instanceof IronGolem || victim instanceof Wither || victim instanceof EnderDragon) {
                event.setDamage(event.getDamage() + (giant * 2.5));
            }
        }

        // 10. Inquisitor (More damage to Illagers)
        int inq = manager.getLevel(weapon, EnchantKeys.INQUISITOR);
        if (inq > 0) {
            if (victim instanceof Illager || victim instanceof Witch) {
                event.setDamage(event.getDamage() + (inq * 2.0));
            }
        }

        // 11. Thunderlord
        int thunder = manager.getLevel(weapon, EnchantKeys.THUNDERLORD);
        if (thunder > 0 && chance(10 * thunder)) {
            victim.getWorld().strikeLightning(victim.getLocation());
        }

        // 12. Double Strike
        int doubleStrike = manager.getLevel(weapon, EnchantKeys.DOUBLE_STRIKE);
        if (doubleStrike > 0 && chance(5 * doubleStrike)) {
            victim.damage(event.getDamage()); // Deal same damage again
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        }
    }

    // --- BOW LOGIC ---
    @EventHandler
    public void onBowShoot(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player shooter)) return;

        // Try to get the bow used (Paper API)
        ItemStack bow = arrow.getWeapon(); 
        if (bow == null || bow.getType() == Material.AIR) return;

        // 13. Explosive
        int explosive = manager.getLevel(bow, EnchantKeys.EXPLOSIVE);
        if (explosive > 0) {
            arrow.getWorld().createExplosion(arrow.getLocation(), 1.0f + explosive, false, false);
            arrow.remove();
        }

        // 14. Blaze (Fire)
        int blaze = manager.getLevel(bow, EnchantKeys.BLAZE);
        if (blaze > 0) {
            if (event.getHitEntity() != null) {
                event.getHitEntity().setFireTicks(60 * blaze);
            }
            arrow.getWorld().spawnParticle(org.bukkit.Particle.FLAME, arrow.getLocation(), 10);
        }

        // 15. Sniper (Distance Damage)
        int sniper = manager.getLevel(bow, EnchantKeys.SNIPER);
        if (sniper > 0 && event.getHitEntity() instanceof LivingEntity victim) {
            double distance = shooter.getLocation().distance(victim.getLocation());
            if (distance > 20) {
                double bonus = (distance / 10.0) * sniper;
                victim.damage(bonus, shooter);
                shooter.sendMessage(net.kyori.adventure.text.Component.text("Sniper Bonus! +" + String.format("%.1f", bonus) + " dmg"));
            }
        }
    }

    private boolean chance(double percent) {
        return random.nextDouble() * 100 < percent;
    }
}
