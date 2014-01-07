package com.dre.brewery.listeners;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import com.dre.brewery.Brew;

public class InventoryListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH)
	public void onBrew(BrewEvent event) {
		int slot = 0;
		BrewerInventory inv = event.getContents();
		ItemStack item;
		boolean custom = false;
		Boolean[] contents = new Boolean[3];
		while (slot < 3) {
			item = inv.getItem(slot);
			contents[slot] = false;
			if (item != null) {
				if (item.getType() == Material.POTION) {
					if (item.hasItemMeta()) {
						int uid = Brew.getUID(item);
						if (Brew.potions.containsKey(uid)) {
							// has custom potion in "slot"
							if (Brew.get(uid).canDistill()) {
								// is further distillable
								contents[slot] = true;
								custom = true;
							}
						}
					}
				}
			}
			slot++;
		}
		if (custom) {
			event.setCancelled(true);
			Brew.distillAll(inv, contents);
		}

	}

	// convert to non colored Lore when taking out of Barrel/Brewer
	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory().getType() == InventoryType.BREWING) {
			if (event.getSlot() > 2) {
				return;
			}
		} else if (event.getInventory().getType() == InventoryType.CHEST) {
			if (!event.getInventory().getTitle().equals("Fass")) {
				return;
			}
		} else {
			return;
		}

		ItemStack item = event.getCurrentItem();
		if (item != null) {
			if (item.getTypeId() == 373) {
				if (item.hasItemMeta()) {
					PotionMeta meta = (PotionMeta) item.getItemMeta();
					Brew brew = Brew.get(meta);
					if (brew != null) {
						if (Brew.hasColorLore(meta)) {
							brew.convertLore(meta, false);
							item.setItemMeta(meta);
						}
					}
				}
			}
		}
	}

// block the pickup of items where getPickupDelay is > 1000 (puke)  Taken From Orginal
   @EventHandler
   public void onInventoryPickupItem(InventoryPickupItemEvent event){
       Item item = event.getItem();
       
       if (item.getPickupDelay() > 1000){
           event.setCancelled(true);
       }       
   }
}