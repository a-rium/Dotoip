import java.io.IOException;

import src.DomainTree;
import src.CacheServer;
import src.Resolver;
import src.RequestType;

public class Main
{
    public static void main(String[] args)
	throws IOException
    {
	DomainTree domainSpace = DomainTree.fromFile(args[0]);
	domainSpace.loadResourceRecords(args[1]);
	
	CacheServer cacheServer = new CacheServer(domainSpace);
	Resolver resolver       = new Resolver(cacheServer);

	String ipAddr = resolver.askAndWait(args[2], RequestType.IPv4);

	String ipAddr2 = resolver.askAndWait(args[2], RequestType.IPv4);
	
	System.out.printf("'%s' corresponding IPv4 address is:  %s\n",
			  args[1], ipAddr);
    }
}
