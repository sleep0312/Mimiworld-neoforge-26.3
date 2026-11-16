package com.mimiworld.mimiworld.block;

import com.mimiworld.mimiworld.ModItems;
import com.mimiworld.mimiworld.block.entity.MimiTeleporterBlockEntity;
import com.mimiworld.mimiworld.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/**
 * 米米传送器（mimi_teleporter）：米米维度传送门的框架方块。
 *
 * <p>7×7 图案摆放 12 个传送器，逐个手持米米之眼右键插眼；全部插眼后播放激活序列，
 * 在中心生成 2 格米米传送门与顶上 1 格泥米。</p>
 */
public class MimiTeleporterBlock extends Block implements EntityBlock {

    public static final BooleanProperty EYE = BooleanProperty.create("eye");

    public static final int SEQ_WARMUP = 60;
    public static final int SEQ_INTERVAL = 20;
    public static final int SEQ_SHOTS = 12;
    public static final int SEQ_FORM_AT = SEQ_WARMUP + SEQ_INTERVAL * SEQ_SHOTS;

    public static final int[][] RING_OFFSETS = {
            {-2, -3}, {0, -3}, {2, -3},
            {3, -2}, {3, 0}, {3, 2},
            {2, 3}, {0, 3}, {-2, 3},
            {-3, 2}, {-3, 0}, {-3, -2},
    };

    public MimiTeleporterBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(EYE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EYE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MimiTeleporterBlockEntity(pos, state);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.MIMI_TELEPORTER.get()) {
            return null;
        }
        BlockEntityTicker<MimiTeleporterBlockEntity> ticker = MimiTeleporterBlockEntity::serverTick;
        return (BlockEntityTicker<T>) ticker;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level,
                                               BlockPos pos, boolean movedByPiston) {
        breakPortalFor(level, pos);
        clearSequence(level, pos);
    }

    private static void breakPortalFor(Level level, BlockPos framePos) {
        for (int[] offset : RING_OFFSETS) {
            BlockPos center = framePos.offset(-offset[0], 0, -offset[1]);
            if (!level.getBlockState(center).is(ModBlocks.MIMI_PORTAL.get())) {
                continue;
            }
            level.removeBlock(center, false);
            BlockPos second = center.above();
            if (level.getBlockState(second).is(ModBlocks.MIMI_PORTAL.get())) {
                level.removeBlock(second, false);
            }
            BlockPos cap = center.above(2);
            if (level.getBlockState(cap).is(ModBlocks.MUD_RICE.get())) {
                level.removeBlock(cap, false);
            }
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.MIMI_EYE.get())) {
            return InteractionResult.PASS;
        }
        if (state.getValue(EYE)) {
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.setBlock(pos, state.setValue(EYE, true), 3);
            tryActivate(level, pos);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    /** 以 clicked 为插眼中任意一个框架方块，反推中心并校验完整图案。 */
    public static boolean tryActivate(Level level, BlockPos clicked) {
        if (level.isClientSide()) {
            return false;
        }
        long now = level.getGameTime();
        for (int[] offset : RING_OFFSETS) {
            BlockPos center = clicked.offset(-offset[0], 0, -offset[1]);
            if (level.getBlockState(center).is(ModBlocks.MIMI_PORTAL.get())) {
                continue;
            }
            if (!isPatternComplete(level, center)) {
                continue;
            }
            if (!(level.getBlockEntity(center.offset(RING_OFFSETS[0][0], 0, RING_OFFSETS[0][1]))
                    instanceof MimiTeleporterBlockEntity anchor)) {
                continue;
            }
            if (anchor.seqStart == MimiTeleporterBlockEntity.SEQ_NONE) {
                startSequence(level, center, now);
                return true;
            }
            if (now - anchor.seqStart >= SEQ_FORM_AT && formPortal(level, center)) {
                return true;
            }
            return true;
        }
        return false;
    }

    private static void startSequence(Level level, BlockPos center, long start) {
        for (int[] offset : RING_OFFSETS) {
            BlockPos framePos = center.offset(offset[0], 0, offset[1]);
            if (level.getBlockEntity(framePos) instanceof MimiTeleporterBlockEntity tele) {
                syncSequence(level, framePos, tele, start, center);
            }
        }
    }

    private static void clearSequence(Level level, BlockPos framePos) {
        for (int[] offset : RING_OFFSETS) {
            BlockPos center = framePos.offset(-offset[0], 0, -offset[1]);
            boolean active = false;
            for (int[] ring : RING_OFFSETS) {
                if (level.getBlockEntity(center.offset(ring[0], 0, ring[1]))
                        instanceof MimiTeleporterBlockEntity tele
                        && tele.seqStart != MimiTeleporterBlockEntity.SEQ_NONE) {
                    active = true;
                    break;
                }
            }
            if (!active) {
                continue;
            }
            for (int[] ring : RING_OFFSETS) {
                BlockPos frame = center.offset(ring[0], 0, ring[1]);
                if (level.getBlockEntity(frame) instanceof MimiTeleporterBlockEntity tele) {
                    syncSequence(level, frame, tele, MimiTeleporterBlockEntity.SEQ_NONE, null);
                }
            }
        }
    }

    private static void syncSequence(Level level, BlockPos pos, MimiTeleporterBlockEntity tele,
                                     long start, @Nullable BlockPos center) {
        tele.seqStart = start;
        tele.seqCenter = center;
        tele.setChanged();
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    private static boolean isPatternComplete(Level level, BlockPos center) {
        for (int[] offset : RING_OFFSETS) {
            BlockState state = level.getBlockState(center.offset(offset[0], 0, offset[1]));
            if (!state.is(ModBlocks.MIMI_TELEPORTER.get()) || !state.getValue(EYE)) {
                return false;
            }
        }
        return true;
    }

    private static boolean formPortal(Level level, BlockPos center) {
        BlockPos second = center.above();
        BlockPos third = center.above(2);
        if (!level.getBlockState(center).canBeReplaced()
                || !level.getBlockState(second).canBeReplaced()
                || !level.getBlockState(third).canBeReplaced()) {
            return false;
        }
        BlockState portal = ModBlocks.MIMI_PORTAL.get().defaultBlockState();
        level.setBlock(center, portal, 3);
        level.setBlock(second, portal, 3);
        level.setBlock(third, ModBlocks.MUD_RICE.get().defaultBlockState(), 3);
        for (BlockPos pos : new BlockPos[]{center, second, third}) {
            int blockId = Block.getId(level.getBlockState(pos));
            level.levelEvent(2001, pos, blockId);
            level.levelEvent(2001, pos, blockId);
        }
        return true;
    }
}
