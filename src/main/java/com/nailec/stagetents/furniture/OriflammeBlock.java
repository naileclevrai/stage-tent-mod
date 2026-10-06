package com.nailec.stagetents.furniture;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

/** Feather flag, five blocks tall. The cloth is visual only; the pole is what you collide with. */
public class OriflammeBlock extends MultiPropBlock {
    public OriflammeBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.RED, new double[]{6, 0, 6, 10, 16, 10}), new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 4}});
    }

    @Override
    protected PropPartBlock.Form partForm(Direction facing) {
        return PropPartBlock.Form.POLE;
    }
}
