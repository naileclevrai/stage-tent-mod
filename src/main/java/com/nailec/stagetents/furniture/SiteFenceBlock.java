package com.nailec.stagetents.furniture;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

/** Site fence panel, three blocks wide and two high, with a dyed privacy scrim. */
public class SiteFenceBlock extends MultiPropBlock {
    public SiteFenceBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.GREEN, new double[]{0, 0, 6, 16, 16, 10}),
                new int[][]{{-1, 0}, {0, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}});
    }

    @Override
    protected PropPartBlock.Form partForm(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? PropPartBlock.Form.WALL_X : PropPartBlock.Form.WALL_Z;
    }
}
