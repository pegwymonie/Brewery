package com.dre.brewery.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BIngredients;
import com.dre.brewery.Brew;
import com.dre.brewery.Barrel;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Words;
import com.dre.brewery.Wakeup;
import com.dre.brewery.P;

public class PlayerListener implements Listener {
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();

		if (clickedBlock != null) {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				Player player = event.getPlayer();
				if (!player.isSneaking()) {
					if (clickedBlock.getType() == Material.CAULDRON) {
						if (clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.FIRE || clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.STATIONARY_LAVA
								|| clickedBlock.getRelative(BlockFace.DOWN).getType() == Material.LAVA) {
							Material materialInHand = event.getMaterial();
							ItemStack item = event.getItem();

							
							if (materialInHand == Material.WATCH) {
								BCauldron.printTime(player, clickedBlock);

								// fill a glass bottle with potion
							} else if (materialInHand == Material.GLASS_BOTTLE) {
								if (player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
									if (BCauldron.fill(player, clickedBlock)) {
										event.setCancelled(true);
										if (item.getAmount() > 1) {
											item.setAmount(item.getAmount() - 1);
										} else {
											player.setItemInHand(new ItemStack(0));
										}
									}
								} else {
									event.setCancelled(true);
								}

								// reset cauldron when refilling to prevent
								// unlimited source of potions
							} else if (materialInHand == Material.WATER_BUCKET) {
								if (clickedBlock.getData() != 0) {
									if (clickedBlock.getData() < 3) {
										// will only remove when existing
										BCauldron.remove(clickedBlock);
									}
								}

								// add ingredient to cauldron that meet the previous
								// contitions
							} else if (BIngredients.possibleIngredients.contains(materialInHand)) {
								if (BCauldron.ingredientAdd(clickedBlock, materialInHand)) {
									if (item.getAmount() > 1) {
										item.setAmount(item.getAmount() - 1);
									} else if (materialInHand == Material.MILK_BUCKET)
                                                                        {
                                                                        player.setItemInHand(new ItemStack(Material.BUCKET));
                                                                        }
                                                                        
                                                                        else {
										player.setItemInHand(new ItemStack(0));
									}
								}
							}
						}
						// access a barrel
					} else if (clickedBlock.getType() == Material.FENCE || clickedBlock.getType() == Material.NETHER_FENCE || clickedBlock.getType() == Material.SIGN
							|| clickedBlock.getType() == Material.WALL_SIGN) {
						Barrel barrel = Barrel.get(clickedBlock);
						if (barrel != null) {
							event.setCancelled(true);
							Block broken = Barrel.getBrokenBlock(clickedBlock);
							// barrel is built correctly
							if (broken == null) {
								barrel.open(player);
							} else {
								barrel.remove(broken);
							}
						}
					}
				}
			}
		}

		if (event.getAction() == Action.LEFT_CLICK_AIR) {
			if (!event.hasItem()) {
				if (Wakeup.checkPlayer != null) {
					if (event.getPlayer() == Wakeup.checkPlayer) {
						Wakeup.tpNext();
					}
				}
			}
		}

	}

	// player drinks a custom potion
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item != null) {
			if (item.getType() == Material.POTION) {
				if (item.hasItemMeta()) {
					if (BPlayer.drink(Brew.getUID(item), player)) {
						if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
							Brew.remove(item);
						}
					}
				}
			} else if (BPlayer.drainItems.containsKey(item.getType())) {
				BPlayer bplayer = BPlayer.get(player.getName());
				if (bplayer != null) {
					bplayer.drainByItem(player.getName(), item.getType());
				}
			}
		}
	}

	// Player has died! Decrease Drunkeness by 20
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		String playerName = event.getPlayer().getName();
		BPlayer bPlayer = BPlayer.get(playerName);
		if (bPlayer != null) {
			if (bPlayer.getDrunkeness() > 20) {
				bPlayer.setData(bPlayer.getDrunkeness() - 20, 0);
			} else {
				BPlayer.players.remove(playerName);
			}
		}
	}

	// player walks while drunk, push him around!
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (BPlayer.players.containsKey(event.getPlayer().getName())) {
			BPlayer.playerMove(event);
		}
	}

	// player talks while drunk, but he cant speak very well
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		if (BPlayer.players.containsKey(event.getPlayer().getName())) {
			Words.playerChat(event);
		}
	}
	
	// player commands while drunk, distort chat commands
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
		if (BPlayer.players.containsKey(event.getPlayer().getName())) {
			Words.playerCommand(event);
		}
	}

	// player joins while passed out
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerLogin(PlayerLoginEvent event) {
		final Player player = event.getPlayer();
		BPlayer bplayer = BPlayer.get(player.getName());
		if (bplayer != null) {
			if (player.hasPermission("brewery.bypass.logindeny")) {
				if (bplayer.getDrunkeness() > 100) {
					bplayer.setData(100, 0);
				}
				bplayer.join(player);
				return;
			}
			switch (bplayer.canJoin()) {
			case 0:
				bplayer.join(player);
				return;
			case 2:
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, P.p.languageReader.get("Player_LoginDeny"));
				return;
			case 3:
				event.disallow(PlayerLoginEvent.Result.KICK_OTHER, P.p.languageReader.get("Player_LoginDenyLong"));
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer().getName());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
	}
}