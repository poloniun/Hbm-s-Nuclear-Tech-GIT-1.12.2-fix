package com.hbm.tileentity.bomb;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraft.world.World;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
public class TileEntityCrashedBomb extends TileEntity implements ITickable{
	public int live = 0;
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return TileEntity.INFINITE_EXTENT_AABB;
	}
	@Override
	public void update() {
		if(!world.isRemote) {
			float ra = world.rand.nextFloat();			
			if(ra< 0.12) {
				ItemStack out = ra > 0.1 ? new ItemStack(ModItems.powder_balefire): 
				ra> 0.06 ? new ItemStack(ModItems.particle_aschrab):new ItemStack(ModItems.particle_amat);

						MutableBlockPos mPos = new BlockPos.MutableBlockPos();
						int x = pos.getX();
						int y = pos.getY();
						int z = pos.getZ();						
						TileEntity te0 = world.getTileEntity(mPos.setPos(x,  y - 1, z));					
						if(te0 != null && te0.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)){
						IItemHandler chest = te0.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
						for(int k = 0; k < chest.getSlots(); k ++){
						if(chest.insertItem(k, out , false).isEmpty()){
							live++;
							break;	
						}
					}
				}
			}
				if(live >= 180) 
				world.setBlockState(pos, Blocks.AIR.getDefaultState());

	}
}
	
	@Override
	@SideOnly(Side.CLIENT)
	public double getMaxRenderDistanceSquared()
	{
		return 65536.0D;
	}
}
