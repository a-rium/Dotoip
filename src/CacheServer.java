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

  /** Nome del file temporaneo dove e' salvato una copia dell'ultimo log di conversazione*/
  public static final String LastFileWrote = ".serverResolver";

  private static int responseGlobalCounter = 0;

  /** Dato il TLD costruisce il server locale ad esso connesso */
  public CacheServer(DomainTree TLD)
  {
    this.TLD   = TLD;
    this.cache = new ConcurrentHashMap<String,
                       ArrayList<ResourceRecord>>();
    this.responses = new ConcurrentHashMap<Integer, ResourceRecord>();
  }

  /** Ritorna il record contenente la risposta alla richiesta del resolver avente il codice di risposta
   *  indicato, oppure rimane in attesa fino a quando non e' disponibile.
   *
   *  @param responseCode codice di risposta ottenuta dal metodo query
   *  @return il record con la risposta, null se il codice di risposta non corrisponde con alcuna richiesta
   */
  public ResourceRecord getOrWait(int responseCode)
  {
    if(!responses.containsKey(responseCode))
      return null;
    while(responses.get(responseCode) == ResourceRecord.INVALID);

    ResourceRecord response = responses.get(responseCode);
    responses.remove(responseCode);

    return response;
  }

  /** Controlla se nella cache e' contenuto un record che risponde alla richiesta.
   *  Se lo trova lo ritorna, altrimenti ritorna null
   *
   *  @param domainAddress indirizzo di dominio della quale si vuole sapere un indirizzo corrispondente
   *  @param what tipo di indirizzo richiesto
   *  @return record con la risposta, null se non e' presente
   */
  @Deprecated
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

  /** Richiede al database dei domini un determinato indirizzo.<br>
   *  Prima di iniziare una conversazione con i vari server del database controlla se
   *  nella cache e' contenuta un record che risponde alla richiesta.
   *  La chiamata di questo metodo non blocca l'esecuzione del programma fino a quando non viene ricevuta la risposta.
   *  Il valore intero puo' essere utilizzato per ottenere la risposta alla richiesta
   *  passandolo al metodo getOrWait
   *
   *  @param domainAddress indirizzo di dominio della quale si vuole sapere un indirizzo corrispondente
   *  @param what tipo di indirizzo richiesto
   *  @return record con la risposta, null se non e' presente
   */
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

  /** Controlla se nella cache e' contenuto un record che risponde alla richiesta.
   *  Se lo trova lo ritorna, altrimenti ritorna null
   *
   *  @param domainAddress indirizzo di dominio della quale si vuole sapere un indirizzo corrispondente
   *  @param what tipo di indirizzo richiesto
   *  @return record con la risposta, null se non e' presente
   */
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

  /** Thread generato dal metodo query che si occupa di comunicare
   *  con i server del database dei domini in modo asincrono al thread principale.
   *  Una volta ricevuta la risposta la inserisce nella HashMap contenente i risultati delle richieste,
   *  in modo che li si possa ottenere tramite la getOrWait.
   */
  private class ResourceGetter extends Thread
  {
    private String              domainAddress;
    private ResourceRecord.Type type;
    private int responseCode;

    private static final String LogDirectory = "log/server";
    private static final String LogPrefix    = "dt_search_";

    /** Costruisce il thread in modo che possa contattare i server e comunicargli la richiesta
     *
     *  @param responseCode indice della HashMap nella quale sara' salvata la risposta del server
    */
    public ResourceGetter(String domainAddress, ResourceRecord.Type type,
                          int responseCode)
    {
      this.domainAddress = domainAddress;
      this.type          = type;
      this.responseCode  = responseCode;
      cache.put(domainAddress, new ArrayList<ResourceRecord>());
      responses.put(responseCode, ResourceRecord.INVALID);
    }

    /** Metodo eseguito asincronicamente al thread principale. <br>
     *  Contatta il server TLD per ottenere i record che lo possano condurre alla risposta
     *  alla richiesta. Se viene indicato un delegato lo contatta per saperne di piu', e cosi via.<br>
     *  In caso venga trovato un alias iniziera' a richiedere i record relativi anche a server aventi il nome alternativo<br>
     *  Al termine della conversazione salva il risultato nella HashMap dei risultati all'indice indicato
    */
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

    /** Trascrive la conversazione avvenuta con i vari server del database dei domini
     *
     *  @param filename nome del file di log
     *  @param log stringhe corrispondenti al corpo del log
     *  @return esito della scrittura
     */
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
