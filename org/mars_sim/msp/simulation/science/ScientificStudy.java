/**
 * Mars Simulation Project
 * ScientificStudy.java
 * @version 2.87 2009-06-29
 * @author Scott Davis
 */
package org.mars_sim.msp.simulation.science;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mars_sim.msp.simulation.Simulation;
import org.mars_sim.msp.simulation.person.Person;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.time.MarsClock;

/**
 * A class representing a scientific study.
 */
public class ScientificStudy implements Serializable {

    // Study Phases
    public static final String PROPOSAL_PHASE = "Study Proposal";
    public static final String INVITATION_PHASE = "Collaborator Invitation";
    public static final String RESEARCH_PHASE = "Research";
    public static final String PAPER_PHASE = "Writing Paper";
    public static final String PEER_REVIEW_PHASE = "Peer Review";
    
    // Completion States
    public static final String SUCCESSFUL_COMPLETION = "Successful Completion";
    public static final String FAILED_COMPLETION = "Failed Completion";
    public static final String CANCELLED = "Cancelled";
    
    // Maximum number of collaborative researchers.
    public static final int MAX_NUM_COLLABORATORS = 3;
    
    // Base amount of work time (millisols) required for proposal phase.
    private static final double BASE_PROPOSAL_WORK_TIME = 1000D;
    
    // Base amount of work time (millisols) required for primary research.
    private static final double BASE_PRIMARY_RESEARCH_WORK_TIME = 30000D;
    
    // Base amount of work time (millisols) required for collaborative research.
    private static final double BASE_COLLABORATIVE_RESEARCH_WORK_TIME = 10000D;
    
    // Base amount of work time (millisols) required for primary researcher 
    // writing study paper.
    private static final double BASE_PRIMARY_PAPER_WORK_TIME = 5000D;
    
    // Base amount of work time (millisols) required for collaborative researcher 
    // writing study paper.
    private static final double BASE_COLLABORATIVE_PAPER_WORK_TIME = 1000D;
    
    // Amount of time (millisols) allotted for peer review.
    private static final double PEER_REVIEW_TIME = 10000D;
    
    // Amount of time (millisols) allowed as downtime for primary work.
    static final double PRIMARY_WORK_DOWNTIME_ALLOWED = 30000D;
    
    // Amount of time (millisols) allowed as downtime for collaborative work.
    static final double COLLABORATIVE_WORK_DOWNTIME_ALLOWED = 30000D;
    
    // Data members
    private String phase;
    private Science science;
    private int difficultyLevel;
    private Person primaryResearcher;
    private Map<Person, Science> collaborativeResearchers;
    private Map<Person, Boolean> invitedResearchers;
    private double proposalWorkTime;
    private double primaryResearchWorkTime;
    private Map<Person, Double> collaborativeResearchWorkTime;
    private double primaryPaperWorkTime;
    private Map<Person, Double> collaborativePaperWorkTime;
    private MarsClock peerReviewStartTime;
    private boolean completed;
    private String completionState;
    private Settlement primarySettlement;
    private MarsClock lastPrimaryResearchWorkTime;
    private Map<Person, MarsClock> lastCollaborativeResearchWorkTime;
    
    /**
     * Constructor.
     * @param primaryResearcher the primary researcher for the study.
     * @param science the primary field of science in the study.
     * @param difficultyLevel the difficulty level of the study.
     */
    ScientificStudy(Person primaryResearcher, Science science, int difficultyLevel) {
        // Initialize data members.
        this.primaryResearcher = primaryResearcher;
        this.science = science;
        this.difficultyLevel = difficultyLevel;
        
        phase = PROPOSAL_PHASE;
        collaborativeResearchers = new HashMap<Person, Science>(MAX_NUM_COLLABORATORS);
        invitedResearchers = new HashMap<Person, Boolean>();
        proposalWorkTime = 0D;
        primaryResearchWorkTime = 0D;
        collaborativeResearchWorkTime = new HashMap<Person, Double>(MAX_NUM_COLLABORATORS);
        primaryPaperWorkTime = 0D;
        collaborativePaperWorkTime = new HashMap<Person, Double>(MAX_NUM_COLLABORATORS);
        peerReviewStartTime = null;
        completed = false;
        completionState = null;
        primarySettlement = primaryResearcher.getAssociatedSettlement();
        lastPrimaryResearchWorkTime = null;
        lastCollaborativeResearchWorkTime = new HashMap<Person, MarsClock>(MAX_NUM_COLLABORATORS);
    }
    
    /**
     * Gets the study's current phase.
     * @return phase
     */
    public String getPhase() {
        return phase;
    }
    
    /**
     * Sets the study's current phase.
     * @param phase the phase.
     */
    void setPhase(String phase) {
        this.phase = phase;
    }
    
