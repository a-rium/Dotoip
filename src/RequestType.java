package src;

/** Enumeratore che indica il tipo di richiesta in base al tipo di indirizzo ricercato. */
public enum RequestType
{
  IPv4("A"),
  IPv6("AAAA");

  private String type;

  private RequestType(String type)
  {
    this.type = type;
  }
}
