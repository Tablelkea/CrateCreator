package fr.kilian.plugins.model;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Read-only diagnostics for the plugin's live configuration and crate data. */
public final class RunTestGUI implements InventoryHolder {
    public static final int PAGE_SIZE = 45;
    public static final int PREVIOUS_SLOT = 45;
    public static final int RUN_ALL_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final CrateCreator plugin = CrateCreator.getInstance();
    private final MessageService messages = plugin.messages();
    private final Inventory inventory = plugin.getServer().createInventory(
            this, 54, messages.text("gui.run-test-title"));
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final Map<String, Result> results = new HashMap<>();
    private int page;

    public RunTestGUI() {
        diagnostics.add(new Diagnostic("config.file", "", "debug.config-file.title", "debug.config-file.description",
                this::checkConfigFile));
        diagnostics.add(new Diagnostic("config.messages", "", "debug.messages.title", "debug.messages.description",
                this::checkMessages));
        diagnostics.add(new Diagnostic("crates.registry", "", "debug.registry.title", "debug.registry.description",
                this::checkRegistry));
        diagnostics.add(new Diagnostic("crates.storage", "", "debug.storage.title", "debug.storage.description",
                this::checkStorage));
        diagnostics.add(new Diagnostic("commands.registered", "", "debug.commands.title",
                "debug.commands.description", this::checkCommands));
        for(Crate crate : plugin.manager().getCrates()) {
            String prefix = "crate." + crate.persistenceId() + ".";
            diagnostics.add(new Diagnostic(prefix + "block", crate.id(), "debug.crate-block.title",
                    "debug.crate-block.description", () -> checkBlock(crate)));
            diagnostics.add(new Diagnostic(prefix + "hologram", crate.id(), "debug.crate-hologram.title",
                    "debug.crate-hologram.description", () -> plugin.manager().hasHologram(crate)
                    ? ok("les trois lignes de l'hologramme sont actives")
                    : fail("hologramme absent ou incomplet pour le chunk actuel")));
            diagnostics.add(new Diagnostic(prefix + "key", crate.id(), "debug.crate-key.title",
                    "debug.crate-key.description", () -> checkKey(crate)));
            diagnostics.add(new Diagnostic(prefix + "rewards", crate.id(), "debug.crate-rewards.title",
                    "debug.crate-rewards.description", () -> checkRewards(crate)));
            diagnostics.add(new Diagnostic(prefix + "expiration", crate.id(), "debug.crate-expiration.title",
                    "debug.crate-expiration.description", () -> checkExpiration(crate)));
        }
        refresh();
    }

