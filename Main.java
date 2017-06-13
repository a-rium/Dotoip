import java.io.IOException;
import java.io.File;

import src.DomainTree;
import src.CacheServer;
import src.Resolver;
import src.RequestType;

import gui.MainWindow;

public class Main
{
    public static void main(String[] args)
	throws IOException
    {
	new MainWindow();
	/*	String[] dirs = {"log/server", "log/resolver"};
	buildFilesystemStructure(dirs);
	    
	DomainTree domainSpace = DomainTree.fromFile(args[0]);
	domainSpace.loadResourceRecords(args[1]);
	
	CacheServer cacheServer = new CacheServer(domainSpace);
	Resolver resolver       = new Resolver(cacheServer);

	String ipAddr = resolver.askAndWait(args[2], RequestType.IPv4);

	// Cache test
	// String ipAddr2 = resolver.askAndWait(args[2], RequestType.IPv4);
	
	System.out.printf("'%s' corresponding IPv4 address is:  %s\n",
	args[2], ipAddr); */
    }

    private static void buildFilesystemStructure(String[] dirs)
    {
	for(String dir : dirs)
	{
	    File explorer = new File(dir);
	    if(!explorer.exists())
		explorer.mkdirs();
	}
    }
}
