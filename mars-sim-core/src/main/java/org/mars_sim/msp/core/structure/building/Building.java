/**
 * Mars Simulation Project
 * Building.java
 * @version 3.1.0 2017-09-04
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.building;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.LogConsolidated;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.NaturalAttributeType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.task.Maintenance;
import org.mars_sim.msp.core.person.ai.task.Repair;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.BuildingTemplate;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.Structure;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.connection.InsidePathLocation;
import org.mars_sim.msp.core.structure.building.function.Administration;
import org.mars_sim.msp.core.structure.building.function.AstronomicalObservation;
import org.mars_sim.msp.core.structure.building.function.BuildingAirlock;
import org.mars_sim.msp.core.structure.building.function.BuildingConnection;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.Communication;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.structure.building.function.EarthReturn;
import org.mars_sim.msp.core.structure.building.function.Exercise;
import org.mars_sim.msp.core.structure.building.function.FoodProduction;
import org.mars_sim.msp.core.structure.building.function.Function;
import org.mars_sim.msp.core.structure.building.function.GroundVehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.HeatMode;
import org.mars_sim.msp.core.structure.building.function.Heating;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;
import org.mars_sim.msp.core.structure.building.function.LivingAccommodations;
import org.mars_sim.msp.core.structure.building.function.Management;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.structure.building.function.PowerGeneration;
import org.mars_sim.msp.core.structure.building.function.PowerMode;
import org.mars_sim.msp.core.structure.building.function.PowerStorage;
import org.mars_sim.msp.core.structure.building.function.Recreation;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.structure.building.function.ResourceProcessing;
import org.mars_sim.msp.core.structure.building.function.RoboticStation;
import org.mars_sim.msp.core.structure.building.function.Storage;
import org.mars_sim.msp.core.structure.building.function.SystemType;
import org.mars_sim.msp.core.structure.building.function.ThermalGeneration;
import org.mars_sim.msp.core.structure.building.function.VehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.WasteDisposal;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.structure.building.function.cooking.Dining;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.structure.building.function.farming.Farming;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * The Building class is a settlement's building.
 */
public class Building extends Structure implements Malfunctionable, Indoor, // Comparable<Building>,
		LocalBoundedObject, InsidePathLocation, Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	// default logger.
	private static Logger logger = Logger.getLogger(Building.class.getName());

	private static String sourceName = logger.getName().substring(logger.getName().lastIndexOf(".") + 1,
			logger.getName().length());
	
	public static String TYPE = SystemType.BUILDING.getName();
	
	public static final int TISSUE_CAPACITY = 20;
	/** The height of an airlock in meters */
	// Assume an uniform height of 2.5 meters in all buildings
	public static final double HEIGHT = 2.5; 
	/** The volume of an airlock in cubic meters */
	public static final double AIRLOCK_VOLUME_IN_CM = BuildingAirlock.AIRLOCK_VOLUME_IN_CM; // 3 * 2 * 2; //in m^3
	/** 500 W heater for use during EVA ingress */
	public static final double kW_EVA_HEATER = .5D; //
	// Assuming 20% chance for each person to witness or be conscious of the
	// meteorite impact in an affected building
	public static final double METEORITE_IMPACT_PROBABILITY_AFFECTED = 20;

	// The influx of meteorites entering Mars atmosphere can be estimated as
	// log N = -0.689* log(m) + 4.17
	// N is the number of meteorites per year having masses greater than m grams
	// incident
	// on an area of 10^6 km2 (Bland and Smith, 2000).
	// see initial implementation in MeteoriteImpactImpl class

	/**
	 * The thickness of the Aluminum wall of a building in meter. Typically between
	 * 10^-5 (.00001) and 10^-2 (.01) [in m]
	 */
	public static final double WALL_THICKNESS_ALUMINUM = 0.0000254;

	// inflatable greenhouse : 4.815E-4 or 0.0004815
	// large greenhouse : 9.63E-4 or 0.000963

	/** The thickness of the wall of a greenhouse building in meter */
	public static double wall_thickness_inflatable;// = 0.0000211;
	/**
	 * The safety factor when determining the wall/canopy thickness for an
	 * inflatable greenhouse.
	 */
	private static double safety_factor = 1.5D;
	/**
	 * The design pressure when determining the wall/canopy thickness for an
	 * inflatable greenhouse.
	 */
	private static double design_pressure = 14.7 - 4; // [in psi]
	/** The diameter of the canopy thickness for an inflatable greenhouse. */
	private static double diameter;
	/**
	 * The tensile strength of the composite material when determining the
	 * wall/canopy thickness for an inflatable greenhouse.
	 */
	private static double kevlar_tensile_strength = 100000; // [in psi] assume kevlar 49/epoxy

	// Note : the typical values of penetrationThicknessOnAL for a 1 g/cm^3, 1 km/s
	// meteorite can be .0010 to 0.0022 meter

	// Loaded wearLifeTime, maintenanceTime, roomTemperature from buildings.xml
	/** Default : 3340 Sols (5 orbits). */
	private int wearLifeTime = 3340000;
	/** Default : 50 millisols maintenance time. */
	private int maintenanceTime = 50;
	/** Default : 22.5 deg celsius. */
	private double initialTemperature = 22.5D;
	// public double GREENHOUSE_TEMPERATURE = 24D;

	// Data members
	/** The cache for msols */
	private int msolCache;
	/**
	 * Unique template id assigned for the settlement template that this building
	 * belong
	 */
	protected int templateID;
	protected int inhabitableID = -1;
	protected int baseLevel;
	private int solCache = 0;

	protected double width;
	protected double length;
	protected double floorArea;
	protected double xLoc;
	protected double yLoc;
	protected double facing;
	protected double basePowerRequirement;
	protected double basePowerDownPowerRequirement;
	protected double powerNeededForEVAheater;

	boolean isImpactImminent = false;
	/** Checked by getAllImmovableBoundedObjectsAtLocation() in LocalAreaUtil */
	boolean inTransportMode = true;

	/** Type of building. */
	protected String buildingType;
	/** Nick name for this building. */
	private String nickName;
	/** Description for this building. */
	private String description;

	/** A list of functions of this building. */
	protected List<Function> functions;
	// private List<BuildingKit> buildingKit;
