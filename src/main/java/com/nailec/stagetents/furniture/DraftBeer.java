package com.nailec.stagetents.furniture;

import com.nailec.stagetents.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gameplay counters for a festival draft line. Servings and hose length are not a pressure-vessel simulation.
 * Beer runs from the keg to the tap. Gas runs from the cylinder to the keg. Both hoses are placed by hand.
 */
public final class DraftBeer {
    public static final int KEG_SERVINGS = 40;
    public static final int CYLINDER_SERVINGS = 80;
    public static final int BEER_PER_POUR = 1;
    public static final int GAS_PER_POUR = 1;
    public static final int REACH = 12;
    public static final int VERTICAL = 6;
    /** Longest straight run between two pins, in blocks. */
    public static final double SEGMENT = 8.0;
    public static final double MAX_LENGTH = 56.0;
    public static final int MAX_PINS = 20;
    public static final int POUR_TICKS = 80;
    public static final String LINK_KEY = "DraftLink";
    public static final String ROUTE_KEY = "DraftRoute";

    public enum Kind { TAP, KEG, CYLINDER }

    public enum Line { BEER, GAS }

    private DraftBeer() {}

    public static Kind kind(BlockState state) {
        if (state.getBlock() instanceof BeerTapBlock) return Kind.TAP;
        if (state.getBlock() instanceof BeerKegBlock) return Kind.KEG;
        if (state.getBlock() instanceof Co2CylinderBlock) return Kind.CYLINDER;
        return null;
    }

    /** Hose end, in world space. The tap's beer port sits on the back of the column. */
    public static Vec3 anchor(Level level, BlockPos pos, Line line) {
        BlockState state = level.getBlockState(pos);
        double x = 0.5, y = 0.5, z = 0.5;
        Kind kind = kind(state);
        if (kind == Kind.TAP) {
            y = 0.015;
            z = 0.64;
        } else if (kind == Kind.KEG && line == Line.BEER) {
            y = 0.94;
        } else if (kind == Kind.KEG) {
            x = 0.80;
            y = 0.78;
        } else if (kind == Kind.CYLINDER) {
            y = 1.70;
            z = 0.34;
        }
        return world(state, pos, x, y, z);
    }

    /** Nozzle the beer leaves, in world space. */
    public static Vec3 faucet(BlockState state, BlockPos pos) {
        return world(state, pos, 0.50, BeerTapBlock.lift(state) + 0.514, 0.418);
    }

    /** Height of the drip-tray grille, in block space. */
    public static double tray(BlockState state) {
        return BeerTapBlock.lift(state) + 0.026;
    }

    private static Vec3 world(BlockState state, BlockPos pos, double x, double y, double z) {
        Direction facing = state.hasProperty(FurnitureBlock.FACING) ? state.getValue(FurnitureBlock.FACING) : Direction.NORTH;
        double dx = x - 0.5;
        double dz = z - 0.5;
        double rx = dx;
        double rz = dz;
        switch (facing) {
            case SOUTH -> { rx = -dx; rz = -dz; }
            case WEST -> { rx = dz; rz = -dx; }
            case EAST -> { rx = -dz; rz = dx; }
            default -> { }
        }
        return new Vec3(pos.getX() + 0.5 + rx, pos.getY() + y, pos.getZ() + 0.5 + rz);
    }

    /** Points the held hose must pass through, from the marked port toward the loose end. */
    public static List<Vec3> pins(ItemStack hose) {
        List<Vec3> out = new ArrayList<>();
        CompoundTag tag = hose.getTag();
        if (tag != null && tag.contains(ROUTE_KEY)) readPoints(tag.getList(ROUTE_KEY, Tag.TAG_COMPOUND), out);
        return out;
    }

    public static void setPins(ItemStack hose, List<Vec3> pins) {
        if (pins.isEmpty()) hose.removeTagKey(ROUTE_KEY);
        else hose.getOrCreateTag().put(ROUTE_KEY, writePoints(pins));
    }

