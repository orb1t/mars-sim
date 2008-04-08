/**
 * Mars Simulation Project
 * CreditTabPanel.java
 * @version 2.81 2007-08-25
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.standard.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.simulation.CollectionUtils;
import org.mars_sim.msp.simulation.Simulation;
import org.mars_sim.msp.simulation.Unit;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.structure.goods.CreditEvent;
import org.mars_sim.msp.simulation.structure.goods.CreditListener;
import org.mars_sim.msp.simulation.structure.goods.CreditManager;
import org.mars_sim.msp.ui.standard.MainDesktopPane;
import org.mars_sim.msp.ui.standard.MarsPanelBorder;
import org.mars_sim.msp.ui.standard.NumberCellRenderer;
import org.mars_sim.msp.ui.standard.unit_window.TabPanel;

public class CreditTabPanel extends TabPanel {
	
    /**
     * Constructor
     * @param unit the unit to display.
     * @param desktop the main desktop.
     */
	public CreditTabPanel(Unit unit, MainDesktopPane desktop) {
		// Use TabPanel constructor.
		super("Credit", null, "Trade Credit", unit, desktop);
		
        // Prepare credit label panel.
        JPanel creditLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topContentPanel.add(creditLabelPanel);
        
        // Prepare credit label.
        JLabel creditLabel = new JLabel("Trade Credit", JLabel.CENTER);
        creditLabelPanel.add(creditLabel);
        
		// Create scroll panel for the outer table panel.
		JScrollPane creditScrollPanel = new JScrollPane();
		creditScrollPanel.setPreferredSize(new Dimension(220, 280));
		topContentPanel.add(creditScrollPanel);         
        
        // Prepare outer table panel.
        JPanel outerTablePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        outerTablePanel.setBorder(new MarsPanelBorder());
        creditScrollPanel.setViewportView(outerTablePanel);   
        
        // Prepare credit table panel.
        JPanel creditTablePanel = new JPanel(new BorderLayout(0, 0));
        outerTablePanel.add(creditTablePanel);
        
        // Prepare credit table model.
        CreditTableModel creditTableModel = new CreditTableModel((Settlement) unit);
        
        // Prepare credit table.
        JTable creditTable = new JTable(creditTableModel);
        creditTable.setCellSelectionEnabled(false);
        creditTable.setDefaultRenderer(Double.class, new NumberCellRenderer(2));
        creditTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        creditTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        creditTable.getColumnModel().getColumn(2).setPreferredWidth(35);
        creditTablePanel.add(creditTable.getTableHeader(), BorderLayout.NORTH);
        creditTablePanel.add(creditTable, BorderLayout.CENTER);
	}
	
    /**
     * Updates the info on this panel.
     */
	public void update() {
		// Do nothing.
	}
	
    /** 
     * Internal class used as model for the power table.
     */
    private class CreditTableModel extends AbstractTableModel implements CreditListener {
    	
    	// Data members
    	CreditManager manager;
    	Collection settlements;
    	Settlement thisSettlement;
    	
    	private CreditTableModel(Settlement thisSettlement) {
    		this.thisSettlement = thisSettlement;
    		manager = Simulation.instance().getCreditManager();
    		
    		// Get collection of all other settlements.
    		settlements = new ConcurrentLinkedQueue();
    		Iterator<Settlement> i = CollectionUtils.sortByName(Simulation.instance().getUnitManager().getSettlements()).iterator();
    		while (i.hasNext()) {
    			Settlement settlement = i.next();
    			if (settlement != thisSettlement) settlements.add(settlement);
    		}
    		
    		manager.addListener(this);
    	}
    	
        public int getRowCount() {
            return settlements.size();
        }
        
        public int getColumnCount() {
            return 3;
        }
        
        public Class<?> getColumnClass(int columnIndex) {
            Class dataType = super.getColumnClass(columnIndex);
            if (columnIndex == 0) dataType = String.class;
            else if (columnIndex == 1) dataType = Double.class;
            else if (columnIndex == 2) dataType = String.class;
            return dataType;
        }
        
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) return "Settlement";
            else if (columnIndex == 1) return "VP";
            else if (columnIndex == 2) return "Type";
            else return "unknown";
        }
        
        public Object getValueAt(int row, int column) {
        	if (row < getRowCount()) {
        		Settlement settlement = (Settlement) settlements.toArray()[row];
        		if (column == 0) return settlement.getName();
        		else {
        			double credit = 0D;
        			try {
        				credit = manager.getCredit(thisSettlement, settlement);
        			}
        			catch (Exception e) {
        				e.printStackTrace(System.err);
        			}
        			
        			if (column == 1) return Math.abs(credit);
        			else if (column == 2) {
        				if (credit > 0D) return "credit";
        				else if (credit < 0D) return "debt";
        				else return "";
        			}
        			else return "unknown";	
        		}
            }
        	else return "";
        }
        
    	/**
    	 * Catch credit update event.
    	 * @param event the credit event.
    	 */
    	public void creditUpdate(CreditEvent event) {
    		if ((thisSettlement == event.getSettlement1()) || (thisSettlement == event.getSettlement2())) 
    			fireTableDataChanged();
    	}
    	
        /**
         * Prepare for deletion.
         */
        public void destroy() {
        	manager.removeListener(this);
        	settlements = null;
        	thisSettlement = null;
        }
    }
}