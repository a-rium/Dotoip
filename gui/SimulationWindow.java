package gui;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Dialog;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import java.nio.file.Files;

import java.util.List;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
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
import src.ResourceRecord;

import misc.Utils;

public class SimulationWindow
  extends JFrame
  implements ActionListener
{
    private final static String Title = "Dotoip - Simulating DNS service...";

    protected DomainTree  TLD;
    protected CacheServer server;
    protected Resolver    resolver;

    protected JButton backButton;
    protected JButton forwardButton;
    protected JButton requestButton;
    protected JButton returnButton;
    protected JButton saveButton;

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


    	initComponents();

      this.pack();
      // this.setSize(new Dimension(640, 480));
    	this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      this.setLocationRelativeTo(null);
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


    	initComponents();
      updateDisplay();

      this.pack();
      // this.setSize(new Dimension(640, 480));
    	this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      this.setLocationRelativeTo(null);
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

    	backButton    = new JButton("<<");
    	forwardButton = new JButton(">>");

    	backButton.addActionListener(this);
    	forwardButton.addActionListener(this);

      backButton.setEnabled(false);
      forwardButton.setEnabled(false);

      serverListModel = new DefaultListModel<DomainTree>();
      serverList = new JList<DomainTree>(serverListModel);
      serverList.addMouseListener(new DomainListMouseListener());
      serverList.addListSelectionListener((e) -> forwardButton.setEnabled(true));
		  serverList.setComponentPopupMenu(new DomainListPopupMenu());

      JScrollPane scrollPane = new JScrollPane(serverList);

    	tldPanel.add(backButton);
    	tldPanel.add(scrollPane);
    	tldPanel.add(forwardButton);

    	JPanel inputPanel = new JPanel();

    	domainAddressField = new JTextField();
    	requestButton      = new JButton("Ask");

    	domainAddressField.setPreferredSize(new Dimension(375, 40));
    	requestButton.setPreferredSize(new Dimension(90, 40));

    	requestButton.addActionListener(this);

    	inputPanel.add(domainAddressField);
    	inputPanel.add(requestButton);

    	JPanel mainPanel = new JPanel();
    	mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
    	mainPanel.add(infoPanel);
    	mainPanel.add(tldPanel);
    	mainPanel.add(inputPanel);

      JPanel optionPanel = new JPanel();

      returnButton = new JButton("<< Go back to main menu");
      returnButton.addActionListener(this);

      JButton saveButton = new JButton("Save");
      saveButton.addActionListener(this);

      optionPanel.add(returnButton);
      optionPanel.add(saveButton);

    	this.add(mainPanel,   BorderLayout.CENTER);
      this.add(optionPanel, BorderLayout.SOUTH);
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

    @Override
    public void actionPerformed(ActionEvent e)
    {
      Object src = e.getSource();
      if(src.equals(backButton))
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
    	}
      else if(src.equals(forwardButton))
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
    	}
      else if(src.equals(requestButton))
      {
    	    String domainAddress = domainAddressField.getText();
    	    String address = resolver.askAndWait(domainAddress,
    						                               RequestType.IPv4);
          try
          {
            List<String> resolverLogLines = Files.readAllLines(new File(Resolver.LastFileWrote).toPath());
            List<String> serverLogLines = Files.readAllLines(new File(CacheServer.LastFileWrote).toPath());
            ResponseLogDialog logDialog = new ResponseLogDialog(resolverLogLines, serverLogLines, address);
          }
          catch(IOException ie)
          {
            ie.printStackTrace();
          }
    	}
      else if(src.equals(returnButton))
      {
        new MainWindow();
        dispose();
      }
      else if(src.equals(saveButton))
      {
        JFileChooser chooser = new JFileChooser(".");

        int responseCode = chooser.showSaveDialog(SimulationWindow.this);
        if(responseCode == JFileChooser.CANCEL_OPTION)
        {
          return;
        }

        File domainDb = chooser.getSelectedFile();

        responseCode = chooser.showSaveDialog(SimulationWindow.this);
        if(responseCode ==  JFileChooser.APPROVE_OPTION)
        {
          try
          {
            File rrDb = chooser.getSelectedFile();
            TLD.writeDomains(domainDb);
            TLD.writeResourceRecords(rrDb);
          }
          catch(IOException ie)
          {
            ie.printStackTrace();
          }
        }
      }
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
            backButton.setEnabled(true);
            updateDisplay();
          }
        }
      }
    }

    private class DomainListPopupMenu
      extends JPopupMenu
      implements ActionListener
    {
      private JMenuItem addServer;
      private JMenuItem removeServer;
      private JMenuItem changeServer;
      private JMenuItem rootInfo;

      public DomainListPopupMenu()
      {
        super();
        addServer    = new JMenuItem("Add new server...");
        removeServer = new JMenuItem("Remove selected server");
        changeServer = new JMenuItem("Modify selected server");
        rootInfo     = new JMenuItem("Modify root server info");

        addServer.addActionListener(this);
        removeServer.addActionListener(this);
        changeServer.addActionListener(this);
        rootInfo.addActionListener(this);

        this.add(addServer);
        this.add(removeServer);
        this.add(changeServer);
        this.addSeparator();
        this.add(rootInfo);
      }

      @Override
      public void show(Component invoker, int x, int y)
      {
        removeServer.setEnabled(serverList.getSelectedValue() != null);
        changeServer.setEnabled(serverList.getSelectedValue() != null);
        super.show(invoker, x, y);
      }

      @Override
      public void actionPerformed(ActionEvent e)
      {
        Object src = e.getSource();
        if(src.equals(addServer))
        {
          SetupServerDialog dialog = new SetupServerDialog();
          if(dialog.exitState == SetupServerDialog.EXIT_STATE_OK)
          {
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
            forwardButton.setEnabled(false);
          }
        }
        else if(src.equals(changeServer))
        {
          DomainTree aux = serverList.getSelectedValue();
          SetupServerDialog dialog = new SetupServerDialog(aux);
          if(dialog.exitState == SetupServerDialog.EXIT_STATE_OK)
          {
            updateDisplay();
          }
        }
        else if(src.equals(rootInfo))
        {
          SetupServerDialog dialog = new SetupServerDialog(TLD, false);
        }
      }
    }

    private class SetupServerDialog
      extends JDialog
      implements ActionListener
    {
      private final static String Title = "Dotoip - Setup server";

      protected JTextField labelField;
      protected JButton    okButton;
      protected JButton    cancelButton;

      protected JButton addRecordButton;
      protected JButton removeRecordButton;
      protected JButton clearRecordButton;

      protected JCheckBox autoNsRecordCheck;

      protected DefaultListModel<ResourceRecord> rrModel;
      protected JList<ResourceRecord> rrList;

      protected List<ResourceRecord> rrs;
      protected List<ResourceRecord> backupRrs;

      protected DomainTree tree;

      public int exitState;

      public final static int EXIT_STATE_OK = 0;
      public final static int EXIT_STATE_CANCELLED = 1;

      public SetupServerDialog()
      {
        super(SimulationWindow.this, Title, Dialog.ModalityType.DOCUMENT_MODAL);

        this.rrs = new ArrayList<ResourceRecord>();

        this.setSize(new Dimension(440, 440));
        initComponents();
        //this.pack();

        this.setVisible(true);
      }

      public SetupServerDialog(DomainTree tree, boolean modifiableLabel)  // modify already existing tree
      {
        super(SimulationWindow.this, Title, Dialog.ModalityType.DOCUMENT_MODAL);

        this.tree = tree;
        this.rrs  = tree.getResourceRecords();

        this.backupRrs = new ArrayList<ResourceRecord>();
        for(ResourceRecord rr : rrs)
          backupRrs.add(rr);

        this.setSize(new Dimension(440, 440));
        initComponents();
        updateList();

        this.labelField.setText(tree.label);
        this.labelField.setEnabled(modifiableLabel);
        this.autoNsRecordCheck.setEnabled(false);

        this.setLocationRelativeTo(null);
        this.setVisible(true);
      }

      public SetupServerDialog(DomainTree tree)
      {
        this(tree, true);
      }

      private void initComponents()
      {
        JPanel labelPanel = new JPanel();
        labelPanel.setBorder(Utils.createTitledBorder("Server name"));

        JLabel labelLabel = new JLabel("<html>Server name (<i>label</i>) :</html>",
                                      SwingConstants.RIGHT);
        labelField = new JTextField();

        labelLabel.setPreferredSize(new Dimension(110, 40));
        labelField.setPreferredSize(new Dimension(210, 40));

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

        JPanel rrPanel = new JPanel();
        rrPanel.setLayout(new BoxLayout(rrPanel, BoxLayout.PAGE_AXIS));

        JPanel databasePanel = new JPanel();
        databasePanel.setLayout(new BoxLayout(databasePanel, BoxLayout.PAGE_AXIS));
        databasePanel.setBorder(Utils.createTitledBorder("RR database"));

        rrModel = new DefaultListModel<ResourceRecord>();
        rrList = new JList<ResourceRecord>(rrModel);
        JScrollPane scrollPane = new JScrollPane(rrList);

        autoNsRecordCheck = new JCheckBox("Add NS record to the parent server");
        autoNsRecordCheck.setSelected(false);

        JPanel databaseOptionPanel = new JPanel(new GridLayout(1, 3));
        addRecordButton    = new JButton("New record");
        removeRecordButton = new JButton("Remove");
        clearRecordButton  = new JButton("Clear All");

        addRecordButton.addActionListener(this);
        removeRecordButton.addActionListener(this);
        clearRecordButton.addActionListener(this);

        databaseOptionPanel.add(addRecordButton);
        databaseOptionPanel.add(removeRecordButton);
        databaseOptionPanel.add(clearRecordButton);

        databasePanel.add(scrollPane);
        databasePanel.add(autoNsRecordCheck);
        databasePanel.add(databaseOptionPanel);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        mainPanel.add(labelPanel);
        mainPanel.add(databasePanel);

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
          final String validDomainRegex = "([A-Za-z]([A-Za-z0-9]?(\\-[A-Za-z0-9])?)*)";
          String label = labelField.getText();

          if(label.length() >= 64 || (!label.matches(validDomainRegex) && labelField.isEnabled()))
          {
            JOptionPane.showMessageDialog(null,
                                          "Bad domain label, read the instructions",
                                          "Dotoip - Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;
          }
          if(tree == null)
          {
            tree = displayedTree.addDomain(label);
            tree.addResourceRecords(rrs);

            if(autoNsRecordCheck.isSelected())
            {
              for(ResourceRecord rr : rrs)
              {
                if(rr.type == ResourceRecord.Type.A
                  || rr.type == ResourceRecord.Type.AAAA)
                {
                  DomainTree parent = tree;
                  String treeDomainAddress = tree.getDomainAddress();
                  while((parent = parent.getParent()) != null)
                  {
                    ResourceRecord ns = new ResourceRecord(rr.owner,
                                                           ResourceRecord.Type.NS,
                                                           0,
                                                           treeDomainAddress);

                    parent.addResourceRecordToThis(ns);

                    treeDomainAddress = parent.getDomainAddress();
                  }
                }
              }
            }
          }
          else
          {
            tree.label = label;
          }

          exitState = EXIT_STATE_OK;
          dispose();
        }
        else if(src.equals(cancelButton))
        {
          exitState = EXIT_STATE_CANCELLED;
          if(tree != null)
          {
            rrs.clear();
            for(ResourceRecord rr : backupRrs)
              rrs.add(rr);
          }
          dispose();
        }
        else if(src.equals(addRecordButton))
        {
          AddRecordDialog dialog = new AddRecordDialog(this, displayedTree.getDomainAddress() + "." + labelField.getText());
          if(dialog.exitState == AddRecordDialog.EXIT_STATE_OK)
          {
            rrs.add(dialog.rr);
            updateList();
          }
        }
        else if(src.equals(removeRecordButton))
        {
          int i = rrList.getSelectedIndex();
          if(i >= 0)
          {
            rrs.remove(i);
            updateList();
          }
        }
        else if(src.equals(clearRecordButton))
        {
          rrs.clear();
          updateList();
        }
      }

      private void updateList()
      {
        rrModel.clear();
        for(ResourceRecord rr : rrs)
        {
          rrModel.addElement(rr);
        }
      }
    }

    private class AddRecordDialog
      extends JDialog
      implements ActionListener
    {
      private final static String Title = "Dotoip - Add Record Menu";

      private String holder;

      protected JTextField ownerField;
      protected JTextField ttlField;
      protected JTextField dataField;

      protected JComboBox<ResourceRecord.Type> typeBox;

      protected JButton okButton;
      protected JButton cancelButton;

      public int exitState;

      public final static int EXIT_STATE_OK = 0;
      public final static int EXIT_STATE_CANCELLED = 1;

      public ResourceRecord rr;

      public AddRecordDialog(SetupServerDialog ref, String holder)
      {
        super(ref, Title, Dialog.ModalityType.DOCUMENT_MODAL);

        this.holder = holder;

        initComponents();

        this.pack();
        this.setLocationRelativeTo(null);
        this.setVisible(true);
      }

      private void initComponents()
      {
        JPanel mainPanel = new JPanel(new GridLayout(6, 2));
        mainPanel.setBorder(Utils.createTitledBorder("Resource Record information"));

        ownerField = new JTextField();
        ttlField = new JTextField();
        dataField = new JTextField();
        typeBox = new JComboBox<ResourceRecord.Type>(ResourceRecord.Type.getEnums());

        mainPanel.add(new JLabel("Holder:", SwingConstants.LEFT));
        mainPanel.add(new JLabel(holder));

        mainPanel.add(new JLabel("Owner:", SwingConstants.LEFT));
        mainPanel.add(ownerField);

        mainPanel.add(new JLabel("TTL:", SwingConstants.LEFT));
        mainPanel.add(ttlField);

        mainPanel.add(new JLabel("Type:", SwingConstants.LEFT));
        mainPanel.add(typeBox);

        mainPanel.add(new JLabel("Class:", SwingConstants.LEFT));
        mainPanel.add(new JLabel("IN"));

        mainPanel.add(new JLabel("Data:", SwingConstants.LEFT));
        mainPanel.add(dataField);

        JPanel optionPanel = new JPanel(new GridLayout(1, 4));

        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");

        okButton.addActionListener(this);
        cancelButton.addActionListener(this);

        optionPanel.add(new JLabel());
        optionPanel.add(new JLabel());
        optionPanel.add(okButton);
        optionPanel.add(cancelButton);

        this.add(mainPanel,   BorderLayout.CENTER);
        this.add(optionPanel, BorderLayout.SOUTH);
      }

      public void actionPerformed(ActionEvent e)
      {
        Object src = e.getSource();
        if(src.equals(okButton))
        {
          try
          {
            int    ttl   = Integer.parseInt(ttlField.getText());
            if(ttl < 0)
            {
              throw new NumberFormatException("Invalid negative value inserted for TTL");
            }
            String owner = ownerField.getText();
            String data  = dataField.getText();
            ResourceRecord.Type type  = (ResourceRecord.Type) typeBox.getSelectedItem();

            // since the rdata contained in an rr is a generic string,
            // no control is done.

            rr = new ResourceRecord(owner, type, ttl, data);

            exitState = EXIT_STATE_OK;
            dispose();
          }
          catch(NumberFormatException nfe)
          {
            // TODO(marco) handle the exception
            JOptionPane.showMessageDialog(null, "Time to live (TTL) must be a positive or null integer",
                                          "Dotoip - Error", JOptionPane.ERROR_MESSAGE);
          }
        }
        else if(src.equals(cancelButton))
        {
          exitState = EXIT_STATE_CANCELLED;
          dispose();
        }
      }
    }

    private class ResponseLogDialog
      extends JDialog
    {
      private final static String Title = "Dotoip - Response Log";

      private List<String> resolverLogLines;
      private List<String> serverLogLines;
      private String response;

      public ResponseLogDialog(List<String> resolverLogLines, List<String> serverLogLines, String ipAddr)
      {
        super(SimulationWindow.this, Title, Dialog.ModalityType.DOCUMENT_MODAL);
        this.resolverLogLines = resolverLogLines;
        this.serverLogLines   = serverLogLines;
        this.response         = ipAddr;

        initComponents();

        this.setSize(new Dimension(600, 600));
        this.setVisible(true);
      }

      private void initComponents()
      {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

        String resolverLog;
        if(resolverLogLines.isEmpty())
        {
          resolverLog = "<html>Requested address was cached into the server.<br>"+
                        "<b>IP address: " + response + "</b></html>";
        }
        else
        {
          resolverLog = "<html>";
          for(String line : resolverLogLines)
          {
            resolverLog += line + "<br>";
          }
          resolverLog += "</html>";
        }

        JEditorPane resolverArea = new JEditorPane("text/html", resolverLog);
        resolverArea.setEditable(false);
        resolverArea.setBorder(Utils.createTitledBorder("Resolver log"));

        JScrollPane resolverScroll = new JScrollPane(resolverArea);

        String serverLog;
        String color;
        if(serverLogLines.isEmpty())
        {
           serverLog = "<html><i color='grey'>No message exchange, cache-hit</i></html>";
        }
        else
        {
          serverLog = "<html>";
          for(String line : serverLogLines)
          {
            if(line.startsWith("----+"))
            {
              color = "green";
            }
            else if(line.startsWith("---+"))
            {
              color = "blue";
            }
            else if(line.startsWith("--+"))
            {
              color = "purple";
            }
            else
            {
              color = "black";
            }
            serverLog += "<font color='"+color+"'>" + line + "</font><br>";
          }
          serverLog += "</html>";
        }

        JEditorPane serverArea = new JEditorPane("text/html", serverLog);
        serverArea.setEditable(false);
        serverArea.setBorder(Utils.createTitledBorder("Server resolve log"));

        JScrollPane serverScroll = new JScrollPane(serverArea);

        mainPanel.add(resolverScroll);
        mainPanel.add(serverScroll);

        JPanel optionPanel =  new JPanel();

        JButton okButton = new JButton("OK");
        okButton.addActionListener((e) ->
        {
          PrintWriter out;
          try
          {
            // cleaning the temporary files
            out = new PrintWriter(new FileOutputStream(new File(Resolver.LastFileWrote)));
            out.close();
            out = new PrintWriter(new FileOutputStream(new File(CacheServer.LastFileWrote)));
            out.close();
          }
          catch(IOException ie)
          {
            ie.printStackTrace();
          }
          dispose();
        });

        optionPanel.add(okButton);

        this.add(mainPanel, BorderLayout.CENTER);
        this.add(optionPanel, BorderLayout.SOUTH);
      }
    }
}
