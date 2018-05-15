package net.blacklab.lmr.entity.ai;

import net.blacklab.lmr.entity.littlemaid.EntityLittleMaid;
import net.blacklab.lmr.entity.littlemaid.EntityMarkerDummy;
import net.blacklab.lmr.util.helper.MaidHelper;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class EntityAILMFindBlock extends EntityAIBase implements IEntityAILM {

	protected boolean isEnable;
	protected EntityLittleMaid theMaid;
//	protected MovingObjectPosition theBlock;
//	protected int tileY;
//	protected int tileZ;
//	protected boolean isFind;


	public EntityAILMFindBlock(EntityLittleMaid pEntityLittleMaid) {
		theMaid = pEntityLittleMaid;
		isEnable = true;
//		theBlock = null;

		setMutexBits(3);
	}

	@Override
	public boolean shouldExecute() {
//		LMM_EntityModeBase llmode = theMaid.getActiveModeClass();
//		if (!isEnable || theMaid.isWait() || theMaid.getActiveModeClass() == null || !theMaid.getActiveModeClass().isSearchBlock() || theMaid.getCurrentEquippedItem() == null) {
		if (!isEnable || theMaid.isMaidWait() || !theMaid.isActiveModeClass()) {
			return false;
		}
		if (!theMaid.getActiveModeClass().isSearchBlock()) {
			return theMaid.getActiveModeClass().shouldBlock(theMaid.maidMode);
		}

		// ターゲットをサーチ
		int lx = MathHelper.floor_double(theMaid.posX);
		int ly = MathHelper.floor_double(theMaid.posY);
		int lz = MathHelper.floor_double(theMaid.posZ);
		int vt = MathHelper.floor_float(((theMaid.rotationYawHead * 4F) / 360F) + 2.5F) & 3;
		int xx = lx;
		int yy = ly;
		int zz = lz;

		// TODO:Dummy
		EntityMarkerDummy.clearDummyEntity(theMaid);
		boolean flagdammy = false;

		// CW方向に検索領域を広げる
		for (int d = 0; d < 4; d++) {
			for (int a = 0; a < 18; a += 2) {
				int del = a / 2;
				if (vt == 0) {
					xx = lx - del;
					zz = lz - del;
				}
				else if (vt == 1) {
					xx = lx + del;
					zz = lz - del;
				}
				else if (vt == 2) {
					xx = lx + del;
					zz = lz + del;
				}
				else if (vt == 3) {
					xx = lx - del;
					zz = lz + del;
				}
				// TODO:Dummay
				if (!flagdammy) {
					EntityMarkerDummy.setDummyEntity(theMaid, 0x00ff4f4f, xx, ly, zz);
					flagdammy = true;
				}
				int b = 0;
				do {
					for (int c = 0; c < 3; c++) {
						yy = ly + (c == 2 ? -1 : c);
						if (theMaid.getActiveModeClass().checkBlock(theMaid.maidMode, xx, yy, zz)) {
							if (theMaid.getActiveModeClass().outrangeBlock(theMaid.maidMode, xx, yy, zz)) {
								theMaid.setTilePos(xx, yy, zz);
								// TODO:Dummay
								EntityMarkerDummy.setDummyEntity(theMaid, 0x004fff4f, xx, yy, zz);
								flagdammy = true;
								return true;
							}
						}
					}
					// TODO:Dummay
					if (!flagdammy) {
						EntityMarkerDummy.setDummyEntity(theMaid, 0x00ffffcf, xx, ly, zz);
						flagdammy = true;
					}
					// TODO:dammy
					flagdammy = false;

					if (vt == 0) {
						xx++;
					}
					else if (vt == 1) {
						zz++;
					}
					else if (vt == 2) {
						xx--;
					}
					else if (vt == 3) {
						zz--;
					}

				} while(++b < a);
			}
			vt = (vt + 1) & 3;
		}
		if (theMaid.getActiveModeClass().overlooksBlock(theMaid.maidMode)) {
			TileEntity ltile = theMaid.maidTileEntity;
			if (ltile != null) {
				lx = ltile.getPos().getX();
				ly = ltile.getPos().getY();
				lz = ltile.getPos().getZ();
				// TODO:Dummay
				EntityMarkerDummy.setDummyEntity(theMaid, 0x004fff4f, lx, ly, lz);
				flagdammy = true;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean continueExecuting() {
		if (theMaid.isActiveModeClass()) {
			theMaid.getActiveModeClass().updateBlock();
		}

		// 移動中は継続
		if (!theMaid.getNavigator().noPath()) return true;

		double ld = theMaid.getDistanceTilePos();

		// Too far or over tracking range
		if (ld > 100.0D || !MaidHelper.isTargetReachable(theMaid, new Vec3d(theMaid.getCurrentTilePos()), 0)) {
			// 索敵範囲外
			theMaid.getActiveModeClass().farrangeBlock();
			return false;
		} else if (ld > 5.0D) {
			// 射程距離外
			return theMaid.getActiveModeClass().outrangeBlock(theMaid.maidMode);
		} else {
			// 射程距離
			return theMaid.getActiveModeClass().executeBlock(theMaid.maidMode);
		}
	}

	@Override
	public void startExecuting() {
		theMaid.getActiveModeClass().startBlock(theMaid.maidMode);
	}

	@Override
	public void resetTask() {
		theMaid.getActiveModeClass().resetBlock(theMaid.maidMode);
	}

	@Override
	public void updateTask() {
		// ターゲットを見つけている
		theMaid.looksTilePos();
	}


	@Override
	public void setEnable(boolean pFlag) {
		isEnable = pFlag;
	}

	@Override
	public boolean getEnable() {
		return isEnable;
	}

}
