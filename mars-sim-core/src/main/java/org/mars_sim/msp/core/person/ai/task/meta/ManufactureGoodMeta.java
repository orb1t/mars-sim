/**
 * Mars Simulation Project
 * ManufactureGoodMeta.java
 * @version 3.08 2015-06-08
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.io.Serializable;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.location.LocationSituation;
import org.mars_sim.msp.core.person.FavoriteType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.ManufactureGood;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Makerbot;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * Meta task for the ManufactureGood task.
 */
public class ManufactureGoodMeta implements MetaTask, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.manufactureGood"); //$NON-NLS-1$

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new ManufactureGood(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (person.isInSettlement() && !person.getSettlement().getManufactureOverride()) {
            // the person has to be inside the settlement to check for manufacture override

            // See if there is an available manufacturing building.
            Building manufacturingBuilding = ManufactureGood.getAvailableManufacturingBuilding(person);
            if (manufacturingBuilding != null) {
                result = 1D;

                // Crowding modifier.
                result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, manufacturingBuilding);
                result *= TaskProbabilityUtil.getRelationshipModifier(person, manufacturingBuilding);

                // Manufacturing good value modifier.
                result *= ManufactureGood.getHighestManufacturingProcessValue(person, manufacturingBuilding);

                // Capping the probability at 100 as manufacturing process values can be very large numbers.
                if (result > 100D) {
                    result = 100D;
                }

                // If manufacturing building has process requiring work, add
                // modifier.
                SkillManager skillManager = person.getMind().getSkillManager();
                int skill = skillManager.getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);
                if (ManufactureGood.hasProcessRequiringWork(manufacturingBuilding, skill)) {
                    result += 10D;
                }

                // Effort-driven task modifier.
                result *= person.getPerformanceRating();

                // Job modifier.
                Job job = person.getMind().getJob();
                if (job != null) {
                    result *= job.getStartTaskProbabilityModifier(ManufactureGood.class)
                    		* person.getSettlement().getGoodsManager().getManufacturingFactor();
                }

                // Modify if tinkering is the person's favorite activity.
                if (person.getFavorite().getFavoriteActivity() == FavoriteType.TINKERING) {
                    result *= 1.5D;
                }

                // 2015-06-07 Added Preference modifier
                if (result > 0D) {
                    result = result + result * person.getPreference().getPreferenceScore(this)/5D;
                }
                
                if (result < 0) result = 0;


            }
            
            // Cancel any manufacturing processes that's beyond the skill of any people
            // associated with the settlement.
            if (result > 0)
            	ManufactureGood.cancelDifficultManufacturingProcesses(person);

        }

        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
        return new ManufactureGood(robot);
	}

	@Override
	public double getProbability(Robot robot) {

        double result = 0D;

        if (robot.getBotMind().getRobotJob() instanceof Makerbot) {

	        if (robot.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
	            // If settlement has manufacturing override, no new
	            // manufacturing processes can be created.
	            if (!robot.getSettlement().getManufactureOverride()) {
	        	// the person has to be inside the settlement to check for manufacture override

		            // See if there is an available manufacturing building.
		            Building manufacturingBuilding = ManufactureGood.getAvailableManufacturingBuilding(robot);
		            if (manufacturingBuilding != null) {
		                result = 100D;

		                // Crowding modifier.
		                result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(robot, manufacturingBuilding);
		                //result *= TaskProbabilityUtil.getRelationshipModifier(robot, manufacturingBuilding);

		                // Manufacturing good value modifier.
		                result *= ManufactureGood.getHighestManufacturingProcessValue(robot, manufacturingBuilding);

		                if (result > 100D) {
		                    result = 100D;
		                }

		                // If manufacturing building has process requiring work, add
		                // modifier.
		                SkillManager skillManager = robot.getBotMind().getSkillManager();
		                int skill = skillManager.getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);
		                if (ManufactureGood.hasProcessRequiringWork(manufacturingBuilding, skill)) {
		                    result += 10D;
		                }

			            // Effort-driven task modifier.
			            result *= robot.getPerformanceRating();
		            }

		        }
	            
		        // Cancel any manufacturing processes that's beyond the skill of any people
		        // associated with the settlement.
		        if (result > 0)
		        	ManufactureGood.cancelDifficultManufacturingProcesses(robot);

	        }      

        }

        return result;
    }
}