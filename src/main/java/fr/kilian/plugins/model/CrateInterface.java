package fr.kilian.plugins.model;

import org.bukkit.inventory.InventoryHolder;

public interface CrateInterface extends InventoryHolder {
    String crateId();

    Crate crate();

    Type type();

    enum Type {
        EDIT,
        CONTENT,
        REWARDS,
        REWARD_TYPES,
        ITEM_REWARD
    }
}
