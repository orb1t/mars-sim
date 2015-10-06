/**
 * Mars Simulation Project
 * ResearchingSpaceApplication.java
 * @version 3.08 2015-10-05
 * @author Manny Kung
 */

package org.mars_sim.msp.core.reportingAuthority;

public class ResearchingSpaceApplication implements MissionAgenda {
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private final String name = "Researching Space Application";

	@Override
	public String getObjectiveName() {
		return name;
	}

	@Override
	public void reportFindings() {
		System.out.println("I'm putting together a report of possible applied space research in this frontier.");
	}

	@Override
	public void gatherSamples() {
		System.out.println("I'm analyzing how this local region may impact the scope of our research of interest.");
	}
}
