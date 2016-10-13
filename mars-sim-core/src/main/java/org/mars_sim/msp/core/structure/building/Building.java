/**
 * Mars Simulation Project
 * Building.java
 * @version 3.1.0 2016-10-05
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.building;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.malfunction.MalfunctionEvent;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.task.Maintenance;
import org.mars_sim.msp.core.person.ai.task.Repair;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.resource.ItemResource;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.BuildingTemplate;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.Structure;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.connection.InsidePathLocation;
import org.mars_sim.msp.core.structure.building.function.Administration;
import org.mars_sim.msp.core.structure.building.function.AstronomicalObservation;
import org.mars_sim.msp.core.structure.building.function.BuildingConnection;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Communication;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.structure.building.function.EarthReturn;
import org.mars_sim.msp.core.structure.building.function.Exercise;
import org.mars_sim.msp.core.structure.building.function.FoodProduction;
import org.mars_sim.msp.core.structure.building.function.Function;
import org.mars_sim.msp.core.structure.building.function.GroundVehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.HeatMode;
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
import org.mars_sim.msp.core.structure.building.function.ThermalGeneration;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.structure.building.function.cooking.Dining;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.structure.building.function.farming.Crop;
import org.mars_sim.msp.core.structure.building.function.farming.Farming;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;

/**
 * The Building class is a settlement's building.
 */