    public void refresh() {
        inventory.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, diagnostics.size());
        for(int index = start; index < end; index++) {
            Diagnostic diagnostic = diagnostics.get(index);
            inventory.setItem(index - start, diagnosticItem(diagnostic));
        }
        inventory.setItem(PREVIOUS_SLOT, button(Material.ARROW, "gui.run-test-previous"));
        inventory.setItem(RUN_ALL_SLOT, button(Material.REDSTONE_TORCH, "gui.run-test-all"));
        inventory.setItem(NEXT_SLOT, button(Material.ARROW, "gui.run-test-next"));
    }

    public void runAt(int slot, Player player) {
        if(slot < 0 || slot >= PAGE_SIZE) return;
        int index = page * PAGE_SIZE + slot;
        if(index >= diagnostics.size()) return;
        run(diagnostics.get(index), player);
    }

    public void runAll(Player player) {
        for(Diagnostic diagnostic : diagnostics) run(diagnostic, player);
        long failures = results.values().stream().filter(result -> !result.passed()).count();
        player.sendMessage(messages.text("debug.summary", Map.of(
                "passed", results.size() - failures, "failed", failures, "total", results.size())));
        refresh();
    }

    public void previousPage() {
        if(page > 0) page--;
        refresh();
    }

    public void nextPage() {
        if((page + 1) * PAGE_SIZE < diagnostics.size()) page++;
        refresh();
    }

    private void run(Diagnostic diagnostic, Player player) {
        Result result;
        try {
            result = diagnostic.check().run();
        } catch(RuntimeException throwable) {
            String detail = throwable.getClass().getSimpleName() + ": "
                    + (throwable.getMessage() == null ? "erreur sans détail" : throwable.getMessage());
            result = new Result(false, detail);
            plugin.getLogger().severe("Diagnostic " + diagnostic.id() + " failed: " + detail);
        }
        results.put(diagnostic.id(), result);
        player.sendMessage(Component.text(result.passed() ? "[OK] " : "[ERREUR] ",
                        result.passed() ? NamedTextColor.GREEN : NamedTextColor.RED)
                .append(messages.text(diagnostic.titleKey()))
                .append(diagnostic.subject().isBlank() ? Component.empty()
                        : Component.text(" [" + diagnostic.subject() + "]", NamedTextColor.YELLOW))
                .append(Component.text(" — " + result.detail(), NamedTextColor.GRAY)));
        refresh();
    }

    private ItemStack diagnosticItem(Diagnostic diagnostic) {
        Result result = results.get(diagnostic.id());
        ItemStack item = new ItemStack(result == null ? Material.PAPER
                : result.passed() ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta meta = item.getItemMeta();
        Component title = messages.text(diagnostic.titleKey());
        if(!diagnostic.subject().isBlank()) {
            title = title.append(Component.text(" [" + diagnostic.subject() + "]", NamedTextColor.GRAY));
        }
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(messages.text(diagnostic.descriptionKey()).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(messages.text("gui.run-test-action").decoration(TextDecoration.ITALIC, false));
        if(result != null) {
            lore.add(Component.text((result.passed() ? "OK: " : "ERREUR: ") + result.detail(),
                    result.passed() ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack button(Material material, String key) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(messages.text(key).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private Result checkConfigFile() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        return file.isFile() && file.canRead()
                ? ok("messages.yml lisible") : fail("messages.yml absent ou illisible");
    }

    private Result checkMessages() {
        int count = 0;
        for(String key : messages.keys()) {
            String value = messages.value(key);
            try {
                messages.parse(value);
                count++;
            } catch(RuntimeException exception) {
                return fail("MiniMessage invalide pour " + key + ": " + exception.getMessage());
            }
        }
        return ok(count + " messages chargés et analysés");
    }

    private Result checkRegistry() {
        CrateManager manager = plugin.manager();
        for(Crate crate : manager.getCrates()) {
            if(manager.getCrateById(crate.id()) != crate) return fail("index ID invalide: " + crate.id());
            if(manager.getCrate(crate.location()) != crate) return fail("index emplacement invalide: " + crate.id());
        }
        return ok(manager.getCrates().size() + " crate(s), index ID et emplacement cohérents");
    }

    private Result checkStorage() {
        File folder = new File(plugin.getDataFolder(), "crates");
        if(!folder.isDirectory()) return fail("dossier crates/ absent");
        for(Crate crate : plugin.manager().getCrates()) {
            File file = new File(folder, crate.persistenceId() + ".yml");
            if(!file.isFile() || !file.canRead()) return fail("fichier absent/illisible: " + file.getName());
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            if(!crate.id().equalsIgnoreCase(yaml.getString("id", ""))) {
                return fail("ID du fichier incohérent: " + file.getName());
            }
            ItemStack savedKey = yaml.getItemStack("key");
            ItemStack currentKey = crate.key();
            if(currentKey == null ? savedKey != null
                    : savedKey == null || !deepEquals(currentKey.serialize(), savedKey.serialize())) {
                return fail("clé non conforme au fichier: " + file.getName());
            }
            ItemStack[] contents = crate.contents();
            for(int slot = 0; slot < contents.length; slot++) {
                ItemStack saved = yaml.getItemStack("contents." + slot);
                if(contents[slot] == null ? saved != null
                        : saved == null || !deepEquals(contents[slot].serialize(), saved.serialize())) {
                    return fail("récompense différente du YAML au slot " + slot + " ("
                            + itemDifference(contents[slot], saved) + ") : " + file.getName());
                }
            }
        }
        return ok("fichiers YAML présents et IDs concordants");
    }

    private Result checkCommands() {
        var command = plugin.getCommand("crate");
        if(command == null) return fail("commande crate absente de plugin.yml");
        if(command.getExecutor() == null || command.getTabCompleter() == null) {
            return fail("commande sans exécuteur ou autocomplétion");
        }
        return ok("commande principale et autocomplétion enregistrées");
    }

    private Result checkBlock(Crate crate) {
        if(crate.location().getWorld() == null) return fail("monde absent");
        if(!crate.location().getWorld().isChunkLoaded(crate.location().getBlockX() >> 4,
                crate.location().getBlockZ() >> 4)) return fail("chunk de la crate non chargé");
        if(crate.location().getBlock().getType() != Material.CHEST) return fail("bloc de crate non coffre");
        if(!(crate.location().getBlock().getBlockData() instanceof Directional)) {
            return fail("orientation du coffre indisponible");
        }
        return ok("coffre et orientation valides");
    }

    private Result checkKey(Crate crate) {
        ItemStack key = crate.key();
        if(key == null) return fail("aucune clé configurée");
        return crate.hasKey(key) ? ok("clé et identifiant persistant cohérents")
                : fail("identifiant persistant de la clé incohérent");
    }

    private Result checkRewards(Crate crate) {
        ItemStack[] contents = crate.contents();
        int count = 0;
        double total = 0.0;
        for(int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if(item == null || item.getType().isAir()) continue;
            count++;
            double weight = crate.rewardWeight(slot);
            if(!Double.isFinite(weight) || weight <= 0 || weight > 100) {
                return fail("chance invalide au slot " + slot + ": " + weight);
            }
            total += weight;
        }
        if(count == 0) return fail("aucune récompense: ouverture impossible");
        if(total > 100.000001) return fail("total des chances supérieur à 100%: " + total);
        return ok(count + " récompense(s), total " + total + "%; un lot est toujours sélectionné");
    }

    private Result checkExpiration(Crate crate) {
        if(crate.expiresAt() == null) return ok("aucune expiration configurée");
        return crate.isExpired() ? fail("crate expirée, suppression attendue au prochain contrôle")
                : ok("expiration future valide");
    }

    private Result ok(String detail) { return new Result(true, detail); }
    private Result fail(String detail) { return new Result(false, detail); }

    private String itemDifference(ItemStack current, ItemStack saved) {
        if(current == null) return "objet en mémoire absent";
        if(saved == null) return "objet YAML absent";
        Map<String, Object> currentData = current.serialize();
        Map<String, Object> savedData = saved.serialize();
        Set<String> keys = new LinkedHashSet<>(currentData.keySet());
        keys.addAll(savedData.keySet());
        List<String> differences = keys.stream()
                .filter(key -> !deepEquals(currentData.get(key), savedData.get(key)))
                .toList();
        return differences.isEmpty() ? "représentation Bukkit différente" : "champs " + differences;
    }

    private boolean deepEquals(Object first, Object second) {
        if(first == second) return true;
        if(first == null || second == null) return false;
        if(first instanceof Map<?, ?> firstMap && second instanceof Map<?, ?> secondMap) {
            if(!firstMap.keySet().equals(secondMap.keySet())) return false;
            return firstMap.keySet().stream()
                    .allMatch(key -> deepEquals(firstMap.get(key), secondMap.get(key)));
        }
        if(first instanceof List<?> firstList && second instanceof List<?> secondList) {
            if(firstList.size() != secondList.size()) return false;
            for(int index = 0; index < firstList.size(); index++) {
                if(!deepEquals(firstList.get(index), secondList.get(index))) return false;
            }
            return true;
        }
        if(first.getClass().isArray() && second.getClass().isArray()) {
            int length = Array.getLength(first);
            if(length != Array.getLength(second)) return false;
            for(int index = 0; index < length; index++) {
                if(!deepEquals(Array.get(first, index), Array.get(second, index))) return false;
            }
            return true;
        }
        return Objects.equals(first, second);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    private record Diagnostic(String id, String subject, String titleKey, String descriptionKey, Check check) { }
    private record Result(boolean passed, String detail) { }
    @FunctionalInterface private interface Check { Result run(); }
}
