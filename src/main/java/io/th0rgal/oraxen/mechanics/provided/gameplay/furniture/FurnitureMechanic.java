package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import de.jeff_media.customblockdata.CustomBlockData;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.items.OraxenItems;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.evolution.EvolvingFurniture;
import io.th0rgal.oraxen.utils.drops.Drop;
import io.th0rgal.oraxen.utils.drops.Loot;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class FurnitureMechanic extends Mechanic {

    public static final NamespacedKey FURNITURE_KEY = new NamespacedKey(OraxenPlugin.get(), "furniture");
    public static final NamespacedKey SEAT_KEY = new NamespacedKey(OraxenPlugin.get(), "seat");
    public static final NamespacedKey ROOT_KEY = new NamespacedKey(OraxenPlugin.get(), "root");
    public static final NamespacedKey ORIENTATION_KEY = new NamespacedKey(OraxenPlugin.get(), "orientation");
    public static final NamespacedKey EVOLUTION_KEY = new NamespacedKey(OraxenPlugin.get(), "evolution");
    public final boolean farmlandRequired;
    private final List<BlockLocation> barriers;
    private final boolean hasRotation;
    private final boolean hasSeat;
    private final BlockFace facing;
    private final Drop drop;
    private final EvolvingFurniture evolvingFurniture;
    private String placedItemId;
    private ItemStack placedItem;
    private Rotation rotation;
    private float seatHeight;
    private float seatYaw;

    @SuppressWarnings("unchecked")
    public FurnitureMechanic(MechanicFactory mechanicFactory, ConfigurationSection section) {
        super(mechanicFactory, section, itemBuilder -> itemBuilder.setCustomTag(FURNITURE_KEY,
                PersistentDataType.BYTE, (byte) 1));

        if (section.isString("item"))
            placedItemId = section.getString("item");

        barriers = new ArrayList<>();
        if (OraxenPlugin.getProtocolLib() && section.getBoolean("barrier", false))
            barriers.add(new BlockLocation(0, 0, 0));
        if (OraxenPlugin.getProtocolLib() && section.isList("barriers"))
            for (Object barrierObject : section.getList("barriers"))
                barriers.add(new BlockLocation((Map<String, Object>) barrierObject));

        if (section.isConfigurationSection("seat")) {
            ConfigurationSection seatSection = section.getConfigurationSection("seat");
            hasSeat = true;
            seatHeight = (float) seatSection.getDouble("height");
            seatYaw = (float) seatSection.getDouble("yaw");
        } else
            hasSeat = false;

        if (section.isConfigurationSection("evolution")) {
            evolvingFurniture = new EvolvingFurniture(getItemID(), section.getConfigurationSection("evolution"));
            ((FurnitureFactory) getFactory()).registerEvolution();
        } else evolvingFurniture = null;

        if (section.isString("rotation")) {
            rotation = Rotation.valueOf(section.getString("rotation", "NONE").toUpperCase());
            hasRotation = true;
        } else
            hasRotation = false;

        farmlandRequired = section.getBoolean("farmland_required", false);

        facing = BlockFace.valueOf(section.getString("facing", "UP").toUpperCase());

        List<Loot> loots = new ArrayList<>();
        if (section.isConfigurationSection("drop")) {
            ConfigurationSection drop = section.getConfigurationSection("drop");
            for (LinkedHashMap<String, Object> lootConfig : (List<LinkedHashMap<String, Object>>)
                    drop.getList("loots"))
                loots.add(new Loot(lootConfig));

            if (drop.isString("minimal_type")) {
                FurnitureFactory mechanic = (FurnitureFactory) mechanicFactory;
                List<String> bestTools = drop.isList("best_tools")
                        ? drop.getStringList("best_tools")
                        : new ArrayList<>();
                this.drop = new Drop(mechanic.toolTypes, loots, drop.getBoolean("silktouch"),
                        drop.getBoolean("fortune"), getItemID(),
                        drop.getString("minimal_type"),
                        bestTools);
            } else
                this.drop = new Drop(loots, drop.getBoolean("silktouch"), drop.getBoolean("fortune"),
                        getItemID());
        } else
            drop = new Drop(loots, false, false, getItemID());

    }

    public static ItemFrame getItemFrame(Location location) {
        for (Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1))
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == location.getBlockX()
                    && entity.getLocation().getBlockY() == location.getBlockY()
                    && entity.getLocation().getBlockZ() == location.getBlockZ()
                    && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING))
                return frame;
        return null;
    }

    public boolean hasBarriers() {
        return !barriers.isEmpty();
    }

    public List<BlockLocation> getBarriers() {
        return barriers;
    }

    public boolean hasRotation() {
        return hasRotation;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public boolean hasSeat() {
        return hasSeat;
    }

    public float getSeatHeight() {
        return seatHeight;
    }

    public float getSeatYaw() {
        return seatYaw;
    }

    public BlockFace getFacing() {
        return facing;
    }

    public Drop getDrop() {
        return drop;
    }

    public boolean hasEvolution() {
        return evolvingFurniture != null;
    }

    public EvolvingFurniture getEvolution() {
        return evolvingFurniture;
    }

    private void setPlacedItem() {
        if (placedItem == null) {
            placedItem = OraxenItems.getItemById(placedItemId != null ? placedItemId : getItemID()).build();
            ItemMeta meta = placedItem.getItemMeta();
            meta.setDisplayName("");
            placedItem.setItemMeta(meta);
        }
    }

    public void place(Rotation rotation, float yaw, Location location, String entityId) {
        setPlacedItem();
        place(rotation, yaw, location, entityId, placedItem);
    }

    public void place(Rotation rotation, float yaw, Location location, String entityId, ItemStack item) {

        ItemFrame itemFrame = location.getWorld().spawn(location, ItemFrame.class, (ItemFrame frame) -> {
            frame.setVisible(false);
            frame.setFixed(false);
            frame.setPersistent(true);
            frame.setItemDropChance(0);
            if (placedItemId != null) {
                setPlacedItem();
                frame.setItem(placedItem);
            } else frame.setItem(item);
            frame.setRotation(rotation);
            frame.setFacingDirection(getFacing());
            frame.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
            if (hasSeat())
                frame.getPersistentDataContainer().set(SEAT_KEY, PersistentDataType.STRING, entityId);
            if (hasEvolution())
                frame.getPersistentDataContainer().set(EVOLUTION_KEY, PersistentDataType.INTEGER, 0);
        });

        if (hasBarriers())
            for (Location sideLocation : getLocations(yaw, location, getBarriers())) {
                Block block = sideLocation.getBlock();
                PersistentDataContainer data = new CustomBlockData(block, OraxenPlugin.get());
                data.set(FURNITURE_KEY, PersistentDataType.STRING, getItemID());
                if (hasSeat())
                    data.set(SEAT_KEY, PersistentDataType.STRING, entityId);
                data.set(ROOT_KEY, PersistentDataType.STRING, new BlockLocation(location).toString());
                data.set(ORIENTATION_KEY, PersistentDataType.FLOAT, yaw);
                block.setType(Material.BARRIER, false);
            }
    }

    public boolean removeSolid(World world, BlockLocation rootBlockLocation, float orientation) {
        Location rootLocation = rootBlockLocation.toLocation(world);

        for (Location location : getLocations(orientation,
                rootLocation,
                getBarriers()))
            location.getBlock().setType(Material.AIR);

        for (Entity entity : rootLocation.getWorld().getNearbyEntities(rootLocation, 1, 1, 1))
            if (entity instanceof ItemFrame frame
                    && entity.getLocation().getBlockX() == rootLocation.getX()
                    && entity.getLocation().getBlockY() == rootLocation.getY()
                    && entity.getLocation().getBlockZ() == rootLocation.getZ()
                    && entity.getPersistentDataContainer().has(FURNITURE_KEY, PersistentDataType.STRING)) {
                if (entity.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
                    Entity stand = Bukkit.getEntity(UUID.fromString(entity.getPersistentDataContainer()
                            .get(SEAT_KEY, PersistentDataType.STRING)));
                    for (Entity passenger : stand.getPassengers())
                        stand.removePassenger(passenger);
                    stand.remove();
                }
                frame.remove();
                rootLocation.getBlock().setType(Material.AIR);
                return true;
            }
        return false;
    }

    public void removeAirFurniture(ItemFrame frame) {
        if (frame.getPersistentDataContainer().has(SEAT_KEY, PersistentDataType.STRING)) {
            Entity stand = Bukkit.getEntity(UUID.fromString(frame.getPersistentDataContainer()
                    .get(SEAT_KEY, PersistentDataType.STRING)));
            for (Entity passenger : stand.getPassengers())
                stand.removePassenger(passenger);
            stand.remove();
        }
        frame.remove();
    }

    public List<Location> getLocations(float rotation, Location center, List<BlockLocation> relativeCoordinates) {
        List<Location> output = new ArrayList<>();
        for (BlockLocation modifier : relativeCoordinates)
            output.add(modifier.groundRotate(rotation).add(center));
        return output;
    }

    public float getYaw(Rotation rotation) {
        return (Arrays.asList(Rotation.values()).indexOf(rotation) * 360f) / 8f;
    }
}
