package gui;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dialog;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JDialog;
import javax.swing.DefaultListModel;
import javax.swing.BoxLayout;
import javax.swing.SwingConstants;

import src.DomainTree;
import src.CacheServer;
import src.Resolver;
import src.RequestType;

import misc.Utils;

public class SimulationWindow extends JFrame
{
    private final static String Title = "Dotoip - Simulating DNS service...";

    protected DomainTree  TLD;
    protected CacheServer server;
    protected Resolver    resolver;

    protected JTextField domainAddressField;
    protected JLabel     currentDomainAddressLabel;

    protected DomainTree displayedTree;
    protected JList<DomainTree> serverList;
    protected DefaultListModel<DomainTree> serverListModel;

    public SimulationWindow()
    {
    	super(Title);

    	this.TLD = new DomainTree();
    	this.server = new CacheServer(this.TLD);
    	this.resolver = new Resolver(this.server);
      this.displayedTree = this.TLD;

    	this.setSize(new Dimension(640, 480));

    	initComponents();

    	this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	this.setVisible(true);
    }

    public SimulationWindow(String domainDb, String recordDb)
    	throws IOException
    {
    	super(Title);

    	this.TLD = DomainTree.fromFile(domainDb);
    	this.TLD.loadResourceRecords(recordDb);
    	this.server = new CacheServer(this.TLD);
    	this.resolver = new Resolver(this.server);
      this.displayedTree = this.TLD;

      this.setSize(new Dimension(640, 480));

    	initComponents();
      updateDisplay();

    	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	this.setVisible(true);

    }

    private void initComponents()
    {
    	JPanel infoPanel = new JPanel();
    	infoPanel.setBorder(Utils.createTitledBorder("Information"));
    	currentDomainAddressLabel = new JLabel(".");
    	infoPanel.add(new JLabel("Current domain address:", SwingConstants.LEFT));
    	infoPanel.add(currentDomainAddressLabel);

    	JPanel tldPanel = new JPanel();
      tldPanel.setLayout(new BoxLayout(tldPanel, BoxLayout.LINE_AXIS));

    	JButton backButton    = new JButton("<<");
    	JButton forwardButton = new JButton(">>");

    	backButton.addActionListener((e) ->
    	{
  	    DomainTree aux = displayedTree.getParent();
  	    if(aux == null)
  	    {
  		    JOptionPane.showMessageDialog(null, "Current node has no parent",
  					      "Dotoip - Error", JOptionPane.ERROR_MESSAGE);
  	    }
  	    else
  	    {
      		displayedTree = aux;
      		updateDisplay();
          if(displayedTree.getParent() == null)
            backButton.setEnabled(false);
          forwardButton.setEnabled(false);
    	  }
    	});

    	forwardButton.addActionListener((e) ->
    	{
  	    DomainTree aux = serverList.getSelectedValue();
  	    if(aux == null)
  	    {
  		    JOptionPane.showMessageDialog(null, "No server was selected",
  					      "Dotoip - Error", JOptionPane.ERROR_MESSAGE);
  	    }
  	    else
  	    {
  		    displayedTree = aux;
          updateDisplay();
          backButton.setEnabled(true);
          forwardButton.setEnabled(false);
  	    }
    	});

      backButton.setEnabled(false);
      forwardButton.setEnabled(false);

      serverListModel = new DefaultListModel<DomainTree>();
      serverList = new JList<DomainTree>(serverListModel);
      serverList.addMouseListener(new DomainListMouseListener());
      serverList.addListSelectionListener((e) -> forwardButton.setEnabled(true));

      JScrollPane scrollPane = new JScrollPane(serverList);

    	tldPanel.add(backButton);
    	tldPanel.add(scrollPane);
    	tldPanel.add(forwardButton);

    	JPanel inputPanel = new JPanel();

    	domainAddressField = new JTextField();
    	JButton requestButton = new JButton("Ask");

    	domainAddressField.setPreferredSize(new Dimension(getWidth()*7/8 - 10 , 40));
    	requestButton.setPreferredSize(new Dimension(getWidth()/8 - 10, 40));

    	requestButton.addActionListener((e) ->
    	{
    	    String domainAddress = domainAddressField.getText();
    	    String address = resolver.askAndWait(domainAddress,
    						 RequestType.IPv4);
    	    System.out.println("Ip Address: " + address);
    	});

    	inputPanel.add(domainAddressField);
    	inputPanel.add(requestButton);

    	JPanel mainPanel = new JPanel();
    	mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
    	mainPanel.add(infoPanel);
    	mainPanel.add(tldPanel);
    	mainPanel.add(inputPanel);

    	this.add(mainPanel);
    }

