import java.io.IOException;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SpiderLeg {
	private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";//pretend we are using Internet browser
	private List<String> links = new LinkedList<String>();
	

	public boolean crawl(String url) {//crawls individual web page and saves the links, returns True if successful, False otherwise
		try{
			Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);//Connects to web page
			Document htmlDocument = connection.get();//get contents of web page
			
			if(!connection.response().contentType().contains("text/html"))//print out if connection failed
            {
                System.out.println("**Failure** Retrieved something other than HTML");
                return false;
            }
			
			Elements linksOnPage = htmlDocument.select("a[href]");//get all links from web page
			for(Element link:linksOnPage){
				this.links.add(link.absUrl("href"));//save all links in the list
			}
		
		}catch(IOException e){//error handler, no need to print error, they are too common
			return false;
		}
		return true;
	}

	public List<String> getLinks(){//return list of new links
		return this.links;
	}
}
