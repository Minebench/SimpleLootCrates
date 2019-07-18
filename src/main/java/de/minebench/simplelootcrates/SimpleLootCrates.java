package de.minebench.simplelootcrates;

/*
 * SimpleLootCrates
 * Copyright (c) 2019 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.logging.Level;

public final class SimpleLootCrates extends JavaPlugin {

    public static NamespacedKey ID_KEY;
    public static final Random RANDOM = new Random();
    private CrateManager manager;

    private File cratesFolder;

    private Sound defaultOpenSound = Sound.ENTITY_PLAYER_LEVELUP;

    @Override
    public void onEnable() {
        ID_KEY = new NamespacedKey(SimpleLootCrates.this, "id");
        cratesFolder = new File(getDataFolder(), "crates");
        loadConfig();
        getCommand("simplelootcrates").setExecutor(this);
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        manager = new CrateManager(this);

        try {
            defaultOpenSound = Sound.valueOf(getConfig().getString("open-sound").toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().log(Level.WARNING, getConfig().getString("open-sound") + " is not a valid Sound setting for open-sound!");
        }

        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
            try {
                URL url = getClass().getResource("/crates/");
                if (url != null) {
                    URI uri = url.toURI();
                    try (FileSystem fileSystem = (uri.getScheme().equals("jar") ? FileSystems.newFileSystem(uri, Collections.emptyMap()) : null)) {
                        Files.walkFileTree(Paths.get(uri), EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                if (file.toString().endsWith(".yml")) {
                                    saveResource(file.toString().substring(1), false);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }
                } else {
                    getLogger().log(Level.WARNING, "Could not find folder '/crates/' in jar!");
                }
            } catch (URISyntaxException | IOException e) {
                getLogger().log(Level.WARNING, "Failed to automatically load languages from the jar!", e);
            }
        }

        try {
            Files.walkFileTree(cratesFolder.toPath(), EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    File f = file.toFile();
                    if (f.getName().endsWith(".yml")) {
                        getLogger().log(Level.INFO, "Loading " + f.getName());
                        YamlConfiguration config = new YamlConfiguration();
                        try {
                            config.load(f);
                            manager.addCrate(new Crate(f.getName().substring(0, f.getName().length() - ".yml".length()), config));
                        } catch (FileNotFoundException ignored) {
                        } catch (IOException | InvalidConfigurationException ex) {
                            getLogger().log(Level.SEVERE, "Cannot load crate from " + file, ex);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Failed to automatically load crates from " + cratesFolder + "!", e);
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("simplelootcrates.command.reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;
            } else if ("list".equalsIgnoreCase(args[0]) && sender.hasPermission("simplelootcrates.command.list")) {
                sender.sendMessage(ChatColor.YELLOW + "Crates:");
                for (Crate crate : manager.getCrates()) {
                    sender.sendMessage(ChatColor.RESET + crate.getName() + ChatColor.AQUA + " (" + crate.getId() + ")");
                }
                return true;
            }
        } else if (args.length == 2) {
            if ("get".equalsIgnoreCase(args[0]) && sender.hasPermission("simplelootcrates.command.get")) {
                if (sender instanceof Player) {
                    Crate crate = manager.getCrate(args[1]);
                    if (crate != null) {
                        if (((Player) sender).getInventory().addItem(crate.getItemStack()).isEmpty()) {
                            sender.sendMessage(ChatColor.AQUA + "Added crate " + ChatColor.RESET + crate.getName() + ChatColor.AQUA + " to your inventory!");
                        } else {
                            sender.sendMessage(ChatColor.RED + "Not enough space in your inventory to add crate!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "No crate with the ID " + args[1] + " found!");
                    }
                } else {
                    sender.sendMessage("Can only be run by a player. Use /" + label + " give <player> <crate> to give crates from the console!");
                }
                return true;
            }
        } else if (args.length > 3) {
            if ("give".equalsIgnoreCase(args[0]) && sender.hasPermission("simplelootcrates.command.give")) {
                Player target = getServer().getPlayer(args[1]);
                if (target != null) {
                    Crate crate = manager.getCrate(args[2]);
                    if (crate != null) {
                        if (((Player) sender).getInventory().addItem(crate.getItemStack()).isEmpty()) {
                            sender.sendMessage(ChatColor.AQUA + "Added crate " + ChatColor.RESET + crate.getName() + ChatColor.AQUA + " to your inventory!");
                        } else {
                            sender.sendMessage(ChatColor.RED + "Not enough space in your inventory to add crate!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "No crate with the ID " + args[2] + " found!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "No player with the name " + args[1] + " found!");
                }
                return true;
            }
        }
        return false;
    }

    public CrateManager getManager() {
        return manager;
    }

    public Sound getDefaultOpenSound() {
        return defaultOpenSound;
    }
}
