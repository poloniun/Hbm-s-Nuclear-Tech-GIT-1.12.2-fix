package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.forgefluid.FFUtils;
import com.hbm.forgefluid.FluidTypeHandler;
import com.hbm.interfaces.ITankPacketAcceptor;
import com.hbm.packet.FluidTankPacket;
import com.hbm.packet.PacketDispatcher;
import com.hbm.tileentity.TileEntityMachineBase;

import net.minecraft.block.Block;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityBarrel extends TileEntityMachineBase implements ITickable, IFluidHandler, ITankPacketAcceptor {

	public FluidTank tank;
	//Drillgon200: I think this would be much easier to read as an enum.
	public short mode = 0;
	public static final short modes = 4;
	private int age = 0;
	
	public TileEntityBarrel() {
		super(4);
		tank = new FluidTank(-1);
	}
	
	public TileEntityBarrel(int cap) {
		super(4);
		tank = new FluidTank(cap);
	}

	@Override
	public void update() {
		
		if(!world.isRemote){
			FluidTank compareTank = FFUtils.copyTank(tank);
			FFUtils.fillFromFluidContainer(inventory, tank, 0, 1);
			FFUtils.fillFluidContainer(inventory, tank, 2, 3);

			age++;
			if(age >= 20)
				age = 0;
			
			if((mode == 1 || mode == 2) && (age == 9 || age == 19))
				fillFluidInit(tank);
			
			if(tank.getFluid() != null && tank.getFluidAmount() > 0) {
				
				Block b = this.getBlockType();
				
				if(b != ModBlocks.barrel_antimatter && FluidTypeHandler.isAntimatter(tank.getFluid().getFluid())) {
					world.destroyBlock(pos, false);
					world.newExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, true, true);
				}
				
				if(b == ModBlocks.barrel_plastic && (FluidTypeHandler.isCorrosive(tank.getFluid().getFluid()) || FluidTypeHandler.isHot(tank.getFluid().getFluid()))) {
					world.destroyBlock(pos, false);
					world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
				}
				
				//TODO: rip off furnace code and make transition more seamless
				if(b == ModBlocks.barrel_iron && FluidTypeHandler.isCorrosive(tank.getFluid().getFluid())) {
					world.setBlockState(pos, ModBlocks.barrel_corroded.getDefaultState());
					world.setTileEntity(pos, this);
					this.validate();
					/*TileEntityBarrel barrel = (TileEntityBarrel)world.getTileEntity(pos);
					
					if(barrel != null) {
						barrel.tank.setTankType(tank.getTankType());
						barrel.tank.setFill(Math.min(barrel.tank.getMaxFill(), tank.getFill()));
						barrel.slots = copy;
					}*/
					
					//Drillgon200: Heck if I know what random.fizz is.
					world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 1.0F);
				}
				
				if(b == ModBlocks.barrel_corroded && world.rand.nextInt(3) == 0) {
					tank.drain(1, true);
				}
			}
			
			PacketDispatcher.wrapper.sendToAllAround(new FluidTankPacket(pos, new FluidTank[]{tank}), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 100));
			if(!FFUtils.areTanksEqual(tank, compareTank))
				markDirty();
		}
	}
	
	public void fillFluidInit(FluidTank tank) {
		fillFluid(pos.east(), tank);
		fillFluid(pos.west(), tank);
		fillFluid(pos.up(), tank);
		fillFluid(pos.down(), tank);
		fillFluid(pos.south(), tank);
		fillFluid(pos.north(), tank);
	}

	public void fillFluid(BlockPos pos1, FluidTank tank) {
		FFUtils.fillFluid(this, tank, world, pos1, 1000);
	}
	
	@Override
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if(mode == 2 || mode == 3)
			return 0;
		return tank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		if(mode == 0 || mode == 3)
			return null;
		return tank.drain(resource, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		if(mode == 0 || mode == 3)
			return null;
		return tank.drain(maxDrain, doDrain);
	}

	@Override
	public String getName() {
		return "container.barrel";
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setShort("mode", mode);
		compound.setInteger("cap", tank.getCapacity());
		tank.writeToNBT(compound);
		return super.writeToNBT(compound);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		mode = compound.getShort("mode");
		if(tank == null || tank.getCapacity() <= 0)
			tank = new FluidTank(compound.getInteger("cap"));
		tank.readFromNBT(compound);
		super.readFromNBT(compound);
	}
	
	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY){
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		}
		return super.getCapability(capability, facing);
	}
	
	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
	}

	@Override
	public void recievePacket(NBTTagCompound[] tags) {
		if(tags.length == 1)
			tank.readFromNBT(tags[0]);
	}

}
