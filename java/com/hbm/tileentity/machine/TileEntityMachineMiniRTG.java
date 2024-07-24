package com.hbm.tileentity.machine;

import java.util.ArrayList;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.tileentity.TileEntityLoadedBase;

import api.hbm.energy.IEnergyGenerator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;

public class TileEntityMachineMiniRTG extends TileEntityLoadedBase implements ITickable, IEnergyGenerator {

	public long power;
	
	@Override
	public void update() {
		if(!world.isRemote) {
			this.sendPower(world, pos);
			if(this.getBlockType() == ModBlocks.machine_powerrtg)
				power += 50000;
			else
				power += 8000;

			if(power > getMaxPower())
				power = getMaxPower();
		}
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getMaxPower() {
		if(this.getBlockType() == ModBlocks.machine_powerrtg)
			return 2000000;

		return 400000;
	}
}
