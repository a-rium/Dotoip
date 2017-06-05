package src;

public class ResourceRecord
{
    public enum Type
    {
	UNSET, A, AAAA, CNAME, NS;

	public static Type fromString(String s)
	{
	    if(s.equals("A"))
		return A;
	    else if(s.equals("AAAA"))
		return AAAA;
	    else if(s.equals("CNAME"))
		return CNAME;
	    else if(s.equals("NS"))
		return NS;
	    return UNSET;
	}
    }
    
    public String owner;      // The owner is the full address of the DomainTree who the Resource Record is referring to
    public Type   type;
    public int    timeToLive;
    public String rdata;

    public static ResourceRecord INVALID = new ResourceRecord();
    
    public ResourceRecord()
    {
	this.owner      = "";
	this.type       = Type.UNSET;
	this.timeToLive = 0;
	this.rdata      = "";
    }

    public ResourceRecord(String owner, Type type,
			  int timeToLive, String rdata)
    {	
	this.owner      = owner;
	this.type       = type;
	this.timeToLive = timeToLive;
	this.rdata      = rdata;
    }
}
