package net.blacklab.lmr.util.helper;

import net.blacklab.lmr.entity.littlemaid.EntityLittleMaid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class MaidHelper {

	/**
	 * メイドにアイテムを与える
	 */
	public static void giveItem(ItemStack stack, EntityLittleMaid maid) {
		if (!maid.maidInventory.addItemStackToInventory(stack)) {
			maid.entityDropItem(stack, 0);
		}
	}

	/**
	 * Returns if the maid can walk or swim. Used for move-AI.
	 * @param pMaid
	 * @return
	 */
	public static boolean canStartFollow(EntityLittleMaid pMaid) {
		if (!pMaid.isContractEX() || pMaid.isFreedom() || pMaid.getMaidMasterEntity() == null) {
			return false;
		}

		return pMaid.getDistanceSqToMaster() >= pMaid.getActiveModeClass().getDistanceSqToStartFollow();
	}

	public static boolean isOutSideHome(EntityLittleMaid pMaid) {
		if (pMaid.isFreedom()) {
			return pMaid.getDistanceSqToCenter(pMaid.getHomePosition()) > pMaid.getActiveModeClass().getFreedomTrackingRangeSq();
		}
		return false;
	}

	public static boolean isTargetReachable(EntityLittleMaid pMaid, Entity pTarget, double expandRangeSq) {
		return isTargetReachable(pMaid, pTarget.getPositionVector(), expandRangeSq);
	}

	/** Can maid reach target? **/
	public static boolean isTargetReachable(EntityLittleMaid pMaid, Vec3d pTarget, double expandRangeSq) {
		if (pMaid.isFreedom()) {
			return pMaid.getHomePosition().distanceSq(pTarget.xCoord, pTarget.yCoord, pTarget.zCoord)
					<= pMaid.getActiveModeClass().getFreedomTrackingRangeSq() + expandRangeSq;
		}
		if (pMaid.getMaidMasterEntity() == null) {
			return true;
		}
		return pMaid.getMaidMasterEntity().getDistanceSq(pTarget.xCoord, pTarget.yCoord, pTarget.zCoord)
				<= pMaid.getActiveModeClass().getLimitRangeSqOnFollow() + expandRangeSq;
	}

}