//	private Map<Integer, ItemResource> itemMap = new HashMap<Integer, ItemResource>();

	/** Unit location coordinates. */
	private Coordinates location;
	
	protected BuildingManager manager;
	protected MalfunctionManager malfunctionManager;

	private Inventory inv;
	private Settlement settlement;
	
	private transient ThermalGeneration furnace;
	private transient PowerGeneration powerGen;
	private transient PowerStorage powerStorage;
	private transient LifeSupport lifeSupport;
	private transient RoboticStation roboticStation;
	private transient Heating heating;
	private transient EVA eva;
	private transient Farming farm;
	private transient LivingAccommodations livingAccommodations;
	private transient PreparingDessert preparingDessert;
	private transient Cooking cooking;
	private transient Management management;
	private transient MedicalCare medical;
	private transient WasteDisposal waste;
	private transient VehicleMaintenance garage;
	private transient FoodProduction foodFactory;
	private transient ResourceProcessing processing;
	private transient Research lab;
	private transient Manufacture workshop;
	private transient Administration admin;
	private transient Recreation rec;
	private transient Dining dine;
	private transient GroundVehicleMaintenance maint;
	private transient AstronomicalObservation astro;
	private transient Exercise gym;

	private static MarsClock marsClock;
	private static MasterClock masterClock;
	private static BuildingConfig buildingConfig;
	private static Malfunction malfunctionMeteoriteImpact;

	protected PowerMode powerModeCache;
	protected HeatMode heatModeCache;

	// private DecimalFormat fmt = new DecimalFormat("###.####");

	private static Set<AmountResource> tissues = SimulationConfig.instance().getResourceConfiguration()
			.getTissueCultures();

	/**
	 * Constructor 1. Constructs a Building object.
	 * 
	 * @param template the building template.
	 * @param manager  the building's building manager.
	 * @throws BuildingException if building can not be created.
	 */
	public Building(BuildingTemplate template, BuildingManager manager) {
		this(template.getID(), template.getBuildingType(), template.getNickName(), template.getWidth(),
				template.getLength(), template.getXLoc(), template.getYLoc(), template.getFacing(), manager);
		// logger.info("Building's constructor 1 is on " +
		// Thread.currentThread().getName() + " Thread");

		this.manager = manager;
		this.settlement = manager.getSettlement();
		this.location = manager.getSettlement().getCoordinates();
		this.buildingType = template.getBuildingType();
		inv = settlement.getInventory();

		// Set the instance of life support
		// NOTE: needed for setting inhabitable id
		if (hasFunction(FunctionType.LIFE_SUPPORT)) {
			if (lifeSupport == null) {
				lifeSupport = (LifeSupport) getFunction(FunctionType.LIFE_SUPPORT);
				// Set up an inhabitable_building id for tracking composition of air
				int id = manager.getNextInhabitableID();
				setInhabitableID(id);
			}
		}

//		// Set the instance of thermal generation function.
//		if (hasFunction(FunctionType.POWER_GENERATION))
//			if (powerGen == null)
//				powerGen = (PowerGeneration) getFunction(FunctionType.POWER_GENERATION);
//
//		// Set the instance of thermal generation function.
//		if (hasFunction(FunctionType.THERMAL_GENERATION))
//			if (furnace == null)
//				furnace = (ThermalGeneration) getFunction(FunctionType.THERMAL_GENERATION);
//		// if (heating == null)
//		// heating = furnace.getHeating();
//
//		// Set the instance of power storage function.
//		if (hasFunction(FunctionType.POWER_STORAGE))
//			if (powerStorage == null)
//				powerStorage = (PowerStorage) getFunction(FunctionType.POWER_STORAGE);
//
//		// Set the instance of robotic station function.
//		if (hasFunction(FunctionType.ROBOTIC_STATION))
//			if (roboticStation == null)
//				roboticStation = (RoboticStation) getFunction(FunctionType.ROBOTIC_STATION);
//
//		// Set the instance of eva function.
//		if (hasFunction(FunctionType.EVA))
//			if (eva == null)
//				eva = (EVA) getFunction(FunctionType.EVA);
//
//		// Set the instance of farming function.
//		if (hasFunction(FunctionType.FARMING))
//			if (farm == null)
//				farm = (Farming) getFunction(FunctionType.FARMING);
//
//		if (hasFunction(FunctionType.LIVING_ACCOMODATIONS))
//			if (livingAccommodations == null)
//				livingAccommodations = (LivingAccommodations) getFunction(FunctionType.LIVING_ACCOMODATIONS);

	}

	/**
	 * Constructor 2 Constructs a Building object.
	 * 
	 * @param id           the building's unique ID number.
	 * @param buildingType the building Type.
	 * @param nickName     the building's nick name.
	 * @param w            the width (meters) of the building or -1 if not set.
	 * @param l            the length (meters) of the building or -1 if not set.
	 * @param xLoc         the x location of the building in the settlement.
	 * @param yLoc         the y location of the building in the settlement.
	 * @param facing       the facing of the building (degrees clockwise from
	 *                     North).
	 * @param manager      the building's building manager.
	 * @throws BuildingException if building can not be created.
	 */
	public Building(int id, String buildingType, String nickName, double w, double l, double xLoc, double yLoc,
			double facing, BuildingManager manager) {
		super(nickName, manager.getSettlement().getCoordinates());
		// logger.info("Building's constructor 2 is on " +
		// Thread.currentThread().getName() + " Thread");
		
		// Place it within a settlement
//		enter(LocationCodeType.SETTLEMENT);
		
		this.templateID = id;
		this.buildingType = buildingType;
		this.nickName = nickName;
		this.manager = manager;
		this.settlement = manager.getSettlement();
		inv = settlement.getInventory();

		this.xLoc = xLoc;
		this.yLoc = yLoc;
		this.facing = facing;
		this.location = manager.getSettlement().getCoordinates();

		buildingConfig = SimulationConfig.instance().getBuildingConfiguration();
		malfunctionMeteoriteImpact = MalfunctionFactory
				.getMeteoriteImpactMalfunction(MalfunctionFactory.METEORITE_IMPACT_DAMAGE);

//		if (inv == null)
//			inv = settlement.getInventory();
//		if (s_inv == null)
//			s_inv = settlement.getInventory();
//		if (b_inv == null) {
//			b_inv = super.getInventory(); // it's already been created in its super class

//		if (buildingType.equalsIgnoreCase("hallway") || buildingType.equalsIgnoreCase("tunnel")) {
//			//b_inv = new Inventory(this);
//			b_inv.addGeneralCapacity(100);
//		} 
//		else if (this.getBuildingType().toLowerCase().contains("greenhouse"))
//			b_inv.addGeneralCapacity(1_000_000);
//		else
//			b_inv.addGeneralCapacity(100_000);
//		}

		if (buildingType.toLowerCase().contains("greenhouse")) {

			if (buildingType.equalsIgnoreCase("inflatable greenhouse"))
				diameter = 6;
			else if (buildingType.equalsIgnoreCase("inground greenhouse"))
				diameter = 5;
			else if (buildingType.equalsIgnoreCase("large greenhouse"))
				diameter = 12;

			wall_thickness_inflatable = diameter * safety_factor * design_pressure / (2 * kevlar_tensile_strength);
			// System.out.println("wall_thickness_inflatable : " +
			// wall_thickness_inflatable);
			// inflatable greenhouse : 4.815E-4 or 0.0004815
			// large greenhouse : 9.63E-4 or 0.000963
		}

		masterClock = Simulation.instance().getMasterClock();
		marsClock = masterClock.getMarsClock();

		powerModeCache = PowerMode.FULL_POWER;
		heatModeCache = HeatMode.HALF_HEAT;

//		// Get building's dimensions.
//		if (width != -1D) {
//			this.width = width;
//		}
//		else {
//			this.width = buildingConfig.getWidth(buildingType);
//		}
//		
//		if (this.width <= 0D) {
//			throw new IllegalStateException("Invalid building width: " + this.width + " m. for new building " + buildingType);
//		}
//
//		if (length != -1D) {
//			this.length = length;
//		}
//		else {
//			this.length = buildingConfig.getLength(buildingType);
//		}
//		
//		if (this.length <= 0D) {
//			throw new IllegalStateException("Invalid building length: " + this.length + " m. for new building " + buildingType);
//		}

		if (buildingType.toLowerCase().contains("hallway") || buildingType.toLowerCase().contains("tunnel")) {
			length = l;
			width = buildingConfig.getWidth(buildingType);
			// logger.info(nickName + "'s length and width : " + length + " x " + width);
		} else {
			width = buildingConfig.getWidth(buildingType);
			length = buildingConfig.getLength(buildingType);
		}

		floorArea = length * width;

		baseLevel = buildingConfig.getBaseLevel(buildingType);
		description = buildingConfig.getDescription(buildingType);

		// Get the building's functions
		functions = determineFunctions();

		// Get base power requirements.
		basePowerRequirement = buildingConfig.getBasePowerRequirement(buildingType);
		basePowerDownPowerRequirement = buildingConfig.getBasePowerDownPowerRequirement(buildingType);
		wearLifeTime = buildingConfig.getWearLifeTime(buildingType);
		maintenanceTime = buildingConfig.getMaintenanceTime(buildingType);

		// Set room temperature
		initialTemperature = buildingConfig.getRoomTemperature(buildingType);

		// Determine total maintenance time.
		double totalMaintenanceTime = maintenanceTime;
		Iterator<Function> j = functions.iterator();
		while (j.hasNext()) {
			Function function = j.next();
			totalMaintenanceTime += function.getMaintenanceTime();
		}

		// Set up malfunction manager.
		malfunctionManager = new MalfunctionManager(this, wearLifeTime, totalMaintenanceTime);
		// Add scope to malfunction manager.
		malfunctionManager.addScopeString(TYPE);

		// Add each function to the malfunction scope.
		// e.g. malfunctionManager.addScopeString(FunctionType.LIFE_SUPPORT.getName());
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			Function function = i.next();
			int size = function.getMalfunctionScopeStrings().length;
			for (int x = 0; x < size; x++) {
				malfunctionManager.addScopeString(function.getMalfunctionScopeStrings()[x]);
			}
			// malfunctionManager.addScopeString(function.getFunctionType().getName());
		}
		
		
