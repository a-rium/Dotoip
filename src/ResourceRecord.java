package src;

/** Classe che rappresenta un Resource Record, ovvero i record dentro la quale sono
 *  contenuti dati che vanno da l'informazione ricercata ad indicazioni utili per raggiungere la suddetta,
 *  come nomi server delegati ed alias del server ricercato.
*/
public class ResourceRecord
{
  /** Tipologia del record che indica come utilizzare la sezione RDATA */
  public enum Type
  {
    UNSET, A, AAAA, CNAME, NS;

    /** Dato una stringa ritorna l'enum associato
     *  @param  s stringa in input
     *  @return tipo corrispondente
    */
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

    public static Type[] getEnums()
    {
      Type[] types = new Type[4];

      types[0] = A;
      types[1] = AAAA;
      types[2] = CNAME;
      types[3] = NS;

      return types;
    }
  }

  /** Stringa che indica il server alla quale e' riferito il record */
  public String owner;      // The owner is the full address of the DomainTree who the Resource Record is referring to
  /** Tipo del dato contenuto nel record*/
  public Type   type;
  /** Tempo (in secondi) di validita' del record in una cache */
  public int    timeToLive;
  /** Dato il cui significato varia in base al tipo di record */
  public String rdata;

  public static ResourceRecord INVALID = new ResourceRecord();

  /** Costruttore standard che costruisce gli attributi con valori di default */
  public ResourceRecord()
  {
  	this.owner      = "";
  	this.type       = Type.UNSET;
  	this.timeToLive = 0;
  	this.rdata      = "";
  }

  /** Costruttore con parametri */
  public ResourceRecord(String owner, Type type,
		  int timeToLive, String rdata)
  {
  	this.owner      = owner;
  	this.type       = type;
  	this.timeToLive = timeToLive;
  	this.rdata      = rdata;
  }

  /** Ritorna una rappresentazione testuale del record.
   * @return Rappresentazione testuale del record
  */
  @Override
  public String toString()
  {
    return owner + " " + timeToLive + " " + type + " IN " + rdata;
  }
}
