/**
 * Mars Simulation Project
 * ESAMissionControl.java
 * @version 3.08 2015-10-05
 * @author Manny Kung
 */

package org.mars_sim.msp.core.reportingAuthority;

import java.io.Serializable;

/*
 * This class represents the Mission Control of the European Space Agency (ESA)
 */
public class ESAMissionControl extends ReportingAuthority
implements Serializable {
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private final String name = "ESA";

	private final String toolTipStr = "European Space Agency ";

	public String getToolTipStr() {
		return toolTipStr;
	}

	public String getName() {
		return name;
	}

	public ESAMissionControl() {
		missionAgenda = new DevelopingSpaceActivity();

	}

}
