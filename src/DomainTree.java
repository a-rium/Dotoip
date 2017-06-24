package src;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.File;

import java.nio.file.Files;
import java.nio.charset.Charset;

/** Classe che rappresenta un server del database dei domini.<br>
 *  Il server possiede un nome, puo' avere dei server delegati(dei sottoalberi) ma un solo server di livello superiore.
 *  Il server con un nome indicato da una stringa nulla e' speciale, ed e' detto TLD(Top Level Domain).<br>
 *  E' solitamente il server radice, sotto la quale stanno il resto dei server
 */
public class DomainTree
{
  /** Nome del server */
  public String label;
  private DomainTree parent;
  private ArrayList<DomainTree> subtrees;

  private ArrayList<ResourceRecord> rrs;

  /** Costruisce un DomainTree con valori di default */
  public DomainTree()
  {
  	this.label    = null;
  	this.subtrees = new ArrayList<DomainTree>();
  	this.rrs      = new ArrayList<ResourceRecord>();
  }

  /** Costruisce un DomainTree avente un dato nome */
  public DomainTree(String label)
  {
  	this.label    = label;
  	this.subtrees = new ArrayList<DomainTree>();
  	this.rrs      = new ArrayList<ResourceRecord>();
  }

  /** Costruisce un DomainTree avente un dato nome e un dato padre */
  public DomainTree(String label, DomainTree parent)
  {
    this.label    = label;
    this.parent   = parent;
    this.subtrees = new ArrayList<DomainTree>();
    this.rrs      = new ArrayList<ResourceRecord>();
  }

  /** Legge da un file di testo corrispondente ad un database di domini un DomainTree avente la struttura
   *  indicata dalla base di dati.
   *
   *  @param filename nome del database
   *  @return TLD
   *  @throws IOException Se il file indicato non esiste
   */
  public static DomainTree fromFile(String filename) throws IOException
  {
    List<String> lines = Files.readAllLines(new File(filename).toPath(), Charset.forName("UTF-8"));
    DomainTree root = new DomainTree();
    for(String line : lines)
    {
      String[] domains  = line.split("\\.");
      DomainTree cursor = root;
      for(String domain : domains)
      {
        if(domain.isEmpty()) continue;
        boolean exists = false;
        for(DomainTree subtree : cursor.subtrees)
        {
          if(subtree.label.equals(domain))
          {
            cursor = subtree;
            exists = true;
            break;
          }
        }
        if(!exists)
        cursor = cursor.addDomain(domain);
      }
    }
    return root;
  }

  /** Legge da un file di testo corrispondente ad un database di ResourceRecord e riempe
   *  l'albero del database dei server di dominio con i record letti in base alle istruzioni contenute nel database
   *
   *  @param filename nome del database
   *  @throws IOException Se il file indicato non esiste
   */
  public void loadResourceRecords(String filename) throws IOException
  {
    List<String> lines = Files.readAllLines(new File(filename).toPath(), Charset.forName("UTF-8"));

    String lastOwner = "";
    for(String line : lines)
    {
      line = line.trim().replaceAll("([ \t]+)", " ");

      String[] tokens = line.split(" ");
      // trueOwner owns the rr in the domain space
      if(tokens.length == 5)
      {
      	String trueOwner = tokens[0];
      	String owner = lastOwner;
      	int TTL      = Integer.parseInt(tokens[1]);
      	ResourceRecord.Type type = ResourceRecord.Type.fromString(tokens[2]);
        String rdata = tokens[4];
      	addResourceRecord(trueOwner, new ResourceRecord(owner,
      					                     type,
      					                     TTL,
      					                     rdata));
      }
      else if(tokens.length == 6)
      {
      	String trueOwner = tokens[0];
      	String owner = tokens[1];
      	int TTL      = Integer.parseInt(tokens[2]);
      	ResourceRecord.Type type = ResourceRecord.Type.fromString(tokens[3]);
              String rdata = tokens[5];
      	addResourceRecord(trueOwner,
      			              new ResourceRecord(owner,
            					    type,
            					    TTL,
            					    rdata));
      	lastOwner = owner;
      }
    }
  }

