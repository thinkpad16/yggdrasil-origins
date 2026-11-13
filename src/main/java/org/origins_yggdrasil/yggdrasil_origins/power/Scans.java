package org.origins_yggdrasil.yggdrasil_origins.power;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * The answers to "how many of those are around me", shared by every power that asks.
 * <p>
 * Looking around is the only thing in this mod that costs a server real time: a radius of
 * three is three hundred and forty blocks, and a radius of ten over a mob farm is a list of
 * everything alive in it. Three powers of one path used to ask the same question three times
 * a second, each keeping its own answer.
 * <p>
 * So the answer is kept per player, per question, and reused while the player stands on the
 * same block and the answer is less than a second old. The answer is a <em>count</em>, not a
 * yes or no, so tiers of one path ("six blocks", "eighteen", "thirty-six") all read one scan.
 * <p>
 * Nothing here ever loads a chunk: a bonus is not worth generating terrain for, so blocks in
 * a chunk that is not in memory simply do not count.
 */
public final class Scans {

    /** How long an answer stays good while the player does not move, in ticks. */
    private static final long WINDOW = 20L;

    /** question -> player -> {block position, when, count}. Weak keys: a player who logs out
     * falls out on its own, and a respawned player is a new object with a fresh answer. */
    private static final Map<String, Map<ServerPlayer, long[]>> ANSWERS = new java.util.HashMap<>();

    /** The largest number any power asks about per question: counting past it changes no
     * answer, so the scan stops there. */
    private static final Map<String, Integer> CEILINGS = new java.util.HashMap<>();

    private static long scans;
    private static long reused;
    private static long blocksRead;
    private static long entitiesRead;

    private Scans() { }

    /**
     * Says that some power compares this question's count against {@code value}.
     * <p>
     * Called while the data is read, before anything is counted. Three tiers of one path ask
     * about six, eighteen and thirty-six blocks: counting stops at thirty-seven, one past the
     * largest number asked about. A count that reached the ceiling means "more than every
     * number anyone compares against", which settles every comparison - "&gt;=", "&gt;",
     * "&lt;", "&lt;=", "==" and "!=" alike. Stopping at the number itself would not: "&gt; 36"
     * and "&lt;= 36" cannot tell 36 from 50.
     */
    public static void expect(String question, double value) {
        int needed = (int) Math.floor(value) + 1;
        CEILINGS.merge(question, Math.max(1, needed), Math::max);
    }

    private static int ceiling(String question) {
        return CEILINGS.getOrDefault(question, Integer.MAX_VALUE);
    }

    /** Runs the question once per player per block per second, and hands back the count. */
    private static int count(ServerPlayer player, String question, ToIntFunction<ServerPlayer> work) {
        long at = player.blockPosition().asLong();
        long now = player.level().getGameTime();
        Map<ServerPlayer, long[]> perPlayer =
            ANSWERS.computeIfAbsent(question, key -> new WeakHashMap<>());
        long[] last = perPlayer.get(player);
        if (last != null && last[0] == at && now - last[1] < WINDOW && now >= last[1]) {
            reused++;
            return (int) last[2];
        }
        scans++;
        int found = work.applyAsInt(player);
        if (last == null) {
            perPlayer.put(player, new long[] {at, now, found});
        } else {
            last[0] = at;
            last[1] = now;
            last[2] = found;
        }
        return found;
    }

    /** How many blocks around the player pass the test. Chunks not in memory are skipped. */
    public static int blocks(ServerPlayer player, String question, int radius, boolean sphere,
                             Predicate<BlockState> test) {
        int stopAt = ceiling(question);
        return count(player, question, who -> {
            ServerLevel level = who.serverLevel();
            BlockPos origin = who.blockPosition();
            int low = Math.max(level.getMinBuildHeight(), origin.getY() - radius);
            int high = Math.min(level.getMaxBuildHeight() - 1, origin.getY() + radius);
            if (low > high) return 0;
            double reach = (double) radius * radius;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int found = 0;
            // walked chunk by chunk: one lookup per chunk instead of one per block, and a
            // chunk that is not loaded is left alone rather than pulled in
            for (int chunkX = (origin.getX() - radius) >> 4; chunkX <= (origin.getX() + radius) >> 4; chunkX++) {
                for (int chunkZ = (origin.getZ() - radius) >> 4; chunkZ <= (origin.getZ() + radius) >> 4; chunkZ++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                    if (chunk == null) continue;
                    int fromX = Math.max(origin.getX() - radius, chunkX << 4);
                    int toX = Math.min(origin.getX() + radius, (chunkX << 4) + 15);
                    int fromZ = Math.max(origin.getZ() - radius, chunkZ << 4);
                    int toZ = Math.min(origin.getZ() + radius, (chunkZ << 4) + 15);
                    for (int x = fromX; x <= toX; x++) {
                        for (int z = fromZ; z <= toZ; z++) {
                            for (int y = low; y <= high; y++) {
                                pos.set(x, y, z);
                                if (sphere && origin.distSqr(pos) > reach) continue;
                                blocksRead++;
                                if (test.test(chunk.getBlockState(pos))) found++;
                                if (found >= stopAt) return found;
                            }
                        }
                    }
                }
            }
            return found;
        });
    }

    /** How many living things around the player pass the test. */
    public static int entities(ServerPlayer player, String question, double radius, boolean sphere,
                               boolean countSelf, Predicate<LivingEntity> test) {
        return count(player, question, who -> {
            AABB box = new AABB(who.blockPosition()).inflate(radius);
            double reach = radius * radius;
            List<LivingEntity> found = who.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> (countSelf || entity != who) && test.test(entity));
            entitiesRead += found.size();
            if (!sphere) return found.size();
            int near = 0;
            for (int i = 0; i < found.size(); i++) {
                if (found.get(i).distanceToSqr(who) <= reach) near++;
            }
            return near;
        });
    }

    /** How many players around this one carry a given class. */
    public static int classmates(ServerPlayer player, String question, double radius,
                                 boolean countSelf, String wanted) {
        int stopAt = ceiling(question);
        return count(player, question, who -> {
            double reach = radius * radius;
            // the level's own player list, not an entity query: there are never many players
            // and this never touches a chunk
            List<ServerPlayer> players = who.serverLevel().players();
            int found = 0;
            for (int i = 0; i < players.size(); i++) {
                ServerPlayer other = players.get(i);
                if (!countSelf && other == who) continue;
                if (other.distanceToSqr(who) > reach) continue;
                if (org.origins_yggdrasil.yggdrasil_origins.clazz.ClassManager.wears(other, wanted)) {
                    found++;
                    if (found >= stopAt) return found;
                }
            }
            return found;
        });
    }

    /** The {@code /ygg perf} line: what looking around actually costs. */
    public static String report() {
        long asked = scans + reused;
        double shared = asked == 0 ? 0.0 : reused * 100.0 / asked;
        return String.format(
            "looking around: %d questions, %d scans (%.0f%% answered from the last one)"
                + " | %d blocks and %d entities read",
            asked, scans, shared, blocksRead, entitiesRead);
    }

    public static void reset() {
        scans = 0;
        reused = 0;
        blocksRead = 0;
        entitiesRead = 0;
    }

    /** Dropped when the data is reloaded: the questions themselves may be gone. */
    public static void forgetAll() {
        ANSWERS.clear();
        CEILINGS.clear();
    }
}
