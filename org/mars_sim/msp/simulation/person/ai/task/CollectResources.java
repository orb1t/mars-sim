/**
 * Mars Simulation Project
 * CollectRockSamples.java
 * @version 2.76 2004-08-09
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.person.ai.task;

import java.io.Serializable;
import org.mars_sim.msp.simulation.*;
import org.mars_sim.msp.simulation.mars.*;
import org.mars_sim.msp.simulation.person.*;
import org.mars_sim.msp.simulation.vehicle.Rover;

/** 
 * The CollectResources class is a task for collecting resources at a site with an EVA from a rover.
 */
public class CollectResources extends EVAOperation implements Serializable {
	
	private static final String COLLECT_RESOURCES = "Collecting Resources";
	
	// Data members
	protected Rover rover; // Rover used.
	protected double collectionRate; // Collection rate for resource (kg/millisol)
	protected double targettedAmount; // Targetted amount of resource to collect at site. (kg)
	protected double startingCargo; // Amount of resource already in rover cargo at start of task. (kg)
	protected String resourceType; // The resource type (see org.mars_sim.msp.simulation.Resource)
	
	public CollectResources(String taskName, Person person, Rover rover, String resourceType, 
			double collectionRate, double targettedAmount, double startingCargo) {
		
		// Use EVAOperation parent constructor.
		super(taskName, person);
		
		// Initialize data members.
		this.rover = rover;
		this.collectionRate = collectionRate;
		this.targettedAmount = targettedAmount;
		this.startingCargo = startingCargo;
		this.resourceType = resourceType;
		
		// Set initial task phase.
		phase = EXIT_AIRLOCK;
	}

	/** 
	 * Performs this task for a given period of time.
	 * @param time amount of time to perform task (in millisols)
	 * @throws Exception if error performing task.
	 */
	double performTask(double time) throws Exception {
		double timeLeft = super.performTask(time);
		if (subTask != null) return timeLeft;
	
		while ((timeLeft > 0D) && !isDone()) {
			if (phase.equals(EXIT_AIRLOCK)) timeLeft = exitRover(timeLeft);
			else if (phase.equals(COLLECT_RESOURCES)) timeLeft = collectResources(timeLeft);
			else if (phase.equals(ENTER_AIRLOCK)) timeLeft = enterRover(timeLeft);
		}

		// Add experience to "EVA Operations" skill.
		// (1 base experience point per 100 millisols of time spent)
		double experience = time / 100D;
		
		// Experience points adjusted by person's "Experience Aptitude" attribute.
		NaturalAttributeManager nManager = person.getNaturalAttributeManager();
		experience += experience * (((double) nManager.getAttribute("Experience Aptitude") - 50D) / 100D);
		person.getSkillManager().addExperience(Skill.EVA_OPERATIONS, experience);
	
		return timeLeft;
	}
	
	/**
	 * Perform the exit rover phase of the task.
	 * @param time the time to perform this phase (in millisols)
	 * @return the time remaining after performing this phase (in millisols)
	 */
	private double exitRover(double time) {
		try {
			time = exitAirlock(time, rover.getAirlock());
		}
		catch (Exception e) {
			// System.err.println(e.getMessage();
		}
		
		if (exitedAirlock) phase = COLLECT_RESOURCES;
		return time;
	}
	