  /** Aggiunge il record al DomainTree avente il nome indicato.<br>
   *  Se quest'ultimo non corrisponde ne alla radice ne ad un nodo sottostante semplicemente non
   *  inserisce il record.
   *
   *  @param where indirizzo di dominio che indica il server alla quale aggiungere il record
   */
  private void addResourceRecord(String where, ResourceRecord rr)
  {
    if(where.equals("."))
    {
      this.rrs.add(rr); // This is bad.
      return;
    }
    String[] domains = where.split("\\.");
    int index = 0;
    // don't care if it's absolute domain address
    if(domains[0].isEmpty())
      index = 1;
    // @Todo change with getSubtree()
    DomainTree cursor = this;
    while(index < domains.length)
    {
      String domain = domains[index];

      boolean exists = false;
      for(DomainTree child : cursor.subtrees)
      {
      	if(child.label.equals(domain))
      	{
      	    cursor = child;
      	    exists = true;
      	    break;
      	}
      }

      if(!exists)
  	    return;

      index++;
    }
    cursor.rrs.add(rr);
  }

  /** Aggiunge il record a questo DomainTree */
  public void addResourceRecordToThis(ResourceRecord rr)
  {
    this.rrs.add(rr);
  }

  /** Aggiunge la lista dei record a questo DomainTree */
  public void addResourceRecords(List<ResourceRecord> rrs)
  {
    for(ResourceRecord rr : rrs)
    {
      this.rrs.add(rr);
    }
  }

  /** Aggiunge un nuovo DomainTree come sottoalbero corrente, lo denomina con il nome indicato
   *  e ne ritorna un riferimento.
   */
  public DomainTree addDomain(String domain)
  {
    DomainTree subtree = new DomainTree(domain, this);
    subtrees.add(subtree);
    return subtree;
  }

  /** Ritorna una lista dei sottoalberi */
  public List<DomainTree> getSubtrees()
  {
	   return subtrees;
  }

  /** Ritorna la profondita' del nodo, ovvero la distanza in nodi dalla radice */
  public int getDepth()
  {
    DomainTree cursor = this;
    int depth = 0;
    while(cursor.parent != null)
    {
      cursor = cursor.parent;
      depth++;
    }
    return depth;
  }

  /** Ritorna l'indirizzo di domini corrispondente al nodo.
   *
   *  @param reversed se vero ritorna una stringa nel formato www.example.com, altrimenti com.example.www
   */
  public String getDomainAddress(boolean reversed)
  {
    if(parent == null)
      return "";
    if(!reversed)
      return parent.getDomainAddress(reversed) + "." + label;
    else
      return label + "." + parent.getDomainAddress(reversed);
  }

  /** Ritorna l'indirizzo di domini in formato standard corrispondente al nodo.
   *
   *  @param reversed se vero ritorna una strina nel formato www.example.com, altrimenti com.example.www
   */
  public String getDomainAddress()
  {
    return getDomainAddress(false);
  }

  /** Ritorna il DomainTree padre di quello corrente */
  public DomainTree getParent()
  {
    return parent;
  }

  /** Ritorna un riferimento all'arrayc contenente i ResourceRecord del nodo */
  public ArrayList<ResourceRecord> getResourceRecords()
  {
    return rrs;
  }

  /** Ritorna il DomainTree corrispondente all'indirizzo di dominio indicato.
   *
   * @return il DomainTree cercato, null se non esiste
   */
  public DomainTree getSubtree(String domainAddress)
  {
    String[] domains = domainAddress.split("\\.");

    DomainTree cursor = this;
    for(String domain : domains)
    {
      // @Temp
      if(domain.isEmpty())
        continue;
      boolean found = false;
      for(DomainTree subtree : cursor.subtrees)
      {
        if(subtree.label.equals(domain))
        {
          cursor = subtree;
          found  = true;
          break;
        }
      }
      if(!found)
        return null;
    }
    return cursor;
  }

