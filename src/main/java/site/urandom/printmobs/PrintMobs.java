package site.urandom.printmobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class PrintMobs extends JavaPlugin {
    private static final int mobTypeIdBegin = EntityType.CREEPER.getTypeId();
    private static final int defaultChunkRadius = 2;

    private static class ScanResult {
        public final int totalNumber;
        public final HashMap<EntityType, Integer> mobs;

        public ScanResult(Collection<Entity> entities) {
            mobs = new HashMap<>();
            AtomicInteger count = new AtomicInteger();

            entities.forEach(e -> {
                EntityType type = e.getType();
                if (type.getTypeId() >= mobTypeIdBegin) {
                    if (mobs.containsKey(type)) {
                        mobs.put(type, mobs.get(type) + 1);
                    } else {
                        mobs.put(type, 1);
                    }
                    count.addAndGet(1);
                }
            });

            this.totalNumber = count.get();
        }

        private ScanResult(int totalNumber, HashMap<EntityType, Integer> mobs) {
            this.totalNumber = totalNumber;
            this.mobs = mobs;
        }

        public void sendResult(CommandSender sender, String header) {
            sender.sendMessage(String.format(ChatColor.YELLOW + "%s: %d", header, totalNumber));
            mobs.forEach((type, count)
                    -> sender.sendMessage(String.format(ChatColor.GRAY + "* %s: %d", type.name(), count)));
        }

        public static ScanResult concat(Collection<ScanResult> results) {
            int totalNumber = 0;
            HashMap<EntityType, Integer> finalMobs = new HashMap<>();

            for (ScanResult result : results) {
                totalNumber += result.totalNumber;
                result.mobs.forEach((k,v) -> {
                   if(finalMobs.containsKey(k)){
                       finalMobs.put(k, finalMobs.get(k)+v);
                   }else{
                       finalMobs.put(k, v);
                   }
                });
            }

            return new ScanResult(totalNumber, finalMobs);
        }
    }

    private void scanChunks(Player player, int radius) {
        Chunk currentChunk = player.getLocation().getChunk();
        Collection<Chunk> nearbyChunks = nearbyChunks(currentChunk, radius);

        Collection<Entity> entities = new ArrayList<>();

        for (Chunk chunk : nearbyChunks) {
            entities.addAll(Arrays.asList(chunk.getEntities()));
        }

        ScanResult result = new ScanResult(entities);

        result.sendResult(player, String.format("Mobs in nearby chunk(s)(radius=%d)", radius));
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

    private void scanCurrentWorld(Player player) {
        World world = player.getWorld();
        Collection<Entity> entities = world.getEntities();

        ScanResult result = new ScanResult(entities);

        result.sendResult(player, String.format("Mobs in current world(world=%s)", world.getName()));
    }

    private void scanAllWorlds(CommandSender sender) {
        List<ScanResult> results = new ArrayList<>();

        for (World world: Bukkit.getWorlds()) {
            results.add(new ScanResult(world.getEntities()));
        }

        ScanResult finalResult = ScanResult.concat(results);

        finalResult.sendResult(sender, "Mobs in all worlds");
    }

    public void onEnable() {
        Objects.requireNonNull(getCommand("pm"))
                .setExecutor((sender, command, label, args) -> {
                    if (args.length > 0 && args[0].equals("all")){
                        scanAllWorlds(sender);
                        return true;
                    }

                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "This command can only be executed by player!");
                        return false;
                    }

                    Player player = (Player) sender;

                    if (args.length > 0 && args[0].equals("world")){
                        scanCurrentWorld(player);
                        return true;
                    }

                    int radius = defaultChunkRadius;

                    if (args.length > 0)
                        radius = Integer.parseInt(args[0]);

                    scanChunks(player, radius);

                    return true;
                });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
