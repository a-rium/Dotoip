package src;

import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.concurrent.ConcurrentHashMap;

/** Classe che rappresenta la componente del resolver, ovvero cio' che si occupa
 *  di contattare il server local per richiedere l'IP corrispondente ad un dato
 *  indirizzo di dominio.
 */
public class Resolver
{
  private CacheServer server;
  private ConcurrentHashMap<Integer, String> pendingRequests;

  private static final String NotSet = "NotSet";

  private static int globalRequestCounter = 0;

  private static final String LogDirectory  = "log/resolver";
  private static final String LogPrefix     = "resolve_";

  /** Nome del file temporaneo dove e' salvato una copia dell'ultimo log di conversazione*/
  public static final String LastFileWrote = ".resolverRequest";

  /** Costruisce il Resolver collegandolo al dato server*/
  public Resolver(CacheServer server)
  {
    this.server = server;
    this.pendingRequests = new ConcurrentHashMap<Integer, String>();
  }

  /** Richiede al server un determinato indirizzo.<br>
   *  La chiamata di questo metodo non blocca l'esecuzione del programma fino a quando non viene ricevuta la risposta.
   *  Il valore intero puo' essere utilizzato per ottenere la risposta alla richiesta
   *  passandolo al metodo getOrWait
   *
   *  @param domainAddress indirizzo di dominio della quale si vuole sapere un indirizzo corrispondente
   *  @param what il tipo di indirizzo richiesto
   *  @return un intero che permette l'accesso alla risposta
   */
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

  /** Richiede al server un determinato indirizzo.<br>
   *  La chiamata di questo metodo blocca l'esecuzione del programma fino a quando non viene ricevuta la risposta.
   *
   *  @param domainAddress indirizzo di dominio della quale si vuole sapere un indirizzo corrispondente
   *  @param what il tipo di indirizzo richiesto
   *  @return indirizzo richiesto o stringa di errore.
   */
  public String askAndWait(String domainAddress, RequestType what)
  {
    int requestCode = ask(domainAddress, what);

    return getOrWait(requestCode);
  }

  /** Ottiene la risposta ad una data richiesta. Se la risposta non e' ancora stata pervenuta
   *  il metodo aspetta fino a quando non sara' ricevuta.
   *
   *  @param requestCode intero ottenuto come valore di ritorno di ask
   *  @return indirizzo richiesto o stringa di errore, null se requestCode non corrisponde ad alcuna richiesta
   */
  public String getOrWait(int requestCode)
  {

    if(!pendingRequests.containsKey(requestCode))
    {
      return null;
    }
    System.out.println("Waiting...");
    while(pendingRequests.get(requestCode).equals(NotSet));

    String response = pendingRequests.get(requestCode);
    pendingRequests.remove(requestCode);

    return response;
  }

  /** Thread generato dai metodi ask e askAndWait che si occupano di comunicare
   *  con il server in modo asincrono al thread principale.
   *  Una volta ricevuta la risposta la inserisce nella HashMap contenente i risultati delle richieste,
   *  in modo che l'utente lo possa ottenere tramite la getOrWait.
   */
  private class ConnectionHandler extends Thread
  {
    private int         requestCode;
    private String      domainAddress;
    private RequestType what;

    /** Costruisce il thread in modo che possa contattare il server e comunicargli la richiesta
     *
     *  @param requestCode indice della HashMap nella quale sara' salvata la risposta del server
    */
    public ConnectionHandler(int requestCode, String domainAddress, RequestType what)
    {
      this.domainAddress = domainAddress;
      this.what          = what;
      this.requestCode   = requestCode;
      pendingRequests.put(requestCode, NotSet);
    }

    /** Metodo eseguito asincronicamente al thread principale. <br>
     *  Contatta il server, scrive il log e inserisce la risposta nella HashMap al dato indice
    */
    public void run()
    {
      long time = System.nanoTime();

      int responseCode        = server.query(domainAddress, what);
      ResourceRecord response = server.getOrWait(responseCode);
      pendingRequests.put(requestCode, response.rdata);

      time = System.nanoTime() - time;
      String filename = LogDirectory + "/" + LogPrefix + domainAddress.replaceAll("\\.", "") + ".txt";
      writeLog(filename, response, time);
    }

    /** Trascrive la conversazione avvenuta con il server su un file di log e su file temporaneo
     *
     *  @param filename nome del file di log
     *  @param response record ottenuto in risposta dal server
     *  @param nanoTime tempo di conversazione in nanosecondi
     *  @return esito della scrittura
     */
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
