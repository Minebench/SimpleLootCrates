# SimpleLootCrates
Simple Bukkit plugin to create loot crate items that give random loot. 

Crates are opened by right clicking the item and will show a small GUI with the items that the player will receive.
Players can then take the items out of the opened GUI or ‒ if the don't ‒ will receive the items into their inventory automatically on closing the GUI. 

Includes the ability to define different item rarities in a single crate as well as configuring everything via a GUI!

## Commands and Permissions
Command                       | Permission                        | Description 
------------------------------|-----------------------------------|----------------
`/simplelootcrates`           | `simplelootcrates.command`        | Main plugin command. Aliases: simplelootcrate, slc, crates, crate, lootcrate, lootcrates
`/slc reload`                 | `simplelootcrates.command.reload` | Reload the plugin config
`/slc gui`                    | `simplelootcrates.command.gui`    | Open a GUI to get and configure crates
`/slc add <crate>`            | `simplelootcrates.command.add`    | Add a new crate with an id
`/slc list`                   | `simplelootcrates.command.list`   | List all configured crates
`/slc get <crate>`            | `simplelootcrates.command.get`    | Get a crate by its id
`/slc give <player> <crate>`  | `simplelootcrates.command.give`   | Give a crate to a player

## Configuration

### Main config.yml
The main config.yml file currently only contains the sound which is played when a player opens a crate. Sounds can also be changed on a per-crate basis.
```yaml
open-sound: entity_player_levelup
```

### Crates configs
Crates config files are in the `crates` folder in the plugin's data folder. Each crate will get its own config file with the naming scheme `<id>.yml`.

The plugin includes the following `ExampleCrate.yml`:
```yaml
name: "&8Example Crate"
item: chest
sound: entity_player_levelup
loot:
- amount: 1
  items:
  - stone
  - sand
- amount: 4
  items:
  - 64 dirt
  - 64 gravel
```
This means that the crate will have the name `&8Example Crate` and look like a `chest`.

It will contain 1 item that is either stone or sand and 4 items which are either 64 dirt or 64 gravel.  
Of course more complex items are supported including full meta/nbt data but I highly recommend configuring that via the GUI!  
A crate can include up to 54 items (as that's the limit on the GUI) but if you really want more then how about crates in crates?

And of course you can set the open sound per crate! The example one will just play the default `entity_player_levelup` sound on open.

#### Via GUI

Crate selection:  
![](https://i.phoenix616.dev/fP51GJFY.png)

Crate editor:  
![](https://i.phoenix616.dev/fLEcilWI.png)  
![](https://i.phoenix616.dev/fIXDpMNd.png)  
![](https://i.phoenix616.dev/fISu6jZ6.png)

Edit loot group (you can simply add or remove items here and adjust the loot group item amount):  
![](https://i.phoenix616.dev/fMqPyqgh.png)

## Downloads
Currently only [development builds](https://ci.minebench.de/job/SimpleLootCrates/) are available.

## [License (GPLv3)](https://github.com/Minebench/SimpleLootCrates/blob/master/LICENSE)
```
 SimpleLootCrates
 Copyright (c) 2019 Max Lee aka Phoenix616 (mail@moep.tv)

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
