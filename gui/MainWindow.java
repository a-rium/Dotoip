package gui;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.Box;

import java.io.IOException;

import misc.Utils;

public class MainWindow extends JFrame
{
  private final static String Title = "Dotoip - Main Menu";

  private final static String LogoFilename = "data/logo.png";

  public MainWindow()
  {
    super(Title);

    initComponents();

    pack();
    this.setSize(new Dimension(320, 560));
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setLocationRelativeTo(null);
    this.setVisible(true);
  }

  private void initComponents()
  {
    JLabel logoLabel = new JLabel();
    logoLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
    try
    {
      logoLabel.setIcon(Utils.readIcon(LogoFilename, 256, 256));
    }
    catch(IOException ie)
    {
      logoLabel.setText("<html><div align=center><font style='font-size: 30' color=orange>Do</font>" +
                        "<font style='font-size: 30' color=black>to</font>" +
                        "<font style='font-size: 30' color=orange>IP</font><br>" +
                        "<font style='font-size: 20' color=blue>DNS service simulator</font></div></html>");
    }
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

    JButton quitButton = new JButton("Quit");
    quitButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
    quitButton.addActionListener((e) -> dispose());

    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel,
    BoxLayout.PAGE_AXIS));

    mainPanel.add(logoLabel);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 50)));
    mainPanel.add(newSimulationButton);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 50)));
    mainPanel.add(loadSimulationButton);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 50)));
    mainPanel.add(quitButton);

    this.add(mainPanel);
  }
}
