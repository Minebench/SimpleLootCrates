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
import org.bukkit.Sound;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Crate {

    private final String id;
    private final FileConfiguration config;

    private String name;
    private ItemStack item = null;
    private Sound openSound = null;

    private List<Loot> loot = new ArrayList<>();

    public Crate(String id, FileConfiguration config) throws InvalidConfigurationException {
        this.id = id;
        this.config = config;
        loadConfig();
    }

    private void loadConfig() throws InvalidConfigurationException {
        String nameStr = getConfig().getString("name");
        if (nameStr == null) {
            throw new InvalidConfigurationException("No name defined for crate " + getId() + "!");
        }
        name = ChatColor.translateAlternateColorCodes('&', nameStr);

        item = SimpleLootCrates.configToItem(getConfig().get("item"));

        String soundStr = getConfig().getString("open-sound");
        if (soundStr != null) {
            try {
                openSound = Sound.valueOf(soundStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidConfigurationException("Invalid open-sound " + soundStr + " in config of crate " + getId() + "!");
            }
        }

        List<Map<?, ?>> loot = getConfig().getMapList("loot");
        for (Map<?, ?> map : loot) {
            try {
                this.loot.add(new Loot((Map<String, Object>) map));
            } catch (ClassCastException e) {
                throw new InvalidConfigurationException("Loot map is not valid for crate " + getId() + "!" + map);
            }
        }
    }

    public void saveConfig() {
        getConfig().set("name", name.replace(ChatColor.COLOR_CHAR, '&'));
        getConfig().set("item", SimpleLootCrates.itemToConfig(item));
        getConfig().set("open-sound", openSound.name().toLowerCase());
        List<Map<String, Object>> lootList = new ArrayList<>();
        for (Loot l : loot) {
            lootList.add(l.serialize());
        }
        getConfig().set("loot", lootList);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Sound getOpenSound() {
        return openSound;
    }

    public List<Loot> getLoot() {
        return loot;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public ItemStack getItemStack() {
        if (item == null) {
            throw new IllegalStateException("Item is null?");
        }
        ItemStack i = item.clone();
        ItemMeta meta = i.getItemMeta();
        if (!meta.hasDisplayName()) {
            meta.setDisplayName(getName());
        }
        meta.getPersistentDataContainer().set(SimpleLootCrates.ID_KEY, PersistentDataType.STRING, getId());
        i.setItemMeta(meta);
        return i;
    }

    /**
     * Set the crate's item to a clone of the input item
     * @param item The item to set the crate item to
     */
    public void setItemStack(ItemStack item) {
        this.item = item.clone();
        this.item.setAmount(1);
    }

    public List<ItemStack> getRandomLoot() {
        List<ItemStack> l = new ArrayList<>();
        for (Loot loot : loot) {
            l.addAll(loot.getRandomItems());
        }
        return l;
    }

    /**
     * Get the amount of item stacks that this crate will return
     * @return The amount of item stacks this crate will return
     */
    public int getAmount() {
        int amount = 0;
        for (Loot loot : loot) {
            amount += loot.getAmount();
        }
        return amount;
    }
}
