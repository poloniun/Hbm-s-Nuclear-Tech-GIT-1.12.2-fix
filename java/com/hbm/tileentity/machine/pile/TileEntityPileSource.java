package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.blocks.machine.pile.BlockGraphiteDrilledBase;
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
public class TileEntityPileSource extends TileEntityPileBase {
	int live = 0;
	@Override
	public void update() {
		if(!world.isRemote) {
			float ra = world.rand.nextFloat();			
			int n = this.getBlockType() == ModBlocks.block_graphite_source ? 2 : 5;
			
			for(int i = 0; i < 16; i++) {
				this.castRay(n, 5);
			}

			if(ra< 0.00036 && n == 5) {
				ItemStack out = ra > 0.0003 ? new ItemStack(ModItems.nugget_bismuth):new ItemStack(ModItems.nugget_technetium);

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
						for(int k = 0; k < chest.getSlots(); k ++){
						if(chest.insertItem(k, out , false).isEmpty()){
							live++;	
							break here;
						}
						}
						}
						}
					}


		}
				if(live >= 18) {
				world.setBlockState(pos, ModBlocks.block_graphite_drilled.getDefaultState().withProperty(BlockGraphiteDrilledBase.AXIS, world.getBlockState(pos).getValue(BlockGraphiteDrilledBase.AXIS)), 3);
				here: 
				for (int i = -4 ; i <= 4 ; i++){
					for (int j = -4 ; j <= 4 ; j++){
						MutableBlockPos mPos = new BlockPos.MutableBlockPos();
						int x = pos.getX();
						int y = pos.getY();
						int z = pos.getZ();						
						TileEntity te0 = world.getTileEntity(mPos.setPos(x + i, y, z + j));					
						if(te0 != null && te0.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)){
						IItemHandler check = te0.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
						if(check instanceof IItemHandlerModifiable){
						IItemHandlerModifiable chest = (IItemHandlerModifiable)check;
						for(int k = 0; k < chest.getSlots(); k ++){
						if(!chest.getStackInSlot(k).isEmpty()) {
						if(chest.getStackInSlot(k).getItem()==ModItems.pile_rod_plutonium){
							chest.getStackInSlot(k).shrink(1);
							if(chest.getStackInSlot(k).isEmpty())
								chest.setStackInSlot(k, ItemStack.EMPTY);
							world.setBlockState(pos, ModBlocks.block_graphite_plutonium.getDefaultState().withProperty(BlockGraphiteDrilledBase.AXIS, world.getBlockState(pos).getValue(BlockGraphiteDrilledBase.AXIS)), 3);
							break here;
						}else if(k==chest.getSlots()-1 ) break here;
						}
						}}

				}}}


}
		}
	}
}