	/**
	 * Perform the collect resources phase of the task.
	 * @param time the time to perform this phase (in millisols)
	 * @return the time remaining after performing this phase (in millisols)
	 */
	private double collectResources(double time) {

		// Check for an accident during the EVA operation.
		checkForAccident(time);
	    
		// Check if there is reason to cut the collection phase short and return
		// to the rover.
		if (shouldEndEVAOperation()) {
			phase = ENTER_AIRLOCK;
			return time;
		}

		double remainingPersonCapacity = person.getInventory()
			.getResourceRemainingCapacity(resourceType);
		double currentSamplesCollected = rover.getInventory()
			.getResourceMass(resourceType) - startingCargo; 
		double remainingSamplesNeeded = targettedAmount - currentSamplesCollected;
		double sampleLimit = remainingPersonCapacity;
		if (remainingSamplesNeeded < remainingPersonCapacity)
			sampleLimit = remainingSamplesNeeded;

		double samplesCollected = time * collectionRate;

		// Modify collection rate by "Areology" skill.
		int areologySkill = person.getSkillManager().getEffectiveSkillLevel(Skill.AREOLOGY);
		if (areologySkill == 0) samplesCollected /= 2D;
		if (areologySkill > 1) samplesCollected += samplesCollected * (.2D * areologySkill);

		// Modify collection rate by polar region if ice collecting.
		if (resourceType.equals(Resource.ICE)) {
			if (Simulation.instance().getMars().getSurfaceFeatures().inPolarRegion(person.getCoordinates()))
				samplesCollected *= 3D;
		}

		// Add experience to "Areology" skill.
		// 1 base experience point per 10 millisols of collection time spent.
		// Experience points adjusted by person's "Experience Aptitude" attribute.
		double experience = time / 100D;
		experience += experience *
				(((double) person.getNaturalAttributeManager().getAttribute("Experience Aptitude") - 50D) / 100D);
		person.getSkillManager().addExperience(Skill.AREOLOGY, experience);
	
		// Collect rock samples.
		if (samplesCollected <= sampleLimit) {
			person.getInventory().addResource(resourceType, samplesCollected);
			return 0D;
		}
		else {
			if (sampleLimit >= 0D) person.getInventory().addResource(resourceType, sampleLimit);
			phase = ENTER_AIRLOCK;
			return time - (sampleLimit / collectionRate);
		}
	}
	
	/**
	 * Perform the enter rover phase of the task.
	 * @param time the time to perform this phase (in millisols)
	 * @return the time remaining after performing this phase (in millisols)
	 */
	private double enterRover(double time) {

		try {
			time = enterAirlock(time, rover.getAirlock());
		}
		catch (Exception e) { 
			System.out.println(e.getMessage()); 
		}

		if (enteredAirlock) {
			double resources = person.getInventory().getResourceMass(resourceType);

			// Load rock samples into rover.
			if (resources > 0D) {
				resources = person.getInventory().removeResource(resourceType, resources);
				rover.getInventory().addResource(resourceType, resources);
				return 0D;
			}
			else {
				endTask();
				return time;
			}
		}
        
		return 0D;
	}
	
	/**
	 * Checks if a person can perform an CollectResources task.
	 * @param person the person to perform the task
	 * @param rover the rover the person will EVA from
	 * @return true if person can perform the task.
	 */
	public static boolean canCollectResources(Person person, Rover rover) {

		// Check if person can exit the rover.
		boolean exitable = ExitAirlock.canExitAirlock(person, rover.getAirlock());

		SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();

		// Check if it is night time outside.
		boolean sunlight = surface.getSurfaceSunlight(rover.getCoordinates()) > 0;
		
		// Check if in dark polar region.
		boolean darkRegion = surface.inDarkPolarRegion(rover.getCoordinates());

		// Check if person's medical condition will not allow task.
		boolean medical = person.getPerformanceRating() < .5D;
	
		return (exitable && (sunlight || darkRegion) && !medical);
	}
	
	/**
	 * Gets the effective skill level a person has at this task.
	 * @return effective skill level
	 */
	public int getEffectiveSkillLevel() {
		SkillManager manager = person.getSkillManager();
		int EVAOperationsSkill = manager.getEffectiveSkillLevel(Skill.EVA_OPERATIONS);
		int areologySkill = manager.getEffectiveSkillLevel(Skill.AREOLOGY);
		return (int) Math.round((double)(EVAOperationsSkill + areologySkill) / 2D); 
	}
}