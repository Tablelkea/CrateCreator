package fr.kilian.plugins.model;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class ParticleGUI implements InventoryHolder {
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int BACK_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final Crate crate;
    private final MessageService messages;
    private final Particle[] particles;
    private final Inventory inventory;
    private int page;

    public ParticleGUI(Crate crate) {
        this.crate = Objects.requireNonNull(crate, "crate");
        messages = CrateCreator.getInstance().messages();
        particles = Arrays.stream(Particle.values())
                .sorted((first, second) -> first.name().compareToIgnoreCase(second.name()))
                .toArray(Particle[]::new);
        inventory = CrateCreator.getInstance().getServer().createInventory(
                this, 54, messages.text("gui.particle-picker-title"));
        refresh();
    }

    public void refresh() {
        inventory.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, particles.length);
        for(int index = start; index < end; index++) {
            Particle particle = particles[index];
            inventory.setItem(index - start, particleItem(particle));
        }
        inventory.setItem(PREVIOUS_SLOT, button(Material.ARROW, "gui.particle-previous"));
        inventory.setItem(BACK_SLOT, button(Material.BARRIER, "gui.particle-back"));
        inventory.setItem(NEXT_SLOT, button(Material.ARROW, "gui.particle-next"));
    }

    public Particle particleAt(int slot) {
        if(slot < 0 || slot >= PAGE_SIZE) return null;
        int index = page * PAGE_SIZE + slot;
        return index >= particles.length ? null : particles[index];
    }

    public void previousPage() {
        if(page > 0) page--;
        refresh();
    }

    public void nextPage() {
        if((page + 1) * PAGE_SIZE < particles.length) page++;
        refresh();
    }

    public Crate crate() {
        return crate;
    }

    private ItemStack particleItem(Particle particle) {
        ItemStack item = new ItemStack(representativeMaterial(particle));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.text("gui.particle-choice", java.util.Map.of(
                "particle", particle.name()
        )).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(messages.text("gui.particle-select-help")
                .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private Material representativeMaterial(Particle particle) {
        String name = particle.name();
        if(name.contains("EXPLOSION")) return Material.TNT;
        if(name.contains("BUBBLE")) return Material.WATER_BUCKET;
        if(name.contains("SMOKE")) return Material.CAMPFIRE;
        if(name.contains("FLAME") || name.contains("FIRE")) return Material.BLAZE_POWDER;
        if(name.contains("HEART")) return Material.RED_DYE;
        if(name.contains("WITCH")) return Material.POTION;
        if(name.contains("CLOUD")) return Material.WHITE_WOOL;
        if(name.contains("CRIT") || name.contains("SWEEP")) return Material.IRON_SWORD;
        if(name.contains("FIREWORK")) return Material.FIREWORK_ROCKET;
        if(name.contains("VILLAGER")) return Material.EMERALD;
        if(name.contains("NOTE")) return Material.NOTE_BLOCK;
        if(name.contains("PORTAL")) return Material.ENDER_PEARL;
        if(name.contains("ENCHANT")) return Material.ENCHANTING_TABLE;
        if(name.contains("SOUL")) return Material.SOUL_LANTERN;
        if(name.contains("LAVA")) return Material.LAVA_BUCKET;
        if(name.contains("WATER") || name.contains("RAIN") || name.contains("DRIP")) return Material.WATER_BUCKET;
        if(name.contains("HONEY") || name.contains("NECTAR")) return Material.HONEY_BOTTLE;
        if(name.contains("DUST") || name.contains("REDSTONE")) return Material.REDSTONE;
        if(name.contains("ITEM")) return Material.ITEM_FRAME;
        if(name.contains("BLOCK")) return Material.BRICKS;
        if(name.contains("SNEEZE") || name.contains("EGG")) return Material.SNIFFER_EGG;
        if(name.contains("SONIC") || name.contains("SCULK")) return Material.SCULK_SHRIEKER;
        if(name.contains("CHERRY")) return Material.CHERRY_LEAVES;
        if(name.contains("GLOW")) return Material.GLOWSTONE;
        if(name.contains("ELECTRIC") || name.contains("LIGHTNING")) return Material.LIGHTNING_ROD;
        if(name.contains("TOTEM")) return Material.TOTEM_OF_UNDYING;
        if(name.contains("DRAGON")) return Material.DRAGON_EGG;
        if(name.contains("SLIME")) return Material.SLIME_BALL;
        if(name.contains("SHRIEK")) return Material.SCULK_SHRIEKER;
        return Material.AMETHYST_SHARD;
    }

    private ItemStack button(Material material, String messageKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.text(messageKey).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
