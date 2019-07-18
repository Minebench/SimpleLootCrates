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

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiStorageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.SoundCategory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.tags.ItemTagType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public class CrateListener implements Listener {
    private final SimpleLootCrates plugin;

    public CrateListener(SimpleLootCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || event.getItem() == null
                || (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR)
                || !event.getItem().hasItemMeta()) {
            return;
        }

        ItemMeta meta = event.getItem().getItemMeta();
        if (meta.getCustomTagContainer().hasCustomTag(SimpleLootCrates.ID_KEY, ItemTagType.STRING)) {
            event.setCancelled(true);
            String id = meta.getCustomTagContainer().getCustomTag(SimpleLootCrates.ID_KEY, ItemTagType.STRING);
            Crate crate = plugin.getManager().getCrate(id);
            if (crate != null) {
                List<ItemStack> loot = crate.getRandomLoot();
                String[] setup;
                if (loot.size() <= 5) {
                    setup = new String[]{"iiiii"};
                } else if (loot.size() <= 9) {
                    setup = new String[]{"iii","iii","iii"};
                } else {
                    setup = new String[loot.size() / 9 + 1];
                    Arrays.fill(setup, "iiiiiiiii");
                }
                Inventory backingInventory = Bukkit.createInventory(null, (loot.size() / 9 + 1) * 9);
                for (int i = 0; i < loot.size(); i++) {
                    backingInventory.setItem(i, loot.get(i));
                }
                InventoryGui gui = new InventoryGui(plugin, crate.getName(), setup, new GuiStorageElement('i', backingInventory));
                gui.setCloseAction(close -> {
                    for (ItemStack item : backingInventory) {
                        if (item != null) {
                            for (ItemStack rest : close.getPlayer().getInventory().addItem(item).values()) {
                                close.getPlayer().getWorld().dropItem(close.getPlayer().getLocation(), rest);
                            }
                        }
                    }
                    return false;
                });

                if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                    event.getItem().setAmount(event.getItem().getAmount() - 1);
                }
                gui.show(event.getPlayer());
                if (crate.getOpenSound() != null) {
                    event.getPlayer().playSound(event.getPlayer().getLocation(), crate.getOpenSound(), SoundCategory.MASTER, 1.0f, 1.0f);
                } else if (plugin.getDefaultOpenSound() != null) {
                    event.getPlayer().playSound(event.getPlayer().getLocation(), plugin.getDefaultOpenSound(), SoundCategory.MASTER, 1.0f, 1.0f);
                }
            } else {
                plugin.getLogger().log(Level.WARNING, event.getPlayer().getName() + " tried to open crate with unknown ID " + id + "!");
            }
        }

    }
}
