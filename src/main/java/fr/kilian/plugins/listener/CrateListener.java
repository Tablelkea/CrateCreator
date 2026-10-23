package fr.kilian.plugins.listener;

import fr.kilian.plugins.CrateCreator;
import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.model.Crate;
import fr.kilian.plugins.model.CrateContentGUI;
import fr.kilian.plugins.model.CrateDeleteConfirmGUI;
import fr.kilian.plugins.model.CrateExpirationGUI;
import fr.kilian.plugins.model.CrateGUI;
import fr.kilian.plugins.model.CrateHistoryGUI;
import fr.kilian.plugins.model.CrateInterface;
import fr.kilian.plugins.model.CrateItemRewardGUI;
import fr.kilian.plugins.model.CrateRewardsGUI;
import fr.kilian.plugins.model.CrateRewardTypeGUI;
import fr.kilian.plugins.model.KeyEditor;
import fr.kilian.plugins.model.MessageConfigGUI;
import fr.kilian.plugins.model.ParticleGUI;
import fr.kilian.plugins.model.RunTestGUI;
import fr.kilian.plugins.util.CrateMessageFormatter;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Locale;

public final class CrateListener implements Listener {
    private final CrateCreator plugin;
    private final CrateManager crateManager;
    private final Map<UUID, PendingEdit> pendingEdits = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingConfigEdits = new ConcurrentHashMap<>();
    private final Map<UUID, PendingKeyEdit> pendingKeyEdits = new ConcurrentHashMap<>();

    public CrateListener(CrateCreator plugin, CrateManager crateManager) {
        this.plugin = plugin;
        this.crateManager = crateManager;
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent event) {
        if(event.getHand() != EquipmentSlot.HAND) return;
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if(event.getClickedBlock() == null) return;

        Location location = event.getClickedBlock().getLocation();
        if(!crateManager.isCrate(location)) return;

        Crate crate = crateManager.getCrate(location);
        if(crate == null) return;

        event.setCancelled(true);
        handleCrateInteraction(event.getPlayer(), crate, event.getAction());
    }

    private void handleCrateInteraction(Player player, Crate crate, Action action) {
        if(action == Action.LEFT_CLICK_BLOCK) {
            openCrateMenu(player, crate);
            return;
        }
        openCrateWithKey(player, crate);
    }

    private void openCrateMenu(Player player, Crate crate) {
        if(player.isOp() && player.isSneaking()) {
            player.openInventory(crate.gui().getInventory());
        } else {
            player.openInventory(crate.contentGUI().getInventory());
        }
    }

