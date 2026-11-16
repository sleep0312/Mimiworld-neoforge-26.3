package com.mimiworld.mimiworld.fluid;

import com.mimiworld.mimiworld.MimiWorld;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * 饭汤流体：米米世界维度内每 10 tick 流动一次，其他维度 35 tick（比熔岩更慢）。
 */
public final class RiceBrothFluid {

    private RiceBrothFluid() {
    }

    private static final Identifier MIMI_DIMENSION = Identifier.fromNamespaceAndPath(MimiWorld.MODID, "mimi_dimension");

    public static int tickDelay(LevelReader level) {
        if (level instanceof Level realLevel && realLevel.dimension().identifier().equals(MIMI_DIMENSION)) {
            return 10;
        }
        return 35;
    }

    /** 饭汤源流体（静止）。 */
    public static class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }

        @Override
        public int getTickDelay(LevelReader level) {
            return tickDelay(level);
        }
    }

    /** 饭汤流动流体（带 LEVEL 0~7 状态）。 */
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