    /**
     * Gets the study's primary field of science.
     * @return science
     */
    public Science getScience() {
        return science;
    }
    
    /**
     * Gets the study's difficulty level.
     * @return difficulty level.
     */
    public int getDifficultyLevel() {
        return difficultyLevel;
    }
    
    /**
     * Gets the study's primary researcher.
     * @return primary researcher
     */
    public Person getPrimaryResearcher() {
        return primaryResearcher;
    }
    
    /**
     * Gets the total amount of proposal work time required for the study.
     * @return work time (millisols).
     */
    public double getTotalProposalWorkTimeRequired() {
        return BASE_PROPOSAL_WORK_TIME * difficultyLevel;
    }
    
    /**
     * Gets the amount of work time completed for the proposal phase.
     * @return work time (millisols).
     */
    public double getProposalWorkTimeCompleted() {
        return proposalWorkTime;
    }
    
    /**
     * Adds work time to the proposal phase.
     * @param workTime work time (millisols) 
     */
    public void addProposalWorkTime(double workTime) {
        proposalWorkTime += workTime;
        double requiredWorkTime = getTotalProposalWorkTimeRequired();
        if (proposalWorkTime >= requiredWorkTime)
            proposalWorkTime = requiredWorkTime;
        
        // Update primary settlement.
        Settlement settlement = primaryResearcher.getAssociatedSettlement();
        if ((settlement != null) && !primarySettlement.equals(settlement)) 
            primarySettlement = settlement;
    }
    
    /**
     * Gets the study's collaborative researchers and their fields of science.
     * @return map of researchers and their sciences.
     */
    public Map<Person, Science> getCollaborativeResearchers() {
        return new HashMap<Person, Science>(collaborativeResearchers);
    }
    
    /**
     * Adds a collaborative researcher to the study.
     * @param researcher the collaborative researcher.
     * @param science the scientific field to collaborate with.
     */
    public void addCollaborativeResearcher(Person researcher, Science science) {
        collaborativeResearchers.put(researcher, science);
        collaborativeResearchWorkTime.put(researcher, 0D);
        collaborativePaperWorkTime.put(researcher, 0D);
        lastCollaborativeResearchWorkTime.put(researcher, null);
    }
    
    /**
     * Removes a collaborative researcher from a study.
     * @param researcher the collaborative researcher.
     */
    public void removeCollaborativeResearcher(Person researcher) {
        collaborativeResearchers.remove(researcher);
        collaborativeResearchWorkTime.remove(researcher);
        collaborativePaperWorkTime.remove(researcher);
        lastCollaborativeResearchWorkTime.remove(researcher);
    }
    
    /**
     * Checks if a researcher has already been invited to collaborate 
     * on this study.
     * @param researcher the researcher to check.
     * @return true if already invited.
     */
    public boolean hasResearcherBeenInvited(Person researcher) {
        return invitedResearchers.containsKey(researcher);
    }
    
    /**
     * Checks if an invited researcher has responded to the invitation.
     * @param researcher the invited researcher
     * @return true if reseacher has responded.
     */
    public boolean hasInvitedResearcherResponded(Person researcher) {
        boolean result = false;
        if (invitedResearchers.containsKey(researcher)) 
            result = invitedResearchers.get(researcher);
        return result;
    }
    
    /**
     * Adds a researcher to the list of researchers invited to collaborate 
     * on this study.
     * @param researcher the invited researcher.
     */
    public void addInvitedResearcher(Person researcher) {
        if (!invitedResearchers.containsKey(researcher))
            invitedResearchers.put(researcher, false);
    }
    
    /**
     * Sets that an invited researcher has responded.
     * @param researcher the invited researcher.
     */
    public void respondingInvitedResearcher(Person researcher) {
        if (invitedResearchers.containsKey(researcher))
            invitedResearchers.put(researcher, true);
    }
    
    /**
     * Gets the total work time required for primary research.
     * @return work time (millisols).
     */
    public double getTotalPrimaryResearchWorkTimeRequired() {
        return BASE_PRIMARY_RESEARCH_WORK_TIME * difficultyLevel;
    }
    
    /**
     * Gets the work time completed for primary research.
     * @return work time (millisols).
     */
    public double getPrimaryResearchWorkTimeCompleted() {
        return primaryResearchWorkTime;
    }
    
    /**
     * Adds work time for primary research.
     * @param workTime work time (millisols).
     */
    public void addPrimaryResearchWorkTime(double workTime) {
        primaryResearchWorkTime += workTime;
        double requiredWorkTime = getTotalPrimaryResearchWorkTimeRequired();
        if (primaryResearchWorkTime >= requiredWorkTime) {
            primaryResearchWorkTime = requiredWorkTime;
        }
        
        // Update primary settlement.
        Settlement settlement = primaryResearcher.getAssociatedSettlement();
        if ((settlement != null) && !primarySettlement.equals(settlement)) 
            primarySettlement = settlement;
        
        // Update last primary work time.
        lastPrimaryResearchWorkTime = (MarsClock) Simulation.instance().getMasterClock().
                getMarsClock().clone();
    }
    
