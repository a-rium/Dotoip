package src;

import java.util.ArrayList;

public class Message
{
    public enum QueryMethod
    {
	ITERATIVE, RECURSIVE
    }
    
    public Header   header;
    public Question question;
    
    public ArrayList<ResourceRecord> answers;
    public ArrayList<ResourceRecord> authority;
    public ArrayList<ResourceRecord> additional;    
    
    public Message()
    {
	header   = new Header();
	question = new Question();
	
	answers    = new ArrayList<ResourceRecord>();
	authority  = new ArrayList<ResourceRecord>();
	additional = new ArrayList<ResourceRecord>();	
    }

    public class Header
    {
	public QueryMethod method = QueryMethod.ITERATIVE;
    }
    
    public class Question
    {
	public String name;
	public ResourceRecord.Type type;
    }
}
