package src;

import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class CacheServer
{
  private String name;    // @Unused @ForNow

  private DomainTree TLD; // Top Level Domain

  private ConcurrentHashMap<String,  ArrayList<ResourceRecord>> cache;
  private ConcurrentHashMap<Integer, ResourceRecord> responses;

  public static final String LastFileWrote = ".serverResolver";

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

    private static final String LogDirectory = "log/server";
    private static final String LogPrefix    = "dt_search_";

    public ResourceGetter(String domainAddress, ResourceRecord.Type type,
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
      ArrayList<String>     log         = new ArrayList<>();
      ArrayList<String>     domainNames = new ArrayList<>();

      authorities.add(TLD);
      domainNames.add(domainAddress);

      ResourceRecord responseRecord = null;

      boolean gotAnswer = false;
      while(!gotAnswer && authorities.size() > 0)
      {
        DomainTree currentAuthority = authorities.remove(authorities.size()-1);  // pop()

        ArrayList<String> tempNames = new ArrayList<>(); // used to gather all the CNAMEs found
        for(String name : domainNames)
        {
          Message message = new Message();
          message.header.method = Message.QueryMethod.ITERATIVE;
          message.question.name = name;
          message.question.type = this.type;

          log.add("Richiesta: " + message.header.method + " query, cercando nel dominio '" + currentAuthority.getDomainAddress() + "' per " + this.type + " record di '" + name + "'");

          long elapsedTime = System.nanoTime();

          Message response = currentAuthority.query(message);

          elapsedTime = System.nanoTime() - elapsedTime;
          log.add("Risposta ricevuta in " + (((double)elapsedTime)/1000000000) + " secondi.");

          for(ResourceRecord rr : response.answers)
          {
            switch(rr.type)
            {
              case A:
              case AAAA:
              {
                if(type == rr.type)
                {
                  responseRecord = rr;
                  cache.get(domainAddress).add(rr);
                  gotAnswer = true;

                  log.add("----+ Ricevuto indirizzo IP: " + rr.rdata);
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

              log.add("---+ Indicato server delegato '" + rr.rdata + "'");
              } break;
              case CNAME:
              {
                tempNames.add(rr.rdata);

                log.add("--+ TRovato un alias '" + rr.rdata + "'");
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

        for(String names : tempNames) // Moving from the temporary array to the real one
          domainNames.add(names);
      }
      if(!gotAnswer)
      {
        log.add("Non e' stato possibile individuare una risposta appropriata per la richiesta");
        responseRecord = new ResourceRecord(domainAddress, ResourceRecord.Type.UNSET, 0, "Not found");
      }

      responses.put(responseCode, responseRecord);

      String filename = LogDirectory + "/" + LogPrefix + domainAddress.replaceAll("\\.", "") + ".txt";
      writeLog(filename, log);
    }

    private boolean writeLog(String filename, List<String> log)
    {
      try
      {
        PrintWriter out = new PrintWriter(new FileOutputStream(filename));

        out.println("Ricevuto messaggio da resolver, richiesto l'indirizzo IP di '" + domainAddress + "'");
        out.println("---------------------------------------------------");
        for(String line : log)
          out.println(line);

        out.close();

        Files.copy(new File(filename).toPath(),
                   new File(LastFileWrote).toPath(),
                   StandardCopyOption.REPLACE_EXISTING);

        return true;
      }
      catch(IOException ie)
      {
        return false;
      }
    }
  }
}
