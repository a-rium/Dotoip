package src;

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
