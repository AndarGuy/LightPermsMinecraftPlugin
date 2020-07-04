package me.andarguy.lightperm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.*;

public final class LightPerm extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static LightPerm instance;
    private static PermissionDatabase permissionDatabase;
    private HashMap<String, PermissionAttachment> permissionAttachments;
    private HashMap<String, PermissionGroup> permissionGroups;
    private PermissionGroup defaultPermissionGroup;
    private FileConfiguration configuration;

    @Override
    public void onEnable() {
        instance = this;
        permissionDatabase = new PermissionDatabase();
        Bukkit.getPluginManager().registerEvents(this, this);
        loadConfig();
        loadGroups();
        updateAllAttachments();
        registerCommands();
    }

    @Override
    public void onDisable() {
        for (PermissionAttachment attachment : permissionAttachments.values()) attachment.remove();
        permissionAttachments.clear();

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLoginEvent(PlayerLoginEvent e) {
        Player p = e.getPlayer();
        updateAttachment(p);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerLoginDeny(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            this.removeAttachment(event.getPlayer());
            Player player = this.getServer().getPlayer(event.getPlayer().getUniqueId());
            if (player != null && player.isOnline()) {
                this.updateAttachment(player);
            }
        }
    }

    @EventHandler
    public void handleLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        this.removeAttachment(player);
        this.updateAttachment(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.removeAttachment(event.getPlayer());

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.updateAttachment(event.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 3) {
            sender.sendMessage(ChatColor.RED + "Слишком много параметров.");
            return true;
        }

        if (args.length > 0) {
            String action = args[0];
            if (!action.equalsIgnoreCase("изменить") && !action.equalsIgnoreCase("статус")) {
                sender.sendMessage(ChatColor.RED + "Некорректный первый параметр! Варианты ввода: изменить, статус");
                return true;
            }
            else if (action.equalsIgnoreCase("статус")) {
                if (args.length == 1) {
                    if (sender instanceof Player) {
                        sender.sendMessage(ChatColor.GREEN + String.format("Ваша группа — %s.", permissionDatabase.getPlayerGroup(sender.getName())));
                    } else sender.sendMessage(ChatColor.RED + "Консоль не может посмотреть статус своей группы!");
                    return true;
                }
            }
            if (args.length > 1) {
                String playerName = args[1];
                if (args.length > 2) {
                    if (action.equalsIgnoreCase("изменить")) {
                        String group = args[2];
                        if (permissionGroups.containsKey(group)) {
                            permissionDatabase.setPlayerGroup(playerName, group);
                            Player player = Bukkit.getPlayer(playerName);
                            if (player != null) reloadAttachment(player);
                            sender.sendMessage(ChatColor.GREEN + String.format("Группа игрока %s изменена на %s.", playerName, group));
                        } else sender.sendMessage(ChatColor.RED + "Такой группы не существует.");
                    } else sender.sendMessage(ChatColor.RED + "Слишком много параметров.");
                } else {
                    if (action.equalsIgnoreCase("статус")) {
                        if (permissionDatabase.isPlayerExist(playerName)) {
                            sender.sendMessage(ChatColor.GREEN + String.format("Группа игрока %s — %s.", playerName, permissionDatabase.getPlayerGroup(sender.getName())));
                        } else sender.sendMessage(ChatColor.RED + "Игрок не зарегистрирован.");
                    }
                }
            } else sender.sendMessage(ChatColor.RED + "Второй параметр должен быть именем игрока.");
        } else sender.sendMessage(ChatColor.YELLOW + "Самописный плагин на привилегированные группы.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        switch (args.length) {
            case 1:
                return Arrays.asList("изменить", "статус");
            case 2:
                return super.onTabComplete(sender, command, alias, args);
            case 3:
                return new ArrayList<>(permissionGroups.keySet());
        }
        return new ArrayList<>();
    }

    private void registerCommands() {
        PluginCommand groupCommand = Bukkit.getPluginCommand("группа");
        if (groupCommand != null) {
            groupCommand.setExecutor(this);
            groupCommand.setTabCompleter(this);
        }
    }

    private void updateAllAttachments() {
        for (Player p: Bukkit.getOnlinePlayers()) {
            updateAttachment(p);
        }
    }

    private void updateAttachment(Player p) {
        String playerName = p.getName();
        if (!permissionDatabase.isPlayerExist(playerName))
            permissionDatabase.setPlayerGroup(playerName, defaultPermissionGroup.getName());
        String playerPermissionGroupName = permissionDatabase.getPlayerGroup(playerName);
        PermissionGroup playerPermissionGroup = permissionGroups.get(playerPermissionGroupName);

        PermissionAttachment permissionAttachment = this.permissionAttachments.get(playerName);
        if (permissionAttachment == null) {
            permissionAttachment = p.addAttachment(this);
            this.permissionAttachments.put(playerName, permissionAttachment);
            for (String permission : getPermissions(playerPermissionGroup)) {
                permissionAttachment.setPermission(permission, true);
            }
        }

    }

    private void reloadAttachment(Player p) {
        removeAttachment(p);
        updateAttachment(p);
    }

    private void removeAttachment(Player p) {
        PermissionAttachment attach = this.permissionAttachments.remove(p.getName());
        if (attach != null) {
            attach.remove();
        }
    }

    private void loadConfig() {
        if (!Path.of(this.getDataFolder().getPath(), "config.yml").toFile().exists())
            this.saveResource("config.yml", false);
        this.configuration = this.getConfig();
        this.configuration.options().copyDefaults(true);
    }

    private void loadGroups() {
        permissionGroups = new HashMap<>();
        permissionAttachments = new HashMap<>();
        Set<String> groupNames = Objects.requireNonNull(this.configuration.getConfigurationSection("groups")).getKeys(false);
        for (String groupName : groupNames) {
            List<String> permissions = this.configuration.getStringList("groups." + groupName + ".permissions");
            PermissionGroup permissionGroup = null;
            if (this.configuration.contains("groups." + groupName + ".inherit")) {
                permissionGroup = new PermissionGroup(groupName, permissions, this.configuration.getString("groups." + groupName + ".inherit"));
                permissionGroups.put(groupName, permissionGroup);
            } else {
                permissionGroup = new PermissionGroup(groupName, permissions);
                permissionGroups.put(groupName, permissionGroup);
            }

            if (this.configuration.getBoolean("groups." + groupName + ".default", false))
                this.defaultPermissionGroup = permissionGroup;
        }
    }

    private List<String> getPermissions(PermissionGroup permissionGroup) {
        List<String> permissions = new ArrayList<>();
        while (permissionGroup != null) {
            permissions.addAll(permissionGroup.getPermissions());
            permissionGroup = permissionGroups.get(permissionGroup.getInherit());
        }
        return permissions;
    }


    public static LightPerm getInstance() {
        return instance;
    }
}
