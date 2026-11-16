package com.mimiworld.mimiworld.fluid;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * 米汤流体：米米世界维度内每 5 tick 流动一次（同水），其他维度 30 tick（同熔岩）。
 */
public final class RiceSoupFluid {

    private RiceSoupFluid() {
    }

    private static final Identifier MIMI_DIMENSION = Identifier.fromNamespaceAndPath(MimiWorld.MODID, "mimi_dimension");

    public static int tickDelay(LevelReader level) {
        if (level instanceof Level realLevel && realLevel.dimension().identifier().equals(MIMI_DIMENSION)) {
            return 5;
        }
        return 30;
    }

    /** 米汤源流体（静止）。 */
    public static class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return tickDelay(level);
        }
    }

    /** 米汤流动流体（带 LEVEL 0~7 状态）。 */
    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return tickDelay(level);
        }
    }
}