public class Building
extends Structure
implements Malfunctionable, Serializable, // Comparable<Building>,
LocalBoundedObject, InsidePathLocation {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	// default logger.
	private static Logger logger = Logger.getLogger(Building.class.getName());

	// Assuming 20% chance for each person to witness or be conscious of the meteorite impact in an affected building
	public static final double METEORITE_IMPACT_PROBABILITY_AFFECTED = 20; 
	// The influx of meteorites entering Mars atmosphere can be estimated as
	// log N = -0.689* log(m) + 4.17
	// N is the number of meteorites per year having masses greater than m grams incident 
	// on an area of 10^6 km2 (Bland and Smith, 2000).
	// see initial implementation in MeteoriteImpactImpl class
	
	// Note: typical values of penetrationThicknessOnAL for a 1 g/cm^3, 1 km/s meteorite can be .0010 to 0.0022 meter
	public static final double WALL_THICKNESS_ALUMINUM = 0.0000254; // typically between 10^-5 (.00001) and 10^-2 (.01) [in meters]
	public static final double WALL_THICKNESS_INFLATABLE = 0.0000211;// [in meters]

	// 2015-03-12 Loaded wearLifeTime, maintenanceTime, roomTemperature from buildings.xml
	/** Default : 3340 Sols (5 orbits). */
	private int wearLifeTime = 3340000;
	/** Default : 50 millisols maintenance time. */
	private int maintenanceTime = 50;
	/** Default : 22.5 deg celsius. */
    private double roomTemperature = 22.5D;
    //public double GREENHOUSE_TEMPERATURE = 24D;

    // Data members
	protected int id;
    // 2015-12-30 Added inhabitable_id
	protected int inhabitable_id = -1;
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
	boolean inTransportMode = true;

	protected String buildingType;
	protected String nickName;
	// 2014-11-27 Added description for each building
	protected String description = "Stay tuned";

	protected List<Function> functions;
	//private List<BuildingKit> buildingKit;
	private Map<Integer, ItemResource> itemMap = new HashMap<Integer, ItemResource>();

	/** Unit location coordinates. */
	private Coordinates location;// = manager.getSettlement().getCoordinates();
	protected BuildingManager manager;
	// 2014-10-28  changed variable's name from "name" to "buildingType"
	protected ThermalGeneration furnace;
	protected MalfunctionManager malfunctionManager;
	protected LifeSupport lifeSupport;
	private EVA eva;
    private Inventory b_inv, s_inv;
    private Settlement settlement;
	private BuildingConfig config;
    
	private static MarsClock marsClock;
	private static MasterClock masterClock;

	protected PowerMode powerMode;
	//2014-10-23  Modified thermal control parameters in the building */
	protected HeatMode heatMode;
	// 2014-11-02 Added HeatModeCache
	protected HeatMode heatModeCache;

	DecimalFormat fmt = new DecimalFormat("###.####");

	/** Constructor 1
	 * Constructs a Building object.
	 * @param template the building template.
	 * @param manager the building's building manager.
	 * @throws BuildingException if building can not be created.
	 */
	public Building(BuildingTemplate template, BuildingManager manager) {
		this(template.getID(), template.getBuildingType(), template.getNickName(), template.getWidth(),
		        template.getLength(), template.getXLoc(), template.getYLoc(),
				template.getFacing(), manager);
	    //logger.info("Building's constructor 1 is on " + Thread.currentThread().getName() + " Thread");

		this.manager = manager;
		this.settlement = manager.getSettlement();
		this.location = manager.getSettlement().getCoordinates();
		this.buildingType = template.getBuildingType();

		if (s_inv == null)
			s_inv = settlement.getInventory();
		if (b_inv == null) {
			b_inv = getInventory();//new Inventory(this);
			if (buildingType.toLowerCase().contains("hallway") || buildingType.toLowerCase().contains("tunnel")) {
				//b_inv = new Inventory(this);
				b_inv.addGeneralCapacity(100);
			} else if (this.getBuildingType().toLowerCase().contains("greenhouse"))
				b_inv.addGeneralCapacity(10000);
			else
				b_inv.addGeneralCapacity(1000);
		}

		powerMode = PowerMode.FULL_POWER;
		heatMode = HeatMode.ONLINE;

		if (hasFunction(BuildingFunction.LIFE_SUPPORT)) {
			if (lifeSupport == null) {
				lifeSupport = (LifeSupport) getFunction(BuildingFunction.LIFE_SUPPORT);
			    // 2015-12-30 Added setting up an inhabitable_id
				int id = manager.getNextInhabitableID();
				setInhabitable_id(id);
			}
		}
		if (hasFunction(BuildingFunction.THERMAL_GENERATION))
			if (furnace == null)
				furnace = (ThermalGeneration) getFunction(BuildingFunction.THERMAL_GENERATION);

	}

	/** Constructor 2
	 * Constructs a Building object.
	 * @param id the building's unique ID number.
	 * @param buildingType the building Type.
	 * @param nickName the building's nick name.
	 * @param width the width (meters) of the building or -1 if not set.
	 * @param length the length (meters) of the building or -1 if not set.
	 * @param xLoc the x location of the building in the settlement.
	 * @param yLoc the y location of the building in the settlement.
	 * @param facing the facing of the building (degrees clockwise from North).
	 * @param manager the building's building manager.
	 * @throws BuildingException if building can not be created.
	 */
	//2014-10-27  changed variable "name" to "buildingType"
	public Building(int id, String buildingType, String nickName, double width, double length,
	        double xLoc, double yLoc, double facing, BuildingManager manager) {
		super(nickName, manager.getSettlement().getCoordinates());
	    //logger.info("Building's constructor 2 is on " + Thread.currentThread().getName() + " Thread");

		this.id = id;
		this.buildingType = buildingType;
		this.nickName = nickName;
		this.manager = manager;
		this.settlement = manager.getSettlement();

		this.xLoc = xLoc;
		this.yLoc = yLoc;
		this.facing = facing;
		this.location = manager.getSettlement().getCoordinates();

		config = SimulationConfig.instance().getBuildingConfiguration();

		if (s_inv == null)
			s_inv = settlement.getInventory();
		if (b_inv == null) {
			b_inv = getInventory();
			if (buildingType.toLowerCase().contains("hallway") || buildingType.toLowerCase().contains("tunnel")) {
				//b_inv = new Inventory(this);
				b_inv.addGeneralCapacity(100);
			} else if (this.getBuildingType().toLowerCase().contains("greenhouse"))
				b_inv.addGeneralCapacity(10000);
			else
				b_inv.addGeneralCapacity(1000);
		}

		if (masterClock == null)
			masterClock = Simulation.instance().getMasterClock();

		powerMode = PowerMode.FULL_POWER;
		heatMode = HeatMode.ONLINE;


		// Get building's dimensions.
		if (width != -1D) {
			this.width = width;
			}
		else {
			this.width = config.getWidth(buildingType);
			}
		if (this.width <= 0D) {
			throw new IllegalStateException("Invalid building width: " + this.width + " m. for new building " + buildingType);
		}

		if (length != -1D) {
			this.length = length;
		}
		else {
			this.length = config.getLength(buildingType);
		}
		if (this.length <= 0D) {
			throw new IllegalStateException("Invalid building length: " + this.length + " m. for new building " + buildingType);
		}

		floorArea = length * width;
		
		baseLevel = config.getBaseLevel(buildingType);
		description = config.getDescription(buildingType);

		// Get the building's functions
		functions = determineFunctions();

		// Get base power requirements.
		basePowerRequirement = config.getBasePowerRequirement(buildingType);
		basePowerDownPowerRequirement = config.getBasePowerDownPowerRequirement(buildingType);
		wearLifeTime = config.getWearLifeTime(buildingType);
		maintenanceTime = config.getMaintenanceTime(buildingType);

		// Set room temperature
		roomTemperature = config.getRoomTemperature(buildingType);
		// TODO: determine the benefit of adding other heat requirements.
		//baseHeatRequirement = config.getBaseHeatRequirement(buildingType);
		//baseHeatDownHeatRequirement = config.getBasePowerDownHeatRequirement(buildingType);

		// Determine total maintenance time.
		double totalMaintenanceTime = maintenanceTime;
		Iterator<Function> j = functions.iterator();
		while (j.hasNext()) {
			Function function = j.next();
			totalMaintenanceTime += function.getMaintenanceTime();
		}

		// Set up malfunction manager.
		malfunctionManager = new MalfunctionManager(this, wearLifeTime, totalMaintenanceTime);
		malfunctionManager.addScopeString("Building");

		// Add each function to the malfunction scope.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			Function function = i.next();
			for (int x = 0; x < function.getMalfunctionScopeStrings().length; x++) {
				malfunctionManager.addScopeString(function.getMalfunctionScopeStrings()[x]);
			}
		}
	}

	/** Constructor 3 (for use by Mock Building in Unit testing) */
	protected Building(BuildingManager manager) {
		super("Mock Building", new Coordinates(0D, 0D));
	}

	/**
	 * Gets the building inventory of this building.
	 * @return inventory
	 */
    public Inventory getBuildingInventory() {
    	return b_inv;
    }

	/**
	 * Gets the settlement inventory of this building.
	 * @return inventory
	 */
	public Inventory getSettlementInventory() {
		return s_inv;
	}

	/**
     * Gets the description of a building.
     * @return String description
     */
	//2014-11-27  Added getDescription()
    public String getDescription() {
            return description;
    }
	/**
	 * Sets building nickname
	 * @return none
	 */
	// 2014-10-28  Added setBuildingNickName()
    public void setBuildingNickName(String nickName) {
        this.nickName = nickName;
    }

	/**
     * Gets the initial temperature of a building.
     * @return temperature (deg C)
     */
	//2014-10-23  Added getInitialTemperature()
    public double getInitialTemperature() {
    	//double result;
		//if (config.hasFarming(buildingType))
		//if (buildingType.equals("Inflatable Greenhouse")
		//		|| buildingType.equals("Large Greenhouse")
		//		||	buildingType.equals("Inground Greenhouse") )
		//	return GREENHOUSE_TEMPERATURE;
		//else
            return roomTemperature;
    }

	public LifeSupport getLifeSupport() {
		return lifeSupport;
	}

	public ThermalGeneration getThermalGeneration() {
		return furnace;
	}

    /**
     * Gets the temperature of a building.
     * @return temperature (deg C)
     */
	//2014-10-17  Added getTemperature()
    public double getCurrentTemperature() {
    	if (getThermalGeneration() != null)
            return getThermalGeneration().getHeating().getCurrentTemperature();
    	else
    		return roomTemperature;
    }

	/**
	 * Determines the building functions.
	 * @return list of building functions.
	 * @throws Exception if error in functions.
	 */
	private List<Function> determineFunctions() {
		//System.out.println("Building's determineFunctions");
		List<Function> buildingFunctions = new ArrayList<Function>();

		// Set power generation function.
		if (config.hasPowerGeneration(buildingType)) buildingFunctions.add(new PowerGeneration(this));
		//2014-10-17 mkung
		// Added thermal generation function.
		if (config.hasThermalGeneration(buildingType)) buildingFunctions.add(new ThermalGeneration(this));

		// Set life support function.
		if (config.hasLifeSupport(buildingType)) buildingFunctions.add(new LifeSupport(this));

		// Set living accommodations function.
		if (config.hasLivingAccommodations(buildingType)) buildingFunctions.add(new LivingAccommodations(this));

		// Set research function.
		if (config.hasResearchLab(buildingType)) buildingFunctions.add(new Research(this));

		// Set communication function.
		if (config.hasCommunication(buildingType)) buildingFunctions.add(new Communication(this));

		// Set EVA function.
		//eva = new EVA(this);
		//if (config.hasEVA(buildingType)) buildingFunctions.add(eva);
		if (config.hasEVA(buildingType)) buildingFunctions.add(new EVA(this));

		// Set recreation function.
		if (config.hasRecreation(buildingType)) buildingFunctions.add(new Recreation(this));

		// Set dining function.
		if (config.hasDining(buildingType)) buildingFunctions.add(new Dining(this));

		// Set resource processing function.
		if (config.hasResourceProcessing(buildingType)) buildingFunctions.add(new ResourceProcessing(this));

		// Set storage function.
		if (config.hasStorage(buildingType)) buildingFunctions.add(new Storage(this));

		// Set medical care function.
		if (config.hasMedicalCare(buildingType)) buildingFunctions.add(new MedicalCare(this));

		// Set farming function.
		if (config.hasFarming(buildingType)) buildingFunctions.add(new Farming(this));

		// Set exercise function.
		if (config.hasExercise(buildingType)) buildingFunctions.add(new Exercise(this));

		// Set ground vehicle maintenance function.
		if (config.hasGroundVehicleMaintenance(buildingType)) buildingFunctions.add(new GroundVehicleMaintenance(this));

		// Set cooking function.
		if (config.hasCooking(buildingType)) buildingFunctions.add(new Cooking(this));
		if (config.hasCooking(buildingType)) buildingFunctions.add(new PreparingDessert(this));

		// Set manufacture function.
		if (config.hasManufacture(buildingType)) buildingFunctions.add(new Manufacture(this));

		//2014-11-23 Added food production
		if (config.hasFoodProduction(buildingType)) buildingFunctions.add(new FoodProduction(this));

		// Set power storage function.
		if (config.hasPowerStorage(buildingType)) buildingFunctions.add(new PowerStorage(this));

		//2014-10-17  Added and imported ThermalStorage
		// Set thermal storage function.
		//if (config.hasThermalStorage(buildingType)) buildingFunctions.add(new ThermalStorage(this));

		// Set astronomical observation function
		if (config.hasAstronomicalObservation(buildingType)) buildingFunctions.add(new AstronomicalObservation(this));

		// Set management function.
		if (config.hasManagement(buildingType)) buildingFunctions.add(new Management(this));

		// Set Earth return function.
		if (config.hasEarthReturn(buildingType)) buildingFunctions.add(new EarthReturn(this));

		// Set building connection function.
		if (config.hasBuildingConnection(buildingType)) buildingFunctions.add(new BuildingConnection(this));

		// Set administration function.
		if (config.hasAdministration(buildingType)) buildingFunctions.add(new Administration(this));

		// Set robotic function.
		if (config.hasRoboticStation(buildingType)) buildingFunctions.add(new RoboticStation(this));

		return buildingFunctions;
	}

	/**
	 * Checks if a building has a particular function.
	 * @param function the name of the function.
	 * @return true if function.
	 */
	public boolean hasFunction(BuildingFunction function) {
		boolean result = false;
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			if (i.next().getFunction() == function)
				result = true;
		}
		return result;
	}

	/**
	 * Gets a function if the building has it.
	 * @param functionType {@link BuildingFunction} the function of the building.
	 * @return function.
	 * @throws BuildingException if building doesn't have the function.
	 */
	public Function getFunction(BuildingFunction functionType) {
		Function result = null;
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			Function function = i.next();
			if (function.getFunction() == functionType)
				result = function;
		}
		//if (result != null) return result;
		//else throw new IllegalStateException(buildingType + " does not have " + functionType);
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

	/**
	 * Gets the building's building manager.
	 * @return building manager
	 */
	public BuildingManager getBuildingManager() {
		return manager;
	}

	/**
	 * Gets the building's unique ID number.
	 * @return ID integer.
	 */
	public int getID() {
		return id;
	}

	/**
	 * Sets the building's nickName
	 * @return none
	 */
	// 2014-10-27  Called by TabPanelBuilding.java for building nickname change
	public void setNickName(String nickName) {
		//System.out.println("input nickName is " + nickName);
		this.nickName = nickName;
		//System.out.println("new nickName is " + this.nickName);
	}
	/**
	 * Gets the building's nickName
	 * @return building's nickName as a String
	 */
	// 2014-10-27  Called by TabPanelBuilding.java for building nickname change
	public String getNickName() {
		return nickName;
	}

	/**
	 * Gets the building type, not building's nickname
	 * @return building type as a String.
	 * @deprecated
	 * TODO internationalize building names for display in user interface.
	 */
	// 2014-10-28 change data field from "name" to "buildingType"
	// TODO: change getName() to getBuildingType()
	// getName() has 120 occurrences in MSP
	// will retain its name for the time being
	public String getName() {
		return buildingType;
	}

	/**
	 * Gets the building type.
	 * @return building type as a String.
	 * TODO internationalize building names for display in user interface.
	 */
	//2014-11-02  Added getBuildingType()
	public String getBuildingType() {
		return buildingType;
	}

	/**
	 * Sets the building's type (formerly name)
	 * @return none
	 * "buildingType" was formerly "name"
	 */
	//2014-10-28  Called by TabPanelBuilding.java for generating a building list
	public void setBuildingType(String type) {
		//System.out.println("input nickName is " + nickName);
		this.buildingType = type;
		//System.out.println("new buildingType is " + this.buildingType);
	}
	@Override
	public double getWidth() {
		return width;
	}

	@Override
	public double getLength() {
		return length;
	}

	public double getFloorArea() {
		return floorArea;
	}
	
	@Override
	public double getXLocation() {
		return xLoc;
	}


	public void setXLocation(double x) {
		this.xLoc = x ;
	}

	@Override
	public double getYLocation() {
		return yLoc;
	}

	public void setYLocation(double y) {
		this.yLoc = y ;
	}

	@Override
	public double getFacing() {
		return facing;
	}

	public void setFacing(double facing) {
		this.facing = facing ;
	}

	public boolean getInTransport() {
		return inTransportMode;
	}

	public void setInTransport(boolean value) {
		inTransportMode = value;
	}

	/**
     * Gets the base level of the building.
     * @return -1 for in-ground, 0 for above-ground.
     */
    public int getBaseLevel() {
        return baseLevel;
    }

	/**
	 * Gets the power this building currently requires for full-power mode.
	 * @return power in kW.
	 */
	//2014-11-02  Modified getFullPowerRequired()
	public double getFullPowerRequired()  {
		double result = basePowerRequirement;

		// Determine power required for each function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) result += i.next().getFullPowerRequired();

		//2014-11-02 Added getFullHeatRequired()
		result = result + getFullHeatRequired();

		return result;
	}

	/**
	 * Gets the power the building requires for power-down mode.
	 * @return power in kW.
	 */
	public double getPoweredDownPowerRequired() {
		double result = basePowerDownPowerRequirement;

		// Determine power required for each function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) result += i.next().getPoweredDownPowerRequired();

		return result;
	}

	/**
	 * Gets the building's heat mode.
	 */
	public PowerMode getPowerMode() {
		return powerMode;
	}

	/**
	 * Sets the building's heat mode.
	 */
	public void setPowerMode(PowerMode powerMode) {
		this.powerMode = powerMode;
	}

	/**
	 * Gets the heat this building currently requires for full-power mode.
	 * @return heat in kJ/s.
	 */
	//2014-11-02  Modified getFullHeatRequired()
	public double getFullHeatRequired()  {
		//double result = baseHeatRequirement;
		double result = 0;

		if (furnace != null && furnace.getHeating() != null )
			furnace.getHeating().getFullHeatRequired();

		result += powerNeededForEVAheater;

		return result;
	}
	//2014-11-02 Added setHeatGenerated()
	public void setHeatGenerated(double heatGenerated) {
		furnace.getHeating().setHeatGenerated(heatGenerated);
	}

	/**
	 * Gets the heat the building requires for power-down mode.
	 * @return heat in kJ/s.
	*/
	//2014-10-17  Added getPoweredDownHeatRequired()
	public double getPoweredDownHeatRequired() {
		double result = 0;
		if (furnace != null && furnace.getHeating() != null)
			furnace.getHeating().getPoweredDownHeatRequired();
		return result;
	}

	/**
	 * Gets the building's power mode.
	 */
	//2014-10-17  Added heat mode
	public HeatMode getHeatMode() {
		return heatMode;
	}

	/**
	 * Sets the building's heat mode.
	 */
	//2014-10-17  Added heat mode
	public void setHeatMode(HeatMode heatMode) {
		if ( heatModeCache != heatMode) {
			// if heatModeCache is different from the its last value
			heatModeCache = heatMode;
			this.heatMode = heatMode;
		}
	}

	/**
	 * Gets the entity's malfunction manager.
	 * @return malfunction manager
	 */
	public MalfunctionManager getMalfunctionManager() {
		return malfunctionManager;
	}

    /**
     * Gets the total amount of lighting power in this greenhouse.
     * @return power (kW)
     */
    public double getTotalPowerForEVA() {
        return powerNeededForEVAheater;
    }


	public int numOfPeopleInAirLock() {
        int num = 0;
		//if (getFunction(BuildingFunction.EVA) != null) {
			//EVA eva = (EVA) getFunction(BuildingFunction.EVA);
	        num = ((EVA) getFunction(BuildingFunction.EVA)).getAirlock().getOccupants().size();
			//if (num > 0) System.out.println("num is " + num);
		//}
	        powerNeededForEVAheater = num * 1D; // set to 1kW for each person
        return num;
/*
        List<Building> evaBuildings = manager.getBuildings(BuildingFunction.EVA);
        if (evaBuildings.size() > 0) {
            Iterator<Building> i = evaBuildings.iterator();
    		while (i.hasNext()) {
            	Building building = i.next();
            	//building.get
    		}
        }


         		int result = 0;
		//Collection<Person> people = getInhabitants();
		// Check all people in settlement.
		//Iterator<Person> i = people.iterator();
		Iterator<Person> i = manager.getSettlement().getInhabitants().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			//System.out.println(person.getName() + "'s location : " + person.getBuildingLocation().getNickName());
			//System.out.println("Building location with heating : " + getNickName());
			if (person.getBuildingLocation().getNickName().equals(getNickName())) {
				Task task = person.getMind().getTaskManager().getTask();
				// Add all people maintaining this building.
				if (task instanceof EnterAirlock || task instanceof ExitAirlock ) {
					result++;
					System.out.println(result + " in the airlock : ");
				}
			}
		}
		if (result > 0)
			System.out.println("result : "+ result);
		return result;
*/

	}


    public Collection<Person> getInhabitants() {
		Collection<Person> people = new ConcurrentLinkedQueue<Person>();

		// If building has life support, add all occupants of the building.
		if (hasFunction(BuildingFunction.LIFE_SUPPORT)) {
			LifeSupport lifeSupport = (LifeSupport) getFunction(BuildingFunction.LIFE_SUPPORT);
			Iterator<Person> i = lifeSupport.getOccupants().iterator();
			while (i.hasNext()) {
				Person occupant = i.next();
				if (!people.contains(occupant)) people.add(occupant);
			}
		}

		return people;
    }

	/**
	 * Gets a collection of people affected by this entity.
	 * Children buildings should add additional people as necessary.
	 * @return person collection
	 */
	public Collection<Person> getAffectedPeople() {
/*
		Collection<Person> people = new ConcurrentLinkedQueue<Person>();

		// If building has life support, add all occupants of the building.
		if (hasFunction(BuildingFunction.LIFE_SUPPORT)) {
			LifeSupport lifeSupport = (LifeSupport) getFunction(BuildingFunction.LIFE_SUPPORT);
			Iterator<Person> i = lifeSupport.getOccupants().iterator();
			while (i.hasNext()) {
				Person occupant = i.next();
				if (!people.contains(occupant)) people.add(occupant);
			}
		}
*/
		Collection<Person> people = getInhabitants();
		// Check all people in settlement.
		Iterator<Person> i = manager.getSettlement().getInhabitants().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			Task task = person.getMind().getTaskManager().getTask();

			// Add all people maintaining this building.
			if (task instanceof Maintenance) {
				if (((Maintenance) task).getEntity() == this) {
					if (!people.contains(person)) people.add(person);
				}
			}

			// Add all people repairing this facility.
			if (task instanceof Repair) {
				if (((Repair) task).getEntity() == this) {
					if (!people.contains(person)) people.add(person);
				}
			}
		}

		return people;
	}


	public Collection<Robot> getAffectedRobots() {
		Collection<Robot> robots = new ConcurrentLinkedQueue<Robot>();

		if (hasFunction(BuildingFunction.ROBOTIC_STATION)) {
	       	RoboticStation roboticStation = (RoboticStation) getFunction(BuildingFunction.ROBOTIC_STATION);
			Iterator<Robot> i = roboticStation.getRobotOccupants().iterator();
			while (i.hasNext()) {
				Robot occupant = i.next();
				if (!robots.contains(occupant)) robots.add(occupant);
			}
		}

		// Check all robots in settlement.
		Iterator<Robot> i = manager.getSettlement().getRobots().iterator();
		while (i.hasNext()) {
			Robot robot = i.next();
			Task task = robot.getBotMind().getTaskManager().getTask();

			// Add all robots maintaining this building.
			if (task instanceof Maintenance) {
				if (((Maintenance) task).getEntity() == this) {
					if (!robots.contains(robot)) robots.add(robot);
				}
			}

			// Add all robots repairing this facility.
			if (task instanceof Repair) {
				if (((Repair) task).getEntity() == this) {
					if (!robots.contains(robot)) robots.add(robot);
				}
			}
		}

		return robots;
	}

	/**
	 * String representation of this building.
	 * @return The settlement and building's nickName.
	 */
	// TODO: To prevent crash, check which classes still rely on toString() to return buildingType
	// 2014-10-29 change buildingType to nickName
	public String toString() {
//		return buildingType;
		return nickName;
	}

	/**
	 * Compares this object with the specified object for order.
	 * @param o the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is less than,
	 * equal to, or greater than the specified object.
	 */
	// TODO: find out if we should use nickName vs. buildingType
	public int compareTo(Building o) {
		return buildingType.compareToIgnoreCase(o.buildingType);
	}


	/**
	 * Time passing for building.
	 * @param time amount of time passing (in millisols)
	 */
	public void timePassing(double time) {
		// Check for valid argument.
		if (time < 0D) throw new IllegalArgumentException("Time must be > 0D");

		// Send time to each building function.
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) i.next().timePassing(time);

		// 2015-06-03 determine if a meteorite impact will occur within the new sol
		checkForMeteoriteImpact();

		// Update malfunction manager.
		malfunctionManager.timePassing(time);

		// If powered up, active time passing.
		if (powerMode == PowerMode.FULL_POWER)
			malfunctionManager.activeTimePassing(time);

		inTransportMode = false;
	}

	public List<Function> getFunctions() {
		return functions;
	}

	public Map<Integer, ItemResource> getItemMap() {
		return itemMap;
	}

	/*
	 * Checks for possible meteorite impact for this building
	 */
	public void checkForMeteoriteImpact() {
		if (masterClock == null)
			masterClock = Simulation.instance().getMasterClock();
		if (marsClock == null)
			marsClock = masterClock.getMarsClock();

        // check for the passing of each day
        int solElapsed = marsClock.getSolElapsedFromStart();
        int moment_of_impact = 0;

        if (solElapsed != solCache) {
        	solCache = solElapsed;
        	  
			double probability  = floorArea * manager.getProbabilityOfImpactPerSQMPerSol();

			// assume a degree of randomness centered at the probability can be 5 times as much
			//probability = probability * ( 1 + RandomUtil.getRandomDouble(4) - RandomUtil.getRandomDouble(4));

			// assume a gauissan profile 
			probability = probability * ( 1 + RandomUtil.getGaussianDouble());

			if (probability < 0)
				probability = 0;
			
			// probability is in percentage unit between 0% and 100%
			if (RandomUtil.getRandomDouble(100D) <= probability) {
				System.out.println(this.getNickName() + "'s probability of impact : "+ probability);// + "    rand : "+ rand);
				isImpactImminent = true;
	        	// set a time for the impact to happen any time between 0 and 1000 milisols
				moment_of_impact = RandomUtil.getRandomInt(1000);
			}
		}

        if (isImpactImminent) {
        	
        	int now = (int) marsClock.getMillisol();
            // Note: at the fastest sim speed, up to ~5 millisols may be skipped. 
        	// need to set up detection of the impactTimeInMillisol with a +/- 3 range.
        	if (now > moment_of_impact - 3 && now < moment_of_impact + 3) {
	
	        	// reset the boolean immmediately. This is for keeping track of whether the impact has occurred at msols
				isImpactImminent = false;
	
				// find the length this meteorite can penetrate
				double penetrated_length = manager.getWallPenetration();
				
				double wallThickness = 0;
				
				if (this.getBuildingType().toLowerCase().contains("greenhouse"))
					// if it's a greenhouse
					wallThickness = WALL_THICKNESS_INFLATABLE;
				else
					wallThickness = WALL_THICKNESS_ALUMINUM;

				//System.out.println(getNickName() + "'s penetrated_length : " + penetrated_length);
				//System.out.println(getNickName() + "'s WALL_THICKNESS_INFLATABLE : " + WALL_THICKNESS_INFLATABLE);
				//System.out.println(getNickName() + "'s WALL_THICKNESS_ALUMINUM : " + WALL_THICKNESS_ALUMINUM);
				
				if (penetrated_length >= wallThickness) {
					// Yes it's breached !			
	
		        	Malfunction item = MalfunctionFactory.getMeteoriteImpactMalfunction(MalfunctionFactory.METEORITE_IMPACT_DAMAGE);
		
		        	// Simulate the meteorite impact as a malfunction event for now
					try {
						malfunctionManager.getUnit().fireUnitUpdate(UnitEventType.MALFUNCTION_EVENT, item);
					}
					catch (Exception e) {
						e.printStackTrace(System.err);
					}
					
					HistoricalEvent newEvent = new MalfunctionEvent(this, item, true);
					Simulation.instance().getEventManager().registerNewEvent(newEvent);
		
					//check if someone under this roof may have seen/affected by the impact
					Iterator<Person> i = getInhabitants().iterator();
					while (i.hasNext()) {
						Person person = i.next();
						if (RandomUtil.lessThanRandPercent(METEORITE_IMPACT_PROBABILITY_AFFECTED)) {
							
							// TODO: someone got hurt, declare medical emergency
							// TODO: delineate the accidents from those listed in malfunction.xml
							// currently, malfunction whether a person gets hurt is handled by Malfunction above						
							
							PhysicalCondition pc = person.getPhysicalCondition();
				            int resilience = person.getNaturalAttributeManager().getAttribute(NaturalAttribute.STRESS_RESILIENCE);
				            int courage = person.getNaturalAttributeManager().getAttribute(NaturalAttribute.COURAGE);
				            double factor = 1 + RandomUtil.getRandomDouble(1) - resilience/100 - courage/100D;
							if (factor > 1)
								pc.setStress(person.getStress() * factor);						
							
							logger.info(person.getName() + " witnessed the latest meteorite impact in " + this + " at " + settlement);
						}
						else {
							//logger.info(person.getName() + " did not witness the latest meteorite impact in " + this + " at " + settlement);
						}
						
					}
        	}
			}
		}
	}

	public Coordinates getLocation() {
		return location;
	}

	public int getInhabitable_id() {
		return inhabitable_id;
	}

	public void setInhabitable_id(int id) {
		this.inhabitable_id = id;
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		buildingType = null;
		manager = null;
		powerMode = null;
		heatMode = null;
		malfunctionManager.destroy();
		malfunctionManager = null;
		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}
	}
}