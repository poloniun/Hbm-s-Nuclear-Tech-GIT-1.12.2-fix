package com.hbm.inventory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.Level;

import com.google.gson.Gson;
import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.forgefluid.ModForgeFluids;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.NbtComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemAssemblyTemplate;
import com.hbm.items.machine.ItemFluidTank;
import com.hbm.items.special.ItemCell;
import com.hbm.items.tool.ItemFluidCanister;
import com.hbm.main.MainRegistry;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

public class AssemblerRecipes {

	public static File config;
	public static File template;
	private static final Gson gson = new Gson();
	private static IForgeRegistry<Item> itemRegistry;
	private static IForgeRegistry<Block> blockRegistry;
	public static HashMap<ComparableStack, AStack[]> recipes = new HashMap<>();
	public static HashMap<ComparableStack, Integer> time = new HashMap<>();
	public static List<ComparableStack> recipeList = new ArrayList<>();
	public static HashSet<ComparableStack> hidden = new HashSet<>();
	
	//Backup client recipes
	public static HashMap<ComparableStack, AStack[]> backupRecipes;
	public static HashMap<ComparableStack, Integer> backupTime;
	public static List<ComparableStack> backupRecipeList;
	public static HashSet<ComparableStack> backupHidden = new HashSet<>();

	/**
	 * Pre-Init phase: Finds the recipe config (if exists) and checks if a
	 * template is present, if not it generates one.
	 * 
	 * @param dir
	 *            The suggested config folder
	 */
	public static void preInit(File dir) {

		if(dir == null || !dir.isDirectory())
			return;

		template = dir;

		List<File> files = Arrays.asList(dir.listFiles());
		for(File file : files) {
			if(file.getName().equals("hbmAssembler.json")) {
				config = file;
			}
		}
	}

	public static void loadRecipes() {
		registerDefaults();
		loadRecipesFromConfig();
		generateList();
	}

	public static ItemStack getOutputFromTempate(ItemStack stack) {

		if(stack != null && stack.getItem() instanceof ItemAssemblyTemplate) {

			int i = ItemAssemblyTemplate.getRecipeIndex(stack);
			if(i >= 0 && i < recipeList.size()) {
				return recipeList.get(i).toStack();
			}
		}

		return null;
	}

	public static List<AStack> getRecipeFromTempate(ItemStack stack) {

		if(stack != null && stack.getItem() instanceof ItemAssemblyTemplate) {

			int i = ItemAssemblyTemplate.getRecipeIndex(stack);

			if(i >= 0 && i < recipeList.size()) {
				ItemStack out = recipeList.get(i).toStack();

				if(out != null) {
					ComparableStack comp = new ComparableStack(out);
					AStack[] ret = recipes.get(comp);
					return Arrays.asList(ret);
				}
			}
		}

		return null;
	}

	/**
	 * Generates an ordered list of outputs, used by the template item to
	 * generate subitems
	 */
	private static void generateList() {

		List<ComparableStack> list = new ArrayList<>(recipes.keySet());
		Collections.sort(list);
		recipeList = list;
	}

