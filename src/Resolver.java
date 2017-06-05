package src;

import java.util.concurrent.ConcurrentHashMap;

public class Resolver
{    
    private CacheServer server;
    private ConcurrentHashMap<Integer, String> pendingRequests;

    private static final String NotSet = "NotSet";

    private static int globalRequestCounter = 0;
    
    public Resolver(CacheServer server)
    {
	this.server = server;
	this.pendingRequests = new ConcurrentHashMap<Integer, String>();
    }

    public int ask(String domainAddress, RequestType what)
    {
	// Basically ping pong
	// Search for A/AAAA until you get it
	// When NS or CNAME are found handle them by asking the servers
	int requestCode = globalRequestCounter++;
	ConnectionHandler connection = new ConnectionHandler(requestCode, domainAddress, what);
	connection.start();

	return requestCode;
    }
    
    public String askAndWait(String domainAddress, RequestType what)
    {
	int requestCode = ask(domainAddress, what);
	
	return getOrWait(requestCode);
    }

    public String getOrWait(int requestCode)
    {

	if(!pendingRequests.containsKey(requestCode))
	    return null;
	System.out.println("Waiting...");	
	while(pendingRequests.get(requestCode).equals(NotSet));
	
	String response = pendingRequests.get(requestCode);
	pendingRequests.remove(requestCode);


	return response;
    }

    private class ConnectionHandler extends Thread
    {
	private int         requestCode;
	private String      domainAddress;
	private RequestType what;
	
	public ConnectionHandler(int requestCode, String domainAddress, RequestType what)
	{
	    this.domainAddress = domainAddress;
	    this.what          = what;
	    this.requestCode   = requestCode;
	    pendingRequests.put(requestCode, NotSet);
	}

	public void run()
	{
	    int responseCode        = server.query(domainAddress, what);
	    ResourceRecord response = server.getOrWait(responseCode);

	    pendingRequests.put(requestCode, response.rdata);
       	}
    }
}
