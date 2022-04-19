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

import de.themoep.inventorygui.DynamicGuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
            } else if ("gui".equalsIgnoreCase(args[0]) && sender.hasPermission("simplelootcrates.command.gui")) {
                if (sender instanceof Player) {
                    InventoryGui gui = new InventoryGui(this,
                            "Select crate",
                            new String[]{
                                    "ccccccccc",
                                    "ccccccccc",
                                    "p       n"
                            },
                            new GuiPageElement('p', new ItemStack(Material.ARROW), GuiPageElement.PageAction.PREVIOUS, "Previous"),
                            new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Next")
                    );
                    GuiElementGroup group = new GuiElementGroup('c');
                    gui.addElement(group);
                    for (Crate crate : getManager().getCrates()) {
                        group.addElement(new StaticGuiElement('c', crate.getItemStack(), click -> {
                            if (click.getEvent().isLeftClick()) {
                                giveCrate((Player) click.getEvent().getWhoClicked(), crate);
                            } else if (click.getEvent().isRightClick()) {
                                openEditGui((Player) sender, crate);
                            }
                            return true;
                        }, crate.getName(), "Left click to get", "Right click to edit"));
                    }
                    gui.show((HumanEntity) sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "This is a player command!");
                }
                return true;
            }
        } else if (args.length == 2) {
            if ("get".equalsIgnoreCase(args[0]) && sender.hasPermission("simplelootcrates.command.get")) {
                if (sender instanceof Player) {
                    Crate crate = manager.getCrate(args[1]);
                    if (crate != null) {
                        giveCrate((Player) sender, crate);
                    } else {
                        sender.sendMessage(ChatColor.RED + "No crate with the ID " + args[1] + " found!");
                    }
                } else {
                    sender.sendMessage("Can only be run by a player. Use /" + label + " give <player> <crate> to give crates from the console!");
                }
                return true;
            }
        } else if (args.length > 2 && "add".equalsIgnoreCase(args[0]) && sender.hasPermission("simplelootcrates.command.add")) {
            if (getManager().getCrate(args[1]) == null) {
                Crate crate = new Crate(args[1].toLowerCase(), Arrays.stream(args).skip(2).collect(Collectors.joining(" ")));
                getManager().addCrate(crate);
                openEditGui((Player) sender, crate);
            } else {
                sender.sendMessage(ChatColor.RED + "There already is a crate with the ID " + args[2] + "!");
            }
            return true;
        } else if (args.length > 3) {
            if ("give".equalsIgnoreCase(args[0]) && sender.hasPermission("simplelootcrates.command.give")) {
                Player target = getServer().getPlayer(args[1]);
                if (target != null) {
                    Crate crate = manager.getCrate(args[2]);
                    if (crate != null) {
                        giveCrate((Player) sender, crate);
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

    private void openEditGui(Player player, Crate crate) {
        InventoryGui gui = new InventoryGui(this, "Edit crate " + crate.getName(), new String[] {
                "lllllllll",
                "lllllllla",
                "p   i   n"
        }, new StaticGuiElement('i', crate.getItemStack(), click -> {
            if (click.getEvent().getCursor() != null && (click.getType() == ClickType.LEFT || click.getType() == ClickType.RIGHT)) {
                crate.setItemStack(click.getEvent().getCursor());
                crate.saveConfig(cratesFolder);
                return false;
            }
            return true;
        }, "Click to set item"),
                new GuiPageElement('p', new ItemStack(Material.ARROW), GuiPageElement.PageAction.PREVIOUS, "Previous"),
                new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Next"),
                new StaticGuiElement('a', new ItemStack(Material.SUNFLOWER), click -> {
                    Loot loot = new Loot();
                    crate.getLoot().add(loot);
                    openEditGui(player, crate, loot);
                    return true;
                }, "Add new loot")
        );
        gui.setCloseAction(close -> {
            crate.saveConfig(cratesFolder);
            return true;
        });

        gui.addElement(new DynamicGuiElement('l', who -> {
            GuiElementGroup lootGroup = new GuiElementGroup('l');
            for (Loot loot : crate.getLoot()) {
                lootGroup.addElement(new DynamicGuiElement('l', () -> {
                    List<String> text = new ArrayList<>();
                    text.add("Possible items:");
                    Map<String, Integer> items = new HashMap<>();
                    for (ItemStack item : loot.getItems()) {
                        StringBuilder name = new StringBuilder().append(ChatColor.DARK_GRAY).append(item.getAmount()).append("x ");
                        if (item.hasItemMeta()) {
                            ItemMeta meta = item.getItemMeta();
                            if (meta.hasDisplayName()) {
                                name.append(ChatColor.WHITE).append(ChatColor.ITALIC).append(meta.getDisplayName()).append(" ");
                            }
                        }
                        name.append(ChatColor.GRAY).append(item.getType().name().toLowerCase());
                        items.put(name.toString(), items.getOrDefault(name.toString(), 0) + 1);
                    }
                    items.entrySet().stream()
                            .map(e -> new AbstractMap.SimpleEntry<>(getPercent(e.getValue(), items.size()), e.getKey()))
                            .sorted((o1, o2) -> o2.getKey().compareTo(o1.getKey()))
                            .forEachOrdered(e -> text.add(String.format("%,.2f%% %s", e.getKey(), e.getValue())));
                    return new StaticGuiElement('l', loot.getItems().isEmpty() ? new ItemStack(Material.DIRT) : loot.getItems().get(0), loot.getAmount(), click -> {
                        openEditGui(player, crate, loot);
                        return true;
                    }, text.toArray(new String[0]));
                }));
            }
            return lootGroup;
        }));

        gui.show(player);
    }

    private double getPercent(int amount, int total) {
        return amount / (double) total * 100;
    }

    private void openEditGui(Player player, Crate crate, Loot loot) {
        InventoryGui gui = new InventoryGui(this, "Edit loot of " + crate.getName(), new String[] {
                "iiiiiiiii",
                "iiiiiiiii",
                "p   a   n"
        },
                new GuiPageElement('p', new ItemStack(Material.ARROW), GuiPageElement.PageAction.PREVIOUS, "Previous"),
                new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Next"),
                new DynamicGuiElement('a', () -> new StaticGuiElement('a', new ItemStack(Material.LEVER), loot.getAmount(), click -> {
                    if (click.getEvent().isLeftClick() || click.getEvent().isRightClick()) {
                        int amount = loot.getAmount() +
                                ((click.getEvent().isLeftClick() ? 1 : -1)
                                        * (click.getEvent().isShiftClick() ? 10 : 1));
                        if (amount < 1) {
                            amount = 1;
                        } else if (amount > 9 * 6 - crate.getAmount()) {
                            amount = 9 * 6 - crate.getAmount();
                        }
                        if (amount != loot.getAmount()) {
                            loot.setAmount(amount);
                            click.getGui().build();
                        }
                    }
                    return true;
                }, "Amount", "Left click +1", "Right click -1", "+Shift = 10"))
        );

        GuiElementGroup itemsGroup = new GuiElementGroup('i');
        gui.addElement(itemsGroup);
        for (int i = 0; i < 9 * 6 - crate.getAmount(); i++) {
            int finalI = i;
            itemsGroup.addElement(new DynamicGuiElement('i', () -> {
                ItemStack item = null;
                if (finalI < loot.getItems().size()) {
                    item = loot.getItems().get(finalI);
                }
                return new StaticGuiElement('i', item, click -> {
                    if (click.getEvent().getCursor() != null && !click.getEvent().getCursor().getType().isAir()
                            && (click.getType() == ClickType.LEFT || click.getType() == ClickType.RIGHT)) {
                        if (finalI < loot.getItems().size()) {
                            loot.getItems().set(finalI, click.getEvent().getCursor().clone());
                        } else {
                            loot.getItems().add(click.getEvent().getCursor().clone());
                            click.getGui().build();
                        }
                        return false;
                    } else if (loot.getItems().size() > 1 && click.getType() == ClickType.MIDDLE || click.getType() == ClickType.LEFT) {
                        loot.getItems().remove(finalI);
                        click.getGui().build();
                        return click.getType() == ClickType.MIDDLE;
                    }
                    return true;
                });
            }));
        }

        gui.show(player);
    }

    private void giveCrate(Player player, Crate crate) {
        if (player.getInventory().addItem(crate.getItemStack()).isEmpty()) {
            player.sendMessage(ChatColor.AQUA + "Added crate " + ChatColor.RESET + crate.getName() + ChatColor.AQUA + " to your inventory!");
        } else {
            player.sendMessage(ChatColor.RED + "Not enough space in your inventory to add crate!");
        }
    }

    public CrateManager getManager() {
        return manager;
    }

    public Sound getDefaultOpenSound() {
        return defaultOpenSound;
    }

    /**
     * Convert an item to a simple config string if possible
     * @param item The item
     * @return The config string or the item itself to be set in the config directly
     */
    public static Object itemToConfig(ItemStack item) {
        if (item.hasItemMeta()) {
            return item;
        } else if (item.getAmount() > 0) {
            return item.getAmount() + " " + item.getType().name().toLowerCase();
        }
        return item.getType().name().toLowerCase();
    }

    public static ItemStack configToItem(Object item) throws InvalidConfigurationException {
        if (item instanceof ItemStack) {
            return (ItemStack) item;
        } else if (item instanceof Map) {
            return ItemStack.deserialize((Map<String, Object>) item);
        } else if (item instanceof String) {
            String[] parts = ((String) item).split(" ", 2);
            int amount = 1;
            try {
                amount = Integer.parseInt(parts[0]);
                if (parts.length > 1) {
                    parts = parts[1].split(" ", 2);
                } else {
                    throw new InvalidConfigurationException("Invalid item config " + item + "!");
                }
            } catch (NumberFormatException ignored) {}

            String matStr = parts[0];
            Material mat = Material.matchMaterial(matStr);
            if (mat != null) {
                ItemStack itemStack = new ItemStack(mat, amount);
                if (parts.length == 2) {
                    Bukkit.getUnsafe().modifyItemStack(itemStack, parts[1]);
                }
                return itemStack;
            } else {
                throw new InvalidConfigurationException("Invalid material name " + matStr + "!");
            }
        } else {
            throw new InvalidConfigurationException("Not a valid item! " + item);
        }
    }
}
