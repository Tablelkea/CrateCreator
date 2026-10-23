package fr.kilian.plugins;

import fr.kilian.plugins.manager.CrateManager;
import fr.kilian.plugins.manager.CrateHistoryManager;
import fr.kilian.plugins.manager.SubCommandManager;
import fr.kilian.plugins.listener.CrateListener;
import fr.kilian.plugins.util.MessageService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class CrateCreator extends JavaPlugin {

    private static CrateCreator instance;
    private CrateManager manager;
    private MessageService messages;
    private CrateHistoryManager history;
    private CrateListener listener;
    private BukkitTask expirationTask;

    @Override
    public void onEnable() {

        this.instance = this;
        this.manager = new CrateManager();
        this.messages = new MessageService(this);
        this.history = new CrateHistoryManager(getDataFolder(), getLogger());
        manager.loadCrates();

        SubCommandManager.register(this);
        this.listener = new CrateListener(this, manager);
        getServer().getPluginManager().registerEvents(listener, this);
        getServer().getScheduler().runTask(this, manager::refreshLoadedHolograms);
        expirationTask = getServer().getScheduler().runTaskTimer(this, () ->
                manager.getCrates().stream().filter(crate -> crate.isExpired()).toList()
                        .forEach(manager::deleteCrate), 20L, 20L);
    }

    @Override
    public void onDisable() {
        if(expirationTask != null) expirationTask.cancel();
        if(manager != null) manager.clearHolograms();
    }

    public static CrateCreator getInstance() {
        return instance;
    }

    public CrateManager manager() {
        return manager;
    }

    public MessageService messages() {
        return messages;
    }

    public CrateHistoryManager history() {
        return history;
    }

    public CrateListener listener() {
        return listener;
    }
}