    private void openCrateWithKey(Player player, Crate crate) {
        ItemStack requiredKey = crate.key();
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if(requiredKey == null || !crate.hasKey(heldItem)) {
            String keyName = requiredKey == null
                    ? messages().plain("chat.crate.unconfigured-key")
                    : getItemName(requiredKey);
            messages().send(player, "chat.crate.no-key", Map.of("key", keyName));
            applyKnockback(player);
            return;
        }

        List<RewardChoice> rewards = getAvailableRewards(crate);
        if(rewards.isEmpty()) {
            messages().send(player, "chat.crate.empty-rewards");
            return;
        }

        consumeOneKey(player, heldItem);
        playCrateParticle(crate);
        ItemStack reward = selectReward(rewards);
        giveRandomReward(player, reward);
        plugin.history().recordOpen(player, crate.id(), crate.name(), reward);
        sendCrateOpenMessage(player, crate);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        crateManager.refreshHologramsInChunk(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        crateManager.refreshPlayerKeys(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onKeyMovedFromContainer(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(event.getClickedInventory() == null
                || event.getClickedInventory() == player.getInventory()
                || event.getAction() == InventoryAction.NOTHING) return;

        // A key's PDC can be stale after a key edit, so don't use the pre-click
        // item as a gate. Inspect the player's inventory after Bukkit moves it.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if(player.isOnline()) crateManager.refreshPlayerKeys(player);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onKeyDraggedFromContainer(InventoryDragEvent event) {
        if(!(event.getWhoClicked() instanceof Player player)) return;
        int topSize = event.getView().getTopInventory().getSize();
        if(event.getRawSlots().stream().noneMatch(rawSlot -> rawSlot < topSize)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if(player.isOnline()) crateManager.refreshPlayerKeys(player);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onKeyPickedUp(EntityPickupItemEvent event) {
        if(!(event.getEntity() instanceof Player player)) return;
        ItemStack pickedUp = event.getItem().getItemStack().clone();
        if(!crateManager.isCrateKey(pickedUp)) return;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if(player.isOnline()) crateManager.refreshPlayerKeys(player, pickedUp);
        });
    }

    private void consumeOneKey(Player player, ItemStack heldItem) {
        if(heldItem.getAmount() == 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            heldItem.setAmount(heldItem.getAmount() - 1);
        }
    }

    private ItemStack selectReward(List<RewardChoice> rewards) {
        double draw = ThreadLocalRandom.current().nextDouble(100.0);
        for(RewardChoice reward : rewards) {
            if(draw < reward.weight()) return reward.item();
            draw -= reward.weight();
        }
        // Any probability left after the configured percentages still awards a reward.
        return rewards.get(ThreadLocalRandom.current().nextInt(rewards.size())).item();
    }

    private void playCrateParticle(Crate crate) {
        Particle particle = crate.particle();
        if(particle == null) return;
        Location effectLocation = crate.location().add(0.5, 1.0, 0.5);
        effectLocation.getWorld().spawnParticle(particle, effectLocation, 40, 0.8, 0.7, 0.8, 0.02);
    }

    private void giveRandomReward(Player player, ItemStack selectedReward) {
        ItemStack reward = selectedReward.clone();
        reward = stripRewardMetadata(reward);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        messages().send(player, "chat.crate.reward-won", Map.of("item", getItemName(reward)));
    }

    private ItemStack stripRewardMetadata(ItemStack reward) {
        ItemStack cleanReward = reward.clone();
        ItemMeta meta = cleanReward.getItemMeta();
        meta.getPersistentDataContainer().remove(new org.bukkit.NamespacedKey(plugin, "reward_weight"));
        meta.getPersistentDataContainer().remove(new org.bukkit.NamespacedKey(plugin, "crate_key_id"));
        cleanReward.setItemMeta(meta);
        return cleanReward;
    }

    private void sendCrateOpenMessage(Player player, Crate crate) {
        String message = crate.openMessage();
        if(message != null && !message.isBlank()) {
            var formattedMessage = CrateMessageFormatter.format(message, player, crate);
            if(crate.broadcastOpenMessage()) {
                plugin.getServer().broadcast(formattedMessage);
            } else {
                player.sendMessage(formattedMessage);
            }
        }
    }

    @EventHandler
    public void onCrateGuiClick(InventoryClickEvent event) {
        if(event.getView().getTopInventory().getHolder() instanceof RunTestGUI runTestGUI) {
            event.setCancelled(true);
            if(!(event.getWhoClicked() instanceof Player player)) return;
            if(!player.isOp()) {
                player.closeInventory();
                messages().send(player, "chat.crate.edit-denied");
                return;
            }
            if(event.getClickedInventory() != event.getView().getTopInventory()) return;
            int slot = event.getRawSlot();
            if(slot == RunTestGUI.PREVIOUS_SLOT) runTestGUI.previousPage();
            else if(slot == RunTestGUI.NEXT_SLOT) runTestGUI.nextPage();
            else if(slot == RunTestGUI.RUN_ALL_SLOT) runTestGUI.runAll(player);
            else runTestGUI.runAt(slot, player);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof KeyEditor keyEditor) {
            event.setCancelled(true);
            if(event.getWhoClicked() instanceof Player player) handleKeyEditorClick(event, player, keyEditor);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof MessageConfigGUI configGUI) {
            event.setCancelled(true);
            if(event.getWhoClicked() instanceof Player player) {
                handleConfigGuiClick(event, player, configGUI);
            }
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof ParticleGUI particleGUI) {
            event.setCancelled(true);
            if(event.getWhoClicked() instanceof Player player) handleParticleGuiClick(event, player, particleGUI);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof CrateDeleteConfirmGUI deleteGUI) {
            event.setCancelled(true);
            if(event.getWhoClicked() instanceof Player player) handleDeleteConfirmClick(event, player, deleteGUI);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof CrateHistoryGUI historyGUI) {
            event.setCancelled(true);
            if(event.getClickedInventory() != event.getView().getTopInventory()) return;
            if(event.getRawSlot() == CrateHistoryGUI.PREVIOUS_SLOT) historyGUI.previousPage();
            else if(event.getRawSlot() == CrateHistoryGUI.NEXT_SLOT) historyGUI.nextPage();
            else if(event.getRawSlot() == CrateHistoryGUI.CLOSE_SLOT) event.getWhoClicked().closeInventory();
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof CrateExpirationGUI expirationGUI) {
            event.setCancelled(true);
            if(!(event.getWhoClicked() instanceof Player player)) return;
            if(!player.isOp()) {
                player.closeInventory();
                messages().send(player, "chat.crate.edit-denied");
                return;
            }
            if(event.getClickedInventory() != event.getView().getTopInventory()) return;
            int slot = event.getRawSlot();
            if(slot == CrateExpirationGUI.PREVIOUS_MONTH_SLOT) {
                expirationGUI.previousMonth();
            } else if(slot == CrateExpirationGUI.NEXT_MONTH_SLOT) {
                expirationGUI.nextMonth();
            } else if(slot == CrateExpirationGUI.CLEAR_SLOT) {
                expirationGUI.crate().setExpiresAt(null);
                player.openInventory(expirationGUI.crate().gui().getInventory());
            } else if(slot == CrateExpirationGUI.BACK_SLOT) {
                player.openInventory(expirationGUI.crate().gui().getInventory());
            } else {
                java.time.LocalDate date = expirationGUI.dateAt(slot);
                if(date == null) return;
                java.time.Instant expiresAt = date.atTime(java.time.LocalTime.MAX)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant();
                expirationGUI.crate().setExpiresAt(expiresAt);
                player.openInventory(expirationGUI.crate().gui().getInventory());
            }
            return;
        }
        if(!(event.getView().getTopInventory().getHolder() instanceof CrateInterface crateInterface)) return;

        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)) return;
        if(crateInterface.type() == CrateInterface.Type.CONTENT) {
            handleContentGuiClick(event, player);
            return;
        }
        if(crateInterface.type() == CrateInterface.Type.REWARDS) {
            handleRewardsGuiClick(event, player, crateInterface.crate());
            return;
        }
        if(crateInterface.type() == CrateInterface.Type.REWARD_TYPES) {
            if(event.getClickedInventory() != event.getView().getTopInventory()) return;
            handleRewardTypeClick(player, crateInterface.crate(), event.getRawSlot());
            return;
        }
        if(crateInterface.type() == CrateInterface.Type.ITEM_REWARD) {
            handleItemRewardClick(event, player, (CrateItemRewardGUI) crateInterface);
            return;
        }
        if(event.getClickedInventory() != event.getView().getTopInventory()) return;

        handleEditGuiClick(player, crateInterface.crate(), event.getRawSlot());
    }

    @EventHandler
    public void onCrateGuiDrag(InventoryDragEvent event) {
        if(event.getView().getTopInventory().getHolder() instanceof RunTestGUI) {
            event.setCancelled(true);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof KeyEditor) {
            event.setCancelled(true);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof MessageConfigGUI) {
            event.setCancelled(true);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof ParticleGUI) {
            event.setCancelled(true);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof CrateDeleteConfirmGUI) {
            event.setCancelled(true);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof CrateHistoryGUI) {
            event.setCancelled(true);
            return;
        }
        if(event.getView().getTopInventory().getHolder() instanceof CrateExpirationGUI) {
            event.setCancelled(true);
            return;
        }
        if(!(event.getView().getTopInventory().getHolder() instanceof CrateInterface crateInterface)) return;

        event.setCancelled(true);
    }

    private void handleContentGuiClick(InventoryClickEvent event, Player player) {
        if(event.getClickedInventory() == event.getView().getTopInventory()
                && event.getRawSlot() == CrateContentGUI.QUIT_SLOT) {
            player.closeInventory();
        }
    }

    private void handleRewardsGuiClick(InventoryClickEvent event, Player player, Crate crate) {
        if(event.getClickedInventory() != event.getView().getTopInventory()) {
            if(event.isShiftClick()) event.setCancelled(true);
            return;
        }

        switch(event.getRawSlot()) {
            case CrateRewardsGUI.QUIT_SLOT -> player.closeInventory();
            case CrateRewardsGUI.ADD_REWARD_SLOT ->
                    player.openInventory(crate.rewardTypeGUI().getInventory());
            default -> {
                if(event.getRawSlot() < CrateRewardsGUI.REWARD_SIZE) {
                    ItemStack reward = event.getCurrentItem();
                    if(reward != null && !reward.getType().isAir()) {
                        if(event.isShiftClick()) {
                            crate.setContent(event.getRawSlot(), null);
                            messages().send(player, "chat.crate.reward-removed");
                        } else {
                            requestRewardWeight(player, crate, event.getRawSlot());
                        }
                    }
                }
            }
        }
    }

    private void handleEditGuiClick(Player player, Crate crate, int slot) {
        switch(slot) {
            case CrateGUI.ID_SLOT -> requestEdit(player, crate, EditField.ID);
            case CrateGUI.PARTICLE_SLOT -> player.openInventory(new ParticleGUI(crate).getInventory());
            case CrateGUI.NAME_SLOT -> requestEdit(player, crate, EditField.NAME);
            case CrateGUI.KEY_SLOT -> player.openInventory(new KeyEditor(crate).getInventory());
            case CrateGUI.MESSAGE_SLOT -> requestEdit(player, crate, EditField.MESSAGE);
            case CrateGUI.LOCATION_SLOT -> moveCrateToPlayer(player, crate);
            case CrateGUI.REWARDS_SLOT -> player.openInventory(crate.rewardsGUI().getInventory());
            case CrateGUI.BROADCAST_SLOT -> {
                boolean broadcast = !crate.broadcastOpenMessage();
                crate.setBroadcastOpenMessage(broadcast);
                messages().send(player, broadcast
                        ? "chat.crate.broadcast-enabled"
                        : "chat.crate.broadcast-disabled");
            }
            case CrateGUI.DELETE_SLOT ->
                    player.openInventory(new CrateDeleteConfirmGUI(crate).getInventory());
            case CrateGUI.EXPIRATION_SLOT ->
                    player.openInventory(new CrateExpirationGUI(crate).getInventory());
            default -> {
            }
        }
    }

    @EventHandler
    public void onChatEdit(AsyncPlayerChatEvent event) {
        PendingKeyEdit keyEdit = pendingKeyEdits.remove(event.getPlayer().getUniqueId());
        if(keyEdit != null) {
            event.setCancelled(true);
            String value = event.getMessage();
            Player player = event.getPlayer();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if(!player.isOp()) {
                    messages().send(player, "chat.crate.edit-denied");
                    return;
                }
                if(value.trim().equalsIgnoreCase(messages().plain("chat.crate.cancel-word"))) {
                    messages().send(player, "chat.crate.edit-cancelled");
                } else {
                    try {
                        if(keyEdit.field() == KeyEditField.NAME) keyEdit.editor().setName(value);
                        else keyEdit.editor().setLore(value);
                        messages().send(player, "chat.crate.key-updated");
                    } catch(IllegalArgumentException exception) {
                        messages().send(player, "chat.crate.key-invalid-format");
                    }
                }
                player.openInventory(keyEdit.editor().getInventory());
            });
            return;
        }