	/**
	 * Registers regular recipes if there's no custom confiuration
	 */
	private static void registerDefaults() {
		makeRecipe(new ComparableStack(ModItems.plate_iron, 2), new AStack[] { new OreDictStack("ingotIron", 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_gold, 2), new AStack[] { new OreDictStack("ingotGold", 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_titanium, 2), new AStack[] { new OreDictStack("ingotTitanium", 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_aluminium, 2), new AStack[] { new OreDictStack("ingotAluminum", 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_steel, 2), new AStack[] { new OreDictStack("ingotSteel", 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_lead, 2), new AStack[] { new OreDictStack("ingotLead", 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_copper, 2), new AStack[] { new OreDictStack("ingotCopper", 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_advanced_alloy, 2), new AStack[] { new ComparableStack(ModItems.ingot_advanced_alloy, 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_schrabidium, 2), new AStack[] { new ComparableStack(ModItems.ingot_schrabidium, 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_combine_steel, 2), new AStack[] { new ComparableStack(ModItems.ingot_combine_steel, 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_saturnite, 2), new AStack[] { new ComparableStack(ModItems.ingot_saturnite, 3), }, 30);
		makeRecipe(new ComparableStack(ModItems.plate_mixed, 6), new AStack[] { new ComparableStack(ModItems.plate_advanced_alloy, 2), new OreDictStack(OreDictManager.getReflector(), 2), new ComparableStack(ModItems.plate_combine_steel, 1), new OreDictStack("plateLead", 4), }, 100);
		makeRecipe(new ComparableStack(ModItems.wire_aluminium, 6), new AStack[] { new OreDictStack("ingotAluminum", 1), }, 20);
		makeRecipe(new ComparableStack(ModItems.wire_copper, 6), new AStack[] { new OreDictStack("ingotCopper", 1), }, 20);
		makeRecipe(new ComparableStack(ModItems.wire_tungsten, 6), new AStack[] { new OreDictStack("ingotTungsten", 1), }, 20);
		makeRecipe(new ComparableStack(ModItems.wire_red_copper, 6), new AStack[] { new OreDictStack("ingotMingrade", 1), }, 20);
		makeRecipe(new ComparableStack(ModItems.wire_advanced_alloy, 6), new AStack[] { new ComparableStack(ModItems.ingot_advanced_alloy, 1), }, 20);
		makeRecipe(new ComparableStack(ModItems.wire_gold, 6), new AStack[] { new ComparableStack(Items.GOLD_INGOT, 1), }, 20);
		makeRecipe(new ComparableStack(ModItems.wire_schrabidium, 6), new AStack[] { new ComparableStack(ModItems.ingot_schrabidium, 1), }, 20);
		makeRecipe(new ComparableStack(ModItems.wire_magnetized_tungsten, 6), new AStack[] { new ComparableStack(ModItems.ingot_magnetized_tungsten, 1), }, 20);
		makeRecipe(new ComparableStack(ModItems.hazmat_cloth, 4), new AStack[] { new OreDictStack("dustLead", 4), new ComparableStack(Items.STRING, 8), }, 50);
		makeRecipe(new ComparableStack(ModItems.asbestos_cloth, 4), new AStack[] { new ComparableStack(ModItems.ingot_asbestos, 2), new ComparableStack(Items.STRING, 6), new ComparableStack(Blocks.WOOL, 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.filter_coal, 1), new AStack[] { new OreDictStack("dustCoal", 4), new ComparableStack(Items.STRING, 6), new ComparableStack(Items.PAPER, 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.centrifuge_element, 1), new AStack[] { new ComparableStack(ModItems.tank_steel, 2), new ComparableStack(ModItems.coil_tungsten, 2), new ComparableStack(ModItems.wire_red_copper, 6), new ComparableStack(ModItems.motor, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.centrifuge_tower, 1), new AStack[] { new ComparableStack(ModItems.centrifuge_element, 4), new OreDictStack("plateSteel", 4), new ComparableStack(ModItems.wire_red_copper, 6), new OreDictStack("dustLapis", 2), new ComparableStack(ModItems.ingot_polymer, 2), }, 150);
		//makeRecipe(new ComparableStack(ModItems.magnet_dee, 1), new AStack[] { new ComparableStack(ModBlocks.fusion_conductor, 6), new OreDictStack("ingotSteel", 3), new ComparableStack(ModItems.coil_advanced_torus, 1), }, 150);
		makeRecipe(new ComparableStack(ModItems.magnet_circular, 1), new AStack[] { new ComparableStack(ModBlocks.fusion_conductor, 5), new OreDictStack("ingotSteel", 4), new ComparableStack(ModItems.plate_advanced_alloy, 6), }, 300);
		//makeRecipe(new ComparableStack(ModItems.cyclotron_tower, 1), new AStack[] { new ComparableStack(ModItems.magnet_circular, 6), new ComparableStack(ModItems.magnet_dee, 3), new OreDictStack("plateSteel", 12), new ComparableStack(ModItems.wire_advanced_alloy, 8), new ComparableStack(ModItems.plate_polymer, 24), }, 100);
		makeRecipe(new ComparableStack(ModItems.reactor_core, 1), new AStack[] { new OreDictStack("ingotLead", 8), new OreDictStack("ingotBeryllium", 6), new OreDictStack("plateSteel", 16), new OreDictStack(OreDictManager.getReflector(), 8), new ComparableStack(ModItems.ingot_fiberglass, 2) }, 100);
		makeRecipe(new ComparableStack(ModItems.rtg_unit, 1), new AStack[] { new ComparableStack(ModItems.thermo_element, 3), new ComparableStack(ModItems.board_copper, 1), new OreDictStack("ingotLead", 2), new OreDictStack("plateSteel", 2), new ComparableStack(ModItems.circuit_copper, 1), }, 100);
		makeRecipe(new ComparableStack(ModItems.thermo_unit_empty, 1), new AStack[] { new ComparableStack(ModItems.coil_copper_torus, 3), new OreDictStack("ingotSteel", 3), new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.plate_polymer, 12), }, 100);
		makeRecipe(new ComparableStack(ModItems.levitation_unit, 1), new AStack[] { new ComparableStack(ModItems.coil_copper, 4), new ComparableStack(ModItems.coil_tungsten, 2), new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.nugget_schrabidium, 2), }, 100);
		makeRecipe(new ComparableStack(ModItems.drill_titanium, 1), new AStack[] { new OreDictStack("ingotSteel", 2), new ComparableStack(ModItems.ingot_dura_steel, 2), new ComparableStack(ModItems.bolt_dura_steel, 2), new OreDictStack("plateTitanium", 6), }, 100);
		makeRecipe(new ComparableStack(ModItems.telepad, 1), new AStack[] { new ComparableStack(ModItems.ingot_polymer, 12), new ComparableStack(ModItems.plate_schrabidium, 2), new ComparableStack(ModItems.plate_combine_steel, 4), new OreDictStack("plateSteel", 2), new ComparableStack(ModItems.wire_gold, 6), new ComparableStack(ModItems.circuit_schrabidium, 1), }, 300);
		makeRecipe(new ComparableStack(ModItems.entanglement_kit, 1), new AStack[] { new ComparableStack(ModItems.coil_magnetized_tungsten, 6), new OreDictStack("plateLead", 16), new OreDictStack(OreDictManager.getReflector(), 4), new ComparableStack(ModItems.singularity_counter_resonant, 1), new ComparableStack(ModItems.singularity_super_heated, 1), new ComparableStack(ModItems.powder_power, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.dysfunctional_reactor, 1), new AStack[] { new OreDictStack("plateSteel", 15), new OreDictStack("ingotLead", 5), new ComparableStack(ModItems.rod_quad_empty, 10), new OreDictStack("dyeBrown", 3), }, 200);
		//makeRecipe(new ComparableStack(ModItems.generator_front, 1), new AStack[] {new OreDictStack("ingotSteel", 3), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.tank_steel, 4), new ComparableStack(ModItems.turbine_titanium, 1), new ComparableStack(ModItems.wire_red_copper, 6), new ComparableStack(ModItems.wire_gold, 4), },200);
		makeRecipe(new ComparableStack(ModItems.missile_assembly, 1), new AStack[] { new ComparableStack(ModItems.hull_small_steel, 1), new ComparableStack(ModItems.hull_small_aluminium, 4), new OreDictStack("ingotSteel", 2), new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.wire_aluminium, 6), new NbtComparableStack(ItemFluidCanister.getFullCanister(ModForgeFluids.kerosene, 3)), new ComparableStack(ModItems.circuit_targeting_tier1, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.missile_carrier, 1), new AStack[] { new NbtComparableStack(ItemFluidTank.getFullBarrel(ModForgeFluids.kerosene, 16)), new ComparableStack(ModItems.thruster_medium, 4), new ComparableStack(ModItems.thruster_large, 1), new ComparableStack(ModItems.hull_big_titanium, 6), new ComparableStack(ModItems.hull_big_steel, 2), new ComparableStack(ModItems.hull_small_aluminium, 12), new OreDictStack("plateTitanium", 24), new ComparableStack(ModItems.plate_polymer, 128), new ComparableStack(ModBlocks.det_cord, 8), new ComparableStack(ModItems.circuit_targeting_tier3, 12), new ComparableStack(ModItems.circuit_targeting_tier4, 3), }, 4800);
		makeRecipe(new ComparableStack(ModItems.warhead_generic_small, 1), new AStack[] { new OreDictStack("plateTitanium", 5), new OreDictStack("plateSteel", 3), new ComparableStack(Blocks.TNT, 2), }, 100);
		makeRecipe(new ComparableStack(ModItems.warhead_generic_medium, 1), new AStack[] { new OreDictStack("plateTitanium", 8), new OreDictStack("plateSteel", 5), new ComparableStack(Blocks.TNT, 4), }, 150);
		makeRecipe(new ComparableStack(ModItems.warhead_generic_large, 1), new AStack[] { new OreDictStack("plateTitanium", 15), new OreDictStack("plateSteel", 8), new ComparableStack(Blocks.TNT, 8), }, 200);
		makeRecipe(new ComparableStack(ModItems.warhead_incendiary_small, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_small, 1), new ComparableStack(ModItems.powder_fire, 4), }, 100);
		makeRecipe(new ComparableStack(ModItems.warhead_incendiary_medium, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_medium, 1), new ComparableStack(ModItems.powder_fire, 8), }, 150);
		makeRecipe(new ComparableStack(ModItems.warhead_incendiary_large, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_large, 1), new ComparableStack(ModItems.powder_fire, 16), }, 200);
		makeRecipe(new ComparableStack(ModItems.warhead_cluster_small, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_small, 1), new ComparableStack(ModItems.pellet_cluster, 4), }, 100);
		makeRecipe(new ComparableStack(ModItems.warhead_cluster_medium, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_medium, 1), new ComparableStack(ModItems.pellet_cluster, 8), }, 150);
		makeRecipe(new ComparableStack(ModItems.warhead_cluster_large, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_large, 1), new ComparableStack(ModItems.pellet_cluster, 16), }, 200);
		makeRecipe(new ComparableStack(ModItems.warhead_buster_small, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_small, 1), new ComparableStack(ModBlocks.det_cord, 8), }, 100);
		makeRecipe(new ComparableStack(ModItems.warhead_buster_medium, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_medium, 1), new ComparableStack(ModBlocks.det_cord, 4), new ComparableStack(ModBlocks.det_charge, 4), }, 150);
		makeRecipe(new ComparableStack(ModItems.warhead_buster_large, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_large, 1), new ComparableStack(ModBlocks.det_charge, 8), }, 200);
		makeRecipe(new ComparableStack(ModItems.warhead_nuclear, 1), new AStack[] { new ComparableStack(ModItems.boy_shielding, 1), new ComparableStack(ModItems.boy_target, 1), new ComparableStack(ModItems.boy_bullet, 1), new OreDictStack("plateTitanium", 20), new OreDictStack("plateSteel", 12), }, 300);
		makeRecipe(new ComparableStack(ModItems.warhead_mirvlet, 1), new AStack[] { new OreDictStack("ingotSteel", 5), new OreDictStack("plateSteel", 18), new ComparableStack(ModItems.ingot_pu239, 1), new ComparableStack(Blocks.TNT, 2), }, 250);
		makeRecipe(new ComparableStack(ModItems.warhead_mirv, 1), new AStack[] { new OreDictStack("plateTitanium", 20), new OreDictStack("plateSteel", 12), new ComparableStack(ModItems.ingot_pu239, 1), new ComparableStack(Blocks.TNT, 8), new OreDictStack(OreDictManager.getReflector(), 6), new ComparableStack(ModItems.lithium, 4), new NbtComparableStack(ItemCell.getFullCell(ModForgeFluids.deuterium, 6)), }, 500);
		makeRecipe(new ComparableStack(ModItems.warhead_thermo_endo, 1), new AStack[] { new ComparableStack(ModBlocks.therm_endo, 2), new OreDictStack("plateTitanium", 12), new OreDictStack("plateSteel", 6), }, 300);
		makeRecipe(new ComparableStack(ModItems.warhead_thermo_exo, 1), new AStack[] { new ComparableStack(ModBlocks.therm_exo, 2), new OreDictStack("plateTitanium", 12), new OreDictStack("plateSteel", 6), }, 300);
		makeRecipe(new ComparableStack(ModItems.fuel_tank_small, 1), new AStack[] { new NbtComparableStack(ItemFluidCanister.getFullCanister(ModForgeFluids.kerosene, 4)), new OreDictStack("plateTitanium", 6), new OreDictStack("plateSteel", 2), }, 100);
		makeRecipe(new ComparableStack(ModItems.fuel_tank_medium, 1), new AStack[] { new ComparableStack(ModItems.fuel_tank_small, 3), new OreDictStack("plateTitanium", 4), new OreDictStack("plateSteel", 2), }, 150);
		makeRecipe(new ComparableStack(ModItems.fuel_tank_large, 1), new AStack[] { new ComparableStack(ModItems.fuel_tank_medium, 3), new OreDictStack("plateTitanium", 4), new OreDictStack("plateSteel", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.thruster_small, 1), new AStack[] { new OreDictStack("plateSteel", 2), new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.wire_aluminium, 4), }, 100);
		makeRecipe(new ComparableStack(ModItems.thruster_medium, 1), new AStack[] { new ComparableStack(ModItems.thruster_small, 1), new OreDictStack("plateSteel", 2), new ComparableStack(ModItems.hull_small_steel, 1), new ComparableStack(ModItems.hull_big_steel, 1), new ComparableStack(ModItems.wire_copper, 4), }, 150);
		makeRecipe(new ComparableStack(ModItems.thruster_large, 1), new AStack[] { new ComparableStack(ModItems.thruster_medium, 1), new OreDictStack("plateSteel", 4), new ComparableStack(ModItems.hull_big_steel, 2), new ComparableStack(ModItems.wire_red_copper, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.thruster_nuclear, 1), new AStack[] { new ComparableStack(ModItems.thruster_large, 1), new ComparableStack(ModItems.tank_steel, 2), new ComparableStack(ModItems.pipes_steel, 3), new ComparableStack(ModItems.board_copper, 6), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.circuit_targeting_tier4, 2), new ComparableStack(ModBlocks.machine_reactor_small, 1), }, 600);
		makeRecipe(new ComparableStack(ModItems.sat_base, 1), new AStack[] { new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.plate_desh, 4), new ComparableStack(ModItems.hull_big_titanium, 3), new NbtComparableStack(ItemFluidTank.getFullBarrel(ModForgeFluids.kerosene)), new ComparableStack(ModItems.photo_panel, 24), new ComparableStack(ModItems.board_copper, 12), new ComparableStack(ModItems.circuit_gold, 6), new ComparableStack(ModItems.battery_lithium_cell_6, 1), }, 500);
		makeRecipe(new ComparableStack(ModItems.sat_head_mapper, 1), new AStack[] { new OreDictStack("ingotSteel", 4), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.hull_small_steel, 3), new ComparableStack(ModItems.plate_desh, 2), new ComparableStack(ModItems.circuit_gold, 2), new ComparableStack(ModItems.plate_polymer, 12), new ComparableStack(Items.REDSTONE, 6), new ComparableStack(Items.DIAMOND, 1), new ComparableStack(Blocks.GLASS_PANE, 6), }, 400);
		makeRecipe(new ComparableStack(ModItems.sat_head_scanner, 1), new AStack[] { new OreDictStack("ingotSteel", 6), new OreDictStack("plateTitanium", 32), new ComparableStack(ModItems.plate_desh, 6), new ComparableStack(ModItems.magnetron, 6), new ComparableStack(ModItems.coil_advanced_torus, 2), new ComparableStack(ModItems.circuit_gold, 6), new ComparableStack(ModItems.plate_polymer, 6), new ComparableStack(Items.DIAMOND, 1), }, 400);
		makeRecipe(new ComparableStack(ModItems.sat_head_radar, 1), new AStack[] { new OreDictStack("ingotSteel", 4), new OreDictStack("plateTitanium", 32), new ComparableStack(ModItems.magnetron, 12), new ComparableStack(ModItems.plate_polymer, 16), new ComparableStack(ModItems.wire_red_copper, 16), new ComparableStack(ModItems.coil_gold, 3), new ComparableStack(ModItems.circuit_gold, 5), new ComparableStack(Items.DIAMOND, 1), }, 400);
		makeRecipe(new ComparableStack(ModItems.sat_head_laser, 1), new AStack[] { new OreDictStack("ingotSteel", 12), new OreDictStack("ingotTungsten", 16), new ComparableStack(ModItems.ingot_polymer, 6), new ComparableStack(ModItems.plate_polymer, 16), new ComparableStack(ModItems.board_copper, 24), new ComparableStack(ModItems.circuit_targeting_tier5, 2), new ComparableStack(Items.REDSTONE, 16), new ComparableStack(Items.DIAMOND, 5), new ComparableStack(Blocks.GLASS_PANE, 16), }, 450);
		makeRecipe(new ComparableStack(ModItems.sat_head_resonator, 1), new AStack[] { new OreDictStack("ingotSteel", 32), new ComparableStack(ModItems.ingot_polymer, 48), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModItems.crystal_xen, 1), new ComparableStack(ModItems.ingot_starmetal, 7), new ComparableStack(ModItems.circuit_targeting_tier5, 6), new ComparableStack(ModItems.circuit_targeting_tier6, 2), }, 1000);
		makeRecipe(new ComparableStack(ModItems.sat_foeq, 1), new AStack[] { new OreDictStack("plateSteel", 8), new OreDictStack("plateTitanium", 12), new ComparableStack(ModItems.plate_desh, 8), new ComparableStack(ModItems.hull_big_titanium, 3), new NbtComparableStack(ItemFluidTank.getFullBarrel(FluidRegistry.WATER)), new ComparableStack(ModItems.photo_panel, 16), new ComparableStack(ModItems.thruster_nuclear, 1), new ComparableStack(ModItems.rod_quad_uranium_fuel, 2), new ComparableStack(ModItems.circuit_targeting_tier5, 6), new ComparableStack(ModItems.magnetron, 3), new ComparableStack(ModItems.battery_lithium_cell_6, 1), }, 1200);
		makeRecipe(new ComparableStack(ModItems.sat_miner, 1), new AStack[] { new ComparableStack(ModItems.plate_saturnite, 24), new ComparableStack(ModItems.plate_desh, 8), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.drill_titanium, 2), new ComparableStack(ModItems.circuit_targeting_tier4, 2), new NbtComparableStack(ItemFluidTank.getFullBarrel(ModForgeFluids.kerosene)), new ComparableStack(ModItems.thruster_small, 1), new ComparableStack(ModItems.photo_panel, 12), new ComparableStack(ModItems.centrifuge_element, 4), new ComparableStack(ModItems.magnetron, 3), new ComparableStack(ModItems.plate_polymer, 12), new ComparableStack(ModItems.battery_lithium_cell_6, 1), }, 600);
		makeRecipe(new ComparableStack(ModItems.chopper_head, 1), new AStack[] { new ComparableStack(ModBlocks.reinforced_glass, 2), new ComparableStack(ModBlocks.fwatz_computer, 1), new ComparableStack(ModItems.ingot_combine_steel, 22), new ComparableStack(ModItems.wire_magnetized_tungsten, 4), }, 300);
		makeRecipe(new ComparableStack(ModItems.chopper_gun, 1), new AStack[] { new ComparableStack(ModItems.plate_combine_steel, 4), new ComparableStack(ModItems.ingot_combine_steel, 2), new ComparableStack(ModItems.wire_tungsten, 6), new ComparableStack(ModItems.coil_magnetized_tungsten, 1), new ComparableStack(ModItems.motor, 1), }, 150);
		makeRecipe(new ComparableStack(ModItems.chopper_torso, 1), new AStack[] { new ComparableStack(ModItems.ingot_combine_steel, 26), new ComparableStack(ModBlocks.fwatz_computer, 1), new ComparableStack(ModItems.wire_magnetized_tungsten, 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.chopper_blades, 2), }, 350);
		makeRecipe(new ComparableStack(ModItems.chopper_tail, 1), new AStack[] { new ComparableStack(ModItems.plate_combine_steel, 8), new ComparableStack(ModItems.ingot_combine_steel, 5), new ComparableStack(ModItems.wire_magnetized_tungsten, 4), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.chopper_blades, 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.chopper_wing, 1), new AStack[] { new ComparableStack(ModItems.plate_combine_steel, 6), new ComparableStack(ModItems.ingot_combine_steel, 3), new ComparableStack(ModItems.wire_magnetized_tungsten, 2), }, 150);
		makeRecipe(new ComparableStack(ModItems.chopper_blades, 1), new AStack[] { new ComparableStack(ModItems.plate_combine_steel, 8), new OreDictStack("plateSteel", 2), new ComparableStack(ModItems.ingot_combine_steel, 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.circuit_aluminium, 1), new AStack[] { new ComparableStack(ModItems.circuit_raw, 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.circuit_copper, 1), new AStack[] {new ComparableStack(ModItems.circuit_aluminium, 1), new ComparableStack(ModItems.wire_copper, 4), new OreDictStack("dustNetherQuartz", 1), new OreDictStack("plateCopper", 1), },100);
		makeRecipe(new ComparableStack(ModItems.circuit_red_copper, 1), new AStack[] {new ComparableStack(ModItems.circuit_copper, 1), new ComparableStack(ModItems.wire_red_copper, 4), new OreDictStack("dustGold", 1), new ComparableStack(ModItems.plate_polymer, 1), },150);
		makeRecipe(new ComparableStack(ModItems.pellet_rtg, 1), new AStack[] { new ComparableStack(ModItems.nugget_pu238, 5), new OreDictStack("plateIron", 2), }, 50);
		makeRecipe(new ComparableStack(ModItems.pellet_rtg_weak, 1), new AStack[] { new ComparableStack(ModItems.nugget_u238, 4), new ComparableStack(ModItems.nugget_pu238, 1), new OreDictStack("plateIron", 2), }, 50);
		makeRecipe(new ComparableStack(ModItems.tritium_deuterium_cake, 1), new AStack[] { new NbtComparableStack(ItemCell.getFullCell(ModForgeFluids.deuterium, 6)), new NbtComparableStack(ItemCell.getFullCell(ModForgeFluids.tritium, 2)), new ComparableStack(ModItems.lithium, 4), }, 150);
		makeRecipe(new ComparableStack(ModItems.pellet_cluster, 1), new AStack[] { new OreDictStack("plateSteel", 4), new ComparableStack(Blocks.TNT, 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.pellet_buckshot, 1), new AStack[] { new OreDictStack("nuggetLead", 6), }, 50);
		makeRecipe(new ComparableStack(ModItems.australium_iii, 1), new AStack[] { new ComparableStack(ModItems.rod_australium, 1), new OreDictStack("ingotSteel", 1), new OreDictStack("plateSteel", 6), new OreDictStack("plateCopper", 2), new ComparableStack(ModItems.wire_copper, 6), }, 150);
		makeRecipe(new ComparableStack(ModItems.magnetron, 1), new AStack[] { new ComparableStack(ModItems.ingot_advanced_alloy, 1), new ComparableStack(ModItems.plate_advanced_alloy, 2), new ComparableStack(ModItems.wire_tungsten, 1), new ComparableStack(ModItems.coil_tungsten, 1), }, 100);
		makeRecipe(new ComparableStack(ModItems.pellet_schrabidium, 1), new AStack[] { new ComparableStack(ModItems.ingot_schrabidium, 5), new OreDictStack("plateIron", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.pellet_hes, 1), new AStack[] { new ComparableStack(ModItems.ingot_hes, 5), new OreDictStack("plateIron", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.pellet_mes, 1), new AStack[] { new ComparableStack(ModItems.ingot_schrabidium_fuel, 5), new OreDictStack("plateIron", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.pellet_les, 1), new AStack[] { new ComparableStack(ModItems.ingot_les, 5), new OreDictStack("plateIron", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.pellet_beryllium, 1), new AStack[] { new ComparableStack(ModItems.ingot_beryllium, 5), new OreDictStack("plateIron", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.pellet_neptunium, 1), new AStack[] { new ComparableStack(ModItems.ingot_neptunium, 5), new OreDictStack("plateIron", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.pellet_lead, 1), new AStack[] { new OreDictStack("ingotLead", 5), new OreDictStack("plateIron", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.pellet_advanced, 1), new AStack[] { new OreDictStack("ingotDesh", 5), new OreDictStack("plateIron", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.upgrade_template, 1), new AStack[] { new OreDictStack("plateSteel", 1), new OreDictStack("plateIron", 4), new OreDictStack("plateCopper", 2), new ComparableStack(ModItems.wire_copper, 6), }, 100);
		makeRecipe(new ComparableStack(ModItems.upgrade_speed_1, 1), new AStack[] { new ComparableStack(ModItems.upgrade_template, 1), new OreDictStack("dustMingrade", 4), new ComparableStack(Items.REDSTONE, 6), new ComparableStack(ModItems.wire_red_copper, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.upgrade_speed_2, 1), new AStack[] { new ComparableStack(ModItems.upgrade_speed_1, 1), new OreDictStack("dustMingrade", 2), new ComparableStack(Items.REDSTONE, 4), new ComparableStack(ModItems.circuit_red_copper, 4), new ComparableStack(ModItems.wire_red_copper, 4), new ComparableStack(ModItems.ingot_polymer, 2), }, 300);
		makeRecipe(new ComparableStack(ModItems.upgrade_speed_3, 1), new AStack[] { new ComparableStack(ModItems.upgrade_speed_2, 1), new OreDictStack("dustMingrade", 2), new ComparableStack(Items.REDSTONE, 6), new OreDictStack("ingotDesh", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.upgrade_effect_1, 1), new AStack[] { new ComparableStack(ModItems.upgrade_template, 1), new ComparableStack(ModItems.powder_dura_steel, 4), new ComparableStack(ModItems.powder_steel, 6), new ComparableStack(ModItems.wire_red_copper, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.upgrade_effect_2, 1), new AStack[] { new ComparableStack(ModItems.upgrade_effect_1, 1), new ComparableStack(ModItems.powder_dura_steel, 2), new ComparableStack(ModItems.powder_steel, 4), new ComparableStack(ModItems.circuit_red_copper, 4), new ComparableStack(ModItems.wire_red_copper, 4), new ComparableStack(ModItems.ingot_polymer, 2), }, 300);
		makeRecipe(new ComparableStack(ModItems.upgrade_effect_3, 1), new AStack[] { new ComparableStack(ModItems.upgrade_effect_2, 1), new ComparableStack(ModItems.powder_dura_steel, 2), new ComparableStack(ModItems.powder_steel, 6), new OreDictStack("ingotDesh", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.upgrade_power_1, 1), new AStack[] { new ComparableStack(ModItems.upgrade_template, 1), new OreDictStack("dustLapis", 4), new ComparableStack(Items.GLOWSTONE_DUST, 6), new ComparableStack(ModItems.wire_red_copper, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.upgrade_power_2, 1), new AStack[] { new ComparableStack(ModItems.upgrade_power_1, 1), new OreDictStack("dustLapis", 2), new ComparableStack(Items.GLOWSTONE_DUST, 4), new ComparableStack(ModItems.circuit_red_copper, 4), new ComparableStack(ModItems.wire_red_copper, 4), new ComparableStack(ModItems.ingot_polymer, 2), }, 300);
		makeRecipe(new ComparableStack(ModItems.upgrade_power_3, 1), new AStack[] { new ComparableStack(ModItems.upgrade_power_2, 1), new OreDictStack("dustLapis", 2), new ComparableStack(Items.GLOWSTONE_DUST, 6), new OreDictStack("ingotDesh", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.upgrade_fortune_1, 1), new AStack[] { new ComparableStack(ModItems.upgrade_template, 1), new OreDictStack("dustDiamond", 4), new ComparableStack(ModItems.powder_iron, 6), new ComparableStack(ModItems.wire_red_copper, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.upgrade_fortune_2, 1), new AStack[] { new ComparableStack(ModItems.upgrade_fortune_1, 1), new OreDictStack("dustDiamond", 2), new ComparableStack(ModItems.powder_iron, 4), new ComparableStack(ModItems.circuit_red_copper, 4), new ComparableStack(ModItems.wire_red_copper, 4), new ComparableStack(ModItems.ingot_polymer, 2), }, 300);
		makeRecipe(new ComparableStack(ModItems.upgrade_fortune_3, 1), new AStack[] { new ComparableStack(ModItems.upgrade_fortune_2, 1), new OreDictStack("dustDiamond", 2), new ComparableStack(ModItems.powder_iron, 6), new OreDictStack("ingotDesh", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.upgrade_afterburn_1, 1), new AStack[] { new ComparableStack(ModItems.upgrade_template, 1), new ComparableStack(ModItems.powder_polymer, 4), new ComparableStack(ModItems.powder_tungsten, 6), new ComparableStack(ModItems.wire_red_copper, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.upgrade_afterburn_2, 1), new AStack[] { new ComparableStack(ModItems.upgrade_afterburn_1, 1), new ComparableStack(ModItems.powder_polymer, 2), new ComparableStack(ModItems.powder_tungsten, 4), new ComparableStack(ModItems.circuit_red_copper, 4), new ComparableStack(ModItems.wire_red_copper, 4), new ComparableStack(ModItems.ingot_polymer, 2), }, 300);
		makeRecipe(new ComparableStack(ModItems.upgrade_afterburn_3, 1), new AStack[] { new ComparableStack(ModItems.upgrade_afterburn_2, 1), new ComparableStack(ModItems.powder_polymer, 2), new ComparableStack(ModItems.powder_tungsten, 6), new OreDictStack("ingotDesh", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.upgrade_radius, 1), new AStack[] { new ComparableStack(ModItems.upgrade_template, 1), new ComparableStack(Items.GLOWSTONE_DUST, 6), new OreDictStack("dustDiamond", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.upgrade_health, 1), new AStack[] { new ComparableStack(ModItems.upgrade_template, 1), new ComparableStack(Items.GLOWSTONE_DUST, 6), new ComparableStack(ModItems.powder_titanium, 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.upgrade_overdrive_1, 1), new AStack[] { new ComparableStack(ModItems.upgrade_speed_3, 4), new ComparableStack(ModItems.upgrade_effect_3, 2), new OreDictStack("ingotDesh", 8), new ComparableStack(ModItems.powder_power, 16), new ComparableStack(ModItems.crystal_lithium, 4), new ComparableStack(ModItems.circuit_schrabidium, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.upgrade_overdrive_2, 1), new AStack[] { new ComparableStack(ModItems.upgrade_overdrive_1, 1), new ComparableStack(ModItems.upgrade_afterburn_1, 1), new ComparableStack(ModItems.upgrade_speed_3, 2), new ComparableStack(ModItems.upgrade_effect_3, 2), new ComparableStack(ModItems.ingot_saturnite, 12), new ComparableStack(ModItems.powder_nitan_mix, 16), new ComparableStack(ModItems.crystal_starmetal, 6), new ComparableStack(ModItems.circuit_schrabidium, 6), }, 300);
		makeRecipe(new ComparableStack(ModItems.upgrade_overdrive_3, 1), new AStack[] { new ComparableStack(ModItems.upgrade_overdrive_2, 1), new ComparableStack(ModItems.upgrade_afterburn_1, 1), new ComparableStack(ModItems.upgrade_speed_3, 2), new ComparableStack(ModItems.upgrade_effect_3, 2), new OreDictStack("ingotDesh", 8), new ComparableStack(ModItems.powder_power, 16), new ComparableStack(ModItems.crystal_lithium, 4), new ComparableStack(ModItems.circuit_schrabidium, 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.fuse, 1), new AStack[] { new OreDictStack("plateSteel", 2), new ComparableStack(Blocks.GLASS_PANE, 1), new ComparableStack(ModItems.wire_aluminium, 1), }, 100);
		makeRecipe(new ComparableStack(ModItems.redcoil_capacitor, 1), new AStack[] { new OreDictStack("plateGold", 3), new ComparableStack(ModItems.fuse, 1), new ComparableStack(ModItems.wire_advanced_alloy, 4), new ComparableStack(ModItems.coil_advanced_alloy, 6), new ComparableStack(Blocks.REDSTONE_BLOCK, 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.titanium_filter, 1), new AStack[] { new OreDictStack("plateLead", 3), new ComparableStack(ModItems.fuse, 1), new ComparableStack(ModItems.wire_tungsten, 4), new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.ingot_u238, 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.part_lithium, 1), new AStack[] { new ComparableStack(ModItems.plate_polymer, 1), new OreDictStack("dustLithium", 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.part_beryllium, 1), new AStack[] { new ComparableStack(ModItems.plate_polymer, 1), new ComparableStack(ModItems.powder_beryllium, 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.part_carbon, 1), new AStack[] { new ComparableStack(ModItems.plate_polymer, 1), new OreDictStack("dustCoal", 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.part_copper, 1), new AStack[] { new ComparableStack(ModItems.plate_polymer, 1), new OreDictStack("dustCopper", 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.part_plutonium, 1), new AStack[] { new ComparableStack(ModItems.plate_polymer, 1), new ComparableStack(ModItems.powder_plutonium, 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.thermo_element, 1), new AStack[] { new OreDictStack("plateSteel", 3), new OreDictStack("plateIron", 1), new OreDictStack("plateCopper", 2), new ComparableStack(ModItems.wire_red_copper, 2), new ComparableStack(ModItems.wire_aluminium, 2), new OreDictStack("dustNetherQuartz", 4), }, 150);
		//makeRecipe(new ComparableStack(ModItems.limiter, 1), new AStack[] {new OreDictStack("plateSteel", 3), new OreDictStack("plateIron", 1), new ComparableStack(ModItems.circuit_copper, 2), new ComparableStack(ModItems.wire_copper, 4), },150);
		makeRecipe(new ComparableStack(ModItems.plate_dalekanium, 1), new AStack[] { new ComparableStack(ModBlocks.block_meteor, 1), }, 50);
		makeRecipe(new ComparableStack(ModBlocks.block_meteor, 1), new AStack[] { new ComparableStack(ModItems.fragment_meteorite, 100), }, 500);
		makeRecipe(new ComparableStack(ModBlocks.cmb_brick, 8), new AStack[] { new ComparableStack(ModItems.ingot_combine_steel, 1), new ComparableStack(ModItems.plate_combine_steel, 8), }, 100);
		makeRecipe(new ComparableStack(ModBlocks.cmb_brick_reinforced, 8), new AStack[] { new ComparableStack(ModBlocks.block_magnetized_tungsten, 4), new ComparableStack(ModBlocks.brick_concrete, 4), new ComparableStack(ModBlocks.cmb_brick, 1), new OreDictStack("plateSteel", 4), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.seal_frame, 1), new AStack[] { new OreDictStack("ingotSteel", 3), new ComparableStack(ModItems.wire_aluminium, 4), new ComparableStack(Items.REDSTONE, 2), new ComparableStack(ModBlocks.steel_roof, 5), }, 50);
		makeRecipe(new ComparableStack(ModBlocks.seal_controller, 1), new AStack[] { new OreDictStack("ingotSteel", 3), new ComparableStack(ModItems.ingot_polymer, 4), new OreDictStack("ingotMingrade", 1), new ComparableStack(Items.REDSTONE, 4), new ComparableStack(ModBlocks.steel_roof, 5), }, 100);
		makeRecipe(new ComparableStack(ModBlocks.vault_door, 1), new AStack[] { new OreDictStack("ingotSteel", 128), new OreDictStack("ingotTungsten", 32), new OreDictStack("plateLead", 48), new ComparableStack(ModItems.plate_advanced_alloy, 8), new ComparableStack(ModItems.plate_polymer, 16), new ComparableStack(ModItems.bolt_tungsten, 18), new ComparableStack(ModItems.bolt_dura_steel, 27), new ComparableStack(ModItems.motor, 5), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.blast_door, 1), new AStack[] { new OreDictStack("ingotSteel", 16), new OreDictStack("ingotTungsten", 8), new OreDictStack("plateLead", 12), new ComparableStack(ModItems.plate_advanced_alloy, 3), new ComparableStack(ModItems.plate_polymer, 3), new ComparableStack(ModItems.bolt_tungsten, 3), new ComparableStack(ModItems.bolt_dura_steel, 3), new ComparableStack(ModItems.motor, 1), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.sliding_blast_door_2, 1), new AStack[] { new OreDictStack("ingotSteel", 16), new OreDictStack("ingotTungsten", 8), new ComparableStack(ModItems.circuit_gold, 3), new ComparableStack(Blocks.QUARTZ_BLOCK, 10), new ComparableStack(ModItems.plate_polymer, 3), new ComparableStack(ModItems.bolt_tungsten, 3), new ComparableStack(ModItems.bolt_dura_steel, 3), new ComparableStack(ModItems.motor, 2), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.sliding_blast_door, 1), new AStack[] { new OreDictStack("ingotSteel", 16), new OreDictStack("ingotTungsten", 8), new ComparableStack(ModBlocks.reinforced_glass, 4), new ComparableStack(Blocks.QUARTZ_BLOCK, 10), new ComparableStack(ModItems.plate_polymer, 3), new ComparableStack(ModItems.bolt_tungsten, 3), new ComparableStack(ModItems.bolt_dura_steel, 3), new ComparableStack(ModItems.motor, 2), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.machine_centrifuge, 1), new AStack[] { new ComparableStack(ModItems.centrifuge_tower, 1), new OreDictStack("ingotSteel", 4), new OreDictStack("ingotIron", 4), new OreDictStack("plateSteel", 2), new OreDictStack("plateCopper", 2), new ComparableStack(ModItems.wire_red_copper, 8), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.machine_gascent, 1), new AStack[] { new ComparableStack(ModItems.centrifuge_tower, 1), new OreDictStack("ingotSteel", 4), new ComparableStack(ModItems.ingot_polymer, 4), new OreDictStack("ingotDesh", 2), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.plate_advanced_alloy, 4), new ComparableStack(ModItems.wire_red_copper, 8), new ComparableStack(ModItems.wire_gold, 4), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.machine_reactor, 1), new AStack[] { new ComparableStack(ModItems.reactor_core, 1), new OreDictStack("ingotSteel", 12), new OreDictStack("plateLead", 16), new ComparableStack(ModBlocks.reinforced_glass, 4), new ComparableStack(ModItems.ingot_asbestos, 4) }, 150);
		makeRecipe(new ComparableStack(ModBlocks.machine_rtg_furnace_off, 1), new AStack[] { new ComparableStack(Blocks.FURNACE, 1), new ComparableStack(ModItems.rtg_unit, 3), new OreDictStack("plateLead", 6), new OreDictStack(OreDictManager.getReflector(), 4), new OreDictStack("plateCopper", 2), }, 150);
		makeRecipe(new ComparableStack(ModBlocks.machine_radgen, 1), new AStack[] { new OreDictStack("ingotSteel", 8), new OreDictStack("plateSteel", 32), new ComparableStack(ModItems.coil_magnetized_tungsten, 6), new ComparableStack(ModItems.wire_magnetized_tungsten, 24), new ComparableStack(ModItems.circuit_gold, 4), new ComparableStack(ModItems.reactor_core, 3), new ComparableStack(ModItems.ingot_starmetal, 1), new OreDictStack("dyeRed", 1), }, 400);
		makeRecipe(new ComparableStack(ModBlocks.machine_diesel, 1), new AStack[] { new ComparableStack(ModItems.hull_small_steel, 4), new ComparableStack(Blocks.PISTON, 4), new OreDictStack("ingotSteel", 6), new OreDictStack("ingotMingrade", 2), new OreDictStack("plateCopper", 4), new ComparableStack(ModItems.wire_red_copper, 6), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.machine_selenium, 1), new AStack[] { new OreDictStack("ingotSteel", 4), new OreDictStack("plateTitanium", 6), new OreDictStack("plateCopper", 8), new ComparableStack(ModItems.hull_big_steel, 1), new ComparableStack(ModItems.hull_small_steel, 9), new ComparableStack(ModItems.pedestal_steel, 1), new ComparableStack(ModItems.coil_copper, 4), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.machine_reactor_small, 1), new AStack[] { new OreDictStack("ingotSteel", 6), new ComparableStack(ModItems.ingot_polymer, 4), new OreDictStack("plateLead", 8), new OreDictStack("plateCopper", 4), new OreDictStack("ingotLead", 12), new OreDictStack("ingotMingrade", 6), new ComparableStack(ModItems.circuit_copper, 8), new ComparableStack(ModItems.circuit_red_copper, 4), }, 300);
		//makeRecipe(new ComparableStack(ModBlocks.machine_industrial_generator, 1), new AStack[] {new ComparableStack(ModItems.generator_front, 1), new ComparableStack(ModItems.generator_steel, 3), new ComparableStack(ModItems.rotor_steel, 3), new OreDictStack("ingotSteel", 6), new ComparableStack(ModItems.board_copper, 4), new ComparableStack(ModItems.wire_gold, 8), new ComparableStack(ModBlocks.red_wire_coated, 2), new ComparableStack(ModItems.pedestal_steel, 2), new ComparableStack(ModItems.circuit_copper, 4), },500);
		makeRecipe(new ComparableStack(ModBlocks.machine_rtg_grey, 1), new AStack[] { new ComparableStack(ModItems.rtg_unit, 5), new OreDictStack("plateSteel", 8), new ComparableStack(ModItems.wire_red_copper, 4), new ComparableStack(ModItems.ingot_polymer, 6), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.machine_battery, 1), new AStack[] { new OreDictStack("ingotSteel", 4), new OreDictStack("dustSulfur", 12), new OreDictStack("dustLead", 12), new OreDictStack("ingotMingrade", 2), new ComparableStack(ModItems.wire_red_copper, 4), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.machine_lithium_battery, 1), new AStack[] { new ComparableStack(ModItems.ingot_polymer, 4), new OreDictStack("dustCobalt", 12), new OreDictStack("dustLithium", 12), new ComparableStack(ModItems.ingot_advanced_alloy, 2), new ComparableStack(ModItems.wire_red_copper, 4), }, 400);
		makeRecipe(new ComparableStack(ModBlocks.machine_schrabidium_battery, 1), new AStack[] { new OreDictStack("ingotDesh", 4), new ComparableStack(ModItems.powder_neptunium, 12), new ComparableStack(ModItems.powder_schrabidium, 12), new ComparableStack(ModItems.ingot_schrabidium, 2), new ComparableStack(ModItems.wire_schrabidium, 4), }, 800);
		makeRecipe(new ComparableStack(ModBlocks.machine_dineutronium_battery, 1), new AStack[] { new ComparableStack(ModItems.ingot_dineutronium, 24), new ComparableStack(ModItems.powder_spark_mix, 12), new ComparableStack(ModItems.battery_spark_cell_1000, 1), new ComparableStack(ModItems.ingot_combine_steel, 32), new ComparableStack(ModItems.coil_magnetized_tungsten, 8), }, 1600);
		makeRecipe(new ComparableStack(ModBlocks.machine_shredder, 1), new AStack[] { new OreDictStack("ingotSteel", 2), new OreDictStack("plateSteel", 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.wire_red_copper, 2), new ComparableStack(ModBlocks.steel_beam, 2), new ComparableStack(Blocks.IRON_BARS, 2), new ComparableStack(ModBlocks.red_wire_coated, 1), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.machine_well, 1), new AStack[] { new ComparableStack(ModBlocks.steel_scaffold, 20), new ComparableStack(ModBlocks.steel_beam, 8), new ComparableStack(ModItems.tank_steel, 2), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.pipes_steel, 3), new ComparableStack(ModItems.drill_titanium, 1), new ComparableStack(ModItems.wire_red_copper, 6), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.machine_pumpjack, 1), new AStack[] { new ComparableStack(ModBlocks.steel_scaffold, 8), new OreDictStack("blockSteel", 8), new ComparableStack(ModItems.pipes_steel, 4), new ComparableStack(ModItems.tank_steel, 4), new OreDictStack("ingotSteel", 24), new OreDictStack("plateSteel", 16), new OreDictStack("plateAluminum", 6), new ComparableStack(ModItems.drill_titanium, 1), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.wire_red_copper, 8), }, 400);
		makeRecipe(new ComparableStack(ModBlocks.machine_flare, 1), new AStack[] { new ComparableStack(ModBlocks.steel_scaffold, 28), new ComparableStack(ModItems.tank_steel, 2), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModItems.hull_small_steel, 1), new ComparableStack(ModItems.thermo_element, 3), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.machine_refinery, 1), new AStack[] {new OreDictStack("ingotSteel", 16), new OreDictStack("plateSteel", 20), new OreDictStack("plateCopper", 16), new ComparableStack(ModItems.hull_big_steel, 6), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModItems.coil_tungsten, 8), new ComparableStack(ModItems.wire_red_copper, 8), new ComparableStack(ModItems.circuit_copper, 2), new ComparableStack(ModItems.circuit_red_copper, 1), new ComparableStack(ModItems.plate_polymer, 8), },350);
		makeRecipe(new ComparableStack(ModBlocks.machine_epress, 1), new AStack[] { new OreDictStack("plateSteel", 8), new ComparableStack(ModItems.plate_polymer, 4), new ComparableStack(ModItems.pipes_steel, 1), new ComparableStack(ModItems.bolt_tungsten, 4), new ComparableStack(ModItems.coil_copper, 2), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.circuit_copper, 1), new NbtComparableStack(ItemFluidCanister.getFullCanister(ModForgeFluids.lubricant)), }, 160);
		makeRecipe(new ComparableStack(ModBlocks.machine_chemplant, 1), new AStack[] { new OreDictStack("ingotSteel", 8), new OreDictStack("plateCopper", 6), new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.tank_steel, 4), new ComparableStack(ModItems.hull_big_steel, 1), new ComparableStack(ModItems.wire_red_copper, 16), new ComparableStack(ModItems.wire_tungsten, 3), new ComparableStack(ModItems.circuit_copper, 4), new ComparableStack(ModItems.circuit_red_copper, 2), new ComparableStack(ModItems.plate_polymer, 8), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.machine_crystallizer, 1), new AStack[] { new ComparableStack(ModItems.hull_big_steel, 4), new ComparableStack(ModItems.pipes_steel, 4), new OreDictStack("ingotDesh", 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.blades_advanced_alloy, 2), new OreDictStack("ingotSteel", 16), new OreDictStack("plateTitanium", 16), new ComparableStack(Blocks.GLASS, 4), new ComparableStack(ModItems.circuit_gold, 1), }, 400);
		makeRecipe(new ComparableStack(ModBlocks.machine_fluidtank, 1), new AStack[] { new OreDictStack("ingotSteel", 2), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.hull_big_steel, 4), }, 150);
		makeRecipe(new ComparableStack(ModBlocks.machine_drill, 1), new AStack[] { new ComparableStack(ModBlocks.steel_scaffold, 6), new OreDictStack("ingotSteel", 4), new ComparableStack(ModItems.wire_red_copper, 4), new ComparableStack(ModItems.circuit_copper, 1), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.ingot_dura_steel, 2), new ComparableStack(ModItems.bolt_dura_steel, 2), new ComparableStack(ModItems.drill_titanium, 1), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.machine_mining_laser, 1), new AStack[] { new ComparableStack(ModItems.tank_steel, 3), new OreDictStack("ingotSteel", 8), new OreDictStack("plateSteel", 12), new ComparableStack(ModItems.crystal_redstone, 3), new ComparableStack(Items.DIAMOND, 5), new ComparableStack(ModItems.ingot_polymer, 8), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.ingot_dura_steel, 4), new ComparableStack(ModItems.bolt_dura_steel, 6), new ComparableStack(ModBlocks.machine_lithium_battery, 1), }, 400);
		makeRecipe(new ComparableStack(ModBlocks.machine_turbofan, 1), new AStack[] { new ComparableStack(ModItems.hull_big_steel, 1), new ComparableStack(ModItems.hull_big_titanium, 3), new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.turbine_tungsten, 1), new ComparableStack(ModItems.turbine_titanium, 7), new ComparableStack(ModItems.bolt_compound, 8), new OreDictStack("ingotMingrade", 12), new ComparableStack(ModItems.wire_red_copper, 24), }, 500);
		makeRecipe(new ComparableStack(ModBlocks.machine_teleporter, 1), new AStack[] { new OreDictStack("ingotTitanium", 6), new ComparableStack(ModItems.plate_advanced_alloy, 12), new ComparableStack(ModItems.plate_combine_steel, 4), new ComparableStack(ModItems.telepad, 1), new ComparableStack(ModItems.entanglement_kit, 1), new ComparableStack(ModBlocks.machine_battery, 2), new ComparableStack(ModItems.coil_magnetized_tungsten, 4), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.machine_schrabidium_transmutator, 1), new AStack[] { new ComparableStack(ModItems.ingot_magnetized_tungsten, 1), new OreDictStack("ingotTitanium", 24), new ComparableStack(ModItems.plate_advanced_alloy, 18), new OreDictStack("plateSteel", 12), new ComparableStack(ModItems.plate_desh, 6), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModBlocks.machine_battery, 5), new ComparableStack(ModItems.circuit_gold, 5), }, 500);
		makeRecipe(new ComparableStack(ModBlocks.machine_combine_factory, 1), new AStack[] { new OreDictStack("ingotSteel", 8), new ComparableStack(ModItems.ingot_polymer, 6), new OreDictStack("plateTitanium", 4), new OreDictStack("plateCopper", 6), new ComparableStack(ModItems.circuit_gold, 6), new ComparableStack(ModItems.coil_advanced_alloy, 8), new ComparableStack(ModItems.coil_tungsten, 4), new ComparableStack(ModItems.ingot_magnetized_tungsten, 12), }, 150);
		makeRecipe(new ComparableStack(ModBlocks.factory_advanced_hull, 1), new AStack[] { new ComparableStack(ModItems.ingot_advanced_alloy, 4), new ComparableStack(ModItems.plate_advanced_alloy, 4), new ComparableStack(ModItems.wire_advanced_alloy, 6), }, 50);
		makeRecipe(new ComparableStack(ModBlocks.factory_advanced_furnace, 1), new AStack[] { new ComparableStack(ModItems.ingot_advanced_alloy, 4), new ComparableStack(ModItems.plate_advanced_alloy, 4), new OreDictStack("plateSteel", 8), new ComparableStack(ModItems.coil_advanced_alloy, 2), }, 100);
		makeRecipe(new ComparableStack(ModBlocks.factory_advanced_core, 1), new AStack[] { new ComparableStack(ModItems.ingot_advanced_alloy, 6), new ComparableStack(ModItems.plate_advanced_alloy, 6), new OreDictStack("plateSteel", 8), new ComparableStack(ModItems.coil_advanced_alloy, 2), new ComparableStack(ModItems.motor, 16), new ComparableStack(Blocks.PISTON, 6), }, 100);
		makeRecipe(new ComparableStack(ModBlocks.factory_advanced_conductor, 1), new AStack[] { new ComparableStack(ModItems.ingot_advanced_alloy, 8), new ComparableStack(ModItems.plate_advanced_alloy, 6), new ComparableStack(ModItems.wire_advanced_alloy, 4), new ComparableStack(ModItems.fuse, 6), }, 50);
		makeRecipe(new ComparableStack(ModBlocks.reactor_element, 1), new AStack[] { new OreDictStack("ingotSteel", 2), new OreDictStack(OreDictManager.getReflector(), 4), new OreDictStack("plateLead", 2), new ComparableStack(ModItems.rod_empty, 8), }, 150);
		makeRecipe(new ComparableStack(ModBlocks.reactor_control, 1), new AStack[] { new OreDictStack("ingotSteel", 4), new OreDictStack("ingotLead", 6), new ComparableStack(ModItems.bolt_tungsten, 6), new ComparableStack(ModItems.motor, 1), }, 100);
		makeRecipe(new ComparableStack(ModBlocks.reactor_hatch, 1), new AStack[] { new ComparableStack(ModBlocks.brick_concrete, 1), new OreDictStack("plateSteel", 6), }, 150);
		makeRecipe(new ComparableStack(ModBlocks.reactor_conductor, 1), new AStack[] { new OreDictStack("ingotSteel", 4), new OreDictStack("plateCopper", 12), new ComparableStack(ModItems.wire_tungsten, 4), }, 130);
		makeRecipe(new ComparableStack(ModBlocks.reactor_computer, 1), new AStack[] { new ComparableStack(ModBlocks.reactor_conductor, 2), new ComparableStack(ModItems.circuit_targeting_tier3, 4), new ComparableStack(ModItems.circuit_gold, 1), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.fusion_conductor, 1), new AStack[] { new OreDictStack("ingotSteel", 2), new ComparableStack(ModItems.coil_advanced_alloy, 5), }, 150);
		makeRecipe(new ComparableStack(ModBlocks.fusion_center, 1), new AStack[] { new OreDictStack("ingotTungsten", 4), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.wire_advanced_alloy, 24), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.fusion_motor, 1), new AStack[] { new OreDictStack("ingotTitanium", 4), new OreDictStack("ingotSteel", 2), new ComparableStack(ModItems.motor, 4), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.fusion_heater, 1), new AStack[] { new OreDictStack("ingotTungsten", 4), new OreDictStack("plateSteel", 2), new OreDictStack(OreDictManager.getReflector(), 2), new OreDictStack("plateCopper", 4), new ComparableStack(ModItems.magnetron, 1), new ComparableStack(ModItems.wire_advanced_alloy, 4), }, 150);
		makeRecipe(new ComparableStack(ModBlocks.watz_element, 1), new AStack[] { new OreDictStack("ingotTungsten", 4), new ComparableStack(ModItems.plate_advanced_alloy, 4), new ComparableStack(ModItems.rod_empty, 2), new ComparableStack(ModItems.wire_magnetized_tungsten, 2), new ComparableStack(ModItems.wire_advanced_alloy, 4), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.watz_control, 1), new AStack[] { new OreDictStack("ingotTungsten", 4), new ComparableStack(ModItems.ingot_advanced_alloy, 4), new OreDictStack("ingotLead", 2), new ComparableStack(ModItems.wire_magnetized_tungsten, 4), new ComparableStack(ModItems.wire_advanced_alloy, 2), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.watz_cooler, 1), new AStack[] { new OreDictStack("ingotTungsten", 2), new OreDictStack("ingotSteel", 2), new OreDictStack("dustSaltpeter", 4), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.watz_end, 1), new AStack[] { new OreDictStack("ingotTungsten", 2), new OreDictStack("ingotLead", 2), new OreDictStack("ingotSteel", 3), }, 150);
		makeRecipe(new ComparableStack(ModBlocks.watz_hatch, 1), new AStack[] { new ComparableStack(ModBlocks.reinforced_brick, 1), new OreDictStack("plateTitanium", 6), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.watz_conductor, 1), new AStack[] { new OreDictStack("ingotTungsten", 2), new OreDictStack("ingotLead", 2), new OreDictStack("ingotSteel", 2), new ComparableStack(ModItems.wire_red_copper, 6), new ComparableStack(ModItems.wire_magnetized_tungsten, 2), new ComparableStack(ModItems.fuse, 4), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.watz_core, 1), new AStack[] { new ComparableStack(ModBlocks.block_meteor, 1), new ComparableStack(ModItems.circuit_gold, 5), new ComparableStack(ModItems.circuit_schrabidium, 2), new ComparableStack(ModItems.wire_magnetized_tungsten, 12), }, 350);
		makeRecipe(new ComparableStack(ModBlocks.fwatz_hatch, 1), new AStack[] { new OreDictStack("ingotTungsten", 6), new ComparableStack(ModItems.plate_combine_steel, 4), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.fwatz_conductor, 1), new AStack[] { new ComparableStack(ModItems.plate_combine_steel, 10), new ComparableStack(ModItems.coil_magnetized_tungsten, 5), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.fwatz_computer, 1), new AStack[] { new ComparableStack(ModBlocks.block_meteor, 1), new ComparableStack(ModItems.wire_magnetized_tungsten, 16), new OreDictStack("dustDiamond", 6), new ComparableStack(ModItems.powder_magnetized_tungsten, 6), new OreDictStack("dustDesh", 4), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.fwatz_core, 1), new AStack[] { new ComparableStack(ModBlocks.block_meteor, 1), new ComparableStack(ModItems.wire_magnetized_tungsten, 24), new OreDictStack("dustDiamond", 8), new ComparableStack(ModItems.powder_magnetized_tungsten, 12), new OreDictStack("dustDesh", 8), new ComparableStack(ModItems.upgrade_power_3, 1), new ComparableStack(ModItems.upgrade_speed_3, 1), }, 450);
		makeRecipe(new ComparableStack(ModBlocks.nuke_gadget, 1), new AStack[] { new ComparableStack(ModItems.sphere_steel, 1), new ComparableStack(ModItems.fins_flat, 2), new ComparableStack(ModItems.pedestal_steel, 1), new ComparableStack(ModItems.circuit_targeting_tier3, 1), new ComparableStack(ModItems.wire_gold, 6), new OreDictStack("dyeGray", 6), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.nuke_boy, 1), new AStack[] { new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.fins_small_steel, 1), new ComparableStack(ModItems.circuit_targeting_tier2, 1), new ComparableStack(ModItems.wire_aluminium, 6), new OreDictStack("dyeBlue", 4), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.nuke_man, 1), new AStack[] { new ComparableStack(ModItems.sphere_steel, 1), new ComparableStack(ModItems.hull_big_steel, 2), new ComparableStack(ModItems.fins_big_steel, 1), new ComparableStack(ModItems.circuit_targeting_tier2, 2), new ComparableStack(ModItems.wire_copper, 6), new OreDictStack("dyeYellow", 6), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.nuke_mike, 1), new AStack[] { new ComparableStack(ModItems.sphere_steel, 1), new ComparableStack(ModItems.hull_big_aluminium, 4), new ComparableStack(ModItems.cap_aluminium, 1), new ComparableStack(ModItems.circuit_targeting_tier4, 3), new ComparableStack(ModItems.wire_gold, 18), new OreDictStack("dyeLightGray", 12), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.nuke_tsar, 1), new AStack[] { new ComparableStack(ModItems.sphere_steel, 1), new ComparableStack(ModItems.hull_big_titanium, 6), new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.fins_tri_steel, 1), new ComparableStack(ModItems.circuit_targeting_tier4, 5), new ComparableStack(ModItems.wire_gold, 24), new ComparableStack(ModItems.wire_tungsten, 12), new OreDictStack("dyeBlack", 6), }, 600);
		makeRecipe(new ComparableStack(ModBlocks.nuke_prototype, 1), new AStack[] { new ComparableStack(ModItems.dysfunctional_reactor, 1), new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.ingot_euphemium, 3), new ComparableStack(ModItems.circuit_targeting_tier5, 1), new ComparableStack(ModItems.wire_gold, 16), }, 500);
		makeRecipe(new ComparableStack(ModBlocks.nuke_fleija, 1), new AStack[] { new ComparableStack(ModItems.hull_small_aluminium, 1), new ComparableStack(ModItems.fins_quad_titanium, 1), new ComparableStack(ModItems.circuit_targeting_tier4, 2), new ComparableStack(ModItems.wire_gold, 8), new OreDictStack("dyeWhite", 4), }, 400);
		makeRecipe(new ComparableStack(ModBlocks.nuke_solinium, 1), new AStack[] { new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.fins_quad_titanium, 1), new ComparableStack(ModItems.circuit_targeting_tier4, 3), new ComparableStack(ModItems.wire_gold, 10), new ComparableStack(ModItems.pipes_steel, 4), new OreDictStack("dyeGray", 4), }, 400);
		makeRecipe(new ComparableStack(ModBlocks.nuke_n2, 1), new AStack[] { new ComparableStack(ModItems.hull_big_steel, 3), new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.wire_magnetized_tungsten, 12), new ComparableStack(ModItems.pipes_steel, 6), new ComparableStack(ModItems.circuit_targeting_tier4, 3), new OreDictStack("dyeBlack", 12), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.nuke_fstbmb, 1), new AStack[] { new ComparableStack(ModItems.sphere_steel, 1), new ComparableStack(ModItems.hull_big_titanium, 6), new ComparableStack(ModItems.fins_big_steel, 1), new ComparableStack(ModItems.powder_magic, 8), new ComparableStack(ModItems.wire_gold, 12), new ComparableStack(ModItems.circuit_targeting_tier4, 4), new OreDictStack("dyeGray", 6), }, 600);
		makeRecipe(new ComparableStack(ModBlocks.nuke_custom, 1), new AStack[] { new ComparableStack(ModItems.hull_small_steel, 2), new ComparableStack(ModItems.fins_small_steel, 1), new ComparableStack(ModItems.circuit_gold, 1), new ComparableStack(ModItems.wire_gold, 12), new OreDictStack("dyeGray", 4), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.float_bomb, 1), new AStack[] { new OreDictStack("plateTitanium", 12), new ComparableStack(ModItems.levitation_unit, 1), new ComparableStack(ModItems.circuit_gold, 4), new ComparableStack(ModItems.wire_gold, 6), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.therm_endo, 1), new AStack[] { new OreDictStack("plateTitanium", 12), new ComparableStack(ModItems.thermo_unit_endo, 1), new ComparableStack(ModItems.circuit_gold, 2), new ComparableStack(ModItems.wire_gold, 6), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.therm_exo, 1), new AStack[] { new OreDictStack("plateTitanium", 12), new ComparableStack(ModItems.thermo_unit_exo, 1), new ComparableStack(ModItems.circuit_gold, 2), new ComparableStack(ModItems.wire_gold, 6), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.launch_pad, 1), new AStack[] { new OreDictStack("ingotSteel", 4), new ComparableStack(ModItems.ingot_polymer, 2), new OreDictStack("plateSteel", 12), new ComparableStack(ModBlocks.machine_battery, 1), new ComparableStack(ModItems.circuit_gold, 2), }, 250);
		makeRecipe(new ComparableStack(ModItems.spawn_chopper, 1), new AStack[] { new ComparableStack(ModItems.chopper_blades, 5), new ComparableStack(ModItems.chopper_gun, 1), new ComparableStack(ModItems.chopper_head, 1), new ComparableStack(ModItems.chopper_tail, 1), new ComparableStack(ModItems.chopper_torso, 1), new ComparableStack(ModItems.chopper_wing, 2), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.turret_light, 1), new AStack[] { new OreDictStack("ingotSteel", 6), new ComparableStack(ModItems.pipes_steel, 2), new OreDictStack("ingotMingrade", 2), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit_targeting_tier2, 2), }, 200);
		makeRecipe(new ComparableStack(ModBlocks.turret_heavy, 1), new AStack[] { new OreDictStack("ingotSteel", 8), new OreDictStack("ingotAluminum", 4), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModItems.hull_small_steel, 1), new OreDictStack("ingotMingrade", 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit_targeting_tier2, 3), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.turret_rocket, 1), new AStack[] { new OreDictStack("ingotSteel", 12), new OreDictStack("ingotTitanium", 4), new ComparableStack(ModItems.hull_small_steel, 8), new OreDictStack("ingotMingrade", 6), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit_targeting_tier3, 2), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.turret_flamer, 1), new AStack[] { new OreDictStack("ingotSteel", 8), new OreDictStack("ingotTungsten", 2), new ComparableStack(ModItems.pipes_steel, 1), new ComparableStack(ModItems.tank_steel, 2), new OreDictStack("ingotMingrade", 4), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit_targeting_tier3, 2), }, 250);
		makeRecipe(new ComparableStack(ModBlocks.turret_tau, 1), new AStack[] { new OreDictStack("ingotSteel", 16), new OreDictStack("ingotTitanium", 8), new ComparableStack(ModItems.plate_advanced_alloy, 4), new ComparableStack(ModItems.redcoil_capacitor, 3), new OreDictStack("ingotMingrade", 12), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.circuit_targeting_tier4, 2), }, 350);
		makeRecipe(new ComparableStack(ModBlocks.turret_spitfire, 1), new AStack[] { new OreDictStack("ingotSteel", 6), new OreDictStack("ingotMingrade", 6), new OreDictStack("plateSteel", 16), new OreDictStack("plateIron", 8), new ComparableStack(ModItems.hull_small_steel, 4), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 350);
		makeRecipe(new ComparableStack(ModBlocks.turret_cwis, 1), new AStack[] { new OreDictStack("ingotSteel", 6), new OreDictStack("ingotMingrade", 8), new OreDictStack("plateSteel", 10), new OreDictStack("plateTitanium", 4), new ComparableStack(ModItems.hull_small_aluminium, 2), new ComparableStack(ModItems.pipes_steel, 6), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.circuit_targeting_tier4, 2), new ComparableStack(ModItems.magnetron, 3), }, 400);
		makeRecipe(new ComparableStack(ModBlocks.turret_cheapo, 1), new AStack[] { new OreDictStack("ingotSteel", 4), new OreDictStack("plateIron", 4), new ComparableStack(ModItems.pipes_steel, 3), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit_targeting_tier1, 4), }, 200);
		makeRecipe(new ComparableStack(ModItems.missile_generic, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_small, 1), new ComparableStack(ModItems.fuel_tank_small, 1), new ComparableStack(ModItems.thruster_small, 1), new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.circuit_targeting_tier1, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.missile_incendiary, 1), new AStack[] { new ComparableStack(ModItems.warhead_incendiary_small, 1), new ComparableStack(ModItems.fuel_tank_small, 1), new ComparableStack(ModItems.thruster_small, 1), new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.circuit_targeting_tier1, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.missile_cluster, 1), new AStack[] { new ComparableStack(ModItems.warhead_cluster_small, 1), new ComparableStack(ModItems.fuel_tank_small, 1), new ComparableStack(ModItems.thruster_small, 1), new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.circuit_targeting_tier1, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.missile_buster, 1), new AStack[] { new ComparableStack(ModItems.warhead_buster_small, 1), new ComparableStack(ModItems.fuel_tank_small, 1), new ComparableStack(ModItems.thruster_small, 1), new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.circuit_targeting_tier1, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.missile_strong, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_medium, 1), new ComparableStack(ModItems.fuel_tank_medium, 1), new ComparableStack(ModItems.thruster_medium, 1), new OreDictStack("plateTitanium", 10), new OreDictStack("plateSteel", 14), new ComparableStack(ModItems.circuit_targeting_tier2, 1), }, 250);
		makeRecipe(new ComparableStack(ModItems.missile_incendiary_strong, 1), new AStack[] { new ComparableStack(ModItems.warhead_incendiary_medium, 1), new ComparableStack(ModItems.fuel_tank_medium, 1), new ComparableStack(ModItems.thruster_medium, 1), new OreDictStack("plateTitanium", 10), new OreDictStack("plateSteel", 14), new ComparableStack(ModItems.circuit_targeting_tier2, 1), }, 250);
		makeRecipe(new ComparableStack(ModItems.missile_cluster_strong, 1), new AStack[] { new ComparableStack(ModItems.warhead_cluster_medium, 1), new ComparableStack(ModItems.fuel_tank_medium, 1), new ComparableStack(ModItems.thruster_medium, 1), new OreDictStack("plateTitanium", 10), new OreDictStack("plateSteel", 14), new ComparableStack(ModItems.circuit_targeting_tier2, 1), }, 250);
		makeRecipe(new ComparableStack(ModItems.missile_buster_strong, 1), new AStack[] { new ComparableStack(ModItems.warhead_buster_medium, 1), new ComparableStack(ModItems.fuel_tank_medium, 1), new ComparableStack(ModItems.thruster_medium, 1), new OreDictStack("plateTitanium", 10), new OreDictStack("plateSteel", 14), new ComparableStack(ModItems.circuit_targeting_tier2, 1), }, 250);
		makeRecipe(new ComparableStack(ModItems.missile_emp_strong, 1), new AStack[] {new ComparableStack(ModBlocks.emp_bomb, 3), new ComparableStack(ModItems.fuel_tank_medium, 1), new ComparableStack(ModItems.thruster_medium, 1), new OreDictStack("plateTitanium", 10), new OreDictStack("plateSteel", 14), new ComparableStack(ModItems.circuit_targeting_tier2, 1), },250);
		makeRecipe(new ComparableStack(ModItems.missile_burst, 1), new AStack[] { new ComparableStack(ModItems.warhead_generic_large, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 14), new OreDictStack("plateSteel", 20), new OreDictStack("plateAluminum", 12), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 350);
		makeRecipe(new ComparableStack(ModItems.missile_inferno, 1), new AStack[] { new ComparableStack(ModItems.warhead_incendiary_large, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 14), new OreDictStack("plateSteel", 20), new OreDictStack("plateAluminum", 12), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 350);
		makeRecipe(new ComparableStack(ModItems.missile_rain, 1), new AStack[] { new ComparableStack(ModItems.warhead_cluster_large, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 14), new OreDictStack("plateSteel", 20), new OreDictStack("plateAluminum", 12), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 350);
		makeRecipe(new ComparableStack(ModItems.missile_drill, 1), new AStack[] { new ComparableStack(ModItems.warhead_buster_large, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 14), new OreDictStack("plateSteel", 20), new OreDictStack("plateAluminum", 12), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 350);
		makeRecipe(new ComparableStack(ModItems.missile_nuclear, 1), new AStack[] { new ComparableStack(ModItems.warhead_nuclear, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 20), new OreDictStack("plateSteel", 24), new OreDictStack("plateAluminum", 16), new ComparableStack(ModItems.circuit_targeting_tier4, 1), }, 500);
		makeRecipe(new ComparableStack(ModItems.missile_nuclear_cluster, 1), new AStack[] { new ComparableStack(ModItems.warhead_mirv, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 20), new OreDictStack("plateSteel", 24), new OreDictStack("plateAluminum", 16), new ComparableStack(ModItems.circuit_targeting_tier5, 1), }, 600);
		makeRecipe(new ComparableStack(ModItems.missile_volcano, 1), new AStack[]{new ComparableStack(ModItems.warhead_volcano, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 20), new OreDictStack("plateSteel", 24), new OreDictStack("plateAluminum", 16), new ComparableStack(ModItems.circuit_targeting_tier5, 1)}, 600);
		makeRecipe(new ComparableStack(ModItems.missile_endo, 1), new AStack[] { new ComparableStack(ModItems.warhead_thermo_endo, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 14), new OreDictStack("plateSteel", 20), new OreDictStack("plateAluminum", 12), new ComparableStack(ModItems.circuit_targeting_tier4, 1), }, 350);
		makeRecipe(new ComparableStack(ModItems.missile_exo, 1), new AStack[] { new ComparableStack(ModItems.warhead_thermo_exo, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 14), new OreDictStack("plateSteel", 20), new OreDictStack("plateAluminum", 12), new ComparableStack(ModItems.circuit_targeting_tier4, 1), }, 350);
		makeRecipe(new ComparableStack(ModItems.gun_defabricator, 1), new AStack[] { new OreDictStack("ingotSteel", 2), new ComparableStack(ModItems.ingot_polymer, 8), new OreDictStack("plateIron", 5), new ComparableStack(ModItems.mechanism_special, 3), new ComparableStack(Items.DIAMOND, 1), new ComparableStack(ModItems.plate_dalekanium, 3), }, 200);
		makeRecipe(new ComparableStack(ModItems.gun_fatman_ammo, 1), new AStack[] { new OreDictStack("plateSteel", 3), new OreDictStack("plateIron", 1), new ComparableStack(ModItems.nugget_pu239, 3), }, 40);
		makeRecipe(new ComparableStack(ModItems.gun_mirv_ammo, 1), new AStack[] { new OreDictStack("plateSteel", 20), new OreDictStack("plateIron", 10), new ComparableStack(ModItems.nugget_pu239, 24), }, 100);
		makeRecipe(new ComparableStack(ModItems.gun_osipr_ammo, 24), new AStack[] { new OreDictStack("plateSteel", 2), new ComparableStack(Items.REDSTONE, 1), new ComparableStack(Items.GLOWSTONE_DUST, 1), }, 50);
		makeRecipe(new ComparableStack(ModItems.gun_osipr_ammo2, 1), new AStack[] { new ComparableStack(ModItems.plate_combine_steel, 4), new ComparableStack(Items.REDSTONE, 7), new ComparableStack(ModItems.powder_power, 3), }, 200);
		makeRecipe(new ComparableStack(ModItems.grenade_fire, 1), new AStack[] { new ComparableStack(ModItems.grenade_frag, 1), new ComparableStack(ModItems.powder_fire, 1), new OreDictStack("plateCopper", 2), }, 150);
		makeRecipe(new ComparableStack(ModItems.grenade_shrapnel, 1), new AStack[] { new ComparableStack(ModItems.grenade_frag, 1), new ComparableStack(ModItems.pellet_buckshot, 1), new OreDictStack("plateSteel", 2), }, 150);
		makeRecipe(new ComparableStack(ModItems.grenade_cluster, 1), new AStack[] { new ComparableStack(ModItems.grenade_frag, 1), new ComparableStack(ModItems.pellet_cluster, 1), new OreDictStack("plateSteel", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.grenade_flare, 1), new AStack[] { new ComparableStack(ModItems.grenade_generic, 1), new ComparableStack(Items.GLOWSTONE_DUST, 1), new OreDictStack("plateAluminum", 2), }, 100);
		makeRecipe(new ComparableStack(ModItems.grenade_electric, 1), new AStack[] { new ComparableStack(ModItems.grenade_generic, 1), new ComparableStack(ModItems.circuit_red_copper, 1), new OreDictStack("plateGold", 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.grenade_pulse, 4), new AStack[] { new OreDictStack("plateSteel", 1), new OreDictStack("plateIron", 3), new ComparableStack(ModItems.wire_red_copper, 6), new ComparableStack(Items.DIAMOND, 1), }, 300);
		makeRecipe(new ComparableStack(ModItems.grenade_plasma, 2), new AStack[] { new OreDictStack("plateSteel", 3), new ComparableStack(ModItems.plate_advanced_alloy, 1), new ComparableStack(ModItems.coil_advanced_torus, 1), new NbtComparableStack(ItemCell.getFullCell(ModForgeFluids.deuterium)), new NbtComparableStack(ItemCell.getFullCell(ModForgeFluids.tritium)), }, 300);
		makeRecipe(new ComparableStack(ModItems.grenade_tau, 2), new AStack[] { new OreDictStack("plateLead", 3), new ComparableStack(ModItems.plate_advanced_alloy, 1), new ComparableStack(ModItems.coil_advanced_torus, 1), new ComparableStack(ModItems.gun_xvl1456_ammo, 1), }, 300);
		makeRecipe(new ComparableStack(ModItems.grenade_schrabidium, 1), new AStack[] { new ComparableStack(ModItems.grenade_flare, 1), new ComparableStack(ModItems.powder_schrabidium, 1), new OreDictStack(OreDictManager.getReflector(), 2), }, 300);
		makeRecipe(new ComparableStack(ModItems.grenade_nuclear, 1), new AStack[] { new OreDictStack("plateIron", 1), new OreDictStack("plateSteel", 1), new ComparableStack(ModItems.nugget_pu239, 2), new ComparableStack(ModItems.wire_red_copper, 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.grenade_zomg, 1), new AStack[] { new ComparableStack(ModItems.plate_paa, 3), new OreDictStack(OreDictManager.getReflector(), 1), new ComparableStack(ModItems.coil_magnetized_tungsten, 3), new ComparableStack(ModItems.powder_power, 3), }, 300);
		makeRecipe(new ComparableStack(ModItems.grenade_black_hole, 1), new AStack[] { new ComparableStack(ModItems.ingot_polymer, 6), new OreDictStack(OreDictManager.getReflector(), 3), new ComparableStack(ModItems.coil_magnetized_tungsten, 2), new ComparableStack(ModItems.black_hole, 1), }, 500);
		makeRecipe(new ComparableStack(ModItems.multitool_dig, 1), new AStack[] { new ComparableStack(ModItems.rod_reiium, 1), new ComparableStack(ModItems.rod_weidanium, 1), new ComparableStack(ModItems.rod_australium, 1), new ComparableStack(ModItems.rod_verticium, 1), new ComparableStack(ModItems.rod_unobtainium, 1), new ComparableStack(ModItems.rod_daffergon, 1), new ComparableStack(ModItems.ingot_polymer, 4), new ComparableStack(ModItems.circuit_gold, 1), new ComparableStack(ModItems.ducttape, 1), }, 600);
		makeRecipe(new ComparableStack(ModItems.gadget_explosive, 1), new AStack[] { new ComparableStack(Blocks.TNT, 3), new OreDictStack("plateSteel", 2), new OreDictStack("plateAluminum", 4), new ComparableStack(ModItems.wire_gold, 3), }, 200);
		makeRecipe(new ComparableStack(ModItems.gadget_wireing, 1), new AStack[] { new OreDictStack("plateIron", 1), new ComparableStack(ModItems.wire_gold, 12), }, 100);
		makeRecipe(new ComparableStack(ModItems.gadget_core, 1), new AStack[] { new ComparableStack(ModItems.nugget_pu239, 7), new ComparableStack(ModItems.nugget_u238, 3), }, 200);
		makeRecipe(new ComparableStack(ModItems.boy_shielding, 1), new AStack[] { new OreDictStack(OreDictManager.getReflector(), 12), new OreDictStack("plateSteel", 4), }, 150);
		makeRecipe(new ComparableStack(ModItems.boy_target, 1), new AStack[] { new ComparableStack(ModItems.nugget_u235, 7), }, 200);
		makeRecipe(new ComparableStack(ModItems.boy_bullet, 1), new AStack[] { new ComparableStack(ModItems.nugget_u235, 3), }, 100);
		makeRecipe(new ComparableStack(ModItems.boy_propellant, 1), new AStack[] { new ComparableStack(Blocks.TNT, 3), new OreDictStack("plateIron", 8), new OreDictStack("plateAluminum", 4), new ComparableStack(ModItems.wire_red_copper, 4), }, 100);
		makeRecipe(new ComparableStack(ModItems.boy_igniter, 1), new AStack[] { new OreDictStack("plateAluminum", 6), new OreDictStack("plateSteel", 1), new ComparableStack(ModItems.circuit_red_copper, 1), new ComparableStack(ModItems.wire_red_copper, 3), }, 100);
		makeRecipe(new ComparableStack(ModItems.man_explosive, 1), new AStack[] { new ComparableStack(Blocks.TNT, 2), new ComparableStack(ModItems.ingot_semtex, 3), new OreDictStack("plateSteel", 2), new OreDictStack("plateTitanium", 4), new ComparableStack(ModItems.wire_red_copper, 3), }, 150);
		makeRecipe(new ComparableStack(ModItems.man_igniter, 1), new AStack[] { new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.circuit_red_copper, 1), new ComparableStack(ModItems.wire_red_copper, 9), }, 200);
		makeRecipe(new ComparableStack(ModItems.man_core, 1), new AStack[] { new ComparableStack(ModItems.nugget_pu239, 8), new ComparableStack(ModItems.nugget_beryllium, 2), }, 150);
		makeRecipe(new ComparableStack(ModItems.mike_core, 1), new AStack[] { new ComparableStack(ModItems.nugget_u238, 24), new OreDictStack("ingotLead", 6), }, 250);
		makeRecipe(new ComparableStack(ModItems.mike_deut, 1), new AStack[] { new OreDictStack("plateIron", 12), new OreDictStack("plateSteel", 16), new NbtComparableStack(ItemCell.getFullCell(ModForgeFluids.deuterium, 10)), }, 250);
		makeRecipe(new ComparableStack(ModItems.mike_cooling_unit, 1), new AStack[] { new OreDictStack("plateIron", 8), new ComparableStack(ModItems.coil_copper, 5), new ComparableStack(ModItems.coil_tungsten, 5), new ComparableStack(ModItems.motor, 2), }, 200);
		makeRecipe(new ComparableStack(ModItems.fleija_igniter, 1), new AStack[] { new OreDictStack("plateTitanium", 6), new ComparableStack(ModItems.wire_schrabidium, 2), new ComparableStack(ModItems.circuit_schrabidium, 1), }, 300);
		makeRecipe(new ComparableStack(ModItems.fleija_core, 1), new AStack[] { new ComparableStack(ModItems.nugget_u235, 8), new ComparableStack(ModItems.nugget_neptunium, 2), new ComparableStack(ModItems.nugget_beryllium, 4), new ComparableStack(ModItems.coil_copper, 2), }, 500);
		makeRecipe(new ComparableStack(ModItems.fleija_propellant, 1), new AStack[] { new ComparableStack(Blocks.TNT, 3), new ComparableStack(ModItems.plate_schrabidium, 8), }, 400);
		makeRecipe(new ComparableStack(ModItems.solinium_igniter, 1), new AStack[] { new OreDictStack("plateTitanium", 4), new ComparableStack(ModItems.wire_advanced_alloy, 2), new ComparableStack(ModItems.circuit_schrabidium, 1), new ComparableStack(ModItems.coil_gold, 1), }, 400);
		makeRecipe(new ComparableStack(ModItems.solinium_core, 1), new AStack[] { new ComparableStack(ModItems.nugget_solinium, 9), new ComparableStack(ModItems.nugget_euphemium, 1), }, 400);
		makeRecipe(new ComparableStack(ModItems.solinium_propellant, 1), new AStack[] { new ComparableStack(Blocks.TNT, 3), new OreDictStack(OreDictManager.getReflector(), 2), new ComparableStack(ModItems.plate_polymer, 6), new ComparableStack(ModItems.wire_tungsten, 6), new ComparableStack(ModItems.biomass_compressed, 4), }, 350);
		makeRecipe(new ComparableStack(ModItems.schrabidium_hammer, 1), new AStack[] { new ComparableStack(ModBlocks.block_schrabidium, 15), new ComparableStack(ModItems.ingot_polymer, 128), new ComparableStack(Items.NETHER_STAR, 3), new ComparableStack(ModItems.fragment_meteorite, 512), }, 1000);
		makeRecipe(new ComparableStack(ModItems.component_limiter, 1), new AStack[] { new ComparableStack(ModItems.hull_big_steel, 2), new OreDictStack("plateSteel", 32), new OreDictStack("plateTitanium", 18), new ComparableStack(ModItems.plate_desh, 12), new ComparableStack(ModItems.pipes_steel, 4), new ComparableStack(ModItems.circuit_gold, 8), new ComparableStack(ModItems.circuit_schrabidium, 4), new ComparableStack(ModItems.ingot_starmetal, 14), new ComparableStack(ModItems.plate_dalekanium, 5), new ComparableStack(ModItems.powder_magic, 16), new ComparableStack(ModBlocks.fwatz_computer, 3), }, 2500);
		makeRecipe(new ComparableStack(ModItems.component_emitter, 1), new AStack[] { new ComparableStack(ModItems.hull_big_steel, 3), new ComparableStack(ModItems.hull_big_titanium, 2), new OreDictStack("plateSteel", 32), new OreDictStack("plateLead", 24), new ComparableStack(ModItems.plate_desh, 24), new ComparableStack(ModItems.pipes_steel, 8), new ComparableStack(ModItems.circuit_gold, 12), new ComparableStack(ModItems.circuit_schrabidium, 8), new ComparableStack(ModItems.ingot_starmetal, 26), new ComparableStack(ModItems.powder_magic, 48), new ComparableStack(ModBlocks.fwatz_computer, 2), new ComparableStack(ModItems.crystal_xen, 1), }, 2500);
		makeRecipe(new ComparableStack(ModBlocks.ams_limiter, 1), new AStack[] { new ComparableStack(ModItems.component_limiter, 5), new OreDictStack("plateSteel", 64), new OreDictStack("plateTitanium", 128), new ComparableStack(ModItems.plate_dineutronium, 16), new ComparableStack(ModItems.circuit_schrabidium, 6), new ComparableStack(ModItems.pipes_steel, 16), new ComparableStack(ModItems.motor, 12), new ComparableStack(ModItems.coil_advanced_torus, 12), new ComparableStack(ModItems.entanglement_kit, 1), }, 6000);
		makeRecipe(new ComparableStack(ModBlocks.ams_emitter, 1), new AStack[] { new ComparableStack(ModItems.component_emitter, 16), new OreDictStack("plateSteel", 128), new OreDictStack("plateTitanium", 192), new ComparableStack(ModItems.plate_dineutronium, 32), new ComparableStack(ModItems.circuit_schrabidium, 12), new ComparableStack(ModItems.coil_advanced_torus, 24), new ComparableStack(ModItems.entanglement_kit, 3), new ComparableStack(ModItems.crystal_horn, 1), new ComparableStack(ModBlocks.fwatz_core, 1), }, 6000);
		makeRecipe(new ComparableStack(ModBlocks.machine_radar, 1), new AStack[] { new OreDictStack("ingotSteel", 8), new OreDictStack("plateSteel", 16), new ComparableStack(ModItems.ingot_polymer, 4), new ComparableStack(ModItems.plate_polymer, 24), new ComparableStack(ModItems.magnetron, 10), new ComparableStack(ModItems.motor, 3), new ComparableStack(ModItems.circuit_gold, 4), new ComparableStack(ModItems.coil_copper, 12), }, 300);
		makeRecipe(new ComparableStack(ModBlocks.machine_forcefield, 1), new AStack[] { new ComparableStack(ModItems.plate_advanced_alloy, 8), new ComparableStack(ModItems.plate_desh, 4), new ComparableStack(ModItems.coil_gold_torus, 6), new ComparableStack(ModItems.coil_magnetized_tungsten, 12), new ComparableStack(ModItems.motor, 1), new ComparableStack(ModItems.upgrade_radius, 1), new ComparableStack(ModItems.upgrade_health, 1), new ComparableStack(ModItems.circuit_targeting_tier5, 1), new ComparableStack(ModBlocks.machine_transformer, 1), }, 1000);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_10_kerosene, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new ComparableStack(ModItems.pipes_steel, 1), new OreDictStack("ingotTungsten", 4), new OreDictStack("plateSteel", 4), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_10_solid, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new ComparableStack(ModItems.coil_tungsten, 1), new ComparableStack(ModItems.ingot_dura_steel, 4), new OreDictStack("plateSteel", 4), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_10_xenon, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new OreDictStack("plateSteel", 4), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModItems.arc_electrode, 4), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_kerosene, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.pipes_steel, 4), new OreDictStack("ingotTungsten", 8), new OreDictStack("plateSteel", 6), new OreDictStack("ingotDesh", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_kerosene_dual, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.pipes_steel, 2), new OreDictStack("ingotTungsten", 4), new OreDictStack("plateSteel", 6), new OreDictStack("ingotDesh", 1), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_kerosene_triple, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.pipes_steel, 3), new OreDictStack("ingotTungsten", 6), new OreDictStack("plateSteel", 6), new OreDictStack("ingotDesh", 2), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_solid, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.ingot_dura_steel, 6), new ComparableStack(ModItems.coil_tungsten, 3), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_solid_hexdecuple, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.ingot_dura_steel, 12), new ComparableStack(ModItems.coil_tungsten, 6), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_hydrogen, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.pipes_steel, 4), new OreDictStack("ingotTungsten", 8), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.tank_steel, 1), new OreDictStack("ingotDesh", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_hydrogen_dual, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.pipes_steel, 2), new OreDictStack("ingotTungsten", 4), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.tank_steel, 1), new OreDictStack("ingotDesh", 1), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_balefire_short, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModBlocks.reactor_element, 1), new OreDictStack("ingotDesh", 8), new ComparableStack(ModItems.plate_saturnite, 12), new ComparableStack(ModItems.board_copper, 2), new ComparableStack(ModItems.ingot_uranium_fuel, 4), new ComparableStack(ModItems.pipes_steel, 2), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_balefire, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.plate_polymer, 16), new ComparableStack(ModBlocks.reactor_element, 2), new OreDictStack("ingotDesh", 16), new ComparableStack(ModItems.plate_saturnite, 24), new ComparableStack(ModItems.board_copper, 4), new ComparableStack(ModItems.ingot_uranium_fuel, 8), new ComparableStack(ModItems.pipes_steel, 2), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_15_balefire_large, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.plate_polymer, 16), new ComparableStack(ModBlocks.reactor_element, 2), new OreDictStack("ingotDesh", 24), new ComparableStack(ModItems.plate_saturnite, 32), new ComparableStack(ModItems.board_copper, 4), new ComparableStack(ModItems.ingot_uranium_fuel, 8), new ComparableStack(ModItems.pipes_steel, 2), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_20_kerosene, 1), new AStack[] { new ComparableStack(ModItems.seg_20, 1), new ComparableStack(ModItems.pipes_steel, 8), new OreDictStack("ingotTungsten", 16), new OreDictStack("plateSteel", 12), new OreDictStack("ingotDesh", 8), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_20_kerosene_dual, 1), new AStack[] { new ComparableStack(ModItems.seg_20, 1), new ComparableStack(ModItems.pipes_steel, 4), new OreDictStack("ingotTungsten", 8), new OreDictStack("plateSteel", 6), new OreDictStack("ingotDesh", 4), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_20_kerosene_triple, 1), new AStack[] { new ComparableStack(ModItems.seg_20, 1), new ComparableStack(ModItems.pipes_steel, 6), new OreDictStack("ingotTungsten", 12), new OreDictStack("plateSteel", 8), new OreDictStack("ingotDesh", 6), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_20_solid, 1), new AStack[] { new ComparableStack(ModItems.seg_20, 1), new ComparableStack(ModItems.coil_tungsten, 8), new ComparableStack(ModItems.ingot_dura_steel, 16), new OreDictStack("plateSteel", 12), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_20_solid_multi, 1), new AStack[] { new ComparableStack(ModItems.seg_20, 1), new ComparableStack(ModItems.coil_tungsten, 12), new ComparableStack(ModItems.ingot_dura_steel, 18), new OreDictStack("plateSteel", 12), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_thruster_20_solid_multier, 1), new AStack[] { new ComparableStack(ModItems.seg_20, 1), new ComparableStack(ModItems.coil_tungsten, 16), new ComparableStack(ModItems.ingot_dura_steel, 20), new OreDictStack("plateSteel", 12), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_kerosene, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 2), new ComparableStack(ModBlocks.steel_scaffold, 3), new OreDictStack("plateTitanium", 12), new OreDictStack("plateSteel", 3), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_solid, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 2), new ComparableStack(ModBlocks.steel_scaffold, 3), new OreDictStack("plateTitanium", 12), new OreDictStack("plateAluminum", 3), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_xenon, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 2), new ComparableStack(ModBlocks.steel_scaffold, 3), new OreDictStack("plateTitanium", 12), new ComparableStack(ModItems.board_copper, 3), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_long_kerosene, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 2), new ComparableStack(ModBlocks.steel_scaffold, 6), new OreDictStack("plateTitanium", 24), new OreDictStack("plateSteel", 6), }, 200);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_long_solid, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 2), new ComparableStack(ModBlocks.steel_scaffold, 6), new OreDictStack("plateTitanium", 24), new OreDictStack("plateAluminum", 6), }, 200);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_15_kerosene, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModBlocks.steel_scaffold, 9), new OreDictStack("plateTitanium", 36), new OreDictStack("plateSteel", 9), }, 300);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_15_solid, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModBlocks.steel_scaffold, 9), new OreDictStack("plateTitanium", 36), new OreDictStack("plateAluminum", 9), }, 300);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_15_hydrogen, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModBlocks.steel_scaffold, 9), new OreDictStack("plateTitanium", 36), new OreDictStack("plateIron", 9), }, 300);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_10_15_balefire, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModBlocks.steel_scaffold, 9), new OreDictStack("plateTitanium", 36), new ComparableStack(ModItems.plate_saturnite, 9), }, 300);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_15_kerosene, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 2), new ComparableStack(ModBlocks.steel_scaffold, 12), new OreDictStack("plateTitanium", 48), new OreDictStack("plateSteel", 12), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_15_solid, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 2), new ComparableStack(ModBlocks.steel_scaffold, 12), new OreDictStack("plateTitanium", 48), new OreDictStack("plateAluminum", 12), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_15_hydrogen, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 2), new ComparableStack(ModBlocks.steel_scaffold, 12), new OreDictStack("plateTitanium", 48), new OreDictStack("plateIron", 12), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_15_balefire, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 2), new ComparableStack(ModBlocks.steel_scaffold, 12), new OreDictStack("plateTitanium", 48), new ComparableStack(ModItems.plate_saturnite, 12), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_15_20_kerosene, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.seg_20, 1), new ComparableStack(ModBlocks.steel_scaffold, 16), new OreDictStack("plateTitanium", 64), new OreDictStack("plateSteel", 16), }, 600);
		makeRecipe(new ComparableStack(ModItems.mp_fuselage_15_20_solid, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new ComparableStack(ModItems.seg_20, 1), new ComparableStack(ModBlocks.steel_scaffold, 16), new OreDictStack("plateTitanium", 64), new OreDictStack("plateAluminum", 16), }, 600);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_10_he, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new OreDictStack("plateSteel", 6), new ComparableStack(Blocks.TNT, 3), new ComparableStack(ModItems.circuit_targeting_tier2, 1), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_10_incendiary, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new OreDictStack("plateTitanium", 4), new ComparableStack(ModItems.powder_fire, 3), new ComparableStack(Blocks.TNT, 2), new ComparableStack(ModItems.circuit_targeting_tier2, 1), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_10_buster, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new OreDictStack("plateTitanium", 4), new ComparableStack(ModBlocks.det_charge, 1), new ComparableStack(ModBlocks.det_cord, 4), new ComparableStack(ModItems.board_copper, 4), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_10_nuclear, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new OreDictStack("plateSteel", 6), new ComparableStack(ModItems.ingot_pu239, 1), new ComparableStack(Blocks.TNT, 2), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_10_nuclear_large, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new OreDictStack("plateSteel", 8), new OreDictStack("plateAluminum", 4), new ComparableStack(ModItems.ingot_pu239, 2), new ComparableStack(ModBlocks.det_charge, 2), new ComparableStack(ModItems.circuit_targeting_tier4, 1), }, 300);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_10_taint, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new OreDictStack("plateSteel", 12), new ComparableStack(ModBlocks.det_cord, 2), new ComparableStack(ModItems.powder_magic, 12), new NbtComparableStack(FluidUtil.getFilledBucket(new FluidStack(ModForgeFluids.mud_fluid, 1000))), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_10_cloud, 1), new AStack[] { new ComparableStack(ModItems.seg_10, 1), new OreDictStack("plateSteel", 12), new ComparableStack(ModBlocks.det_cord, 2), new ComparableStack(ModItems.grenade_pink_cloud, 2), }, 100);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_15_he, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new OreDictStack("plateSteel", 16), new ComparableStack(ModBlocks.det_charge, 4), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_15_incendiary, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new OreDictStack("plateSteel", 16), new ComparableStack(ModBlocks.det_charge, 2), new ComparableStack(ModItems.powder_fire, 8), new ComparableStack(ModItems.circuit_targeting_tier3, 1), }, 200);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_15_nuclear, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new OreDictStack("plateSteel", 24), new OreDictStack("plateTitanium", 12), new ComparableStack(ModItems.ingot_pu239, 3), new ComparableStack(ModBlocks.det_charge, 4), new ComparableStack(ModItems.circuit_targeting_tier4, 1), }, 500);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_15_n2, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new OreDictStack("plateSteel", 8), new OreDictStack("plateTitanium", 20), new ComparableStack(ModBlocks.det_charge, 24), new ComparableStack(Blocks.REDSTONE_BLOCK, 12), new ComparableStack(ModItems.powder_magnetized_tungsten, 6), new ComparableStack(ModItems.circuit_targeting_tier4, 1), }, 400);
		makeRecipe(new ComparableStack(ModItems.missile_soyuz0, 1), new AStack[] { new ComparableStack(ModItems.rocket_fuel, 40), new ComparableStack(ModBlocks.det_cord, 20), new ComparableStack(ModItems.thruster_medium, 12), new ComparableStack(ModItems.thruster_small, 12), new ComparableStack(ModItems.tank_steel, 10), new ComparableStack(ModItems.circuit_targeting_tier4, 4), new ComparableStack(ModItems.circuit_targeting_tier3, 8), new ComparableStack(ModItems.plate_polymer, 64), new ComparableStack(ModItems.fins_small_steel, 4), new ComparableStack(ModItems.hull_big_titanium, 40), new ComparableStack(ModItems.hull_big_steel, 24), new ComparableStack(ModItems.ingot_fiberglass, 64), }, 600);
		makeRecipe(new ComparableStack(ModItems.missile_soyuz_lander, 1), new AStack[] { new ComparableStack(ModItems.rocket_fuel, 10), new ComparableStack(ModItems.thruster_small, 3), new ComparableStack(ModItems.tank_steel, 2), new ComparableStack(ModItems.circuit_targeting_tier3, 4), new ComparableStack(ModItems.plate_polymer, 32), new ComparableStack(ModItems.hull_big_aluminium, 2), new ComparableStack(ModItems.sphere_steel, 1), new ComparableStack(ModItems.ingot_fiberglass, 12), }, 600);
		makeRecipe(new ComparableStack(ModItems.fusion_shield_tungsten, 1), new AStack[] { new OreDictStack("blockTungsten", 32), new OreDictStack(OreDictManager.getReflector(), 96) }, 600);
		makeRecipe(new ComparableStack(ModItems.fusion_shield_desh, 1), new AStack[] { new OreDictStack("blockDesh", 16), new OreDictStack("blockCobalt", 16), new ComparableStack(ModItems.plate_saturnite, 96) }, 600);
		makeRecipe(new ComparableStack(ModItems.fusion_shield_chlorophyte, 1), new AStack[] { new OreDictStack("blockTungsten", 16), new ComparableStack(ModBlocks.block_dura_steel, 16), new OreDictStack(OreDictManager.getReflector(), 48), new ComparableStack(ModItems.powder_chlorophyte, 48) }, 600);
		makeRecipe(new ComparableStack(ModBlocks.machine_fensu, 1), new AStack[] { new ComparableStack(ModItems.ingot_electronium, 32), new ComparableStack(ModBlocks.machine_dineutronium_battery, 16), new OreDictStack("blockSteel", 32), new ComparableStack(ModBlocks.block_dura_steel, 16), new ComparableStack(ModBlocks.block_starmetal, 64), new ComparableStack(ModBlocks.machine_transformer_dnt, 8), new ComparableStack(ModItems.coil_magnetized_tungsten, 24), new ComparableStack(ModItems.powder_magic, 64), new ComparableStack(ModItems.plate_dineutronium, 24), new ComparableStack(ModItems.ingot_u238m2), new ComparableStack(ModItems.ingot_fiberglass, 128) }, 1200);
		makeRecipe(new ComparableStack(ModBlocks.struct_iter_core, 1), new AStack[] { new OreDictStack("ingotSteel", 6), new OreDictStack("ingotTungsten", 6), new OreDictStack(OreDictManager.getReflector(), 12), new ComparableStack(ModItems.coil_advanced_alloy, 12), new ComparableStack(ModItems.ingot_polymer, 8), new ComparableStack(ModItems.circuit_red_copper, 24), new ComparableStack(ModItems.circuit_gold, 12) }, 600);
		makeRecipe(new ComparableStack(ModBlocks.machine_large_turbine, 1), new AStack[] { new ComparableStack(ModItems.hull_big_steel, 1), new OreDictStack("plateSteel", 12), new ComparableStack(ModItems.turbine_titanium, 3), new ComparableStack(ModItems.rotor_steel, 2), new ComparableStack(ModItems.generator_steel, 2), new ComparableStack(ModItems.bolt_compound, 3), new ComparableStack(ModItems.pipes_steel, 1), new ComparableStack(ModItems.circuit_aluminium, 1), }, 20);

		makeRecipe(new ComparableStack(ModItems.pellet_chlorophyte, 2), new AStack[] { new ComparableStack(ModItems.powder_chlorophyte, 1), new OreDictStack("nuggetLead", 12), }, 50);
		makeRecipe(new ComparableStack(ModItems.pellet_mercury, 2), new AStack[] { new ComparableStack(ModItems.nugget_mercury, 1), new OreDictStack("nuggetLead", 12), }, 50);
		makeRecipe(new ComparableStack(ModItems.pellet_meteorite, 2), new AStack[] { new ComparableStack(ModItems.powder_meteorite, 1), new OreDictStack("nuggetLead", 12), }, 50);
		makeRecipe(new ComparableStack(ModItems.pellet_canister, 2), new AStack[] { new OreDictStack("ingotIron", 3), }, 50);
		makeRecipe(new ComparableStack(ModItems.pellet_rtg_polonium, 1), new AStack[] { new ComparableStack(ModItems.nugget_polonium, 5), new OreDictStack("plateIron", 2), }, 50);
		makeRecipe(new ComparableStack(ModItems.mp_warhead_15_balefire, 1), new AStack[] { new ComparableStack(ModItems.seg_15, 1), new OreDictStack(OreDictManager.getReflector(), 16), new ComparableStack(ModItems.powder_magic, 6), new ComparableStack(ModItems.egg_balefire_shard, 4), new ComparableStack(ModItems.ingot_semtex, 8), new ComparableStack(ModItems.circuit_targeting_tier4, 1), }, 60);

		makeRecipe(new ComparableStack(ModBlocks.machine_cyclotron, 1), new AStack[] { new ComparableStack(ModBlocks.machine_lithium_battery, 3), new ComparableStack(ModBlocks.fusion_conductor, 8), new ComparableStack(ModItems.wire_advanced_alloy, 96), new OreDictStack("ingotSteel", 16), new OreDictStack("plateSteel", 32), new OreDictStack("plateAluminum", 32), new ComparableStack(ModItems.ingot_polymer, 24), new ComparableStack(ModItems.plate_polymer, 64), new ComparableStack(ModItems.board_copper, 8), new ComparableStack(ModItems.circuit_red_copper, 8), new ComparableStack(ModItems.circuit_gold, 3), }, 600);
		makeRecipe(new ComparableStack(ModItems.gun_zomg, 1), new AStack[] { new ComparableStack(ModItems.crystal_xen, 2), new ComparableStack(ModItems.singularity_counter_resonant, 1), new ComparableStack(ModItems.mechanism_special, 3), new ComparableStack(ModItems.plate_paa, 12), new OreDictStack(OreDictManager.getReflector(), 8), new ComparableStack(ModItems.coil_magnetized_tungsten, 5), new ComparableStack(ModItems.powder_magic, 4), new OreDictStack("ingotAsbestos", 8) }, 200);
		
		makeRecipe(new ComparableStack(ModBlocks.machine_industrial_generator, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_coal_off, 2),
				new ComparableStack(ModBlocks.machine_boiler_off, 2),
				new ComparableStack(ModBlocks.machine_large_turbine, 1),
				new ComparableStack(ModBlocks.machine_transformer, 1),
				new ComparableStack(ModBlocks.steel_scaffold, 20),
				new OreDictStack("ingotSteel", 12),
				new OreDictStack("plateLead", 8),
				new OreDictStack("plateAluminum", 12),
				new ComparableStack(ModItems.pipes_steel, 1)
			}, 200);

		makeRecipe(new ComparableStack(ModBlocks.block_cap_nuka, 1), new AStack[] { new ComparableStack(ModItems.cap_nuka, 128) }, 10);
		makeRecipe(new ComparableStack(ModBlocks.block_cap_quantum, 1), new AStack[] { new ComparableStack(ModItems.cap_quantum, 128) }, 10);
		makeRecipe(new ComparableStack(ModBlocks.block_cap_sparkle, 1), new AStack[] { new ComparableStack(ModItems.cap_sparkle, 128) }, 10);
		makeRecipe(new ComparableStack(ModBlocks.block_cap_rad, 1), new AStack[] { new ComparableStack(ModItems.cap_rad, 128) }, 10);
		makeRecipe(new ComparableStack(ModBlocks.block_cap_korl, 1), new AStack[] { new ComparableStack(ModItems.cap_korl, 128) }, 10);
		makeRecipe(new ComparableStack(ModBlocks.block_cap_fritz, 1), new AStack[] { new ComparableStack(ModItems.cap_fritz, 128) }, 10);
		makeRecipe(new ComparableStack(ModBlocks.block_cap_sunset, 1), new AStack[] { new ComparableStack(ModItems.cap_sunset, 128) }, 10);
		makeRecipe(new ComparableStack(ModBlocks.block_cap_star, 1), new AStack[] { new ComparableStack(ModItems.cap_star, 128) }, 10);
		
		makeRecipe(new ComparableStack(ModItems.ammo_75bolt, 2), new AStack[] {
				new OreDictStack("plateSteel", 2),
				new OreDictStack("plateCopper", 1),
				new ComparableStack(ModItems.primer_50, 5),
				new ComparableStack(ModItems.casing_50, 5),
				new ComparableStack(ModItems.ingot_semtex, 2),
				new ComparableStack(ModItems.cordite, 3),
				new ComparableStack(ModItems.ingot_u238, 1)
			}, 60);
		makeRecipe(new ComparableStack(ModItems.ammo_75bolt_incendiary, 2), new AStack[] {
				new OreDictStack("plateSteel", 2),
				new OreDictStack("plateCopper", 1),
				new ComparableStack(ModItems.primer_50, 5),
				new ComparableStack(ModItems.casing_50, 5),
				new ComparableStack(ModItems.ingot_semtex, 3),
				new ComparableStack(ModItems.cordite, 3),
				new ComparableStack(ModItems.ingot_phosphorus, 3)
			}, 60);

		makeRecipe(new ComparableStack(ModItems.ammo_75bolt_he, 2), new AStack[] {
				new OreDictStack("plateSteel", 2),
				new OreDictStack("plateCopper", 1),
				new ComparableStack(ModItems.primer_50, 5),
				new ComparableStack(ModItems.casing_50, 5),
				new ComparableStack(ModItems.ingot_semtex, 5),
				new ComparableStack(ModItems.cordite, 5),
				new ComparableStack(Items.REDSTONE, 3)
			}, 60);
		makeRecipe(new ComparableStack(ModItems.spawn_worm, 1), new AStack[] {
				new OreDictStack("blockTitanium", 75),
				new ComparableStack(ModItems.motor, 75),
				new ComparableStack(ModBlocks.glass_trinitite, 25),
				new ComparableStack(Items.REDSTONE, 75),
				new ComparableStack(ModItems.wire_gold, 75),
				new ComparableStack(ModBlocks.block_polonium, 10),
				new ComparableStack(ModItems.plate_armor_titanium, 50),
				new ComparableStack(ModItems.coin_worm, 1)
			}, 1200);
		makeRecipe(new ComparableStack(ModBlocks.turret_chekhov, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_battery, 1),
				new OreDictStack("ingotSteel", 16),
				new ComparableStack(ModItems.ingot_dura_steel, 4),
				new ComparableStack(ModItems.motor, 3),
				new ComparableStack(ModItems.circuit_targeting_tier3, 1),
				new ComparableStack(ModItems.pipes_steel, 1),
				new ComparableStack(ModItems.mechanism_rifle_2, 1),
				new ComparableStack(ModBlocks.crate_iron, 1)
			}, 200);
		makeRecipe(new ComparableStack(ModBlocks.turret_friendly, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_battery, 1),
				new OreDictStack("ingotSteel", 16),
				new ComparableStack(ModItems.ingot_dura_steel, 4),
				new ComparableStack(ModItems.motor, 3),
				new ComparableStack(ModItems.circuit_targeting_tier2, 1),
				new ComparableStack(ModItems.pipes_steel, 1),
				new ComparableStack(ModItems.mechanism_rifle_1, 1),
				new ComparableStack(ModBlocks.crate_iron, 1)
			}, 200);
		makeRecipe(new ComparableStack(ModBlocks.turret_jeremy, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_battery, 1),
				new OreDictStack("ingotSteel", 16),
				new ComparableStack(ModItems.ingot_dura_steel, 4),
				new ComparableStack(ModItems.motor, 2),
				new ComparableStack(ModItems.circuit_targeting_tier4, 1),
				new ComparableStack(ModItems.motor_desh, 1),
				new ComparableStack(ModItems.hull_small_steel, 3),
				new ComparableStack(ModItems.mechanism_launcher_2, 1),
				new ComparableStack(ModBlocks.crate_steel, 1)
			}, 200);
		makeRecipe(new ComparableStack(ModBlocks.turret_tauon, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_lithium_battery, 1),
				new OreDictStack("ingotSteel", 16),
				new ComparableStack(ModItems.ingot_polymer, 4),
				new ComparableStack(ModItems.motor, 2),
				new ComparableStack(ModItems.circuit_targeting_tier4, 1),
				new ComparableStack(ModItems.motor_desh, 1),
				new OreDictStack("ingotCopper", 32),
				new ComparableStack(ModItems.mechanism_special, 1),
				new ComparableStack(ModItems.battery_lithium, 1)
			}, 200);
		makeRecipe(new ComparableStack(ModBlocks.turret_richard, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_battery, 1),
				new OreDictStack("ingotSteel", 16),
				new ComparableStack(ModItems.ingot_dura_steel, 4),
				new ComparableStack(ModItems.motor, 2),
				new ComparableStack(ModItems.circuit_targeting_tier4, 1),
				new ComparableStack(ModItems.ingot_polymer, 2),
				new ComparableStack(ModItems.hull_small_steel, 8),
				new ComparableStack(ModItems.mechanism_launcher_2, 1),
				new ComparableStack(ModBlocks.crate_steel, 1)
			}, 200);
		makeRecipe(new ComparableStack(ModBlocks.turret_howard, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_battery, 1),
				new OreDictStack("ingotSteel", 24),
				new ComparableStack(ModItems.ingot_dura_steel, 6),
				new ComparableStack(ModItems.motor, 2),
				new ComparableStack(ModItems.motor_desh, 2),
				new ComparableStack(ModItems.circuit_targeting_tier3, 2),
				new ComparableStack(ModItems.pipes_steel, 2),
				new ComparableStack(ModItems.mechanism_rifle_2, 2),
				new ComparableStack(ModBlocks.crate_steel, 1)
			}, 200);
		makeRecipe(new ComparableStack(ModBlocks.turret_maxwell, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_lithium_battery, 1),
				new OreDictStack("ingotSteel", 24),
				new ComparableStack(ModItems.ingot_dura_steel, 6),
				new ComparableStack(ModItems.motor, 2),
				new ComparableStack(ModItems.circuit_targeting_tier4, 2),
				new ComparableStack(ModItems.pipes_steel, 1),
				new ComparableStack(ModItems.mechanism_special, 3),
				new ComparableStack(ModItems.magnetron, 16),
				new ComparableStack(ModItems.ingot_tcalloy, 8)
			}, 200);
		makeRecipe(new ComparableStack(ModBlocks.turret_fritz, 1), new AStack[] {
				new ComparableStack(ModBlocks.machine_battery, 1),
				new OreDictStack("ingotSteel", 16),
				new ComparableStack(ModItems.ingot_dura_steel, 4),
				new ComparableStack(ModItems.motor, 3),
				new ComparableStack(ModItems.circuit_targeting_tier3, 1),
				new ComparableStack(ModItems.pipes_steel, 1),
				new ComparableStack(ModItems.mechanism_launcher_1, 1),
				new ComparableStack(ModBlocks.barrel_steel, 1)
			}, 200);
		makeRecipe(new ComparableStack(ModItems.gun_egon, 1), new AStack[] {new ComparableStack(ModItems.mechanism_special, 4), new OreDictStack("plateSteel", 16), new OreDictStack("plateLead", 24), new ComparableStack(ModItems.coil_advanced_torus, 32), new ComparableStack(ModItems.circuit_targeting_tier6, 4), new ComparableStack(ModItems.plate_polymer, 8), new ComparableStack(ModBlocks.machine_lithium_battery, 2), new ComparableStack(ModBlocks.machine_waste_drum, 1), new ComparableStack(ModItems.wire_copper, 8)}, 256);
		makeRecipe(new ComparableStack(ModBlocks.silo_hatch, 1), new AStack[]{new ComparableStack(ModItems.motor, 8), new OreDictStack("ingotSteel", 32), new ComparableStack(ModItems.hull_big_steel, 8), new ComparableStack(ModItems.plate_polymer, 4), new ComparableStack(ModItems.pipes_steel, 2), new OreDictStack("dustRedstone", 4), }, 300);
		
		makeRecipe(new ComparableStack(ModItems.sat_gerald, 1), new AStack[] {
				new ComparableStack(ModItems.burnt_bark, 1),
				new ComparableStack(ModItems.combine_scrap, 1),
				new ComparableStack(ModItems.crystal_horn, 1),
				new ComparableStack(ModItems.crystal_charred, 1),
				new ComparableStack(ModBlocks.pink_log, 1),
				new ComparableStack(ModItems.mp_warhead_15_balefire, 1),
				new ComparableStack(ModBlocks.det_nuke, 16),
				new ComparableStack(ModItems.ingot_starmetal, 32),
				new ComparableStack(ModItems.coin_creeper, 1),
				new ComparableStack(ModItems.coin_radiation, 1),
				new ComparableStack(ModItems.coin_maskman, 1),
				new ComparableStack(ModItems.coin_worm, 1),
			}, 1200);
		
		makeRecipe(new ComparableStack(ModBlocks.machine_chungus, 1), new AStack[] {
				new ComparableStack(ModItems.hull_big_steel, 6),
				new OreDictStack("plateSteel", 32),
				new OreDictStack("plateTitanium", 12),
				new ComparableStack(ModItems.ingot_tcalloy, 16),
				new ComparableStack(ModItems.turbine_tungsten, 5),
				new ComparableStack(ModItems.turbine_titanium, 3),
				new ComparableStack(ModItems.flywheel_beryllium, 1),
				new ComparableStack(ModItems.generator_steel, 10),
				new ComparableStack(ModItems.bolt_compound, 16),
				new ComparableStack(ModItems.pipes_steel, 3)
			}, 600);
		
		makeRecipe(new ComparableStack(ModBlocks.machine_silex, 1), new AStack[] {
				new ComparableStack(Blocks.GLASS, 12),
				new ComparableStack(ModItems.motor, 2),
				new ComparableStack(ModItems.ingot_dura_steel, 4),
				new OreDictStack("plateSteel", 8),
				new OreDictStack("ingotDesh", 2),
				new ComparableStack(ModItems.tank_steel, 1),
				new ComparableStack(ModItems.pipes_steel, 1),
				new ComparableStack(ModItems.crystal_diamond, 1)
			}, 400);
		
		makeRecipe(new ComparableStack(ModBlocks.rbmk_blank, 1), new AStack[] {
				new ComparableStack(ModBlocks.concrete_asbestos, 4),
				new OreDictStack("plateSteel", 4),
				new OreDictStack("ingotCopper", 4),
				new ComparableStack(ModItems.plate_polymer, 4)
			}, 100);
		
		makeRecipe(new ComparableStack(ModItems.multitool_hit, 1), new AStack[] {
				new OreDictStack("ingotTcAlloy", 4),
				new OreDictStack("plateSteel", 4),
				new ComparableStack(ModItems.wire_gold, 12),
				new ComparableStack(ModItems.motor, 4),
				new ComparableStack(ModItems.circuit_tantalium, 16)
			}, 100);
		
		if(!GeneralConfig.enable528) {
			makeRecipe(new ComparableStack(ModBlocks.reactor_element, 1), new AStack[] {new OreDictStack("ingotSteel", 2), new OreDictStack(OreDictManager.getReflector(), 4), new OreDictStack("plateLead", 2), new ComparableStack(ModItems.ingot_zirconium, 2), },150);
			makeRecipe(new ComparableStack(ModBlocks.reactor_control, 1), new AStack[] {new OreDictStack("ingotSteel", 4), new OreDictStack("ingotLead", 6), new ComparableStack(ModItems.bolt_tungsten, 6), new ComparableStack(ModItems.motor, 1), },100);
			makeRecipe(new ComparableStack(ModBlocks.reactor_hatch, 1), new AStack[] {new ComparableStack(ModBlocks.brick_concrete, 1), new OreDictStack("plateSteel", 6), },150);
			makeRecipe(new ComparableStack(ModBlocks.reactor_conductor, 1), new AStack[] {new OreDictStack("ingotSteel", 4), new OreDictStack("plateCopper", 12), new ComparableStack(ModItems.wire_tungsten, 4), },130);
			makeRecipe(new ComparableStack(ModBlocks.reactor_computer, 1), new AStack[] {new ComparableStack(ModBlocks.reactor_conductor, 2), new ComparableStack(ModItems.circuit_targeting_tier3, 4), new ComparableStack(ModItems.circuit_gold, 1), },250);
			makeRecipe(new ComparableStack(ModBlocks.machine_radgen, 1), new AStack[] {new OreDictStack("ingotSteel", 8), new OreDictStack("plateSteel", 32), new ComparableStack(ModItems.coil_magnetized_tungsten, 6), new ComparableStack(ModItems.wire_magnetized_tungsten, 24), new ComparableStack(ModItems.circuit_gold, 4), new ComparableStack(ModItems.reactor_core, 3), new ComparableStack(ModItems.ingot_starmetal, 1), new OreDictStack("dyeRed", 1), },400);
			makeRecipe(new ComparableStack(ModBlocks.machine_reactor, 1), new AStack[] {new ComparableStack(ModItems.reactor_core, 1), new OreDictStack("ingotSteel", 12), new OreDictStack("plateLead", 16), new ComparableStack(ModBlocks.reinforced_glass, 4), new ComparableStack(ModItems.ingot_asbestos, 4), new ComparableStack(ModItems.ingot_tcalloy, 4)},150);
			makeRecipe(new ComparableStack(ModBlocks.machine_reactor_small, 1), new AStack[] {new OreDictStack("ingotSteel", 6), new ComparableStack(ModItems.ingot_polymer, 4), new OreDictStack("plateLead", 8), new OreDictStack("plateCopper", 4), new OreDictStack("ingotLead", 12), new OreDictStack("ingotMingrade", 6), new ComparableStack(ModItems.circuit_copper, 8), new ComparableStack(ModItems.circuit_red_copper, 4), },300);
		
		} else {
			addTantalium(new ComparableStack(ModBlocks.machine_centrifuge, 1), 5);
			addTantalium(new ComparableStack(ModBlocks.machine_gascent, 1), 25);
			addTantalium(new ComparableStack(ModBlocks.machine_crystallizer, 1), 15);
			addTantalium(new ComparableStack(ModBlocks.machine_large_turbine, 1), 10);
			addTantalium(new ComparableStack(ModBlocks.machine_chungus, 1), 50);
			addTantalium(new ComparableStack(ModBlocks.machine_refinery, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.machine_silex, 1), 15);
			addTantalium(new ComparableStack(ModBlocks.machine_radar, 1), 20);
			addTantalium(new ComparableStack(ModBlocks.machine_mining_laser, 1), 30);
			
			addTantalium(new ComparableStack(ModBlocks.turret_chekhov, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.turret_friendly, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.turret_jeremy, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.turret_tauon, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.turret_richard, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.turret_howard, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.turret_maxwell, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.turret_fritz, 1), 3);
			addTantalium(new ComparableStack(ModBlocks.launch_pad, 1), 5);
			
			makeRecipe(new ComparableStack(ModBlocks.machine_cyclotron, 1), new AStack[] {
					new ComparableStack(ModBlocks.machine_lithium_battery, 3),
					new ComparableStack(ModBlocks.hadron_coil_neodymium, 8),
					new ComparableStack(ModItems.wire_advanced_alloy, 64),
					new OreDictStack("ingotSteel", 16),
					new OreDictStack("plateSteel", 32),
					new OreDictStack("plateAluminum", 32),
					new ComparableStack(ModItems.ingot_polymer, 24),
					new ComparableStack(ModItems.plate_polymer, 64),
					new ComparableStack(ModItems.board_copper, 8),
					new ComparableStack(ModItems.circuit_red_copper, 8),
					new ComparableStack(ModItems.circuit_gold, 3),
					new ComparableStack(ModItems.circuit_tantalium, 50),
				}, 600);
			
			makeRecipe(new ComparableStack(ModBlocks.rbmk_console, 1), new AStack[] {
					new OreDictStack("ingotSteel", 16),
					new OreDictStack("plateAluminum", 32),
					new ComparableStack(ModItems.plate_polymer, 16),
					new ComparableStack(ModItems.circuit_gold, 5),
					new ComparableStack(ModItems.circuit_tantalium, 20),
				}, 300);
			
			makeRecipe(new ComparableStack(ModBlocks.rbmk_console, 1), new AStack[] {
					new OreDictStack("ingotSteel", 16),
					new OreDictStack("plateAluminum", 32),
					new ComparableStack(ModItems.plate_polymer, 16),
					new ComparableStack(ModItems.circuit_gold, 5),
					new ComparableStack(ModItems.circuit_tantalium, 20),
				}, 300);
			
			makeRecipe(new ComparableStack(ModBlocks.hadron_core, 1), new AStack[] {
					new ComparableStack(ModBlocks.hadron_coil_alloy, 24),
					new OreDictStack("ingotSteel", 8),
					new ComparableStack(ModItems.ingot_polymer, 16),
					new ComparableStack(ModItems.ingot_tcalloy, 8),
					new ComparableStack(ModItems.circuit_gold, 5),
					new ComparableStack(ModItems.circuit_schrabidium, 5),
					new ComparableStack(ModItems.circuit_tantalium, 192),
				}, 300);
			
			makeRecipe(new ComparableStack(ModBlocks.struct_launcher_core, 1), new AStack[] {
					new ComparableStack(ModBlocks.machine_battery, 3),
					new ComparableStack(ModBlocks.steel_scaffold, 10),
					new OreDictStack("ingotSteel", 16),
					new ComparableStack(ModItems.ingot_polymer, 8),
					new ComparableStack(ModItems.circuit_red_copper, 5),
					new ComparableStack(ModItems.circuit_tantalium, 15),
				}, 200);
			
			makeRecipe(new ComparableStack(ModBlocks.struct_launcher_core_large, 1), new AStack[] {
					new ComparableStack(ModBlocks.machine_battery, 5),
					new ComparableStack(ModBlocks.steel_scaffold, 10),
					new OreDictStack("ingotSteel", 24),
					new ComparableStack(ModItems.ingot_polymer, 12),
					new ComparableStack(ModItems.circuit_gold, 5),
					new ComparableStack(ModItems.circuit_tantalium, 25),
				}, 200);
			
			makeRecipe(new ComparableStack(ModBlocks.struct_soyuz_core, 1), new AStack[] {
					new ComparableStack(ModBlocks.machine_lithium_battery, 5),
					new ComparableStack(ModBlocks.steel_scaffold, 24),
					new OreDictStack("ingotSteel", 32),
					new ComparableStack(ModItems.ingot_polymer, 24),
					new ComparableStack(ModItems.circuit_gold, 5),
					new ComparableStack(ModItems.upgrade_power_3, 3),
					new ComparableStack(ModItems.circuit_tantalium, 100),
				}, 200);
		}
		makeRecipe(new ComparableStack(ModItems.missile_inferno, 1), new AStack[] {new ComparableStack(ModItems.warhead_incendiary_large, 1), new ComparableStack(ModItems.fuel_tank_large, 1), new ComparableStack(ModItems.thruster_large, 1), new OreDictStack("plateTitanium", 14), new OreDictStack("plateSteel", 20), new OreDictStack("plateAluminum", 12), new ComparableStack(ModItems.circuit_targeting_tier3, 1), },350);
		makeRecipe(new ComparableStack(ModItems.warhead_volcano, 1), new AStack[] {new OreDictStack("plateTitanium", 24), new OreDictStack("plateSteel", 16), new ComparableStack(ModBlocks.det_nuke, 3), new OreDictStack("blockUranium238", 24), new ComparableStack(ModItems.circuit_tantalium, 5) }, 600);
		
		makeRecipe(new ComparableStack(ModBlocks.machine_bat9000, 1), new AStack[] {new OreDictStack("plateSteel", 16), new ComparableStack(ModItems.ingot_tcalloy, 16), new ComparableStack(ModBlocks.steel_scaffold, 16), new ComparableStack(ModItems.oil_tar, 16), },150);
		makeRecipe(new ComparableStack(ModBlocks.machine_orbus, 1), new AStack[] {new OreDictStack("ingotSteel", 12), new ComparableStack(ModItems.ingot_tcalloy, 12), new OreDictStack("plateSaturnite", 12), new ComparableStack(ModItems.coil_advanced_alloy, 12), new ComparableStack(ModItems.battery_sc_polonium, 1) }, 200);
		
		makeRecipe(new ComparableStack(ModBlocks.large_vehicle_door, 1), new AStack[]{new OreDictStack("plateSteel", 36), new OreDictStack("plateAdvancedAlloy", 4), new ComparableStack(ModItems.plate_polymer, 2), new OreDictStack("blockSteel", 4), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.bolt_dura_steel, 12), new OreDictStack("dyeGreen", 4)}, 500);
		makeRecipe(new ComparableStack(ModBlocks.water_door, 1), new AStack[]{new OreDictStack("plateSteel", 12), new OreDictStack("plateAdvancedAlloy", 2), new ComparableStack(ModItems.bolt_dura_steel, 2), new OreDictStack("dyeRed", 1)}, 500);
		makeRecipe(new ComparableStack(ModBlocks.qe_containment, 1), new AStack[]{new OreDictStack("plateSteel", 24), new OreDictStack("plateAdvancedAlloy", 8), new ComparableStack(ModItems.plate_polymer, 8), new OreDictStack("blockSteel", 2), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.bolt_dura_steel, 16), new OreDictStack("dyeBlack", 4)}, 500);
		makeRecipe(new ComparableStack(ModBlocks.qe_sliding_door, 1), new AStack[]{new OreDictStack("plateSteel", 12), new ComparableStack(ModItems.plate_polymer, 2), new OreDictStack("blockSteel", 1), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.bolt_dura_steel, 2), new OreDictStack("dyeWhite", 4), new ComparableStack(Blocks.GLASS, 4)}, 200);
		makeRecipe(new ComparableStack(ModBlocks.fire_door, 1), new AStack[]{new OreDictStack("plateSteel", 36), new ComparableStack(ModItems.ingot_asbestos, 12), new ComparableStack(ModItems.plate_polymer, 6), new OreDictStack("blockSteel", 4), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.bolt_dura_steel, 6), new OreDictStack("dyeRed", 8)}, 500);
		makeRecipe(new ComparableStack(ModBlocks.small_hatch, 1), new AStack[]{new OreDictStack("plateSteel", 8), new OreDictStack("plateAdvancedAlloy", 2), new ComparableStack(ModItems.bolt_dura_steel, 1), new ComparableStack(ModBlocks.brick_concrete, 1), new ComparableStack(ModBlocks.ladder_red, 1)}, 200);
		makeRecipe(new ComparableStack(ModBlocks.round_airlock_door, 1), new AStack[]{new OreDictStack("plateSteel", 32), new OreDictStack("plateAdvancedAlloy", 12), new ComparableStack(ModItems.plate_polymer, 12), new OreDictStack("blockSteel", 6), new ComparableStack(ModItems.motor, 6), new ComparableStack(ModItems.bolt_dura_steel, 12), new OreDictStack("dyeGreen", 4)}, 500);
		makeRecipe(new ComparableStack(ModBlocks.secure_access_door, 1), new AStack[]{new OreDictStack("plateSteel", 48), new OreDictStack("plateAdvancedAlloy", 16), new ComparableStack(ModItems.plate_polymer, 2), new OreDictStack("blockSteel", 6), new ComparableStack(ModItems.motor, 4), new ComparableStack(ModItems.bolt_dura_steel, 24), new OreDictStack("dyeRed", 8)}, 1000);
		makeRecipe(new ComparableStack(ModBlocks.sliding_seal_door, 1), new AStack[]{new OreDictStack("plateSteel", 12), new OreDictStack("plateAdvancedAlloy", 4), new ComparableStack(ModItems.plate_polymer, 2), new OreDictStack("blockSteel", 1), new ComparableStack(ModItems.motor, 2), new ComparableStack(ModItems.bolt_dura_steel, 2), new OreDictStack("dyeWhite", 2)}, 500);
		makeRecipe(new ComparableStack(ModBlocks.transition_seal, 1), new AStack[]{new ComparableStack(ModBlocks.cmb_brick_reinforced, 16), new OreDictStack("plateSteel", 64), new OreDictStack("plateAdvancedAlloy", 40), new ComparableStack(ModItems.plate_polymer, 36), new OreDictStack("blockSteel", 24), new ComparableStack(ModItems.motor_desh, 16), new ComparableStack(ModItems.bolt_dura_steel, 12), new OreDictStack("dyeYellow", 4)}, 5000);
		
		makeRecipe(new ComparableStack(ModBlocks.control0, 1), new AStack[]{new ComparableStack(ModItems.circuit_targeting_tier5), new OreDictStack("blockSteel", 1), new ComparableStack(ModItems.wire_copper, 24), new ComparableStack(ModBlocks.pole_top)}, 100);
		makeRecipe(new ComparableStack(ModBlocks.railgun_plasma, 1), new AStack[]{new OreDictStack("plateSteel", 24), new ComparableStack(ModItems.hull_big_steel, 2), new ComparableStack(ModItems.hull_small_steel, 6), new ComparableStack(ModItems.pipes_steel, 2), new ComparableStack(ModBlocks.machine_lithium_battery, 4), new ComparableStack(ModItems.coil_copper, 16), new ComparableStack(ModItems.coil_copper_torus, 8), new ComparableStack(ModItems.plate_desh, 4), new ComparableStack(ModItems.circuit_targeting_tier4, 4), new ComparableStack(ModItems.circuit_targeting_tier3, 2), new OreDictStack("ingotPolymer", 4)}, 500);
		
		/// HIDDEN ///
		hidden.add(new ComparableStack(ModBlocks.machine_radgen, 1));
	}

	public static void addTantalium(ComparableStack out, int amount) {
		
		AStack[] ins = recipes.get(out);
		
		if(ins != null) {
			
			AStack[] news = new AStack[ins.length + 1];
			
			for(int i = 0; i < ins.length; i++)
				news[i] = ins[i];
			
			news[news.length - 1] = new ComparableStack(ModItems.circuit_tantalium, amount);
			
			recipes.put(out, news);
		}
	}
	
	public static void makeRecipe(ComparableStack out, AStack[] in, int duration) {

		if(out == null || Item.REGISTRY.getNameForObject(out.item) == null) {
			MainRegistry.logger.error("Canceling assembler registration, item was null!");
			return;
		}

		recipes.put(out, in);
		time.put(out, duration);
	}

	public static void loadRecipesFromConfig() {
		itemRegistry = GameRegistry.findRegistry(Item.class);
		blockRegistry = GameRegistry.findRegistry(Block.class);
		
		File recipeConfig = new File(MainRegistry.proxy.getDataDir().getPath() + "/config/hbm/assemblerConfig.cfg");
		if (!recipeConfig.exists())
			try {
				recipeConfig.getParentFile().mkdirs();
				FileWriter write = new FileWriter(recipeConfig);
				write.write("# Format: time;itemName,meta,amount|nextItemName,meta,amount;productName,meta,amount\n"
						  + "# One line per recipe.\n"
						  + "# For an oredict input item, replace the mod id with oredict, like oredict:plateSteel. These do not require metatdata\n"
						  + "# Example for iron plates: 30;minecraft:iron_ingot,0,3;oredict:plateIron,2\n"
						  + "# For an NBT item, use a 4th item parameter with the nbt string of the tag.\n"
						  + "# The NBT string format is the same as used in commands\n"
						  + "# Example for turning kerosene canisters into steel plates:\n"
						  + "# 20;hbm:canister_fuel,0,2,{HbmFluidKey:{FluidName:\"kerosene\",Amount:1000}};hbm:plate_steel,0,32\n"
						  + "#\n"
						  + "# To remove a recipe, use the format: \n"
						  + "# remove hbm:plate_iron,0,2\n"
						  + "# This will remove any recipe with the output of two iron plates");
				addConfigRecipes(write);
				write.close();
				
			} catch (IOException e) {
				MainRegistry.logger.log(Level.ERROR, "ERROR: Could not create config file: " + recipeConfig.getAbsolutePath());
				e.printStackTrace();
				return;
			}
		
		
		
		BufferedReader read = null;
		try {
			read = new BufferedReader(new FileReader(recipeConfig));
			String currentLine = null;
			int lineCount = 0;
			
			while((currentLine = read.readLine()) != null){
				lineCount ++;
				if(currentLine.startsWith("#") || currentLine.length() == 0)
					continue;
				if(currentLine.startsWith("remove"))
					parseRemoval(currentLine, lineCount);
				else
					parseRecipe(currentLine, lineCount);
			}
		} catch (FileNotFoundException e) {
			MainRegistry.logger.log(Level.ERROR, "Could not find assembler config file! This should never happen.");
			e.printStackTrace();
		} catch (IOException e){
			MainRegistry.logger.log(Level.ERROR, "Error reading assembler config!");
			e.printStackTrace();
		} finally {
			if(read != null)
				try {
					read.close();
				} catch (IOException e) {}
		}
		
	}
	
	public static void parseRemoval(String currentLine, int line){
		String[] parts = currentLine.split(" ");
		if(parts.length != 2){
			MainRegistry.logger.log(Level.WARN, "Could not parse assembler recipe removal on line " + line + ": does not have two parts. Skipping...");
			return;
		}
		AStack stack = parseAStack(parts[1], 64);
		if(stack == null){
			MainRegistry.logger.log(Level.WARN, "Could not parse assembler output itemstack from \"" + parts[1] + "\" on line " + line + ". Skipping...");
			return;
		}
		if(!(stack instanceof ComparableStack)){
			MainRegistry.logger.log(Level.WARN, "Oredict stacks are not allowed as assembler outputs! Line number: " + line + " Skipping...");
			return;
		}
		ComparableStack cStack = (ComparableStack) stack;
		recipes.remove(cStack);
		time.remove(cStack);
		recipeList.remove(cStack);
	}

	private static void parseRecipe(String currentLine, int line) {
		String[] parts = currentLine.split(";");
		if(parts.length != 3){
			MainRegistry.logger.log(Level.WARN, "Could not parse assembler recipe on line " + line + ": does not have three parts. Skipping...");
			return;
		}
		int recipeTime = 0;
		try {
			recipeTime = Integer.parseInt(parts[0]);
		} catch (NumberFormatException e){
			MainRegistry.logger.log(Level.WARN, "Could not parse assembler process time from \"" + parts[0] + "\" on line " + line + ". Skipping...");
			return;
		}
		List<AStack> input = new ArrayList<>();
		for(String s : parts[1].split("\\|")){
			AStack stack = parseAStack(s, 12*64);
			if(stack == null){
				MainRegistry.logger.log(Level.WARN, "Could not parse assembler input itemstack from \"" + s + "\" on line " + line + ". Skipping...");
				return;
			}
			input.add(stack);
		}
		AStack output = parseAStack(parts[2], 64);
		if(output == null){
			MainRegistry.logger.log(Level.WARN, "Could not parse assembler output itemstack from \"" + parts[2] + "\" on line " + line + ". Skipping...");
			return;
		}
		if(!(output instanceof ComparableStack)){
			MainRegistry.logger.log(Level.WARN, "Oredict stacks are not allowed as assembler outputs! Line number: " + line + " Skipping...");
			return;
		}
		if(recipes.containsKey(output)){
			MainRegistry.logger.log(Level.WARN, "Found duplicate assembler recipe outputs! This is not allowed! Line number: " + line + " Skipping...");
		}
		recipes.put((ComparableStack) output, input.toArray(new AStack[input.size()]));
		time.put((ComparableStack) output, recipeTime);
		recipeList.add((ComparableStack) output);
	}
	
	//The whole point of these two methods below is to ignore the part inside braces for nbt tags.
	//I'm sure there's a cleaner way to do this, but I'm not going to spend more time trying to figure it out.
	private static String readSplitPart(int idx, String s){
		StringBuilder build = new StringBuilder();
		int braceCount = 0;
		for(int i = idx; i < s.length(); i ++){
			char c = s.charAt(i);
			if(c == '{'){
				braceCount ++;
			} else if(c == '}'){
				braceCount --;
			}
			if(braceCount == 0 && (c == '|' || c == ',' || c == ';'))
				break;
			build.append(c);
		}
		if(build.length() == 0)
			return null;
		return build.toString();
	}
	
	private static String[] splitStringIgnoreBraces(String s){
		List<String> list = new ArrayList<>();
		int idx = 0;
		while(idx < s.length()){
			String part = readSplitPart(idx, s);
			if(part != null)
				list.add(part);
			else
				break;
			if(list.size() >= 4)
				break;
			idx += part.length()+1;
		}
		return list.toArray(new String[list.size()]);
	}

	private static AStack parseAStack(String s, int maxSize){
		String[] parts = splitStringIgnoreBraces(s);
		if(parts.length == 3 || parts.length == 4){
			Block block = null;
			Item item = itemRegistry.getValue(new ResourceLocation(parts[0]));
			if(item == null)
				block = blockRegistry.getValue(new ResourceLocation(parts[0]));
			if(item == null && block == null){
				MainRegistry.logger.log(Level.WARN, "Could not parse item or block from \"" + parts[0] + "\", probably isn't a registered item. Skipping...");
				return null;
			}
			int meta = 0;
			try {
				meta = Integer.parseInt(parts[1]);
			} catch (NumberFormatException e) {
				MainRegistry.logger.log(Level.WARN, "Could not parse item metadata from \"" + parts[1] + "\". Skipping...");
				return null;
			}
			if(meta < 0){
				MainRegistry.logger.log(Level.WARN, "Bad item meta: " + meta + ". Skipping...");
				return null;
			}
			int amount = 0;
			try {
				amount = Integer.parseInt(parts[2]);
			} catch (NumberFormatException e){
				MainRegistry.logger.log(Level.WARN, "Could not parse item amount from \"" + parts[2] + "\". Skipping...");
				return null;
			}
			if(amount < 0 || amount > maxSize){
				MainRegistry.logger.log(Level.WARN, "Bad item amount: " + amount + ". Skipping...");
				return null;
			}
			if(parts.length == 4){
				String name = parts[3];
				name.trim();
				NBTTagCompound tag = parseNBT(name);
				if(tag == null){
					MainRegistry.logger.log(Level.WARN, "Failed to parse NBT tag: " + parts[3] + ". Skipping...");
					return null;
				}
				ItemStack stack;
				if(item == null)
					stack = new ItemStack(block, amount, meta);
				else
					stack = new ItemStack(item, amount, meta);
				stack.setTagCompound(tag);
				return new NbtComparableStack(stack);
			} else {
				if(item == null)
					return new ComparableStack(block, amount, meta);
				return new ComparableStack(item, amount, meta);
			}
		}
		if(parts.length == 2){
			String[] ore = parts[0].split(":");
			if(ore.length == 2 && ore[0].equals("oredict")){
				String name = ore[1];
				int amount = 0;
				try {
					amount = Integer.parseInt(parts[1]);
				} catch (NumberFormatException e){
					MainRegistry.logger.log(Level.WARN, "Could not parse item amount from \"" + parts[1] + "\". Skipping...");
					return null;
				}
				if(amount < 0 || amount > 12*64){
					MainRegistry.logger.log(Level.WARN, "Bad item amount: " + amount + ". Skipping...");
					return null;
				}
				return new OreDictStack(name, amount);
			} else {
				MainRegistry.logger.log(Level.WARN, "Could not parse ore dict name from \"" + parts[0] + "\". Skipping...");
			}
		}
		return null;
	}
	
	private static NBTTagCompound parseNBT(String json){
		try {
			return JsonToNBT.getTagFromJson(json);
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	private static void addConfigRecipes(FileWriter write) throws IOException {
			write.write("\n");
	}
}