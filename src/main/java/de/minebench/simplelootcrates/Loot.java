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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Loot {
    private final int amount;
    private final List<ItemStack> items = new ArrayList<>();

    public Loot(Map<String, Object> map) throws InvalidConfigurationException {
        Object itemsObject = map.get("items");
        if (!(itemsObject instanceof List) || ((List) itemsObject).isEmpty()) {
            throw new InvalidConfigurationException("Items list is empty");
        }
        amount = (int) map.getOrDefault("amount", 1);
        for (Object o : ((List) itemsObject)) {
            if (o instanceof String) {
                String[] parts = ((String) o).split(" ", 3);
                int amount = 1;
                String matStr = null;
                if (parts.length == 1) {
                    matStr = parts[0];
                } else {
                    try {
                        amount = Integer.parseInt(parts[0]);
                    } catch (IllegalArgumentException e) {
                        throw new InvalidConfigurationException("Invalid amount name " + parts[0] + " in " + o + "!");
                    }
                    matStr = parts[1];
                }
                Material mat = Material.matchMaterial(matStr);
                if (mat != null) {
                    ItemStack item = new ItemStack(mat, amount);
                    if (parts.length == 3) {
                        Bukkit.getUnsafe().modifyItemStack(item, parts[2]);
                    }
                    items.add(item);
                } else {
                    throw new InvalidConfigurationException("Invalid material name " + matStr + " in " + o + "!");
                }
            } else if (o instanceof Map) {
                items.add(ItemStack.deserialize((Map<String, Object>) o));
            }
        }
    }

    public Collection<ItemStack> getRandomItems() {
        List<ItemStack> itemStacks = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            itemStacks.add(items.get(SimpleLootCrates.RANDOM.nextInt(items.size())));
        }
        return itemStacks;
    }
}
