package com.nailec.stagetents;

import com.nailec.stagetents.block.CanvasBlock;
import com.nailec.stagetents.block.RiggingBarBlock;
import com.nailec.stagetents.block.StretchPoleBlock;
import com.nailec.stagetents.block.TentBlock;
import com.nailec.stagetents.block.TentBlockEntity;
import com.nailec.stagetents.block.TentBlockItem;
import com.nailec.stagetents.block.TentWrenchItem;
import com.nailec.stagetents.furniture.FurnitureBlock;
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
    public static final RegistryObject<TentBlock> STRETCH = tent(TentType.STRETCH);

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

    // Event furniture. Shapes are given facing north, in pixels.
    public static final RegistryObject<FurnitureBlock> ROUND_TABLE = furniture("round_table", false, -1,
            new double[]{1, 0, 1, 15, 12, 15});
    public static final RegistryObject<FurnitureBlock> STANDING_TABLE = furniture("standing_table", false, -1,
            new double[]{3, 0, 3, 13, 18, 13});
    public static final RegistryObject<FurnitureBlock> BANQUET_CHAIR = furniture("banquet_chair", true, 0.45,
            new double[]{3, 0, 3, 13, 8, 13}, new double[]{3, 8, 11, 13, 18, 13});
    public static final RegistryObject<FurnitureBlock> BAR_COUNTER = furniture("bar_counter", true, -1,
            new double[]{0, 0, 0, 16, 17, 16});
    public static final RegistryObject<FurnitureBlock> BLEACHER = furniture("bleacher", true, 0.95,
            new double[]{0, 0, 0, 16, 8, 8}, new double[]{0, 0, 8, 16, 16, 16});

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC).sized(0.01F, 0.01F)
                    .noSave().noSummon().clientTrackingRange(8).build("seat"));

    public static final RegistryObject<Item> BIG_TOP_ITEM = tentItem("big_top", BIG_TOP);
    public static final RegistryObject<Item> PAGODA_ITEM = tentItem("pagoda", PAGODA);
    public static final RegistryObject<Item> FRAME_TENT_ITEM = tentItem("frame", FRAME_TENT);
    public static final RegistryObject<Item> GAZEBO_ITEM = tentItem("gazebo", GAZEBO);
    public static final RegistryObject<Item> ARCH_ITEM = tentItem("arch", ARCH);
    public static final RegistryObject<Item> STRETCH_ITEM = tentItem("stretch", STRETCH);
    public static final RegistryObject<Item> STRETCH_POLE_ITEM = ITEMS.register("stretch_pole", () -> new BlockItem(STRETCH_POLE.get(), new Item.Properties()));
    public static final RegistryObject<Item> RIGGING_BAR_ITEM = ITEMS.register("rigging_bar", () -> new BlockItem(RIGGING_BAR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ROUND_TABLE_ITEM = blockItem("round_table", ROUND_TABLE);
    public static final RegistryObject<Item> STANDING_TABLE_ITEM = blockItem("standing_table", STANDING_TABLE);
    public static final RegistryObject<Item> BANQUET_CHAIR_ITEM = blockItem("banquet_chair", BANQUET_CHAIR);
    public static final RegistryObject<Item> BAR_COUNTER_ITEM = blockItem("bar_counter", BAR_COUNTER);
    public static final RegistryObject<Item> BLEACHER_ITEM = blockItem("bleacher", BLEACHER);
    public static final RegistryObject<Item> WRENCH = ITEMS.register("tent_wrench", () -> new TentWrenchItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockEntityType<TentBlockEntity>> TENT_BE = BLOCK_ENTITIES.register("tent",
            () -> BlockEntityType.Builder.of(TentBlockEntity::new, BIG_TOP.get(), PAGODA.get(), FRAME_TENT.get(),
                    GAZEBO.get(), ARCH.get(), STRETCH.get()).build(null));

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
                out.accept(ARCH_ITEM.get());
                out.accept(RIGGING_BAR_ITEM.get());
                out.accept(ROUND_TABLE_ITEM.get());
                out.accept(STANDING_TABLE_ITEM.get());
                out.accept(BANQUET_CHAIR_ITEM.get());
                out.accept(BAR_COUNTER_ITEM.get());
                out.accept(BLEACHER_ITEM.get());
                out.accept(WRENCH.get());
            })
            .build());

    private ModRegistry() {}

    private static RegistryObject<TentBlock> tent(TentType type) {
        return BLOCKS.register(type.id, () -> new TentBlock(type,
                BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion()));
    }

    private static RegistryObject<FurnitureBlock> furniture(String id, boolean directional, double seat, double[]... shape) {
        return BLOCKS.register(id, () -> new FurnitureBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL)
                .strength(1.0F).sound(SoundType.WOOD).noOcclusion(), directional, seat, shape));
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
