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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CrateManager {
    private final SimpleLootCrates plugin;

    private Map<String, Crate> crates = new HashMap<>();
    public CrateManager(SimpleLootCrates plugin) {
        this.plugin = plugin;
    }

    public void addCrate(Crate crate) {
        crates.put(crate.getId().toLowerCase(), crate);
        plugin.getLogger().log(Level.INFO, "Added crate " + crate.getName());
    }

    public Collection<Crate> getCrates() {
        return crates.values();
    }

    public Crate getCrate(String id) {
        return crates.get(id.toLowerCase());
    }
}
