package gui;

import java.io.IOException;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import misc.Utils;

public class LoadSimulationWindow extends JFrame
{
  private final static String Title = "Dotoip - Load Setup";

  protected JTextField dbDomainField;
  protected JTextField dbRecordField;

  private final static String DefaultDomainDb = "domain-db.dat";
  private final static String DefaultRrDb = "rr-db.dat";

  public LoadSimulationWindow()
  {
    super(Title);

    initComponents();

    this.pack();
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setLocationRelativeTo(null);
    this.setResizable(false);
    this.setVisible(true);
  }

  private void initComponents()
  {
    JLabel domainDbLabel = new JLabel("Domain DB filepath:", SwingConstants.RIGHT);
    dbDomainField = new JTextField(DefaultDomainDb);
    JButton chooseDomainDb = new JButton("...");
    chooseDomainDb.addActionListener(new FileChooserOpener(dbDomainField));

    domainDbLabel.setPreferredSize(new Dimension(290, 20));
    dbDomainField.setPreferredSize(new Dimension(290, 20));
    chooseDomainDb.setPreferredSize(new Dimension(40, 20));

    JPanel domainPathPanel = new JPanel();
    domainPathPanel.add(domainDbLabel);
    domainPathPanel.add(dbDomainField);
    domainPathPanel.add(chooseDomainDb);

    JLabel recordDbLabel = new JLabel("Resource Record DB filepath:", SwingConstants.RIGHT);
    dbRecordField = new JTextField(DefaultRrDb);
    JButton chooseRecordDb = new JButton("...");
    chooseRecordDb.addActionListener(new FileChooserOpener(dbRecordField));

    recordDbLabel.setPreferredSize(new Dimension(290, 20));
    dbRecordField.setPreferredSize(new Dimension(290, 20));
    chooseRecordDb.setPreferredSize(new Dimension(40, 20));

    JPanel recordPathPanel = new JPanel();
    recordPathPanel.add(recordDbLabel);
    recordPathPanel.add(dbRecordField);
    recordPathPanel.add(chooseRecordDb);

    JButton loadDbsButton = new JButton("Load database");
    loadDbsButton.addActionListener((e) ->
    {
      String domainPath = dbDomainField.getText();
      if(domainPath == null)
      {
  	    JOptionPane.showMessageDialog(null,
  				      "No domain database path was choosen. Please choose one.",
  				      "Dotoip - Errore",
  				      JOptionPane.ERROR_MESSAGE);
      }
      String recordPath = dbRecordField.getText();
      if(recordPath == null)
  	  {
        JOptionPane.showMessageDialog(null,
  				      "No record database was choosen. Please choose one.",
  				      "Dotoip - Errore",
  				      JOptionPane.ERROR_MESSAGE);
      }

      try
      {
  	    new SimulationWindow(domainPath, recordPath);
  	    dispose();
      }
      catch(IOException ie)
      {
  	    ie.printStackTrace();
      }
    });

    JPanel pathsPanel = new JPanel(new GridLayout(3, 1));
    pathsPanel.setBorder(Utils.createTitledBorder("Databases paths"));
    pathsPanel.add(domainPathPanel);
    pathsPanel.add(recordPathPanel);
    pathsPanel.add(loadDbsButton);


    JPanel mainPanel = new JPanel();
    mainPanel.add(pathsPanel);

    this.add(mainPanel);
  }

  private class FileChooserOpener
    implements ActionListener
  {
    private JFileChooser fileChooser;
    private JTextField   refField;

    public FileChooserOpener(JTextField refField)
    {
        this.refField = refField;
        this.fileChooser = new JFileChooser();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        int result = fileChooser.showOpenDialog(LoadSimulationWindow.this);

        if(result == JFileChooser.APPROVE_OPTION)
    	{
    	    refField.setText(fileChooser.getSelectedFile().getAbsolutePath());
    	    repaint();
    	}
    }
  }
}