    private boolean updateDisplay()
    {
      if(displayedTree != null)
      {
        currentDomainAddressLabel.setText(displayedTree.getDomainAddress());
        serverListModel.clear();
        for(DomainTree subtree : displayedTree.getSubtrees())
          serverListModel.addElement(subtree);
        return true;
      }
      return false;
    }

    private class DomainListMouseListener
      extends MouseAdapter
    {
      @Override
      public void mousePressed(MouseEvent e)
      {
        if(e.getClickCount() == 2)
        {
          DomainTree aux = serverList.getSelectedValue();
          if(aux != null)
          {
            displayedTree = aux;
            updateDisplay();
          }
        }
        else if(e.isPopupTrigger())
        {
          generatePopupMenu(e);
        }
      }

      private void generatePopupMenu(MouseEvent e)
      {
        DomainListPopupMenu popup = new DomainListPopupMenu();
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }

    private class DomainListPopupMenu
      extends JPopupMenu
      implements ActionListener
    {
      private JMenuItem addServer;
      private JMenuItem removeServer;

      public DomainListPopupMenu()
      {
        super();
        addServer    = new JMenuItem("Add new server...");
        removeServer = new JMenuItem("Remove selected server");

        addServer.addActionListener(this);
        removeServer.addActionListener(this);

        removeServer.setEnabled(serverList.getSelectedValue() != null);

        this.add(addServer);
        this.add(removeServer);
      }

      @Override
      public void actionPerformed(ActionEvent e)
      {
        Object src = e.getSource();
        if(src.equals(addServer))
        {
          AddServerDialog dialog = new AddServerDialog();
          if(dialog.exitState == AddServerDialog.EXIT_STATE_OK)
          {
            System.out.println("Popup closed successfully");
            updateDisplay();
          }
        }
        else if(src.equals(removeServer))
        {
          int answer = JOptionPane.showConfirmDialog(null, "Are you sure? Deleting this server will also eliminate all the sub-servers connected to this one",
                                                    "Dotoip - Warning",
                                                    JOptionPane.YES_NO_OPTION);
          if(answer == JOptionPane.YES_OPTION)
          {
            DomainTree aux = serverList.getSelectedValue();
            if(aux != null)
            {
              displayedTree.removeDomain(aux);
              updateDisplay();
            }
          }
        }
      }
    }

    private class AddServerDialog
      extends JDialog
      implements ActionListener
    {
      private final static String Title = "Dotoip - Add server";

      protected JTextField labelField;
      protected JButton    okButton;
      protected JButton    cancelButton;

      public int exitState;

      public final static int EXIT_STATE_OK = 0;
      public final static int EXIT_STATE_CANCELLED = 1;

      public AddServerDialog()
      {
        super(SimulationWindow.this, Title, Dialog.ModalityType.DOCUMENT_MODAL);

        this.setSize(new Dimension(440, 480));
        initComponents();

        this.setVisible(true);
      }

      private void initComponents()
      {
        JPanel labelPanel = new JPanel();
        labelPanel.setBorder(Utils.createTitledBorder("Server name"));

        JLabel labelLabel = new JLabel("<html>Server name (<i>label</i>) :</html>",
                                      SwingConstants.RIGHT);
        labelField = new JTextField();

        labelLabel.setPreferredSize(new Dimension(getWidth()/4, 40));
        labelField.setPreferredSize(new Dimension(getWidth()/2-10, 40));

        labelPanel.add(labelLabel);
        labelPanel.add(labelField);
        labelPanel.add(new JLabel("<html>(A valid label must be shorter than 64 characters.<br>Only alphanumerical values and hyphens are accepted.)</html>"));

        JPanel optionPanel = new JPanel(new GridLayout(1, 5));

        okButton = new JButton("OK");
        okButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        optionPanel.add(new JLabel());
        optionPanel.add(new JLabel());
        optionPanel.add(new JLabel());
        optionPanel.add(okButton);
        optionPanel.add(cancelButton);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        mainPanel.add(labelPanel);

        this.add(mainPanel,   BorderLayout.CENTER);
        this.add(optionPanel, BorderLayout.SOUTH);
      }

      @Override
      public void actionPerformed(ActionEvent e)
      {
        Object src = e.getSource();
        if(src.equals(okButton))
        {
          // TODO(marco) add checks that guarantees that the label is fine
          String label = labelField.getText();

          displayedTree.addDomain(label);

          exitState = EXIT_STATE_OK;
          dispose();
        }
        else if(src.equals(cancelButton))
        {
          exitState = EXIT_STATE_CANCELLED;
          dispose();
        }
      }
    }
}
