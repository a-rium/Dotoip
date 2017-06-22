package src;

import java.util.ArrayList;

/**
 *  Classe che rappresenta un messaggio utilizzato nel dialogo tra server locale e
 *  ed i server del database per il trasporto di ResourceRecord.
 *  Tutti i suoi attributi sono pubblici, in modo da essere direttamente accessibili
 *  da chiunque.
 */
public class Message
{
  /** Enumeratore che indica la tipologia della query richiesta */
  public enum QueryMethod
  {
  	ITERATIVE, RECURSIVE
  }

  /** Header del messaggio */
  public Header   header;
  /** Domanda del messaggio */
  public Question question;

  /** Record che contengono le informazioni richieste (Recorda A e AAAA)*/
  public ArrayList<ResourceRecord> answers;
  /** Record che contengono informazioni utili per poter rintracciare il server con i record ricercati(Record NS e CNAME) */
  public ArrayList<ResourceRecord> authority;
  /** Record che contengono informazioni addizionali a quelle richieste */
  public ArrayList<ResourceRecord> additional;

  /**
   *  Costruttore standard, costruisce gli attributi con i valori di default
   */
  public Message()
  {
  	header   = new Header();
  	question = new Question();

  	answers    = new ArrayList<ResourceRecord>();
  	authority  = new ArrayList<ResourceRecord>();
  	additional = new ArrayList<ResourceRecord>();
  }

  /** Header del messaggio dove e' indicato il tipo di richiesta */
  public class Header
  {
	  public QueryMethod method = QueryMethod.ITERATIVE;
  }

  /** Domanda contenuta in un Message, indica il nome del server alla quale e' indirizzato ed il tipo di record richiesto. */
  public class Question
  {
    public String name;
    public ResourceRecord.Type type;
  }
}
