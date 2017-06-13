package gui;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.BoxLayout;


public class MainWindow extends JFrame
{
    private final static String Title = "Dotoip - Main Menu";
	
    public MainWindow()
    {
	super(Title);
		
	initComponents();
		
	this.setSize(new Dimension(320, 480));
	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	this.setVisible(true);
    }
	
    private void initComponents()
    {
	JButton newSimulationButton = new JButton("Start new simulation...");
	newSimulationButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
	newSimulationButton.addActionListener((e) ->
	{
	    new SimulationWindow();
	    dispose();
	});
		
	JButton loadSimulationButton = new JButton("Load existing simulation");
	loadSimulationButton.setAlignmentX(JButton.CENTER_ALIGNMENT); 
	loadSimulationButton.addActionListener((e) ->
	{
	    new LoadSimulationWindow();
	    dispose();
	});
	
	JPanel mainPanel = new JPanel();
	mainPanel.setLayout(new BoxLayout(mainPanel,
					  BoxLayout.PAGE_AXIS));
		
	mainPanel.add(newSimulationButton);
	mainPanel.add(loadSimulationButton);
	    
	this.add(mainPanel);
    }
}