    /**
     * Checks if primary research has been completed.
     * @return true if primary research completed.
     */
    public boolean isPrimaryResearchCompleted() {
        return (primaryResearchWorkTime >= getTotalPrimaryResearchWorkTimeRequired());
    }
    
    /**
     * Gets the total work time required for a collaborative researcher.
     * @return work time (millisols).
     */
    public double getTotalCollaborativeResearchWorkTimeRequired() {
        return BASE_COLLABORATIVE_RESEARCH_WORK_TIME * difficultyLevel;
    }
    
    /**
     * Gets the work time completed for a collaborative researcher.
     * @param researcher the collaborative researcher.
     * @return work time (millisols).
     */
    public double getCollaborativeResearchWorkTimeCompleted(Person researcher) {
        if (collaborativeResearchWorkTime.containsKey(researcher))
            return collaborativeResearchWorkTime.get(researcher);
        else throw new IllegalArgumentException(researcher + 
                " is not a collaborative researcher in this study.");
    }
    
    /**
     * Adds work time for collaborative research.
     * @param researcher the collaborative researcher.
     * @param workTime the work time (millisols).
     */
    public void addCollaborativeResearchWorkTime(Person researcher, double workTime) {
        if (collaborativeResearchWorkTime.containsKey(researcher)) {
            double currentWorkTime = collaborativeResearchWorkTime.get(researcher);
            currentWorkTime += workTime;
            double requiredWorkTime = getTotalCollaborativeResearchWorkTimeRequired();
            if (currentWorkTime >= requiredWorkTime) currentWorkTime = requiredWorkTime;
            collaborativeResearchWorkTime.put(researcher, currentWorkTime);
            
            // Update last collaborative work time.
            lastCollaborativeResearchWorkTime.put(researcher, (MarsClock) Simulation.instance().
                    getMasterClock().getMarsClock().clone());
        }
        else throw new IllegalArgumentException(researcher + 
                " is not a collaborative researcher in this study.");
    }
    
    /**
     * Checks if collaborative research has been completed by a given researcher.
     * @param researcher the collaborative researcher.
     */
    public boolean isCollaborativeResearchCompleted(Person researcher) {
        if (collaborativeResearchWorkTime.containsKey(researcher)) {
            double currentWorkTime = collaborativeResearchWorkTime.get(researcher);
            double requiredWorkTime = getTotalCollaborativeResearchWorkTimeRequired();
            return (currentWorkTime >= requiredWorkTime);
        }
        else throw new IllegalArgumentException(researcher + 
                " is not a collaborative researcher in this study.");
    }
    
    /**
     * Checks if all collaborative research has been completed.
     * @return true if research completed.
     */
    public boolean isAllCollaborativeResearchCompleted() {
        boolean result = true;
        Iterator<Person> i = collaborativeResearchWorkTime.keySet().iterator();
        while (i.hasNext()) {
            if (!isCollaborativeResearchCompleted(i.next())) result = false;
        }
        return result;
    }
    
    /**
     * Checks if all research in study has been completed.
     * @return true if research completed.
     */
    public boolean isAllResearchCompleted() {
        return (isPrimaryResearchCompleted() && isAllCollaborativeResearchCompleted());
    }
    
    /**
     * Gets the total work time required for primary researcher writing paper.
     * @return work time (millisols).
     */
    public double getTotalPrimaryPaperWorkTimeRequired() {
        return BASE_PRIMARY_PAPER_WORK_TIME * difficultyLevel;
    }
    
    /**
     * Gets the work time completed for primary researcher writing paper.
     * @return work time (millisols).
     */
    public double getPrimaryPaperWorkTimeCompleted() {
        return primaryPaperWorkTime;
    }
    
    /**
     * Adds work time for primary researcher writing paper.
     * @param workTime work time (millisols).
     */
    public void addPrimaryPaperWorkTime(double workTime) {
        primaryPaperWorkTime += workTime;
        double requiredWorkTime = getTotalPrimaryPaperWorkTimeRequired();
        if (primaryPaperWorkTime >= requiredWorkTime) {
            primaryPaperWorkTime = requiredWorkTime;
        }
        
        // Update primary settlement.
        Settlement settlement = primaryResearcher.getAssociatedSettlement();
        if ((settlement != null) && !primarySettlement.equals(settlement)) 
            primarySettlement = settlement;
    }
    
