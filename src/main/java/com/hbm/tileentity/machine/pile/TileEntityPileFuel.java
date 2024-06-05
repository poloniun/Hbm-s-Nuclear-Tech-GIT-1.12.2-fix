package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;

import api.hbm.block.IPileNeutronReceiver;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

import com.hbm.items.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraft.block.Block;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
public class TileEntityPileFuel extends TileEntityPileBase implements IPileNeutronReceiver {

	public int heat;
	public static final int maxHeat = 1000;
	public int neutrons;
	public int lastNeutrons;
	public int progress;
	public static final int maxProgress = 5000;

	@Override
	public void update() {
		
		if(!world.isRemote) {
			react();
			

			
			if(this.progress >= maxProgress) {
				world.setBlockState(pos, ModBlocks.block_graphite_plutonium.getDefaultState().withProperty(BlockGraphiteDrilledBase.AXIS, world.getBlockState(pos).getValue(BlockGraphiteDrilledBase.AXIS)), 3);
				float ra = world.rand.nextFloat();		
				here:
				for (int i = -4 ; i <= 4 ; i++){
					for (int j = -4 ; j <= 4 ; j++){
						MutableBlockPos mPos = new BlockPos.MutableBlockPos();
						int x = pos.getX();
						int y = pos.getY();
						int z = pos.getZ();						
						TileEntity te0 = world.getTileEntity(mPos.setPos(x + i, y, z + j));					
						if(te0 != null && te0.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)){
						IItemHandler chest = te0.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
						if(ra< 0.2) {
						ItemStack out = ra > 0.175 ? new ItemStack(ModItems.nugget_bismuth):new ItemStack(ModItems.nugget_technetium);
						for(int k = 0; k < chest.getSlots(); k ++){
							if(chest.insertItem(k, out , false).isEmpty()){
								break;
						}}}
						for(int k = 0; k < chest.getSlots(); k ++){
						if(chest.insertItem(k, new ItemStack(ModItems.pile_rod_pu239) , false).isEmpty()){					
							world.setBlockState(pos, ModBlocks.block_graphite_drilled.getDefaultState().withProperty(BlockGraphiteDrilledBase.AXIS, world.getBlockState(pos).getValue(BlockGraphiteDrilledBase.AXIS)), 3);
							break ;
						}
						}
						if(chest instanceof IItemHandlerModifiable){
						IItemHandlerModifiable chest1 = (IItemHandlerModifiable)chest;
						for(int k = 0; k < chest1.getSlots(); k ++){
						if(!chest1.getStackInSlot(k).isEmpty()) {
						if(chest1.getStackInSlot(k).getItem()==ModItems.pile_rod_uranium){
							chest1.getStackInSlot(k).shrink(1);
							if(chest1.getStackInSlot(k).isEmpty())
								chest1.setStackInSlot(k, ItemStack.EMPTY);
							world.setBlockState(pos, ModBlocks.block_graphite_fuel.getDefaultState().withProperty(BlockGraphiteDrilledBase.AXIS, world.getBlockState(pos).getValue(BlockGraphiteDrilledBase.AXIS)), 3);
							break here;
						}else if(k==chest1.getSlots()-1 ) break here;
						}
						}
						}
					}
			}}
			}
		}
	}

	private void react() {
		
		int reaction = (int) (this.neutrons * (1D - ((double)this.heat / (double)maxHeat) * 0.5D)); //max heat reduces reaction by 50% due to thermal expansion
		
		this.lastNeutrons = this.neutrons;
		this.neutrons = 0;;
		
		this.progress += reaction;
		
		if(reaction <= 0)
			return;
		
		
		for(int i = 0; i < 16; i++)
			this.castRay((int) Math.max(reaction * 0.25, 1), 5);
	}

	@Override
	public void receiveNeutrons(int n) {
		this.neutrons += n;
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.heat = nbt.getInteger("heat");
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("heat", this.heat);
		return nbt;
	}
}