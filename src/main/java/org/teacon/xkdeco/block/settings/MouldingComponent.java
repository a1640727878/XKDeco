package org.teacon.xkdeco.block.settings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;

public record MouldingComponent(boolean customPlacement) implements XKBlockComponent {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
	private static final MouldingComponent TRUE = new MouldingComponent(true);
	private static final MouldingComponent FALSE = new MouldingComponent(false);
	public static final Type<MouldingComponent> TYPE = XKBlockComponent.register(
			"moulding",
			RecordCodecBuilder.create(instance -> instance.group(
					Codec.BOOL.optionalFieldOf("custom_placement", false).forGetter(MouldingComponent::customPlacement)
			).apply(instance, MouldingComponent::getInstance)));

	public static MouldingComponent getInstance(boolean customPlacement) {
		return customPlacement ? TRUE : FALSE;
	}

	@Override
	public Type<?> type() {
		return TYPE;
	}

	@Override
	public void injectProperties(Block block, StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SHAPE);
	}

	@Override
	public BlockState registerDefaultState(BlockState state) {
		return state.setValue(FACING, Direction.NORTH).setValue(SHAPE, StairsShape.STRAIGHT);
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState pState) {
		return true;
	}

	private StairsShape getShapeAt(BlockState ourState, BlockGetter pLevel, BlockPos pPos) {
		Direction ourFacing = ourState.getValue(FACING);
		BlockState theirState = pLevel.getBlockState(pPos.relative(ourFacing));
		Direction theirFacing;
		if (canBeConnected(ourState, theirState)) {
			theirFacing = theirState.getValue(FACING);
			if (theirFacing.getAxis() != ourFacing.getAxis() && canTakeShape(ourState, pLevel, pPos, theirFacing.getOpposite())) {
				if (theirFacing == ourFacing.getCounterClockWise()) {
					return StairsShape.OUTER_LEFT;
				}
				return StairsShape.OUTER_RIGHT;
			}
		}

		theirState = pLevel.getBlockState(pPos.relative(ourFacing.getOpposite()));
		if (canBeConnected(ourState, theirState)) {
			theirFacing = theirState.getValue(FACING);
			if (theirFacing.getAxis() != ourFacing.getAxis() && canTakeShape(ourState, pLevel, pPos, theirFacing)) {
				if (theirFacing == ourFacing.getCounterClockWise()) {
					return StairsShape.INNER_LEFT;
				}
				return StairsShape.INNER_RIGHT;
			}
		}
		return StairsShape.STRAIGHT;
	}

	private boolean canTakeShape(BlockState ourState, BlockGetter pLevel, BlockPos pPos, Direction pFace) {
		BlockState blockState = pLevel.getBlockState(pPos.relative(pFace));
		return !canBeConnected(ourState, blockState) || blockState.getValue(FACING) != ourState.getValue(FACING);
	}

	private boolean canBeConnected(BlockState ourState, BlockState theirState) {
		//TODO add a new field Optional<BlockPredicate> to check?
		return ourState.is(theirState.getBlock());
	}

	@Override
	public BlockState getStateForPlacement(BlockState state, BlockPlaceContext context) {
		if (customPlacement) {
			return state;
		}
		BlockPos blockpos = context.getClickedPos();
		BlockState blockstate = state.setValue(FACING, context.getHorizontalDirection());
		blockstate = blockstate.setValue(SHAPE, getShapeAt(blockstate, context.getLevel(), blockpos));
		if (blockstate.canSurvive(context.getLevel(), context.getClickedPos())) {
			return blockstate;
		}
		return null;
	}

	@Override
	public BlockState updateShape(
			BlockState pState,
			Direction pDirection,
			BlockState pNeighborState,
			LevelAccessor pLevel,
			BlockPos pPos,
			BlockPos pNeighborPos) {
		if (pDirection.getAxis().isHorizontal()) {
			pState = pState.setValue(SHAPE, getShapeAt(pState, pLevel, pPos));
		}
		return pState;
	}

	@Override
	public BlockState rotate(BlockState pState, Rotation pRotation) {
		return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState pState, Mirror pMirror) {
		Direction direction = pState.getValue(FACING);
		StairsShape stairsshape = pState.getValue(SHAPE);
		switch (pMirror) {
			case LEFT_RIGHT:
				if (direction.getAxis() == Direction.Axis.Z) {
					switch (stairsshape) {
						case INNER_LEFT:
							return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
						case INNER_RIGHT:
							return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
						case OUTER_LEFT:
							return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
						case OUTER_RIGHT:
							return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
						default:
							return pState.rotate(Rotation.CLOCKWISE_180);
					}
				}
				break;
			case FRONT_BACK:
				if (direction.getAxis() == Direction.Axis.X) {
					switch (stairsshape) {
						case INNER_LEFT:
							return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
						case INNER_RIGHT:
							return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
						case OUTER_LEFT:
							return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
						case OUTER_RIGHT:
							return pState.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
						case STRAIGHT:
							return pState.rotate(Rotation.CLOCKWISE_180);
					}
				}
		}
		return pState;
	}
}
