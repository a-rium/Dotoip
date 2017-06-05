package src;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import java.io.IOException;
import java.io.File;

import java.nio.file.Files;
import java.nio.charset.Charset;

public class DomainTree
{
    private String label;
    private DomainTree parent;
    private ArrayList<DomainTree> subtrees;

    private ArrayList<ResourceRecord> rrs;
    //    private HashMap<ResourceRecord.Type, ArrayList<ResourceRecord>> rrs;

    public DomainTree()
    {
	this.label    = null;
	this.subtrees = new ArrayList<DomainTree>();
	this.rrs      = new ArrayList<ResourceRecord>();
	//this.rrs      = new HashMap<ResourceRecord.Type, ArrayList<ResourceRecord>>();
    }
    
    public DomainTree(String label)
    {
	this.label    = label;
	this.subtrees = new ArrayList<DomainTree>();
	this.rrs      = new ArrayList<ResourceRecord>();
	//	this.rrs      = new HashMap<ResourceRecord.Type, ArrayList<ResourceRecord>>();
    }

    public DomainTree(String label, DomainTree parent)
    {
	this.label    = label;
	this.parent   = parent;
	this.subtrees = new ArrayList<DomainTree>();
	this.rrs      = new ArrayList<ResourceRecord>();
	//	this.rrs      = new HashMap<ResourceRecord.Type, ArrayList<ResourceRecord>>();
    }

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

    public void loadResourceRecords(String filename) throws IOException
    {
	List<String> lines = Files.readAllLines(new File(filename).toPath(), Charset.forName("UTF-8"));

	String lastOwner = "";
	for(String line : lines)
        {
	    line = line.trim().replaceAll("[{ +}\\t]", " ");

	    String[] tokens = line.split(" ");
	    // trueOwner owns the rr in the domain space
	    if(tokens.length == 5)
	    {
		String trueOwner = tokens[0];
		String owner = lastOwner;
		int TTL      = Integer.parseInt(tokens[1]);
		ResourceRecord.Type type = ResourceRecord.
		    Type.fromString(tokens[2]);
	        String rdata = tokens[4];
		addResourceRecord(trueOwner,
				  new ResourceRecord(owner,
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

    private void addResourceRecord(String where, ResourceRecord rr)
    {
	if(where.equals("."))
	{
	    this.rrs.add(rr);
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

    public DomainTree addDomain(String domain)
    {
	DomainTree subtree = new DomainTree(domain, this);
	subtrees.add(subtree);
	return subtree;
    }
    
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
    
    public String getDomainAddress(boolean reversed)
    {
	if(parent == null)
	    return "";
	if(!reversed)
	    return parent.getDomainAddress(reversed) + "." + label;
	else
	    return label + "." + parent.getDomainAddress(reversed);
    }

    public String getDomainAddress()
    {
	return getDomainAddress(false);
    }

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

    public Message query(Message request)
    {
	if(request.header.method == Message.QueryMethod.ITERATIVE)
	{
	    System.out.println("Responding...");
	    Message response = new Message();
	    
	    String requestedAddress = request.question.name;
	    System.out.println("RR array size: " + rrs.size());
	    for(ResourceRecord rr : rrs)
	    {
		if(rr.owner.equals(requestedAddress))
		{
		    if(rr.type == request.question.type)
		    {
			 // @WhatIf there are multiple address for the given domain address
			response.answers.add(rr);
		    }
		    else if(rr.type == ResourceRecord.Type.NS)
		    {
			response.authority.add(rr);
		    }
		    // Implement CNAME
		    else
			response.additional.add(rr);
		    System.out.println("Found " + rr.type);
		}
	    }
	    
	    return response;
	}
	return null;
    }

    @Override
    public String toString()
    {
	String spacing = "";
	for(int i = 0; i<getDepth(); i++)
	    spacing += "----";
	
	String string = spacing + this.label + '\n';
        for(DomainTree subtree : subtrees)
	    string += subtree.toString();
	return string;
    }
}
