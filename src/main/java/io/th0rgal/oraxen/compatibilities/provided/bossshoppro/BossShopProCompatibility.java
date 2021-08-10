package io.th0rgal.oraxen.compatibilities.provided.bossshoppro;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.black_ixx.bossshop.BossShop;
import org.black_ixx.bossshop.events.BSCreatedShopItemEvent;
import org.black_ixx.bossshop.events.BSRegisterTypesEvent;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BossShopProCompatibility extends CompatibilityProvider<BossShop> {

    @EventHandler
    public void onBSCreatedShopItem(BSCreatedShopItemEvent event) {
        ConfigurationSection menuItem = event.getConfigurationSection().getConfigurationSection("MenuItem");
        if (menuItem == null)
            return;
        String itemID = menuItem.getString("oraxen");
        int amount = menuItem.getInt("amount");
        List<String> lores = menuItem.getStringList("lores");
        if (itemID == null)
            return;
        ItemStack itemStack = new ItemStack(Material.AIR);
        if (OraxenItems.exists(itemID))
            itemStack = OraxenItems.getItemById(itemID).build().clone();
        else
            Message.ITEM_NOT_FOUND.log("item", itemID);
        if (amount != 0)
            itemStack.setAmount(amount);

        ItemMeta meta = itemStack.getItemMeta();

        List<String> itemLores = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        for (String lore : lores)
            itemLores.add(parseComponentString(lore));

        meta.setLore(itemLores);
        itemStack.setItemMeta(meta);

        event.getShopItem().setItem(itemStack, false);
    }

    @EventHandler
    public void onBSRegisterTypes(BSRegisterTypesEvent event) {
        new OraxenReward().register();
        new OraxenPrice().register();
    }

    private String parseComponentString(String miniString) {
        return Utils.LEGACY_COMPONENT_SERIALIZER.serialize(MiniMessage.get()
                .parse(miniString, OraxenPlugin.get().getFontManager().getMiniMessagePlaceholders()));
    }

}
