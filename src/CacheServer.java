package src;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class CacheServer
{
    private String name;    // @Unused @ForNow

    private DomainTree TLD; // Top Level Domain
    
    private ConcurrentHashMap<String,  ArrayList<ResourceRecord>> cache;
    private ConcurrentHashMap<Integer, ResourceRecord> responses;

    private static int responseGlobalCounter = 0;
    
    public CacheServer(DomainTree TLD)
    {
	this.TLD   = TLD;
	this.cache = new ConcurrentHashMap<String,
	                                   ArrayList<ResourceRecord>>();
	this.responses = new ConcurrentHashMap<Integer, ResourceRecord>();
    }

    public ResourceRecord getOrWait(int responseCode)
    {
	if(!responses.containsKey(responseCode))
	    return null;
	while(responses.get(responseCode) == ResourceRecord.INVALID);

	ResourceRecord response = responses.get(responseCode);
	responses.remove(responseCode);
	
	return response;
    }

    // @Useless?
    public ResourceRecord getFromCache(String domainAddress,
				       RequestType what)
    {
	ResourceRecord.Type type = null;
	if(what == RequestType.IPv4)
	    type = ResourceRecord.Type.A;
	else if(what == RequestType.IPv6)
	    type = ResourceRecord.Type.AAAA;
	
	return checkCache(domainAddress, type);
    }
    
    public int query(String domainAddress, RequestType what)
    {
	ResourceRecord.Type type = null;
	if(what == RequestType.IPv4)
	    type = ResourceRecord.Type.A;
	else if(what == RequestType.IPv6)
	    type = ResourceRecord.Type.AAAA;

	int responseCode = responseGlobalCounter++;
	
	ResourceRecord cachedRecord = checkCache(domainAddress, type);
	if(cachedRecord != null) // cache-hit
	{
	    responses.put(responseCode, cachedRecord);
	    System.out.println("Cache-hit");
	    return responseCode;
	}
	    	
	ResourceGetter getter = new ResourceGetter(domainAddress, type,
						   responseCode);
	getter.start();

	return responseCode;
    }

    private ResourceRecord checkCache(String domainAddress,
				      ResourceRecord.Type type)
    {
	ArrayList<ResourceRecord> rrs = cache.get(domainAddress);

	if(rrs != null)
	{
	   for(ResourceRecord rr : rrs)
	   {
               if(rr.type == type)
		   return rr;
	   }
	}
	return null;
    }

    private class ResourceGetter extends Thread
    {
	private String              domainAddress;
	private ResourceRecord.Type type;
	private int responseCode;
	
	public ResourceGetter(String domainAddress,
			      ResourceRecord.Type type,
			      int responseCode)
	{
	    this.domainAddress = domainAddress;
	    this.type          = type;
	    this.responseCode  = responseCode;
	    cache.put(domainAddress, new ArrayList<ResourceRecord>());
	    responses.put(responseCode, ResourceRecord.INVALID);
	}

	public void run()
	{
       	    ArrayList<DomainTree> authorities = new ArrayList<>();
	    authorities.add(TLD);
	    
	    boolean gotAnswer = false;
	    while(!gotAnswer && authorities.size() > 0)
	    {
		DomainTree currentAuthority = authorities.remove(authorities.size()-1);  // pop()
		
		Message message = new Message();
		message.header.method = Message.QueryMethod.ITERATIVE;
		message.question.name = domainAddress;
		message.question.type = this.type;

		Message response = currentAuthority.query(message);

		for(ResourceRecord rr : response.answers)
		{
		    switch(rr.type)
		    {
		        case A:
		        case AAAA:
			{
			    if(type == rr.type)
			    {
				responses.put(responseCode, rr);
				cache.get(domainAddress).add(rr); 
				gotAnswer = true;
			    }
			} break;
		    }
		}
		    
		for(ResourceRecord rr : response.authority)
		{
		    switch(rr.type)
		    {
		        case NS:
			{
			    DomainTree delegate = TLD.getSubtree(rr.rdata);
			    authorities.add(delegate);
			} break;
		    }
		}

		for(ResourceRecord rr : response.additional)
		{
		    System.out.println(rr.rdata);
		    /* switch(rr.type)
		    {
		    }*/
		}
	    }
	    if(!gotAnswer)
		responses.put(responseCode, new ResourceRecord(domainAddress, ResourceRecord.Type.UNSET, 0, "Not found"));
	}
    }
}
