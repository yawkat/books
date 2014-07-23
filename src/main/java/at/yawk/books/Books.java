package at.yawk.books;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author yawkat
 */
public class Books extends JavaPlugin implements Listener {
    private int slot;
    private ItemStack stack;
    private boolean prohibitInteraction;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        JsonObject config;
        try (Reader reader = new FileReader(new File(getDataFolder(), "config.json"))) {
            config = new JsonParser().parse(reader).getAsJsonObject();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load config.json", e);
            return;
        }

        slot = config.get("slot").getAsInt();
        stack = new ItemStack(Material.WRITTEN_BOOK);
        if (config.has("amount")) {
            stack.setAmount(config.get("amount").getAsInt());
        }
        prohibitInteraction = config.get("prohibit_interaction").getAsBoolean();
        BookMeta meta = (BookMeta) stack.getItemMeta();
        meta.setDisplayName(config.get("display_name").getAsString());
        meta.setAuthor(config.get("author").getAsString());
        meta.setTitle(config.get("title").getAsString());
        List<String> pages = ImmutableList.copyOf(Iterables.transform(config.getAsJsonArray("pages"),
                                                                      new Function<JsonElement, String>() {
                                                                          @Override
                                                                          public String apply(JsonElement jsonElement) {
                                                                              return jsonElement.getAsString();
                                                                          }
                                                                      }));
        meta.setPages(pages);
        stack.setItemMeta(meta);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        give(event.getPlayer());
    }

    private void give(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack copy = stack.clone();
        inventory.remove(copy);
        if (inventory.getItem(slot) == null) {
            inventory.setItem(slot, copy);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (prohibitInteraction && event.getClickedInventory() instanceof PlayerInventory && event.getSlot() == slot) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (prohibitInteraction && event.getPlayer().getInventory().getHeldItemSlot() == slot) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(final PlayerDropItemEvent event) {
        if (prohibitInteraction && event.getItemDrop().getItemStack().equals(this.stack)) {
            event.setCancelled(true);
            getServer().getScheduler().runTask(this, new Runnable() {
                @Override
                public void run() {
                    give(event.getPlayer());
                }
            });
        }
    }
}