  /** Data la richiesta del messaggio compila un'adeguata risposta inserendo nelle varie sezioni
   *  del messaggio i ResourceRecord posseduti.
   *
   *  @param request messaggio di richiesta inviato da un server locale
   *  @return il messaggio di risposta
   */
  public Message query(Message request)
  {
    if(request.header.method == Message.QueryMethod.ITERATIVE)
    {
      Message response = new Message();

      String requestedAddress = request.question.name;
      for(ResourceRecord rr : rrs)
      {
        String owner = (rr.owner.startsWith(".")) ? rr.owner : "." + rr.owner;
      	if(owner.equals(requestedAddress))
      	{
    	    if(rr.type == request.question.type)
    	    {
        		 // @WhatIf there are multiple address for the given domain address
        		response.answers.add(rr);
    	    }
    	    else if(rr.type == ResourceRecord.Type.NS ||
    		    rr.type == ResourceRecord.Type.CNAME)
    	    {
    		    response.authority.add(rr);
    	    }
    	    // Implement CNAME
    	    else
    		    response.additional.add(rr);
      	}
      }
      return response;
    }
    return null;
  }

  /** Rimuove il DomainTree riferito da parametro */
  public boolean removeDomain(DomainTree e)
  {
    int i = 0;
    while(i<subtrees.size())
    {
      // @Important : I know that I am comparing the references
      // In this application I will be getting references always from the
      // same tree, so there is not any problem. However, if this coded
      // will be reused, it would be safer to change this to a value comparison
      if(e == subtrees.get(i))
      {
        subtrees.remove(i);
        return true;
      }
    }
    return false;
  }

  /** Ritorna il nome del DomainTree */
  @Override
  public String toString()
  {
    return label;
  }

  /** Ritorna una rappresentazione testuale del DomainTree e di tutti i nodi sottostanti */
  public String toStringSubtree()
  {
  	String spacing = "";
  	for(int i = 0; i<getDepth(); i++)
	    spacing += "----";

  	String string = spacing + this.label + '\n';
    for(DomainTree subtree : subtrees)
      string += subtree.toString();
  	return string;
  }

  /** Trascrive sul file un database dei domini testuale che puo' essere ricaricato con fromFile
   *
   *  @throws IOException in caso di errore di IO
   */
  public void writeDomains(File file)
    throws IOException
  {
    if(file.exists())
    {
      PrintWriter wout = new PrintWriter(new FileOutputStream(file));
      wout.close();
    }
    PrintWriter wout = new PrintWriter(new FileOutputStream(file, true));
    writeDomainsRecursive(wout);
    wout.close();
  }

  /** Metodo di supporto a writeDomains, utilizzata per scrivere sul file richiamando ricorsivamente questo metodo*/
  private void writeDomainsRecursive(PrintWriter out)
    throws IOException
  {
    if(subtrees.isEmpty())
    {
      if(label == null || label.isEmpty())
      {
        out.println(".");
      }
      else
      {
        out.println(getDomainAddress());
      }
    }
    else
    {
      for(DomainTree subtree : subtrees)
      {
        subtree.writeDomainsRecursive(out);
      }
    }
  }

  /** Trascrive sul file un database dei ResourceRecord testuale che puo' essere ricaricato con loadResourceRecords
   *
   *  @throws IOException in caso di errore di IO
   */
  public void writeResourceRecords(File file)
    throws IOException
  {
    if(file.exists())
    {
      PrintWriter wout = new PrintWriter(new FileOutputStream(file));
      wout.close();
    }
    PrintWriter wout = new PrintWriter(new FileOutputStream(file, true));
    writeRecords(wout);
    wout.close();
  }

  /** Metodo di supporto a writeResourceRecords, utilizzata per scrivere sul file richiamando ricorsivamente questo metodo*/
  private void writeRecords(PrintWriter out)
    throws IOException
  {
    for(ResourceRecord rr : rrs)
    {
      if(label == null || label.isEmpty())
      {
        out.println(". " + rr);
      }
      else
      {
        out.println(getDomainAddress() + " " + rr);
      }
    }
    // wout.close();
    for(DomainTree subtree : subtrees)
    {
      subtree.writeRecords(out);
    }
  }
}
