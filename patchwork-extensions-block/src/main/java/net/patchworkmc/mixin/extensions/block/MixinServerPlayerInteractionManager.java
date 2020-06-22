/*
 * Minecraft Forge, Patchwork Project
 * Copyright (c) 2016-2020, 2019-2020
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.patchworkmc.mixin.extensions.block;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraftforge.common.extensions.IForgeBlockState;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import net.patchworkmc.impl.extensions.block.BlockContext;
import net.patchworkmc.impl.extensions.block.Signatures;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class MixinServerPlayerInteractionManager {
	// removedByPlayer, canHarvestBlock, isAir

	private boolean patchwork$removeBlock(BlockPos pos, boolean canHarvest) {
		ServerPlayerInteractionManager me = (ServerPlayerInteractionManager) (Object) this;
		BlockState state = me.world.getBlockState(pos);
		boolean removed = ((IForgeBlockState) state).removedByPlayer(me.world, pos, me.player, canHarvest, me.world.getFluidState(pos));

		if (removed) {
			state.getBlock().onBroken(me.world, pos, state);
		}

		return removed;
	}

	@Redirect(method = "tryBreakBlock", at = @At(value = "INVOKE", target = Signatures.Block_onBreak, ordinal = 0))
	private void patchwork$tryBreakBlock_onBreak(Block block, World world, BlockPos pos, BlockState state, PlayerEntity player) {
		// Suppress this call
	}

	@Redirect(method = "tryBreakBlock", at = @At(value = "INVOKE", target = Signatures.ServerWorld_removeBlock, ordinal = 0))
	private boolean patchwork$tryBreakBlock_removeBlock_qwq(ServerWorld world, BlockPos pos, boolean bool) {
		return true; // bypass if (bl && bl2) {
	}

	@Redirect(method = "tryBreakBlock", at = @At(value = "INVOKE", target = Signatures.Block_onBroken, ordinal = 0))
	private void patchwork$tryBreakBlock_onBroken(Block block, IWorld world, BlockPos pos, BlockState state) {
		// Suppress this call
	}

	@Redirect(method = "tryBreakBlock", at = @At(value = "INVOKE", target = Signatures.ServerPlayerInteractionManager_isCreative, ordinal = 0))
	private boolean patchwork$tryBreakBlock_isCreative(ServerPlayerInteractionManager me, BlockPos pos) {
		boolean isCreative = me.isCreative();

		if (isCreative) {
			patchwork$removeBlock(pos, false);
		}

		return isCreative;
	}

	private static final ThreadLocal<Object> tryBreakBlock_canHarvest = BlockContext.createContext(); // flag1
	@Redirect(method = "tryBreakBlock", at = @At(value = "INVOKE", target = Signatures.ServerPlayerEntity_isUsingEffectiveTool, ordinal = 0))
	private boolean patchwork$tryBreakBlock_isUsingEffectiveTool(ServerPlayerEntity player, BlockState blockState, BlockPos pos) {
		ServerPlayerInteractionManager me = (ServerPlayerInteractionManager) (Object) this;
		boolean canHarvest = ((IForgeBlockState) blockState).canHarvestBlock(me.world, pos, player);
		BlockContext.pushContext(tryBreakBlock_canHarvest, canHarvest);
		return true; // bypass if (bl && bl2) {
	}

	@Redirect(method = "tryBreakBlock", at = @At(value = "INVOKE", target = Signatures.Block_afterBreak, ordinal = 0))
	private void patchwork$tryBreakBlock_afterBreak(Block block, World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack stack) {
		boolean canHarvest = BlockContext.popContext(tryBreakBlock_canHarvest);
		boolean removed = patchwork$removeBlock(pos, canHarvest);

		if (removed && canHarvest) {
			block.afterBreak(world, player, pos, state, blockEntity, stack);
		}

		// TODO: need BlockBreakEvent
		// if (removed && exp > 0) blockstate.getBlock().dropExperience(world, pos, exp);
	}
}