//		for (Function f : functions)
//			for (String s : f.getMalfunctionScopeStrings())
//				malfunctionManager.addScopeString(f.getMalfunctionScopeStrings()[s]);
		

		// Initialize lab space for storing crop tissue cultures
		if (hasFunction(FunctionType.RESEARCH) && getResearch().hasSpecialty(ScienceType.BOTANY)) {
			lab = getResearch();
			// Set<AmountResource> tissues =
			// SimulationConfig.instance().getResourceConfiguration().getTissueCultures();
			for (AmountResource ar : tissues) {
				getInventory().addAmountResourceTypeCapacity(ar, TISSUE_CAPACITY);
				getInventory().storeAmountResource(ar, .1, false);
				getInventory().addAmountDemand(ar, .1);
			}
		}
	}

	/** Constructor 3 (for use by Mock Building in Unit testing) */
	protected Building(BuildingManager manager) {
		super("Mock Building", new Coordinates(0D, 0D));
		// Place it in a settlement
//		enter(LocationCodeType.SETTLEMENT);
	}

	/**
	 * Gets the settlement inventory of this building.
	 * 
	 * @return inventory
	 */
	public Inventory getSettlementInventory() {
		return inv;
	}

	/**
	 * Gets the settlement inventory of this building.
	 * 
	 * @return inventory
	 */
	public Inventory getInventory() {
		return inv;
	}

	/**
	 * Gets the description of a building.
	 * 
	 * @return String description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets building nickname
	 * 
	 * @param nick name
	 */
	public void setBuildingNickName(String name) {
		this.nickName = name;
	}

	/**
	 * Gets the initial temperature of a building.
	 * 
	 * @return temperature (deg C)
	 */
	public double getInitialTemperature() {
		return initialTemperature;
	}

	public LifeSupport getLifeSupport() {
		if (lifeSupport == null)
			lifeSupport = (LifeSupport) getFunction(FunctionType.LIFE_SUPPORT);
		return lifeSupport;
	}

	public RoboticStation getRoboticStation() {
		if (roboticStation == null)
			roboticStation = (RoboticStation) getFunction(FunctionType.ROBOTIC_STATION);

		return roboticStation;
	}

	public ThermalGeneration getThermalGeneration() {
		if (furnace == null)
			furnace = (ThermalGeneration) getFunction(FunctionType.THERMAL_GENERATION);
		return furnace;
	}

	public Farming getFarming() {
		if (farm == null)
			farm = (Farming) getFunction(FunctionType.FARMING);
		return farm;
	}

	public EVA getEVA() {
		// if (hasFunction(BuildingFunction.EVA))
		// eva = (EVA) getFunction(BuildingFunction.EVA);
		// else
		// return null;
		if (eva == null)
			eva = (EVA) getFunction(FunctionType.EVA);
		return eva;
	}

	public PowerGeneration getPowerGeneration() {
		if (powerGen == null)
			powerGen = (PowerGeneration) getFunction(FunctionType.POWER_GENERATION);
		return powerGen;
	}

	public PowerStorage getPowerStorage() {
		if (powerStorage == null)
			powerStorage = (PowerStorage) getFunction(FunctionType.POWER_STORAGE);
		return powerStorage;
	}

	public LivingAccommodations getLivingAccommodations() {
		if (livingAccommodations == null)
			livingAccommodations = (LivingAccommodations) getFunction(FunctionType.LIVING_ACCOMODATIONS);
		return livingAccommodations;
	}

	public PreparingDessert getPreparingDessert() {
		if (preparingDessert == null)
			preparingDessert = (PreparingDessert) getFunction(FunctionType.PREPARING_DESSERT);
		return preparingDessert;
	}

	public Cooking getCooking() {
		if (cooking == null)
			cooking = (Cooking) getFunction(FunctionType.COOKING);
		return cooking;
	}

	public MedicalCare getMedical() {
		if (medical == null)
			medical = (MedicalCare) getFunction(FunctionType.MEDICAL_CARE);
		return medical;
	}

	public WasteDisposal getWaste() {
		if (waste == null)
			waste = (WasteDisposal) getFunction(FunctionType.WASTE_DISPOSAL);
		return waste;
	}

	public VehicleMaintenance getVehicleMaintenance() {
		if (garage == null)
			garage = (VehicleMaintenance) getFunction(FunctionType.GROUND_VEHICLE_MAINTENANCE);
		return garage;
	}

	public FoodProduction getFoodProduction() {
		if (foodFactory == null)
			foodFactory = (FoodProduction) getFunction(FunctionType.FOOD_PRODUCTION);
		return foodFactory;
	}

	public ResourceProcessing getResourceProcessing() {
		if (processing == null)
			processing = (ResourceProcessing) getFunction(FunctionType.RESOURCE_PROCESSING);
		return processing;
	}

	public Research getResearch() {
		if (lab == null)
			lab = (Research) getFunction(FunctionType.RESEARCH);
		return lab;
	}

	public Management getManagement() {
		if (management == null)
			management = (Management) getFunction(FunctionType.MANAGEMENT);
		return management;
	}
	
	public Manufacture getManufacture() {
		if (workshop == null)
			workshop = (Manufacture) getFunction(FunctionType.MANUFACTURE);
		return workshop;
	}

	public Administration getAdministration() {
		if (admin == null)
			admin = (Administration) getFunction(FunctionType.ADMINISTRATION);
		return admin;
	}

	public Recreation getRecreation() {
		if (rec == null)
			rec = (Recreation) getFunction(FunctionType.RECREATION);
		return rec;
	}

	public Dining getDining() {
		if (dine == null)
			dine = (Dining) getFunction(FunctionType.DINING);
		return dine;
	}

	public GroundVehicleMaintenance getGroundVehicleMaintenance() {
		if (maint == null)
			maint = (GroundVehicleMaintenance) getFunction(FunctionType.GROUND_VEHICLE_MAINTENANCE);
		return maint;
	}

	public AstronomicalObservation getAstronomicalObservation() {
		if (astro == null)
			astro = (AstronomicalObservation) getFunction(FunctionType.ASTRONOMICAL_OBSERVATIONS);
		return astro;
	}

	public Exercise getExercise() {
		if (gym == null)
			gym = (Exercise) getFunction(FunctionType.EXERCISE);
		return gym;

	}

	/**
	 * Gets the temperature of a building.
	 * 
	 * @return temperature (deg C)
	 */
	public double getCurrentTemperature() {
		if (heating != null)
			return heating.getCurrentTemperature();
		else
			return initialTemperature;
	}

	/**
	 * Determines the building functions.
	 * 
	 * @return list of building .
	 * @throws Exception if error in functions.
	 */
	private List<Function> determineFunctions() {
		List<Function> buildingFunctions = new ArrayList<Function>();
		// Set<Function> buildingFunctions = new HashSet<Function>();

		// Set administration function.
		if (buildingConfig.hasAdministration(buildingType))
			buildingFunctions.add(new Administration(this));

		// Set astronomical observation function
		if (buildingConfig.hasAstronomicalObservation(buildingType))
			buildingFunctions.add(new AstronomicalObservation(this));

		// Set building connection function.
		if (buildingConfig.hasBuildingConnection(buildingType))
			buildingFunctions.add(new BuildingConnection(this));

		// Set communication function.
		if (buildingConfig.hasCommunication(buildingType))
			buildingFunctions.add(new Communication(this));

		if (buildingConfig.hasCooking(buildingType)) {
			// Set cooking function.
			buildingFunctions.add(new Cooking(this));
			// Set preparing dessert function.
			buildingFunctions.add(new PreparingDessert(this));
		}

		// Set dining function.
		if (buildingConfig.hasDining(buildingType))
			buildingFunctions.add(new Dining(this));

		// Set Earth return function.
		if (buildingConfig.hasEarthReturn(buildingType))
			buildingFunctions.add(new EarthReturn(this));
		// Set EVA function.
		// eva = new EVA(this); if (config.hasEVA(buildingType))
		// buildingFunctions.add(eva);
		if (buildingConfig.hasEVA(buildingType))
			buildingFunctions.add(new EVA(this));

		// Set exercise function.
		if (buildingConfig.hasExercise(buildingType))
			buildingFunctions.add(new Exercise(this));

		// Set farming function.
		if (buildingConfig.hasFarming(buildingType))
			buildingFunctions.add(new Farming(this));

		// Added food production
		if (buildingConfig.hasFoodProduction(buildingType))
			buildingFunctions.add(new FoodProduction(this));

		// Set ground vehicle maintenance function.
		if (buildingConfig.hasGroundVehicleMaintenance(buildingType))
			buildingFunctions.add(new GroundVehicleMaintenance(this));

		// Set life support function.
		if (buildingConfig.hasLifeSupport(buildingType))
			buildingFunctions.add(new LifeSupport(this));

		// Set living accommodations function.
		if (buildingConfig.hasLivingAccommodations(buildingType))
			buildingFunctions.add(new LivingAccommodations(this));

		// Set management function.
		if (buildingConfig.hasManagement(buildingType))
			buildingFunctions.add(new Management(this));

		// Set manufacture function.
		if (buildingConfig.hasManufacture(buildingType))
			buildingFunctions.add(new Manufacture(this));

		// Set medical care function.
		if (buildingConfig.hasMedicalCare(buildingType))
			buildingFunctions.add(new MedicalCare(this));

		// Set power generation function.
		if (buildingConfig.hasPowerGeneration(buildingType))
			buildingFunctions.add(new PowerGeneration(this));

		// Set power storage function.
		if (buildingConfig.hasPowerStorage(buildingType))
			buildingFunctions.add(new PowerStorage(this));

		// Set recreation function.
		if (buildingConfig.hasRecreation(buildingType))
			buildingFunctions.add(new Recreation(this));

		// Set research function.
		if (buildingConfig.hasResearchLab(buildingType))
			buildingFunctions.add(new Research(this));

		// Set resource processing function.
		if (buildingConfig.hasResourceProcessing(buildingType))
			buildingFunctions.add(new ResourceProcessing(this));

		// Set robotic function.
		if (buildingConfig.hasRoboticStation(buildingType))
			buildingFunctions.add(new RoboticStation(this));

		// Set storage function.
		if (buildingConfig.hasStorage(buildingType))
			buildingFunctions.add(new Storage(this));

		// Set thermal generation function.
		if (buildingConfig.hasThermalGeneration(buildingType))
			buildingFunctions.add(new ThermalGeneration(this));

		// Set thermal storage function.
		// if (config.hasThermalStorage(buildingType)) buildingFunctions.add(new
		// ThermalStorage(this));

		return buildingFunctions;
	}

	/**
	 * Checks if this building has the given functions (more than one).
	 * 
	 * @param function the name of the function.
	 * @return true if it does.
	 */
	public boolean hasFunction(FunctionType[] fts) {
		boolean result = false;
		for (Function f : functions) {
			for (FunctionType ft : fts) {
				if (f.getFunctionType() == ft) {
					result = result && true;
				}
			}
		}
		return result;
	}

	/**
	 * Checks if this building has the given functions (more than one).
	 * 
	 * @param function the enum name of the functions.
	 * @return true if it it does.
	 */
	public boolean hasFunction(FunctionType bf1, FunctionType bf2) {
		return hasFunction(new FunctionType[] { bf1, bf2 });
	}

	/**
	 * Checks if this building has a particular function.
	 * 
	 * @param function the enum name of the function.
	 * @return true if it does.
	 */
	public boolean hasFunction(FunctionType functionType) {
		boolean result = false;
		for (Function f : functions) {
			if (f.getFunctionType() == functionType) {
				return true;
			}
		}
//		functions.stream()
//		.filter((f) -> f.getFunction() == functionType)
//		.forEach((f) -> {
//        		return true;
//		});
//
//
//		Iterator<Function> i = functions.iterator();
//		while (i.hasNext()) {
//			if (i.next().getFunction() == function)
//				return true;
//		}
		return result;
	}

	/**
	 * Gets a function if the building has it.
	 * 
	 * @param functionType {@link FunctionType} the function of the building.
	 * @return function.
	 * @throws BuildingException if building doesn't have the function.
	 */
	public Function getFunction(FunctionType functionType) {
		Function result = null;

		for (Function f : functions) {
			if (f.getFunctionType() == functionType) {
				return f;
			}
		}
//		
//		 functions.forEach(f -> { if (f.getFunction() == functionType) return f; });
//		 Iterator<Function> i = functions.iterator(); while (i.hasNext()) { Function
//		 function = i.next(); if (function.getFunction() == functionType) result =
//		 function; }
//		 
		
		// if (result != null) return result;
		// else throw new IllegalStateException(buildingType + " does not have " +
		// functionType);
		return result;
	}

	/**
	 * Remove the building's functions from the settlement.
	 */
	public void removeFunctionsFromSettlement() {

		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			i.next().removeFromSettlement();
		}
	}

	public void removeFunction(Function function) {
		if (functions.contains(function)) {
			functions.remove(function);
			// Call removeOneFunctionfromBFMap()
			manager.removeOneFunctionfromBFMap(this, function);
		}
	}

	/**
	 * Gets the building's building manager.
	 * 
	 * @return building manager
	 */
	public BuildingManager getBuildingManager() {
		return manager;
	}

	/**
	 * Sets the building's nickName
	 * 
	 * @return none
	 */
	// Called by TabPanelBuilding.java for building nickname change
	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	/**
	 * Gets the building's nickName
	 * 
	 * @return building's nickName as a String
	 */
	// Called by TabPanelBuilding.java for building nickname change
	public String getNickName() {
		return nickName;
	}

	/**
	 * Gets the building type, not building's nickname
	 * 
	 * @return building type as a String.
	 * @deprecated TODO internationalize building names for display in user
	 *             interface.
	 */
	// Change data field from "name" to "buildingType"
	// TODO: change getName() to getBuildingType()
	// getName() has 120 occurrences in MSP
	// will retain its name for the time being
	public String getName() {
		return buildingType;
	}

	/**
	 * Gets the building type.
	 * 
	 * @return building type as a String. TODO internationalize building names for
	 *         display in user interface.
	 */

	public String getBuildingType() {
		return buildingType;
	}

	/**
	 * Sets the building's type (formerly name)
	 * 
	 * @return none "buildingType" was formerly "name"
	 */
	// Called by TabPanelBuilding.java for generating a building list
	public void setBuildingType(String type) {
		// System.out.println("input nickName is " + nickName);
		this.buildingType = type;
		// System.out.println("new buildingType is " + this.buildingType);
	}

	public double getWidth() {
		return width;
	}

	public double getLength() {
		return length;
	}

	public double getFloorArea() {
		return floorArea;
	}

	/**
	 * Returns the volume of the building in liter
	 * 
	 * @return volume in liter
	 */
	public double getVolumeInLiter() {
		return floorArea * HEIGHT * 1000; // 1 Cubic Meter = 1,000 Liters
	}

	@Override
	public double getXLocation() {
		return xLoc;
	}

	public void setXLocation(double x) {
		this.xLoc = x;
	}

	@Override
	public double getYLocation() {
		return yLoc;
	}

	public void setYLocation(double y) {
		this.yLoc = y;
	}

	@Override
	public double getFacing() {
		return facing;
	}

	public void setFacing(double facing) {
		this.facing = facing;
	}

	public boolean getInTransport() {
		return inTransportMode;
	}

	public void setInTransport(boolean value) {
		inTransportMode = value;
	}

	/**
	 * Gets the base level of the building.
	 * 
	 * @return -1 for in-ground, 0 for above-ground.
	 */
	public int getBaseLevel() {
		return baseLevel;
	}

	/**
	 * Gets the power this building currently requires for full-power mode.
	 * 
	 * @return power in kW.
	 */
	public double getFullPowerRequired() {
		double result = basePowerRequirement;

		// Determine power required for each function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext())
			result += i.next().getFullPowerRequired();

		result += powerNeededForEVAheater;
		// result = result + getFullHeatRequired();

		return result;
	}

	/**
	 * Gets the power the building requires for power-down mode.
	 * 
	 * @return power in kW.
	 */
	public double getPoweredDownPowerRequired() {
		double result = basePowerDownPowerRequirement;

		// Determine power required for each function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext())
			result += i.next().getPoweredDownPowerRequired();

		return result;
	}

	/**
	 * Gets the building's heat mode.
	 */
	public PowerMode getPowerMode() {
		return powerModeCache;
	}

	/**
	 * Sets the building's heat mode.
	 */
	public void setPowerMode(PowerMode powerMode) {
		this.powerModeCache = powerMode;
	}

	/**
	 * Gets the heat this building currently requires for full-power mode.
	 * 
	 * @return heat in kW.
	 */
	public double getFullHeatRequired() {
		// double result = baseHeatRequirement;
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getFullHeatRequired();

		// result += powerNeededForEVAheater;

		return result;
	}

	public void setHeatGenerated(double heatGenerated) {
		if (heating == null)
			heating = furnace.getHeating();
		heating.setHeatGenerated(heatGenerated);
	}

	public void setPowerRequiredForHeating(double powerReq) {
		if (heating == null)
			heating = furnace.getHeating();
		heating.setPowerRequired(powerReq);
	}

	// public void setPowerGenerated(double powerGenerated) {
	// powerGen.setPowerGenerated(powerGenerated);
	// }

	/**
	 * Gets the heat the building requires for power-down mode.
	 * 
	 * @return heat in kJ/s.
	 */

	public double getPoweredDownHeatRequired() {
		double result = 0;
		if (furnace != null && heating != null)
			result = heating.getPoweredDownHeatRequired();
		return result;
	}

	/**
	 * Gets the building's power mode.
	 */
	public HeatMode getHeatMode() {
		return heatModeCache;
	}

	/**
	 * Sets the building's heat mode.
	 */
	public void setHeatMode(HeatMode heatMode) {
		if (heatModeCache != heatMode) {
			// if heatModeCache is different from the its last value
			heatModeCache = heatMode;
		}
	}

	/**
	 * Gets the entity's malfunction manager.
	 * 
	 * @return malfunction manager
	 */
	public MalfunctionManager getMalfunctionManager() {
		return malfunctionManager;
	}

	/**
	 * Gets the total amount of lighting power in this greenhouse.
	 * 
	 * @return power (kW)
	 */
	public double getTotalPowerForEVA() {
		return powerNeededForEVAheater;
	}

	/**
	 * Calculates the number of people in the airlock
	 * 
	 * @return number of people
	 */
	public int numOfPeopleInAirLock() {
		int num = 0;
		if (eva == null)
			eva = (EVA) getFunction(FunctionType.EVA);
		if (eva != null) {
			num = eva.getAirlock().getOccupants().size();
			powerNeededForEVAheater = num * kW_EVA_HEATER * .5D; // assume half of people are doing EVA ingress
																	// statistically
		}
		return num;
	}

	public Collection<Person> getInhabitants() {
		Collection<Person> people = new ConcurrentLinkedQueue<Person>();

		if (lifeSupport != null) {
			for (Person occupant : lifeSupport.getOccupants()) {
				if (!people.contains(occupant))
					people.add(occupant);
			}
		}
		/*
		 * // If building has life support, add all occupants of the building. if
		 * (hasFunction(BuildingFunction.LIFE_SUPPORT)) { LifeSupport lifeSupport =
		 * (LifeSupport) getFunction(BuildingFunction.LIFE_SUPPORT); Iterator<Person> i
		 * = lifeSupport.getOccupants().iterator(); while (i.hasNext()) { Person
		 * occupant = i.next(); if (!people.contains(occupant)) people.add(occupant); }
		 * }
		 */
		return people;
	}

	/**
	 * Gets a collection of people affected by this entity. Children buildings
	 * should add additional people as necessary.
	 * 
	 * @return person collection
	 */
	public Collection<Person> getAffectedPeople() {
		/*
		 * Collection<Person> people = new ConcurrentLinkedQueue<Person>();
		 * 
		 * // If building has life support, add all occupants of the building. if
		 * (hasFunction(BuildingFunction.LIFE_SUPPORT)) { LifeSupport lifeSupport =
		 * (LifeSupport) getFunction(BuildingFunction.LIFE_SUPPORT); Iterator<Person> i
		 * = lifeSupport.getOccupants().iterator(); while (i.hasNext()) { Person
		 * occupant = i.next(); if (!people.contains(occupant)) people.add(occupant); }
		 * }
		 */
		Collection<Person> people = getInhabitants();
		// Check all people in settlement.
		Iterator<Person> i = manager.getSettlement().getIndoorPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			Task task = person.getMind().getTaskManager().getTask();

			// Add all people maintaining this building.
			if (task instanceof Maintenance) {
				if (((Maintenance) task).getEntity() == this) {
					if (!people.contains(person))
						people.add(person);
				}
			}

			// Add all people repairing this facility.
			if (task instanceof Repair) {
				if (((Repair) task).getEntity() == this) {
					if (!people.contains(person))
						people.add(person);
				}
			}
		}

		return people;
	}

	public Collection<Robot> getAffectedRobots() {
		Collection<Robot> robots = new ConcurrentLinkedQueue<Robot>();

		if (roboticStation != null) {
			for (Robot occupant : roboticStation.getRobotOccupants()) {
				if (!robots.contains(occupant))
					robots.add(occupant);
			}
		}
		
//		 if (hasFunction(BuildingFunction.ROBOTIC_STATION)) { RoboticStation
//		 roboticStation = (RoboticStation)
//		 getFunction(BuildingFunction.ROBOTIC_STATION); Iterator<Robot> i =
//		 roboticStation.getRobotOccupants().iterator(); while (i.hasNext()) { Robot
//		 occupant = i.next(); if (!robots.contains(occupant)) robots.add(occupant); }
//		 }
		 
		// Check all robots in settlement.
		Iterator<Robot> i = manager.getSettlement().getRobots().iterator();
		while (i.hasNext()) {
			Robot robot = i.next();
			Task task = robot.getBotMind().getBotTaskManager().getTask();

			// Add all robots maintaining this building.
			if (task instanceof Maintenance) {
				if (((Maintenance) task).getEntity() == this) {
					if (!robots.contains(robot))
						robots.add(robot);
				}
			}

			// Add all robots repairing this facility.
			if (task instanceof Repair) {
				if (((Repair) task).getEntity() == this) {
					if (!robots.contains(robot))
						robots.add(robot);
				}
			}
		}

		return robots;
	}

	/**
	 * String representation of this building.
	 * 
	 * @return The settlement and building's nickName.
	 */
	// TODO: To prevent crash, check which classes still rely on toString() to
	// return buildingType
	// Change buildingType to nickName
	public String toString() {
//		return buildingType;
		return nickName;
	}

	/**
	 * Compares this object with the specified object for order.
	 * 
	 * @param o the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	// TODO: find out if we should use nickName vs. buildingType
	public int compareTo(Building o) {
		return buildingType.compareToIgnoreCase(o.buildingType);
	}

	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param {@link MasterClock}
	 * @param {{@link MarsClock}
	 */
	public static void justReloaded(MasterClock c0, MarsClock c1) {
		masterClock = c0;
		marsClock = c1;
		buildingConfig = SimulationConfig.instance().getBuildingConfiguration();
		malfunctionMeteoriteImpact = MalfunctionFactory
				.getMeteoriteImpactMalfunction(MalfunctionFactory.METEORITE_IMPACT_DAMAGE);
	}
	
	/**
	 * Time passing for building.
	 * 
	 * @param time amount of time passing (in millisols)
	 */
	public void timePassing(double time) {
		// Check for valid argument.
//		if (time < 0D)
//			throw new IllegalArgumentException("Time must be > 0D");

		// Send time to each building function.
		for (Function f : functions)
			f.timePassing(time);

		int msol = marsClock.getMillisolInt();

		if (msolCache != msol) {
			msolCache = msol;

			// Determine if a meteorite impact will occur within the new sol
			checkForMeteoriteImpact();

		}

		// Update malfunction manager.
		malfunctionManager.timePassing(time); 
		
		// If powered up, active time passing.
		if (powerModeCache == PowerMode.FULL_POWER)
			malfunctionManager.activeTimePassing(time);
		
		inTransportMode = false;
	}

	public List<Function> getFunctions() {
		return functions;
	}