        String configKey = pendingConfigEdits.remove(event.getPlayer().getUniqueId());
        if(configKey != null) {
            event.setCancelled(true);
            String value = event.getMessage();
            Player player = event.getPlayer();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if(!player.isOp()) {
                    messages().send(player, "chat.crate.edit-denied");
                    return;
                }
                if(value.trim().equalsIgnoreCase(messages().plain("chat.crate.cancel-word"))) {
                    messages().send(player, "chat.crate.edit-cancelled");
                    player.openInventory(new MessageConfigGUI().getInventory());
                    return;
                }
                try {
                    messages().setValue(configKey, value);
                    messages().send(player, "chat.crate.config-saved");
                } catch(IllegalStateException exception) {
                    messages().send(player, "chat.crate.config-reload-failed");
                }
                player.openInventory(new MessageConfigGUI().getInventory());
            });
            return;
        }

        PendingEdit edit = pendingEdits.remove(event.getPlayer().getUniqueId());
        if(edit == null) return;

        event.setCancelled(true);
        String value = event.getMessage().trim();
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if(value.equalsIgnoreCase(messages().plain("chat.crate.cancel-word"))) {
                messages().send(player, "chat.crate.edit-cancelled");
                return;
            }
            if(value.isBlank()) {
                messages().send(player, "chat.crate.value-empty");
                requestEdit(player, edit.crate(), edit.field());
                return;
            }

            if(edit.field() == EditField.REWARD_WEIGHT) {
                updateRewardWeight(player, edit, value);
                return;
            }
            if(edit.field() == EditField.NAME) {
                edit.crate().setName(value);
            } else if(edit.field() == EditField.ID) {
                if(!crateManager.updateCrateId(edit.crate(), value)) {
                    messages().send(player, "chat.crate.id-in-use");
                    requestEdit(player, edit.crate(), edit.field());
                    return;
                }
            } else {
                edit.crate().setOpenMessage(value);
            }
            edit.crate().gui().refresh();
            messages().send(player, "chat.crate.updated");
        });
    }

    private void handleConfigGuiClick(InventoryClickEvent event, Player player, MessageConfigGUI gui) {
        if(!player.isOp()) {
            player.closeInventory();
            messages().send(player, "chat.crate.edit-denied");
            return;
        }
        if(event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getRawSlot();
        if(slot == MessageConfigGUI.RELOAD_SLOT) {
            try {
                messages().reload();
                gui.refresh();
                messages().send(player, "chat.crate.config-reloaded");
            } catch(IllegalStateException exception) {
                messages().send(player, "chat.crate.config-reload-failed");
            }
            return;
        }
        if(slot == MessageConfigGUI.PREVIOUS_SLOT) {
            gui.previousPage();
            return;
        }
        if(slot == MessageConfigGUI.NEXT_SLOT) {
            gui.nextPage();
            return;
        }
        String category = gui.categoryAt(slot);
        if(category != null) {
            gui.selectCategory(category);
            return;
        }
        String key = gui.keyAt(slot);
        if(key == null) return;
        beginConfigEdit(player, key);
    }

    public void beginConfigEdit(Player player, String key) {
        if(!player.isOp()) {
            messages().send(player, "chat.crate.edit-denied");
            return;
        }
        if(!messages().hasKey(key)) {
            messages().send(player, "chat.crate.config-key-unknown", Map.of("key", key));
            return;
        }
        pendingConfigEdits.put(player.getUniqueId(), key);
        player.closeInventory();
        messages().send(player, "chat.crate.config-edit-prompt", Map.of("key", key));
    }

    private void requestEdit(Player player, Crate crate, EditField field) {
        pendingEdits.put(player.getUniqueId(), new PendingEdit(crate, field, -1));
        String label = switch(field) {
            case NAME -> messages().plain("chat.crate.field-name");
            case ID -> messages().plain("chat.crate.field-id");
            case MESSAGE -> messages().plain("chat.crate.field-message");
            case REWARD_WEIGHT -> messages().plain("chat.crate.field-reward-weight");
        };
        player.closeInventory();
        messages().send(player, "chat.crate.edit-prompt", Map.of("field", label));
        if(field == EditField.NAME || field == EditField.MESSAGE) {
            messages().send(player, "chat.crate.placeholders");
        }
    }

    private void requestRewardWeight(Player player, Crate crate, int slot) {
        double chance = crate.rewardChance(slot);
        pendingEdits.put(player.getUniqueId(), new PendingEdit(crate, EditField.REWARD_WEIGHT, slot));
        player.closeInventory();
        messages().send(player, "chat.crate.reward-weight-prompt", Map.of(
                "chance", formatNumber(chance)
        ));
    }

    private void updateRewardWeight(Player player, PendingEdit edit, String value) {
        double weight;
        try {
            weight = Double.parseDouble(value.replace(',', '.'));
        } catch(NumberFormatException exception) {
            messages().send(player, "chat.crate.reward-weight-invalid");
            requestRewardWeight(player, edit.crate(), edit.slot());
            return;
        }
        if(!Double.isFinite(weight) || weight <= 0.0 || weight > 100.0) {
            messages().send(player, "chat.crate.reward-weight-invalid");
            requestRewardWeight(player, edit.crate(), edit.slot());
            return;
        }
        try {
            edit.crate().setRewardWeight(edit.slot(), weight);
        } catch(IllegalArgumentException exception) {
            messages().send(player, "chat.crate.reward-chance-total-too-high");
            requestRewardWeight(player, edit.crate(), edit.slot());
            return;
        } catch(IllegalStateException exception) {
            messages().send(player, "chat.crate.rewards-full");
            return;
        }
        messages().send(player, "chat.crate.reward-weight-updated", Map.of(
                "chance", formatNumber(edit.crate().rewardChance(edit.slot()))
        ));
        player.openInventory(edit.crate().rewardsGUI().getInventory());
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private void handleParticleGuiClick(InventoryClickEvent event, Player player, ParticleGUI gui) {
        if(!player.isOp()) {
            player.closeInventory();
            messages().send(player, "chat.crate.edit-denied");
            return;
        }
        if(event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if(slot == ParticleGUI.PREVIOUS_SLOT) {
            gui.previousPage();
        } else if(slot == ParticleGUI.NEXT_SLOT) {
            gui.nextPage();
        } else if(slot == ParticleGUI.BACK_SLOT) {
            player.openInventory(gui.crate().gui().getInventory());
        } else {
            Particle particle = gui.particleAt(slot);
            if(particle == null) return;
            gui.crate().setParticle(particle);
            messages().send(player, "chat.crate.particle-selected", Map.of("particle", particle.name()));
            player.closeInventory();
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location effectLocation = gui.crate().location().clone().add(0.5, 1.0, 0.5);
                if(effectLocation.getWorld() != null) {
                    effectLocation.getWorld().spawnParticle(particle, effectLocation,
                            40, 0.8, 0.7, 0.8, 0.02);
                }
                if(player.isOnline()) player.openInventory(gui.getInventory());
            }, 20L);
        }
    }

    private void moveCrateToPlayer(Player player, Crate crate) {
        if(crateManager.updateCrateLocation(crate, player.getLocation(),
                CrateManager.horizontalFacing(player.getLocation().getYaw()))) {
            messages().send(player, "chat.crate.moved");
        } else {
            messages().send(player, "chat.crate.move-blocked");
        }
    }

    private void handleRewardTypeClick(Player player, Crate crate, int slot) {
        switch(slot) {
            case CrateRewardTypeGUI.ITEM_SLOT ->
                    openItemRewardGUI(player, crate);
            case CrateRewardTypeGUI.MONEY_SLOT,
                 CrateRewardTypeGUI.EXPERIENCE_SLOT,
                 CrateRewardTypeGUI.COMMAND_SLOT,
                 CrateRewardTypeGUI.MUSEUM_SLOT ->
                    messages().send(player, "chat.crate.plugin-missing");
            case CrateRewardTypeGUI.BACK_SLOT ->
                    player.openInventory(crate.rewardsGUI().getInventory());
            default -> {
            }
        }
    }

    private void openItemRewardGUI(Player player, Crate crate) {
        crate.itemRewardGUI().clearSelection();
        player.openInventory(crate.itemRewardGUI().getInventory());
    }

    private void handleDeleteConfirmClick(InventoryClickEvent event, Player player,
                                          CrateDeleteConfirmGUI gui) {
        if(!player.isOp()) {
            player.closeInventory();
            messages().send(player, "chat.crate.edit-denied");
            return;
        }
        if(event.getClickedInventory() != event.getView().getTopInventory()) return;
        if(event.getRawSlot() == CrateDeleteConfirmGUI.CANCEL_SLOT) {
            player.openInventory(gui.crate().gui().getInventory());
        } else if(event.getRawSlot() == CrateDeleteConfirmGUI.CONFIRM_SLOT) {
            String crateName = gui.crate().name();
            player.closeInventory();
            if(crateManager.deleteCrate(gui.crate())) {
                messages().send(player, "chat.crate.deleted", Map.of("name", crateName));
            } else {
                messages().send(player, "chat.crate.crate-not-found", Map.of("id", gui.crate().id()));
            }
        }
    }

    private void handleKeyEditorClick(InventoryClickEvent event, Player player, KeyEditor editor) {
        if(!player.isOp()) {
            player.closeInventory();
            messages().send(player, "chat.crate.edit-denied");
            return;
        }
        if(event.getClickedInventory() == event.getView().getBottomInventory()) {
            ItemStack selected = event.getCurrentItem();
            if(selected != null && !selected.getType().isAir()) {
                editor.selectBaseItem(selected);
                messages().send(player, "chat.crate.key-item-selected");
            }
            return;
        }
        if(event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if(slot == KeyEditor.NAME_SLOT || slot == KeyEditor.LORE_SLOT) {
            KeyEditField field = slot == KeyEditor.NAME_SLOT ? KeyEditField.NAME : KeyEditField.LORE;
            pendingKeyEdits.put(player.getUniqueId(), new PendingKeyEdit(editor, field));
            player.closeInventory();
            messages().send(player, field == KeyEditField.NAME
                    ? "chat.crate.key-name-prompt" : "chat.crate.key-lore-prompt");
        } else if(slot == KeyEditor.CLEAR_SLOT) {
            editor.clearKey();
            messages().send(player, "chat.crate.key-removed");
        } else if(slot == KeyEditor.DONE_SLOT) {
            giveConfiguredKey(player, editor.crate());
            player.closeInventory();
            player.openInventory(editor.crate().gui().getInventory());
        }
    }

    private void giveConfiguredKey(Player player, Crate crate) {
        ItemStack configuredKey = crate.key();
        if(configuredKey == null) return;

        ItemStack[] inventory = player.getInventory().getContents();
        for(int slot = 0; slot < inventory.length; slot++) {
            ItemStack current = inventory[slot];
            if(current == null || !crate.hasKey(current)) continue;
            if(current.getAmount() == 1) {
                player.getInventory().setItem(slot, configuredKey);
            } else {
                current.setAmount(current.getAmount() - 1);
                player.getInventory().setItem(slot, current);
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(configuredKey);
                overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            }
            messages().send(player, "chat.crate.key-set");
            return;
        }

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(configuredKey);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        messages().send(player, "chat.crate.key-set");
    }

    private void handleItemRewardClick(InventoryClickEvent event, Player player,
                                      CrateItemRewardGUI itemRewardGUI) {
        Crate crate = itemRewardGUI.crate();
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        // Handle the two controls by their top-inventory slot before inspecting the clicked inventory.
        if(rawSlot == CrateItemRewardGUI.CONFIRM_SLOT) {
            ItemStack selectedItem = itemRewardGUI.selectedItem();
            if(selectedItem == null) {
                messages().send(player, "chat.crate.item-select-first");
                return;
            }
            if(!crate.addReward(selectedItem)) {
                messages().send(player, crate.hasRewardSpace()
                        ? "chat.crate.reward-chance-total-too-high" : "chat.crate.rewards-full");
                return;
            }
            itemRewardGUI.clearSelection();
            messages().send(player, "chat.crate.reward-added");
            player.openInventory(crate.rewardsGUI().getInventory());
            return;
        }
        if(rawSlot == CrateItemRewardGUI.CANCEL_SLOT) {
            itemRewardGUI.clearSelection();
            player.openInventory(crate.rewardTypeGUI().getInventory());
            return;
        }

        if(rawSlot >= topSize && event.getClickedInventory() != null) {
            ItemStack clickedItem = event.getCurrentItem();
            if(clickedItem != null && !clickedItem.getType().isAir()) {
                itemRewardGUI.select(clickedItem);
                messages().send(player, "chat.crate.item-selected",
                        Map.of("item", getItemName(clickedItem)));
            }
            return;
        }
    }

    private List<RewardChoice> getAvailableRewards(Crate crate) {
        List<RewardChoice> rewards = new ArrayList<>();
        ItemStack[] contents = crate.contents();
        for(int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if(item != null && !item.getType().isAir()) {
                rewards.add(new RewardChoice(item, crate.rewardWeight(item)));
            }
        }
        return rewards;
    }

    private String getItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if(meta != null && meta.hasDisplayName()) {
            return MiniMessage.miniMessage().serialize(meta.displayName());
        }
        return item.getType().name();
    }

    private void applyKnockback(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        if(direction.lengthSquared() == 0) {
            direction = new Vector(0, 0, 1);
        }
        player.setVelocity(direction.multiply(-0.8).setY(0.4));
    }

    private MessageService messages() {
        return plugin.messages();
    }

    private enum EditField {
        NAME,
        ID,
        MESSAGE,
        REWARD_WEIGHT
    }

    private record RewardChoice(ItemStack item, double weight) {
    }

    private record PendingEdit(Crate crate, EditField field, int slot) {
    }

    private enum KeyEditField {
        NAME,
        LORE
    }

    private record PendingKeyEdit(KeyEditor editor, KeyEditField field) {
    }
}
