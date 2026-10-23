package fr.kilian.plugins.manager;

import fr.kilian.plugins.commands.subcommands.SubCommand;
import fr.kilian.plugins.commands.subcommands.SubCommandRegister;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class SubCommandManager {
    private static final String COMMAND_PACKAGE = "fr.kilian.plugins.commands";
    private static final String COMMAND_PACKAGE_PATH = COMMAND_PACKAGE.replace('.', '/');

    private SubCommandManager() {
    }

    public static void register(JavaPlugin plugin) {
        Map<String, Map<String, SubCommand>> commands = discoverSubCommands(plugin.getClass().getClassLoader());
        for(Map.Entry<String, Map<String, SubCommand>> entry : commands.entrySet()) {
            PluginCommand command = plugin.getCommand(entry.getKey());
            if(command == null) {
                throw new IllegalStateException("Command '" + entry.getKey()
                        + "' is missing from plugin.yml");
            }

            CommandDispatcher dispatcher = new CommandDispatcher(entry.getValue());
            command.setExecutor(dispatcher);
            command.setTabCompleter(dispatcher);
        }
    }

    private static Map<String, Map<String, SubCommand>> discoverSubCommands(ClassLoader classLoader) {
        Map<String, Map<String, SubCommand>> commands = new HashMap<>();

        try {
            Enumeration<URL> resources = classLoader.getResources(COMMAND_PACKAGE_PATH);
            while(resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if("file".equals(resource.getProtocol())) {
                    discoverFromDirectory(Path.of(resource.toURI()), classLoader, commands);
                } else if("jar".equals(resource.getProtocol())) {
                    discoverFromJar(resource, classLoader, commands);
                }
            }
        } catch(IOException | URISyntaxException | ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to discover subcommands", exception);
        }

        Map<String, Map<String, SubCommand>> immutableCommands = new HashMap<>();
        commands.forEach((name, subCommands) -> immutableCommands.put(name, Map.copyOf(subCommands)));
        return Map.copyOf(immutableCommands);
    }

    private static void discoverFromDirectory(Path directory, ClassLoader classLoader,
                                              Map<String, Map<String, SubCommand>> commands)
            throws IOException, ReflectiveOperationException {
        try(var files = Files.walk(directory)) {
            for(Path file : files.filter(path -> path.toString().endsWith(".class")
                    && !path.getFileName().toString().contains("$")).toList()) {
                String className = COMMAND_PACKAGE_PATH + "/"
                        + directory.relativize(file).toString().replace('\\', '/');
                loadSubCommand(className.substring(0, className.length() - 6).replace('/', '.'),
                        classLoader, commands);
            }
        }
    }

    private static void discoverFromJar(URL resource, ClassLoader classLoader,
                                        Map<String, Map<String, SubCommand>> commands)
            throws IOException, ReflectiveOperationException {
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        JarFile jar = connection.getJarFile();
        Enumeration<JarEntry> entries = jar.entries();
        String prefix = COMMAND_PACKAGE_PATH + "/";
        while(entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if(name.startsWith(prefix) && name.endsWith(".class") && !name.contains("$")) {
                loadSubCommand(name.substring(0, name.length() - 6).replace('/', '.'),
                        classLoader, commands);
            }
        }
    }

    private static void loadSubCommand(String className, ClassLoader classLoader,
                                       Map<String, Map<String, SubCommand>> commands)
            throws ReflectiveOperationException {
        Class<?> type = classLoader.loadClass(className);
        SubCommandRegister annotation = type.getAnnotation(SubCommandRegister.class);
        if(annotation == null || !SubCommand.class.isAssignableFrom(type)
                || Modifier.isAbstract(type.getModifiers())) {
            return;
        }

        String commandName = annotation.command().toLowerCase(Locale.ROOT);
        String subCommandName = annotation.value().toLowerCase(Locale.ROOT);
        if(commandName.isBlank() || subCommandName.isBlank()) {
            throw new IllegalStateException("Command and subcommand names cannot be blank: " + className);
        }

        SubCommand subCommand = (SubCommand) type.getDeclaredConstructor().newInstance();
        Map<String, SubCommand> subCommands = commands.computeIfAbsent(commandName, ignored -> new HashMap<>());
        if(subCommands.putIfAbsent(subCommandName, subCommand) != null) {
            throw new IllegalStateException("Duplicate subcommand '/" + commandName + " "
                    + subCommandName + "'");
        }
    }

    private static final class CommandDispatcher implements CommandExecutor, TabCompleter {
        private final Map<String, SubCommand> subCommands;

        private CommandDispatcher(Map<String, SubCommand> subCommands) {
            this.subCommands = subCommands;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                 @NotNull String label, @NotNull String @NotNull [] args) {
            if(!(sender instanceof Player player)) return false;

            if(args.length == 0) {
                SubCommand root = subCommands.get("root");
                if(root == null) root = subCommands.get("help");
                if(root == null) return false;
                root.execute(player, args);
                return true;
            }

            SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
            if(subCommand == null) return false;
            subCommand.execute(player, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                    @NotNull String alias, @NotNull String @NotNull [] args) {
            if(args.length > 1) {
                if(!(sender instanceof Player player)) return List.of();
                SubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
                if(subCommand == null) return List.of();
                return subCommand.tabComplete(player, Arrays.copyOfRange(args, 1, args.length));
            }

            if(args.length == 0) return List.of();
            String input = args[0].toLowerCase(Locale.ROOT);
            return subCommands.keySet().stream()
                    .filter(name -> !name.equals("root"))
                    .filter(name -> name.startsWith(input))
                    .sorted()
                    .toList();
        }
    }
}