//	public Map<Integer, ItemResource> getItemMap() {
//		return itemMap;
//	}

	/*
	 * Checks for possible meteorite impact for this building
	 */
	public void checkForMeteoriteImpact() {
		// check for the passing of each day
		int solElapsed = marsClock.getMissionSol();
		int moment_of_impact = 0;

		if (solCache != solElapsed) {
			solCache = solElapsed;
			// Assume a gauissan profile
			double probability = floorArea * manager.getProbabilityOfImpactPerSQMPerSol() * (.5 + RandomUtil.getGaussianDouble());
//			System.out.println("Meteorite : " + probability);
			// probability is in percentage unit between 0% and 100%
			if (probability > 0 && RandomUtil.getRandomDouble(100D) <= probability) {
				// 		logger.info("Sensors just picked up the new probability
				// 		of a meteorite impact for " + nickName
				// 		+ " in " + settlement + " to be " + Math.round(probability*100D)/100D + " %.");

				isImpactImminent = true;
				// set a time for the impact to happen any time between 0 and 1000 milisols
				moment_of_impact = RandomUtil.getRandomInt(1000);
			}
		}

		if (isImpactImminent) {
			int now = marsClock.getMillisolInt();
			// Note: at the fastest sim speed, up to ~5 millisols may be skipped.
			// need to set up detection of the impactTimeInMillisol with a +/- 3 range.
			int delta = (int) Math.sqrt(Math.sqrt(masterClock.getTimeRatio()));
			if (now > moment_of_impact - 2 * delta && now < moment_of_impact + 2 * delta) {
				LogConsolidated.log(logger, Level.WARNING, 0, sourceName,
						"[" + settlement + "] A meteorite impact over " + nickName + " is imminent.", null);
				// reset the boolean immmediately. This is for keeping track of whether the
				// impact has occurred at msols
				isImpactImminent = false;

				// find the length this meteorite can penetrate
				double penetrated_length = manager.getWallPenetration();

				double wallThickness = 0;

				if (buildingType.toLowerCase().contains("greenhouse"))
					// if it's a greenhouse
					wallThickness = wall_thickness_inflatable;
				else
					wallThickness = WALL_THICKNESS_ALUMINUM;

				if (penetrated_length >= wallThickness) {
					// Yes it's breached !
//					if (malfunctionMeteoriteImpact == null)
//					malfunctionMeteoriteImpact = MalfunctionFactory
//								.getMeteoriteImpactMalfunction(MalfunctionFactory.METEORITE_IMPACT_DAMAGE);
					// Simulate the meteorite impact as a malfunction event for now
					try {
						malfunctionManager.activateMalfunction(MalfunctionFactory
								.getMeteoriteImpactMalfunction(MalfunctionFactory.METEORITE_IMPACT_DAMAGE),
								true);
					} catch (Exception e) {
						e.printStackTrace(System.err);
					}

					String victimName = "None";
					String task = "N/A";

					// check if someone under this roof may have seen/affected by the impact
					for (Person person : getInhabitants()) {
						if (person.getBuildingLocation() == this
								&& RandomUtil.lessThanRandPercent(METEORITE_IMPACT_PROBABILITY_AFFECTED)) {

							// TODO: someone got hurt, declare medical emergency
							// TODO: delineate the accidents from those listed in malfunction.xml
							// currently, malfunction whether a person gets hurt is handled by Malfunction
							// above

							PhysicalCondition pc = person.getPhysicalCondition();
							int resilience = person.getNaturalAttributeManager()
									.getAttribute(NaturalAttributeType.STRESS_RESILIENCE);
							int courage = person.getNaturalAttributeManager()
									.getAttribute(NaturalAttributeType.COURAGE);
							double factor = 1 + RandomUtil.getRandomDouble(1) - resilience / 100 - courage / 100D;
							if (factor > 1)
								pc.setStress(person.getStress() * factor);

							victimName = person.getName();
							task = person.getTaskDescription();
							malfunctionMeteoriteImpact.setTraumatized(victimName);

							logger.warning(victimName + " was traumatized by the meteorite impact in " + this + " at "
									+ settlement);
						}
						// else {
						// logger.info(person.getName() + " did not witness the latest meteorite impact
						// in " + this + " at " + settlement);
						// }
					}

//					HistoricalEvent hEvent = new HazardEvent(EventType.HAZARD_METEORITE_IMPACT, malfunctionMeteoriteImpact,
//							"Meteorite Impact", //"Natural Cause",
//							task, victimName, this.getNickName(), settlement.getName());
//					Simulation.instance().getEventManager().registerNewEvent(hEvent);

				}
			}
		}
	}

	public Coordinates getLocation() {
		return location;
	}

	/**
	 * Gets the building's inhabitable ID number.
	 * 
	 * @return id.
	 */
	public int getInhabitableID() {
		return inhabitableID;
	}

	/**
	 * Sets the building's settlement inhabitable ID number.
	 * 
	 * @param id.
	 */
	public void setInhabitableID(int id) {
		inhabitableID = id;
	}

	/**
	 * Gets the building's settlement template ID number.
	 * 
	 * @return id.
	 */
	public int getTemplateID() {
		return templateID;
	}

	/**
	 * Sets the building's settlement template ID number.
	 * 
	 * @param id.
	 */
	public void setTemplateID(int id) {
		templateID = id;
	}

	public Settlement getSettlement() {
		return manager.getSettlement();
	}

	public void extractHeat(double heat) {
		// Set the instance of thermal generation function.
		if (furnace == null)
			furnace = (ThermalGeneration) getFunction(FunctionType.THERMAL_GENERATION);

		heating.setHeatLoss(heat);
	}

	/*
	 * @Override public String getShortLocationName() { return nickName + " in " +
	 * getSettlement().getName(); //getLocationTag().getSettlementName(); }
	 */
	@Override
	public String getImmediateLocation() {
		return getLocationTag().getImmediateLocation();
	}

	@Override
	public String getLocale() {
		return getLocationTag().getLocale();
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {

		functions = null;
//		itemMap = null;
		location = null;
		manager = null;
		settlement = null;
		furnace = null;
		lifeSupport = null;
		roboticStation = null;
		powerGen = null;
		marsClock = null;
		masterClock = null;
		buildingConfig = null;
		heatModeCache = null;
		// fmt = null;
		buildingType = null;
		manager = null;
		powerModeCache = null;
		heatModeCache = null;
		malfunctionManager.destroy();
		malfunctionManager = null;
		
//		Iterator<Function> i = functions.iterator(); while (i.hasNext()) {
//		i.next().destroy(); }
		
	}

	@Override
	public Building getBuildingLocation() {
		return this;
	}

	@Override
	public Settlement getAssociatedSettlement() {
		return this.getAssociatedSettlement();
	}

	@Override
	public Unit getUnit() {
		return this;
	}

}