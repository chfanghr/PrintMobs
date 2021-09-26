package site.urandom.printmobs;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PrintMobs extends JavaPlugin {
    private static final int mobTypeIdBegin = EntityType.CREEPER.getTypeId();
    private static final int defaultChunkRadius = 2;

    private void ScanChunks(Player player, int radius) {
        Chunk currentChunk = player.getLocation().getChunk();
        Collection<Chunk> nearbyChunks = nearbyChunks(currentChunk, radius);

        Collection<Entity> entities
                = new ArrayList<Entity>(Arrays.asList(currentChunk.getEntities()));

        for (Chunk chunk : nearbyChunks) {
            entities.addAll(Arrays.asList(chunk.getEntities()));
        }

        HashMap<EntityType, Integer> mobs = new HashMap<>();

        AtomicInteger totalNumber = new AtomicInteger();

        entities.forEach(e -> {
            EntityType type = e.getType();
            if (type.getTypeId() >= mobTypeIdBegin) {
                if (mobs.containsKey(type)) {
                    mobs.put(type, mobs.get(type) + 1);
                } else {
                    mobs.put(type, 1);
                }
                totalNumber.addAndGet(1);
            }
        });

        player.sendMessage(String.format(ChatColor.YELLOW + "In nearby chunks(radius=%d): %d", radius, totalNumber.get()));

        mobs.forEach((type, count) -> {
            player.sendMessage(String.format(ChatColor.GRAY + "* %s : %d", type.name(), count));
        });
    }

    public static Collection<Chunk> nearbyChunks(Chunk origin, int radius) {
        if (radius <= 0)
            return Collections.singletonList(origin);

        World world = origin.getWorld();

        int length = (radius * 2) + 1;
        Set<Chunk> chunks = new HashSet<>(length * length);

        int cX = origin.getX();
        int cZ = origin.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                chunks.add(world.getChunkAt(cX + x, cZ + z));
            }
        }

        return chunks;
    }


    public void onEnable() {
        Objects.requireNonNull(getCommand("pm"))
                .setExecutor((sender, command, label, args) -> {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "This command can only be executed by player!");
                        return false;
                    }

                    int radius = defaultChunkRadius;

                    if(args.length > 0)
                        radius = Integer.parseInt(args[0]);

                    ScanChunks((Player) sender, radius);

                    return true;
                });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
