package me.teatimetim.smallbag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class SmallBag extends JavaPlugin implements Listener {

    private final NamespacedKey KEY  = new NamespacedKey(this, "small_bag");
    private final HashMap<Player, Inventory> _openBags = new HashMap<>();

    @Override
    public void onEnable() {
        //create ItemStack
        ItemStack item = new ItemStack(Material.BROWN_SHULKER_BOX);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.AQUA + "Small Bag");
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 0);

        item.setItemMeta(meta);

        //implement recipe
        ShapedRecipe recipe = new ShapedRecipe(KEY, item);

        recipe.shape(
                "LLL",
                "ICI",
                "LLL"
        );

        recipe.setIngredient('L', Material.LEATHER);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('C', Material.CHEST);
        Bukkit.addRecipe(recipe);

        //implement listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        //validate player right-clicking w/ a bag w/o interacting with a block
        ItemStack heldItem = event.getItem();
        Block clickedBlock = event.getClickedBlock();
        if (!clickIsRightClick(event)
                || !heldItemIsSmallBag(heldItem)
                || blockIsInteractable(clickedBlock))
            return;

        //open inventory, allow player to interact w/ inventory
        BlockStateMeta bagMeta = (BlockStateMeta) heldItem.getItemMeta();
        ShulkerBox bag = (ShulkerBox) bagMeta.getBlockState();

        Inventory displayedInventory = Bukkit.createInventory(null, 9, bag.getCustomName());
        Inventory bagInventory = bag.getInventory();

        for (int i = 0; i < displayedInventory.getSize(); i++) {
            displayedInventory.setItem(i, bagInventory.getItem(i));
        }

        Player player = event.getPlayer();
        player.openInventory(displayedInventory);
        _openBags.putIfAbsent(player, displayedInventory);
    }

    @EventHandler
    public void onCloseBag(InventoryCloseEvent event) {
        //ignore inventories that aren't small bags
        Player player = (Player) event.getPlayer();
        if (!_openBags.containsKey(player)) return;

        //write contents of bag's inventory into the shulker's inventory
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        BlockStateMeta bagMeta = (BlockStateMeta) heldItem.getItemMeta();
        ShulkerBox bag = (ShulkerBox) bagMeta.getBlockState();

        Inventory bagNewInventory = event.getInventory();
        Inventory bagCurrentInventory = bag.getInventory();

        bagCurrentInventory.setContents(bagNewInventory.getContents());

        bagMeta.setBlockState(bag);
        heldItem.setItemMeta(bagMeta);

        _openBags.remove(player);
    }

    @EventHandler
    public void onTryPlaceBag(BlockPlaceEvent event) {
        //ignore placement(s) of small bag
        ItemStack heldItem = event.getItemInHand();
        if (heldItemIsSmallBag(heldItem))
            event.setCancelled(true);
    }

    private boolean clickIsRightClick(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK;
    }

    private boolean blockIsInteractable(Block block) {
        return block != null
                && block.getType().isInteractable();
    }

    private boolean heldItemIsSmallBag(ItemStack item) {
        return item != null
                && item.getItemMeta().getPersistentDataContainer().has(KEY);
    }
}