    /**
     * Checks if primary researcher paper writing has been completed.
     * @return true if primary researcher paper writing completed.
     */
    public boolean isPrimaryPaperCompleted() {
        return (primaryPaperWorkTime >= getTotalPrimaryPaperWorkTimeRequired());
    }
    
    /**
     * Gets the total work time required for a collaborative researcher writing paper.
     * @return work time (millisols).
     */
    public double getTotalCollaborativePaperWorkTimeRequired() {
        return BASE_COLLABORATIVE_PAPER_WORK_TIME * difficultyLevel;
    }
    
    /**
     * Gets the work time completed for a collaborative researcher writing paper.
     * @param researcher the collaborative researcher.
     * @return work time (millisols).
     */
    public double getCollaborativePaperWorkTimeCompleted(Person researcher) {
        if (collaborativePaperWorkTime.containsKey(researcher))
            return collaborativePaperWorkTime.get(researcher);
        else throw new IllegalArgumentException(researcher + 
                " is not a collaborative researcher in this study.");
    }
    
    /**
     * Adds work time for collaborative researcher writing paper.
     * @param researcher the collaborative researcher.
     * @param workTime the work time (millisols).
     */
    public void addCollaborativePaperWorkTime(Person researcher, double workTime) {
        if (collaborativePaperWorkTime.containsKey(researcher)) {
            double currentWorkTime = collaborativePaperWorkTime.get(researcher);
            currentWorkTime += workTime;
            double requiredWorkTime = getTotalCollaborativePaperWorkTimeRequired();
            if (currentWorkTime >= requiredWorkTime) currentWorkTime = requiredWorkTime;
            collaborativePaperWorkTime.put(researcher, currentWorkTime);
        }
        else throw new IllegalArgumentException(researcher + 
                " is not a collaborative researcher in this study.");
    }
    
    /**
     * Checks if collaborative paper writing has been completed by a given researcher.
     * @param researcher the collaborative researcher.
     */
    public boolean isCollaborativePaperCompleted(Person researcher) {
        if (collaborativePaperWorkTime.containsKey(researcher)) {
            double currentWorkTime = collaborativePaperWorkTime.get(researcher);
            double requiredWorkTime = getTotalCollaborativePaperWorkTimeRequired();
            return (currentWorkTime >= requiredWorkTime);
        }
        else throw new IllegalArgumentException(researcher + 
                " is not a collaborative researcher in this study.");
    }
    
    /**
     * Checks if all collaborative paper writing has been completed.
     * @return true if paper writing completed.
     */
    public boolean isAllCollaborativePaperCompleted() {
        boolean result = true;
        Iterator<Person> i = collaborativePaperWorkTime.keySet().iterator();
        while (i.hasNext()) {
            if (!isCollaborativePaperCompleted(i.next())) result = false;
        }
        return result;
    }
    
    /**
     * Checks if all paper writing in study has been completed.
     * @return true if paper writing completed.
     */
    public boolean isAllPaperWritingCompleted() {
        return (isPrimaryPaperCompleted() && isAllCollaborativePaperCompleted());
    }
    
    /**
     * Start the peer review phase of the study.
     */
    void startingPeerReview() {
        MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
        peerReviewStartTime = (MarsClock) currentTime.clone();
    }
    
    /**
     * Checks if peer review time has finished.
     * @return true if peer review time finished.
     */
    public boolean isPeerReviewTimeFinished() {
        boolean result = false;
        if (peerReviewStartTime != null) {
            MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
            double peerReviewTime = MarsClock.getTimeDiff(peerReviewStartTime, currentTime);
            if (peerReviewTime >= PEER_REVIEW_TIME) result = true;
        }
        return result;
    }
    
    /**
     * Sets the study as completed.
     * @param completionState the state of completion.
     */
    void setCompleted(String completionState) {
        completed = true;
        this.completionState = completionState;
    }
    
    /**
     * Checks if the study is completed.
     * @return true if completed.
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Gets the study's completion state.
     * @return completion state or null if not completed.
     */
    public String getCompletionState() {
        if (completed) return completionState;
        else return null;
    }
    
    /**
     * Gets the settlement where primary research is conducted.
     * @return settlement.
     */
    public Settlement getPrimarySettlement() {
        return primarySettlement;
    }
    
    /**
     * Gets the last time primary research work was done on the study.
     * @return last time or null if none.
     */
    public MarsClock getLastPrimaryResearchWorkTime() {
        return lastPrimaryResearchWorkTime;
    }
    
    /**
     * Gets the last time collaborative research work was done on the study.
     * @param researcher the collaborative researcher.
     * @return last time or null if none.
     */
    public MarsClock getLastCollaborativeResearchWorkTime(Person researcher) {
        MarsClock result = null;
        if (lastCollaborativeResearchWorkTime.containsKey(researcher)) 
            result = lastCollaborativeResearchWorkTime.get(researcher);
        return result;
    }
}