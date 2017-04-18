package com.hbm.gui.container;

import com.hbm.gui.SlotDiFurnace;
import com.hbm.tileentity.TileEntityMachinePuF6Tank;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerPuF6Tank extends Container {

	private TileEntityMachinePuF6Tank testNuke;
	private int fillState;
	
	public ContainerPuF6Tank(InventoryPlayer invPlayer, TileEntityMachinePuF6Tank tedf) {
		fillState = 0;
		
		testNuke = tedf;
		
		this.addSlotToContainer(new Slot(tedf, 0, 44, 17));
		this.addSlotToContainer(new SlotDiFurnace(invPlayer.player, tedf, 1, 44, 53));
		this.addSlotToContainer(new Slot(tedf, 2, 116, 17));
		this.addSlotToContainer(new SlotDiFurnace(invPlayer.player, tedf, 3, 116, 53));
		
		for(int i = 0; i < 3; i++)
		{
			for(int j = 0; j < 9; j++)
			{
				this.addSlotToContainer(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}
		
		for(int i = 0; i < 9; i++)
		{
			this.addSlotToContainer(new Slot(invPlayer, i, 8 + i * 18, 142));
		}
	}
	
	@Override
	public void addCraftingToCrafters(ICrafting crafting) {
		super.addCraftingToCrafters(crafting);
		crafting.sendProgressBarUpdate(this, 0, this.testNuke.fillState);
	}
	
	@Override
    public ItemStack transferStackInSlot(EntityPlayer p_82846_1_, int par2)
    {
		ItemStack var3 = null;
		Slot var4 = (Slot) this.inventorySlots.get(par2);
		
		if (var4 != null && var4.getHasStack())
		{
			ItemStack var5 = var4.getStack();
			var3 = var5.copy();
			
            if (par2 <= 3) {
				if (!this.mergeItemStack(var5, 4, this.inventorySlots.size(), true))
				{
					return null;
				}
			}
			else if (!this.mergeItemStack(var5, 0, 1, false))
			{
				if (!this.mergeItemStack(var5, 2, 3, false))
					return null;
			}
			
			if (var5.stackSize == 0)
			{
				var4.putStack((ItemStack) null);
			}
			else
			{
				var4.onSlotChanged();
			}
		}
		
		return var3;
    }

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return testNuke.isUseableByPlayer(player);
	}
	
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		
		for(int i = 0; i < this.crafters.size(); i++)
		{
			ICrafting par1 = (ICrafting)this.crafters.get(i);
			
			if(this.fillState != this.testNuke.fillState)
			{
				par1.sendProgressBarUpdate(this, 0, this.testNuke.fillState);
			}
		}
		
		this.fillState = this.testNuke.fillState;
	}
	
	@Override
	public void updateProgressBar(int i, int j) {
		if(i == 0)
		{
			testNuke.fillState = j;
		}
	}

}