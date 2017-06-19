package src;

import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.concurrent.ConcurrentHashMap;

public class Resolver
{
    private CacheServer server;
    private ConcurrentHashMap<Integer, String> pendingRequests;

    private static final String NotSet = "NotSet";

    private static int globalRequestCounter = 0;

    private static final String LogDirectory  = "log/resolver";
    private static final String LogPrefix     = "resolve_";

    public static final String LastFileWrote = ".resolverRequest";

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
	    long time = System.nanoTime();

	    int responseCode        = server.query(domainAddress, what);
	    ResourceRecord response = server.getOrWait(responseCode);
	    pendingRequests.put(requestCode, response.rdata);

	    time = System.nanoTime() - time;
	    String filename = LogDirectory + "/" + LogPrefix + domainAddress.replaceAll("\\.", "") + ".txt";
	    if(!writeLog(filename, response, time))
		System.out.println("Printed correctly");
       	}

	private boolean writeLog(String filename, ResourceRecord response, long nanoTime)
	{
	    try
	    {
    		PrintWriter out = new PrintWriter(new FileOutputStream(filename));

    		out.println("Starting exchange with the cache server");
    		out.println("-------------------------------------------------------");
    		out.println("Requested IP address of: " + domainAddress);
    		out.println("Cache server response: "   + response.rdata);
    		out.println("Time elapsed: " + (((double)nanoTime)/100000000));

    		out.close();

        Files.copy(new File(filename).toPath(),
                   new File(LastFileWrote).toPath(),
                   StandardCopyOption.REPLACE_EXISTING);

    		return true;
	    }
	    catch(IOException ie)
	    {
		ie.printStackTrace();
		return false;
	    }
	}
    }
}
