package com.nailec.stagetents.furniture;

import net.minecraft.world.item.DyeColor;

/** Wall menu slate. A dye recolours the wooden frame; the chalk stays white. */
public class MenuBoardBlock extends FurnitureBlock {
    public MenuBoardBlock(Properties props) {
        super(props, Spec.of(true, DyeColor.BROWN,
                new double[]{2.2, 1.5, 13.9, 13.8, 12.6, 16.0},
                new double[]{4.6, 12.3, 14.8, 6.2, 14.6, 15.6},
                new double[]{9.8, 12.3, 14.8, 11.4, 14.6, 15.6},
                new double[]{3.2, 14.2, 15.4, 12.8, 16.0, 16.0}));
    }
}
