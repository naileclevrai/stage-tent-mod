package com.nailec.stagetents;

import com.nailec.stagetents.block.CanvasBlock;
import com.nailec.stagetents.block.RiggingBarBlock;
import com.nailec.stagetents.block.StretchPoleBlock;
import com.nailec.stagetents.block.TentBlock;
import com.nailec.stagetents.block.TentBlockEntity;
import com.nailec.stagetents.block.TentBlockItem;
import com.nailec.stagetents.block.TentWrenchItem;
import com.nailec.stagetents.furniture.ConnectedFurnitureBlock;
import com.nailec.stagetents.furniture.FurnitureBlock;
import com.nailec.stagetents.furniture.FurnitureBlock.Spec;
import com.nailec.stagetents.furniture.MultiPropBlock;
import com.nailec.stagetents.furniture.PropPartBlock;
import com.nailec.stagetents.furniture.StanchionBlock;
import com.nailec.stagetents.furniture.StandingTableBlock;
import com.nailec.stagetents.furniture.TurnstileBlock;
import com.nailec.stagetents.furniture.TurnstileBlockEntity;
import net.minecraft.world.item.DyeColor;
import com.nailec.stagetents.furniture.SeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.nailec.stagetents.tent.TentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, StageTents.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StageTents.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StageTents.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StageTents.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StageTents.MOD_ID);

    public static final RegistryObject<TentBlock> BIG_TOP = tent(TentType.BIG_TOP);
    public static final RegistryObject<TentBlock> PAGODA = tent(TentType.PAGODA);
    public static final RegistryObject<TentBlock> FRAME_TENT = tent(TentType.FRAME);
    public static final RegistryObject<TentBlock> GAZEBO = tent(TentType.GAZEBO);
    public static final RegistryObject<TentBlock> ARCH = tent(TentType.ARCH);
    public static final RegistryObject<TentBlock> DJ_ARCH = tent(TentType.DJ_ARCH);
    public static final RegistryObject<TentBlock> STRETCH = tent(TentType.STRETCH);
    public static final RegistryObject<TentBlock> TENSILE = tent(TentType.TENSILE);

    public static final RegistryObject<StretchPoleBlock> STRETCH_POLE = BLOCKS.register("stretch_pole", () -> new StretchPoleBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<RiggingBarBlock> RIGGING_BAR = BLOCKS.register("rigging_bar", () -> new RiggingBarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion()));

    /** Invisible collision cells placed by a tent: roof layers, walls and masts. */
    public static final RegistryObject<CanvasBlock> CANVAS = BLOCKS.register("canvas", () -> new CanvasBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(-1.0F, 3600000.0F).sound(SoundType.WOOL)
                    .noOcclusion().noLootTable().forceSolidOn().pushReaction(PushReaction.BLOCK)
                    .isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));

    // Event furniture. Collision boxes are given facing north, in pixels.
    public static final RegistryObject<FurnitureBlock> ROUND_TABLE = BLOCKS.register("round_table", () -> new FurnitureBlock(
            furnitureProps(SoundType.WOOL), Spec.of(false, DyeColor.WHITE, new double[]{0.5, 0, 0.5, 15.5, 12, 15.5})));
    public static final RegistryObject<StandingTableBlock> STANDING_TABLE = BLOCKS.register("standing_table", () -> new StandingTableBlock(
            furnitureProps(SoundType.WOOL), Spec.of(false, DyeColor.WHITE, new double[]{1.6, 0, 1.6, 14.4, 17.6, 14.4})));
    public static final RegistryObject<FurnitureBlock> BANQUET_CHAIR = BLOCKS.register("banquet_chair", () -> new FurnitureBlock(
            furnitureProps(SoundType.WOOD), Spec.seat(0.55, 0, DyeColor.WHITE,
            new double[]{3.4, 0, 3.4, 12.6, 9.4, 12.6}, new double[]{3.4, 9.4, 11.6, 12.6, 18, 13.4})));
    public static final RegistryObject<FurnitureBlock> FOLDING_CHAIR = BLOCKS.register("folding_chair", () -> new FurnitureBlock(
            furnitureProps(SoundType.METAL), Spec.seat(0.55, 0.02, DyeColor.BLACK,
            new double[]{3.1, 0, 3.6, 12.9, 9, 13.2}, new double[]{3.1, 9, 10.1, 12.9, 15.6, 13})));
    public static final RegistryObject<FurnitureBlock> BAR_STOOL = BLOCKS.register("bar_stool", () -> new FurnitureBlock(
            furnitureProps(SoundType.METAL), new Spec(false, 0.84, 0, DyeColor.RED, new double[]{4.2, 0, 4.2, 11.8, 13.75, 11.8})));
    public static final RegistryObject<ConnectedFurnitureBlock> BAR_COUNTER = BLOCKS.register("bar_counter", () -> new ConnectedFurnitureBlock(
            furnitureProps(SoundType.WOOD), Spec.of(true, DyeColor.WHITE, new double[]{0, 0, 0, 16, 17.2, 10.6}), "bar"));
    public static final RegistryObject<ConnectedFurnitureBlock> BLEACHER = BLOCKS.register("bleacher", () -> new ConnectedFurnitureBlock(
            furnitureProps(SoundType.METAL), Spec.seat(0.5, 0.22, DyeColor.WHITE,
            new double[]{0, 0, 0, 16, 1.2, 8}, new double[]{0, 0, 8, 16, 7.4, 16}, new double[]{0, 7.4, 14, 16, 12.8, 16}), "grandstand"));
    public static final RegistryObject<ConnectedFurnitureBlock> BLEACHER_AISLE = BLOCKS.register("bleacher_aisle", () -> new ConnectedFurnitureBlock(
            furnitureProps(SoundType.METAL), Spec.of(true, DyeColor.WHITE,
            new double[]{0, 0, 0, 16, 1.2, 8}, new double[]{0, 0, 8, 16, 8, 16}), "grandstand"));
    public static final RegistryObject<FurnitureBlock> BLEACHER_SUPPORT = BLOCKS.register("bleacher_support", () -> new FurnitureBlock(
            furnitureProps(SoundType.METAL), Spec.of(true, DyeColor.WHITE, new double[]{0, 0, 0, 16, 16, 16})));
    public static final RegistryObject<MultiPropBlock> CROWD_BARRIER = BLOCKS.register("crowd_barrier", () -> new MultiPropBlock(
            furnitureProps(SoundType.CHAIN), Spec.of(true, DyeColor.WHITE, new double[]{0, 0, 7, 16, 24, 9}),
            new int[][]{{-1, 0}, {0, 0}, {1, 0}}, true));
    public static final RegistryObject<StanchionBlock> STANCHION = BLOCKS.register("stanchion", () -> new StanchionBlock(
            furnitureProps(SoundType.METAL), Spec.of(false, DyeColor.RED, new double[]{6, 0, 6, 10, 15.5, 10})));
    public static final RegistryObject<MultiPropBlock> SHOOTING_GALLERY = BLOCKS.register("shooting_gallery", () -> new MultiPropBlock(
            furnitureProps(SoundType.WOOD), Spec.of(true, DyeColor.WHITE, new double[]{0, 0, 0, 16, 16, 16}),
            new int[][]{{-1, 0}, {0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}}));
    public static final RegistryObject<TurnstileBlock> TURNSTILE = BLOCKS.register("turnstile", () -> new TurnstileBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<FurnitureBlock> GUIDE_RAIL = BLOCKS.register("guide_rail", () -> new FurnitureBlock(
            furnitureProps(SoundType.METAL), Spec.of(true, DyeColor.WHITE, new double[]{7, 0, 0, 9, 24, 16})));
    public static final RegistryObject<PropPartBlock> PROP_PART = BLOCKS.register("prop_part", () -> new PropPartBlock(
            BlockBehaviour.Properties.of().strength(-1.0F, 3600000.0F).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)
                    .isValidSpawn((st, l, ps, e) -> false).isSuffocating((st, l, ps) -> false).isViewBlocking((st, l, ps) -> false)));

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC).sized(0.01F, 0.01F)
                    .noSave().noSummon().clientTrackingRange(8).build("seat"));

    public static final RegistryObject<Item> BIG_TOP_ITEM = tentItem("big_top", BIG_TOP);
    public static final RegistryObject<Item> PAGODA_ITEM = tentItem("pagoda", PAGODA);
    public static final RegistryObject<Item> FRAME_TENT_ITEM = tentItem("frame", FRAME_TENT);
    public static final RegistryObject<Item> GAZEBO_ITEM = tentItem("gazebo", GAZEBO);
    public static final RegistryObject<Item> ARCH_ITEM = tentItem("arch", ARCH);
    public static final RegistryObject<Item> DJ_ARCH_ITEM = tentItem("dj_arch", DJ_ARCH);
    public static final RegistryObject<Item> STRETCH_ITEM = tentItem("stretch", STRETCH);
    public static final RegistryObject<Item> TENSILE_ITEM = tentItem("tensile", TENSILE);
    public static final RegistryObject<Item> STRETCH_POLE_ITEM = ITEMS.register("stretch_pole", () -> new BlockItem(STRETCH_POLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RIGGING_BAR_ITEM = ITEMS.register("rigging_bar", () -> new BlockItem(RIGGING_BAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ROUND_TABLE_ITEM = blockItem("round_table", ROUND_TABLE);
    public static final RegistryObject<Item> STANDING_TABLE_ITEM = blockItem("standing_table", STANDING_TABLE);
    public static final RegistryObject<Item> BANQUET_CHAIR_ITEM = blockItem("banquet_chair", BANQUET_CHAIR);
    public static final RegistryObject<Item> BAR_COUNTER_ITEM = blockItem("bar_counter", BAR_COUNTER);
    public static final RegistryObject<Item> BLEACHER_ITEM = blockItem("bleacher", BLEACHER);
    public static final RegistryObject<Item> FOLDING_CHAIR_ITEM = blockItem("folding_chair", FOLDING_CHAIR);
    public static final RegistryObject<Item> BLEACHER_AISLE_ITEM = blockItem("bleacher_aisle", BLEACHER_AISLE);
    public static final RegistryObject<Item> BLEACHER_SUPPORT_ITEM = blockItem("bleacher_support", BLEACHER_SUPPORT);
    public static final RegistryObject<Item> BAR_STOOL_ITEM = blockItem("bar_stool", BAR_STOOL);
    public static final RegistryObject<Item> CROWD_BARRIER_ITEM = blockItem("crowd_barrier", CROWD_BARRIER);
    public static final RegistryObject<Item> STANCHION_ITEM = blockItem("stanchion", STANCHION);
    public static final RegistryObject<Item> TURNSTILE_ITEM = blockItem("turnstile", TURNSTILE);
    public static final RegistryObject<Item> GUIDE_RAIL_ITEM = blockItem("guide_rail", GUIDE_RAIL);
    public static final RegistryObject<Item> SHOOTING_GALLERY_ITEM = ITEMS.register("shooting_gallery",
            () -> new BlockItem(SHOOTING_GALLERY.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WRENCH = ITEMS.register("tent_wrench", () -> new TentWrenchItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockEntityType<TentBlockEntity>> TENT_BE = BLOCK_ENTITIES.register("tent",
            () -> BlockEntityType.Builder.of(TentBlockEntity::new, BIG_TOP.get(), PAGODA.get(), FRAME_TENT.get(),
                    GAZEBO.get(), ARCH.get(), DJ_ARCH.get(), STRETCH.get(), TENSILE.get()).build(null));

    public static final RegistryObject<BlockEntityType<TurnstileBlockEntity>> TURNSTILE_BE = BLOCK_ENTITIES.register("turnstile",
            () -> BlockEntityType.Builder.of(TurnstileBlockEntity::new, TURNSTILE.get()).build(null));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.stagetents"))
            .icon(() -> new ItemStack(BIG_TOP_ITEM.get()))
            .displayItems((params, out) -> {
                out.accept(BIG_TOP_ITEM.get());
                out.accept(PAGODA_ITEM.get());
                out.accept(FRAME_TENT_ITEM.get());
                out.accept(GAZEBO_ITEM.get());
                out.accept(STRETCH_ITEM.get());
                out.accept(STRETCH_POLE_ITEM.get());
                out.accept(TENSILE_ITEM.get());
                out.accept(ARCH_ITEM.get());
                out.accept(DJ_ARCH_ITEM.get());
                out.accept(RIGGING_BAR_ITEM.get());
                out.accept(ROUND_TABLE_ITEM.get());
                out.accept(BANQUET_CHAIR_ITEM.get());
                out.accept(STANDING_TABLE_ITEM.get());
                out.accept(BAR_COUNTER_ITEM.get());
                out.accept(BAR_STOOL_ITEM.get());
                out.accept(FOLDING_CHAIR_ITEM.get());
                out.accept(BLEACHER_ITEM.get());
                out.accept(BLEACHER_AISLE_ITEM.get());
                out.accept(BLEACHER_SUPPORT_ITEM.get());
                out.accept(CROWD_BARRIER_ITEM.get());
                out.accept(STANCHION_ITEM.get());
                out.accept(TURNSTILE_ITEM.get());
                out.accept(GUIDE_RAIL_ITEM.get());
                out.accept(SHOOTING_GALLERY_ITEM.get());
                out.accept(WRENCH.get());
            })
            .build());

    private ModRegistry() {}

    private static RegistryObject<TentBlock> tent(TentType type) {
        return BLOCKS.register(type.id, () -> new TentBlock(type,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion()));
    }

    private static BlockBehaviour.Properties furnitureProps(SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.0F).sound(sound).noOcclusion();
    }

    private static RegistryObject<Item> blockItem(String id, RegistryObject<? extends Block> block) {
        return ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static RegistryObject<Item> tentItem(String id, RegistryObject<TentBlock> block) {
        return ITEMS.register(id, () -> new TentBlockItem(block.get(), new Item.Properties().stacksTo(1)));
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
    }
}
