/**
 * Mars Simulation Project
 * TaskSchedule.java
 * @version 3.1.0 2017-08-30
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person;

import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.function.farming.CropType;
import org.mars_sim.msp.core.time.MarsClock;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.Optional;

/**
 * This class represents the task schedule of a person.
 */
public class TaskSchedule implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(TaskSchedule.class.getName());

	/** Set the number of Sols to be logged (to limit the memory usage & saved file size) */
	public static final int NUM_SOLS = 100;
	public static final int ON_CALL_START = 0;
	public static final int ON_CALL_END = 999;
	public static final int A_START = 0;
	public static final int A_END = 499;
	public static final int B_START = 500;
	public static final int B_END = 999;
	public static final int X_START = 0;
	public static final int X_END = 333;
	public static final int Y_START = 334;
	public static final int Y_END = 665;
	public static final int Z_START = 666;
	public static final int Z_END = 999;

	// Data members
	private int solCache;
	private int startTime;
	
	private String actorName;
	private String taskName;
	private String doAction;
	private String phase;
	
	private ShiftType shiftType, shiftTypePrevious;

	//private Map <Integer, List<OneTask>> schedules;
	//private List<OneTask> todaySchedule;
	private Map <Integer, List<OneActivity>> allActivities;
	private Map <String, Integer> taskDescriptions;
	private Map <String, Integer> taskNames;
	private Map <String, Integer> taskPhases;

	private List<OneActivity> todayActivities;
	
	private static MarsClock marsClock;
	private Person person;
	private Robot robot;

	/**
	 * Constructor for TaskSchedule
	 * @param person
	 */
	public TaskSchedule(Person person) {
		this.person = person;
		actorName = person.getName();
		this.solCache = 1;
		allActivities = new ConcurrentHashMap <>();
		todayActivities = new CopyOnWriteArrayList<OneActivity>();
		//this.schedules = new ConcurrentHashMap <>();
		//this.todaySchedule = new CopyOnWriteArrayList<OneTask>();
		taskDescriptions = new ConcurrentHashMap <String, Integer>();
		taskNames = new ConcurrentHashMap <String, Integer>();
		taskPhases = new ConcurrentHashMap <String, Integer>();

		if (Simulation.instance().getMasterClock() != null)
			marsClock = Simulation.instance().getMasterClock().getMarsClock();
	}

	/**
	 * Constructor for TaskSchedule
	 * @param robot
	 */
	public TaskSchedule(Robot robot) {
		this.robot = robot;
		actorName = robot.getName();
		this.solCache = 1;
		allActivities = new ConcurrentHashMap <>();
		todayActivities = new CopyOnWriteArrayList<OneActivity>();
		//this.schedules = new ConcurrentHashMap <>();
		//this.todaySchedule = new CopyOnWriteArrayList<OneTask>();
		taskDescriptions = new ConcurrentHashMap <String, Integer>();
		taskNames = new ConcurrentHashMap <String, Integer>();
		taskPhases = new ConcurrentHashMap <String, Integer>();

		marsClock = Simulation.instance().getMasterClock().getMarsClock();
	}

	/**
	 * Records a task onto the schedule
	 * @param taskName
	 * @param description
	 */
	public void recordTask(String taskName, String description, String phase) {
		this.taskName = taskName;
		this.doAction = description;
		this.phase = phase;

		int startTime = (int) marsClock.getMillisol();
		int solElapsed = marsClock.getMissionSol();
		if (solElapsed != solCache) {
    		//2016-09-22 Removed the sol log from LAST_SOL ago
        	if (solElapsed > NUM_SOLS) {
        		int diff = solElapsed - NUM_SOLS;
        		allActivities.remove(diff);
        		if (allActivities.containsKey(diff-1))
        			allActivities.remove(diff-1);

        	}

			// save yesterday's schedule (except on the very first day when there's nothing to save from the prior day
        	allActivities.put(solCache, todayActivities);

        	solCache = solElapsed;
        	// create a new schedule for the new day
    		todayActivities = new CopyOnWriteArrayList<OneActivity>();
    		// 2015-10-21 Added recordYestersolTask()
        	recordYestersolLastTask();


		}

		// 2016-11-29 Add maps
		int id0 = getID(taskNames, taskName);
		int id1 = getID(taskDescriptions, description);
		int id2 = getID(taskPhases, phase);

		todayActivities.add(new OneActivity(startTime, id0, id1, id2));

	}

	public int getID(Map<String, Integer> map, String key) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		else {
			int size = map.size();
			map.put(key, size+1);
			return size+1;
		}
	}

	public String getString(Map<String, Integer> map, Integer id) {
	    for(String key : map.keySet()){
	        if(map.get(key).equals(id)){
	            return key; //return the first found
	        }
	    }
	    return null;
	}

	public String convertTaskName(Integer id) {
		return getString(taskNames, id);
	}

	public String convertTaskDescription(Integer id) {
		return getString(taskDescriptions, id);
	}

	public String convertTaskPhase(Integer id) {
		return getString(taskPhases, id);
	}

