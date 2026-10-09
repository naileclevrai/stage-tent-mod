package com.nailec.stagetents.furniture;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

/** Terrace parasol. The pole fills four blocks; the canopy is cloth and does not stop you walking under it. */
public class ParasolBlock extends MultiPropBlock {
    public ParasolBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.WHITE,
                new double[]{2, 0, 2, 14, 3, 14},
                new double[]{7, 0, 7, 9, 16, 9}),
                new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}});
    }

    @Override
    protected PropPartBlock.Form partForm(Direction facing) {
        return PropPartBlock.Form.POLE;
    }
}
