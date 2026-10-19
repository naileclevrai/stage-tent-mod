package com.nailec.stagetents.furniture;

import com.nailec.stagetents.client.MachineItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Machine item, drawn in the hand and the inventory with the same mesh as the placed machine. */
public class MachineItem extends BlockItem {
    public MachineItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return MachineItemRenderer.instance();
            }
        });
    }
}