/*
	private Optional<String> getKey(ConcurrentHashMap<String, Integer> map, Integer value){
	    return map.entrySet().stream().filter(e -> e.getValue().equals(value)).map(e -> e.getKey()).findFirst();
	}
*/
	/*
     * Performs the actions per frame
     * @param time amount of time passing (in millisols).
     */
	// 2015-06-29 Added timePassing()
    public void timePassing(double time) {
    }

    /*
     *  Records the first task of the sol on today's schedule as the last task from yestersol
     */
    // 2015-10-21 Added recordYestersolLastTask()
    public void recordYestersolLastTask() {

    	if (solCache > 1) {
    		// Load the last task from yestersol's schedule
    		List<OneActivity> yesterSolschedule = allActivities.get(solCache-1);
    		if (yesterSolschedule != null) {
    		int size = yesterSolschedule.size();
	    		if (size != 0) {
	    			OneActivity lastTask = yesterSolschedule.get(yesterSolschedule.size()-1);
	    			// Carry over and save the last yestersol task as the first task on today's schedule
	    			todayActivities.add(new OneActivity(0, lastTask.getTaskName(), lastTask.getDescription(), lastTask.getPhase()));
	    		}
    		}
    	}

    }

	/**
	 * Gets all activities of all days a person.
	 * @return all activity schedules
	 */
	public Map <Integer, List<OneActivity>> getAllActivities() {
		return allActivities;
	}

	/**
	 * Gets the today's activities.
	 * @return a list of today's activities
	 */
	public List<OneActivity> getTodayActivities() {
		return todayActivities;
	}

	/**
	 * Gets all schedules of a person.
	 * @return schedules

	public Map <Integer, List<OneTask>> getSchedules() {
		return schedules;
	}
*/
	/**
	 * Gets the today's schedule.
	 * @return todaySchedule

	public List<OneTask> getTodaySchedule() {
		return todaySchedule;
	}
*/
	/**
	 * Gets the current sol.
	 * @return solCache
	 */
	public int getSolCache() {
		return solCache;
	}

	/**
	 * Gets the time the shift starts
	 * @return time in millisols
	 */
	public int getShiftStart() {
		int start = -1;
		if (shiftType.equals(ShiftType.A))
			start = A_START;
		else if (shiftType.equals(ShiftType.B))
			start = B_START;
		else if (shiftType.equals(ShiftType.X))
			start = X_START;
		else if (shiftType.equals(ShiftType.Y))
			start = Y_START;
		else if (shiftType.equals(ShiftType.Z))
			start = Z_START;
		else if (shiftType.equals(ShiftType.ON_CALL))
			start = ON_CALL_START;
		return start;
	}

	/**
	 * Gets the time the shift end
	 * @return time in millisols
	 */
	public int getShiftEnd() {
		int end = -1;
		if (shiftType.equals(ShiftType.A))
			end = A_END;
		else if (shiftType.equals(ShiftType.B))
			end = B_END;
		else if (shiftType.equals(ShiftType.X))
			end = X_END;
		else if (shiftType.equals(ShiftType.Y))
			end = Y_END;
		else if (shiftType.equals(ShiftType.Z))
			end = Z_END;
		else if (shiftType.equals(ShiftType.ON_CALL))
			end = ON_CALL_END;
		return end;
	}

	/***
	 * Gets the shift type
	 * @return shift type
	 */
	public ShiftType getShiftType() {
		return shiftType;
	}
 
	/*
	 * Sets up the shift type
	 * @param shiftType
	 */
	public void setShiftType(ShiftType shiftType){
		// back up the previous shift type
		shiftTypePrevious = this.shiftType;

		if (shiftType != null) {
			
			if (person != null) {
				
				Settlement s = person.getAssociatedSettlement();
				
				if (shiftTypePrevious != null)
					s.decrementAShift(shiftTypePrevious);
				
				s.incrementAShift(shiftType);
				
				if (marsClock == null)
					marsClock = Simulation.instance().getMasterClock().getMarsClock();
						
	        	int now = (int) marsClock.getMillisol();
	      	  	boolean isOnShiftNow = isShiftHour(now);
	            boolean isOnCall = getShiftType() == ShiftType.ON_CALL;

		        // if a person is NOT on-call && is on shift right now
		        if (!isOnCall && isOnShiftNow){
		        	// suppress sleep habit right now
		        	person.updateValueSleepCycle(now, false);
		        }
		        
/*				
				ShiftType settementShift = s.getCurrentSettlementShift();

				
				if (s.getNumShift() == 2) {
					if (settementShift == shiftType) {
						
					}
				}
				
				else if (s.getNumShift() == 2) {
					
				}
*/		
				
			}
			
/*			else if (robot != null) {
				if (shiftTypeCache != null)
					robot.getSettlement().decrementAShift(shiftTypeCache);
				robot.getSettlement().incrementAShift(shiftType);
			}
*/
			this.shiftType = shiftType;
			
			// Call CircadianClock immediately to adjust the sleep hour according
			
			

			
		}
		else
			logger.warning("TaskSchedule: setShiftType() : " + person + "'s new shiftType is null");
	}

	
	/*
	 * Checks if a person is on shift
	 * @param time in millisols
	 * @return true or false
	 */
	public boolean isShiftHour(int millisols){
		boolean result = false;

		if (shiftType == ShiftType.A) {
			if (millisols == 1000 || (millisols >= A_START && millisols <= A_END))
				result = true;
		}

		else if (shiftType == ShiftType.B) {
			if (millisols >= B_START && millisols <= B_END)
				result = true;
		}

		if (shiftType == ShiftType.X) {
			if (millisols == 1000 || (millisols >= X_START && millisols <= X_END))
				result = true;
		}

		else if (shiftType == ShiftType.Y) {
			if (millisols >= Y_START && millisols <= Y_END)
				result = true;
		}

		else if (shiftType == ShiftType.Z) {
			if (millisols >= Z_START && millisols <= Z_END)
				result = true;
		}
		else if (shiftType == ShiftType.ON_CALL) {
			result = true;
		}

		return result;
	}


	/*
	 * This class represents a record of a given activity (task or mission) undertaken by a person
	 */
	public class OneActivity implements Serializable {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members
		private int taskName;
		private int description;
		private int phase;
		private int startTime;

		public OneActivity(int startTime, int taskName, int description, int phase) {
			this.taskName = taskName;
			this.description = description;
			this.startTime = startTime;
			this.phase = phase;
		}

		/**
		 * Gets the start time of the task.
		 * @return start time
		 */
		public int getStartTime() {
			return startTime;
		}

		/**
		 * Gets the task name.
		 * @return task name id
		 */
		public int getTaskName() {
			return taskName;
		}

		/**
		 * Gets the description what the actor is doing.
		 * @return description id
		 */
		public int getDescription() {
			return description;
		}


		/**
		 * Gets the task phase.
		 * @return task phase id
		 */
		public int getPhase() {
			return phase;
		}
	}


	/*
	 * This class represents a record of a given activity (task or mission) undertaken by a person
	 */
	public class OneTask implements Serializable {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members
		private String taskName;
		private String description;
		private String phase;
		private int startTime;

		public OneTask(int startTime, String taskName, String description, String phase) {
			this.taskName = taskName;
			this.description = description;
			this.startTime = startTime;
			this.phase = phase;
		}

		/**
		 * Gets the start time of the task.
		 * @return start time
		 */
		public int getStartTime() {
			return startTime;
		}

		/**
		 * Gets the task name.
		 * @return task name
		 */
		public String getTaskName() {
			return taskName;
		}

		/**
		 * Gets the description what the actor is doing
		 * @return what the actor is doing
		 */
		public String getDescription() {
			return description;
		}


		/**
		 * Gets the task phase.
		 * @return task phase
		 */
		public String getPhase() {
			return phase;
		}
	}

    public void destroy() {
    	person = null;
    	marsClock  = null;
    	robot  = null;
    	//todaySchedule = null;
        //schedules = null;
        allActivities = null;
        todayActivities =  null;
        shiftType = null;
        shiftTypePrevious = null;
    }
}