    public static ListTag writePoints(List<Vec3> points) {
        ListTag list = new ListTag();
        for (Vec3 p : points) {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("X", p.x);
            tag.putDouble("Y", p.y);
            tag.putDouble("Z", p.z);
            list.add(tag);
        }
        return list;
    }

    public static void readPoints(ListTag list, List<Vec3> into) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            into.add(new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")));
        }
    }

    /** Click on the floor, a wall or a counter while a port is marked. Sneak removes the last pin. */
    public static void route(Level level, UseOnContext ctx, Player player, ItemStack hose) {
        if (player.isShiftKeyDown()) {
            unpin(player, hose);
            return;
        }
        BlockPos from = BlockPos.of(hose.getOrCreateTag().getLong(LINK_KEY));
        if (!level.isLoaded(from) || kind(level.getBlockState(from)) == null) {
            clearMark(hose);
            player.displayClientMessage(Component.translatable("message.stagetents.draft_gone"), true);
            return;
        }
        Direction face = ctx.getClickedFace();
        Vec3 pin = ctx.getClickLocation().add(face.getStepX() * 0.04, face.getStepY() * 0.04, face.getStepZ() * 0.04);
        List<Vec3> pins = pins(hose);
        Vec3 prev = pins.isEmpty() ? looseAnchor(level, from) : pins.get(pins.size() - 1);
        if (prev.distanceToSqr(pin) < 0.04) return;
        if (prev.distanceTo(pin) > SEGMENT || length(looseAnchor(level, from), pins) + prev.distanceTo(pin) > MAX_LENGTH) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_far"), true);
            return;
        }
        if (pins.size() >= MAX_PINS) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_pins_full"), true);
            return;
        }
        pins.add(pin);
        setPins(hose, pins);
        level.playSound(null, ctx.getClickedPos(), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.35F, 1.45F);
        player.displayClientMessage(Component.translatable("message.stagetents.draft_pin", pins.size()), true);
    }

    public static void unpin(Player player, ItemStack hose) {
        List<Vec3> pins = pins(hose);
        if (pins.isEmpty()) {
            clearMark(hose);
            player.displayClientMessage(Component.translatable("message.stagetents.draft_cancel"), true);
            return;
        }
        pins.remove(pins.size() - 1);
        setPins(hose, pins);
        player.displayClientMessage(Component.translatable(pins.isEmpty()
                ? "message.stagetents.draft_unpin" : "message.stagetents.draft_pin", pins.size()), true);
    }

    /** Port the loose hose leaves from, before the other end is chosen. */
    public static Vec3 looseAnchor(Level level, BlockPos pos) {
        Kind kind = kind(level.getBlockState(pos));
        if (kind == Kind.CYLINDER) return anchor(level, pos, Line.GAS);
        if (kind == Kind.TAP || kind == Kind.KEG) return anchor(level, pos, Line.BEER);
        return Vec3.atCenterOf(pos);
    }

    private static void clearMark(ItemStack hose) {
        hose.removeTagKey(LINK_KEY);
        hose.removeTagKey(ROUTE_KEY);
    }

    private static double length(Vec3 start, List<Vec3> pins) {
        double total = 0;
        Vec3 prev = start;
        for (Vec3 pin : pins) {
            total += prev.distanceTo(pin);
            prev = pin;
        }
        return total;
    }

    public static void link(Level level, BlockPos pos, Player player, ItemStack hose) {
        Kind here = kind(level.getBlockState(pos));
        if (here == null) return;
        CompoundTag tag = hose.getTag();
        if (tag == null || !tag.contains(LINK_KEY)) {
            clearMark(hose);
            hose.getOrCreateTag().putLong(LINK_KEY, pos.asLong());
            player.displayClientMessage(Component.translatable("message.stagetents.draft_pick"), true);
            return;
        }
        BlockPos from = BlockPos.of(tag.getLong(LINK_KEY));
        if (from.equals(pos)) {
            clearMark(hose);
            player.displayClientMessage(Component.translatable("message.stagetents.draft_cancel"), true);
            return;
        }
        if (!level.isLoaded(from) || !(level.getBlockEntity(from) instanceof DraftBlockEntity source) || kind(level.getBlockState(from)) == null) {
            clearMark(hose);
            player.displayClientMessage(Component.translatable("message.stagetents.draft_gone"), true);
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof DraftBlockEntity target)) return;
        Kind there = kind(level.getBlockState(from));
        Line line = match(here, there);
        if (line == null) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_bad"), true);
            return;
        }
        List<Vec3> pins = pins(hose);
        if (pos.equals(source.peer(line)) && pins.isEmpty()) {
            source.setPeer(line, null);
            target.setPeer(line, null);
            clearMark(hose);
            level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.6F, 1.2F);
            player.displayClientMessage(Component.translatable("message.stagetents.draft_unlink"), true);
            return;
        }
        Vec3 start = anchor(level, from, line);
        Vec3 end = anchor(level, pos, line);
        if (!reaches(from, pos, pins, start, end)) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_far"), true);
            return;
        }
        BlockPos sourceBusy = source.peer(line);
        BlockPos targetBusy = target.peer(line);
        if ((sourceBusy != null && !sourceBusy.equals(pos)) || (targetBusy != null && !targetBusy.equals(from))) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_busy"), true);
            return;
        }
        List<Vec3> back = new ArrayList<>(pins);
        Collections.reverse(back);
        source.setRoute(line, pos, pins);
        target.setRoute(line, from, back);
        clearMark(hose);
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7F, 0.9F);
        player.displayClientMessage(Component.translatable(line == Line.BEER ? "message.stagetents.draft_beer" : "message.stagetents.draft_gas"), true);
    }

    private static boolean reaches(BlockPos from, BlockPos to, List<Vec3> pins, Vec3 start, Vec3 end) {
        if (pins.isEmpty()) {
            double dx = to.getX() - from.getX();
            double dy = to.getY() - from.getY();
            double dz = to.getZ() - from.getZ();
            return Math.sqrt(dx * dx + dz * dz) <= REACH && Math.abs(dy) <= VERTICAL;
        }
        if (pins.size() > MAX_PINS) return false;
        double total = 0;
        Vec3 prev = start;
        for (int i = 0; i < pins.size(); i++) {
            double step = prev.distanceTo(pins.get(i));
            if (step > SEGMENT + (i == 0 ? 1.5 : 0)) return false;
            total += step;
            prev = pins.get(i);
        }
        double step = prev.distanceTo(end);
        return step <= SEGMENT + 1.5 && total + step <= MAX_LENGTH;
    }

    public static void useTap(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof DraftBlockEntity tap)) return;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ModRegistry.BEER_MUG.get())) {
            if (held.isEmpty() || player.isShiftKeyDown()) status(level, pos, player);
            return;
        }
        if (level.getGameTime() < tap.pourUntil()) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_pouring"), true);
            return;
        }
        DraftBlockEntity keg = beerKeg(level, tap);
        if (keg == null) {
            player.displayClientMessage(Component.translatable(tap.peer(Line.BEER) == null
                    ? "message.stagetents.draft_no_keg" : "message.stagetents.draft_unloaded"), true);
            return;
        }
        if (keg.beer() < BEER_PER_POUR) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_empty_keg"), true);
            return;
        }
        DraftBlockEntity cylinder = gasCylinder(level, keg);
        if (cylinder == null) {
            player.displayClientMessage(Component.translatable(keg.peer(Line.GAS) == null
                    ? "message.stagetents.draft_no_gas" : "message.stagetents.draft_unloaded"), true);
            return;
        }
        if (cylinder.gas() < GAS_PER_POUR) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_empty_gas"), true);
            return;
        }
        tap.setPourUntil(level.getGameTime() + POUR_TICKS);
        keg.addBeer(-BEER_PER_POUR);
        cylinder.addGas(-GAS_PER_POUR);
        if (!player.getAbilities().instabuild) held.shrink(1);
        ItemStack beer = new ItemStack(ModRegistry.BEER.get());
        if (held.isEmpty() && !player.getAbilities().instabuild) player.setItemInHand(hand, beer);
        else if (!player.getInventory().add(beer)) player.drop(beer, false);
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable("message.stagetents.draft_pour", keg.beer(), KEG_SERVINGS), true);
    }

    public static void status(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof DraftBlockEntity be)) return;
        Kind kind = kind(level.getBlockState(pos));
        if (kind == Kind.KEG) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_keg", be.beer(), KEG_SERVINGS), true);
        } else if (kind == Kind.CYLINDER) {
            player.displayClientMessage(Component.translatable("message.stagetents.draft_cylinder", be.gas(), CYLINDER_SERVINGS), true);
        } else if (kind == Kind.TAP) {
            DraftBlockEntity keg = beerKeg(level, be);
            DraftBlockEntity cylinder = keg == null ? null : gasCylinder(level, keg);
            if (be.peer(Line.BEER) == null) player.displayClientMessage(Component.translatable("message.stagetents.draft_no_keg"), true);
            else if (keg == null) player.displayClientMessage(Component.translatable("message.stagetents.draft_unloaded"), true);
            else if (keg.beer() < BEER_PER_POUR) player.displayClientMessage(Component.translatable("message.stagetents.draft_empty_keg"), true);
            else if (keg.peer(Line.GAS) == null) player.displayClientMessage(Component.translatable("message.stagetents.draft_no_gas"), true);
            else if (cylinder == null) player.displayClientMessage(Component.translatable("message.stagetents.draft_unloaded"), true);
            else if (cylinder.gas() < GAS_PER_POUR) player.displayClientMessage(Component.translatable("message.stagetents.draft_empty_gas"), true);
            else player.displayClientMessage(Component.translatable("message.stagetents.draft_ready", keg.beer(), cylinder.gas()), true);
        }
    }

    /** The keg this tap is strung to, when that chunk is loaded and the link agrees both ways. */
    public static DraftBlockEntity beerKeg(Level level, DraftBlockEntity tap) {
        return peer(level, tap, Line.BEER, Kind.KEG);
    }

    public static DraftBlockEntity gasCylinder(Level level, DraftBlockEntity keg) {
        return peer(level, keg, Line.GAS, Kind.CYLINDER);
    }

    private static DraftBlockEntity peer(Level level, DraftBlockEntity from, Line line, Kind expect) {
        BlockPos pos = from.peer(line);
        if (pos == null) return null;
        if (!level.isLoaded(pos)) return null;
        if (kind(level.getBlockState(pos)) != expect) {
            from.setPeer(line, null);
            return null;
        }
        if (!(level.getBlockEntity(pos) instanceof DraftBlockEntity other) || !from.getBlockPos().equals(other.peer(line))) {
            from.setPeer(line, null);
            return null;
        }
        return other;
    }

    private static Line match(Kind a, Kind b) {
        if (a == null || b == null || a == b) return null;
        if ((a == Kind.TAP && b == Kind.KEG) || (a == Kind.KEG && b == Kind.TAP)) return Line.BEER;
        if ((a == Kind.KEG && b == Kind.CYLINDER) || (a == Kind.CYLINDER && b == Kind.KEG)) return Line.GAS;
        return null;
    }

    public static int clampBeer(int value) {
        return Mth.clamp(value, 0, KEG_SERVINGS);
    }

    public static int clampGas(int value) {
        return Mth.clamp(value, 0, CYLINDER_SERVINGS);
    }
}
