package spiders;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import org.jsoup.*;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SpiderLeg {
	
	//private SpiderLeg(){}
	
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";//pretend we are using Internet browser
  private List<String> links = new LinkedList<String>();


  /**
   * This method is what actually crawls individual websites
   * @param String The URL to crawl
   * @return boolean If the crawl was successful or not
   */
  public boolean crawl(String url) {
    try {
      Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);//Connects to web page
      Document htmlDocument = connection.get();//get contents of web page

      if (!connection.response().contentType().contains("text/html"))//print out if connection failed
      {
        System.out.println("**Failure** Retrieved something other than HTML");
        return false;
      }
      Elements linksOnPage = htmlDocument.select("a[href]");//get all links from web page
      if(htmlDocument.getElementsByClass("dashboard").size()>0){
    	  System.out.println("downloadable stuff here");
      }
      if(htmlDocument.getElementsByTag("img").size() > 0){
      String imageLocation = htmlDocument.getElementsByTag("img").get(0).absUrl("src");
      System.out.println("found img");
 
      //Open a URL Stream

              URL url2 = new URL(imageLocation);

              InputStream in = url2.openStream();
              OutputStream out = new BufferedOutputStream(new FileOutputStream( "output/imgs/img"+System.currentTimeMillis()));
              for (int b; (b = in.read()) != -1;) {
                  out.write(b);
              }
              out.close();
              in.close();
      }
      
      
      for (Element link : linksOnPage) {
        this.links.add(link.absUrl("href"));//save all links in the list
      }

    } catch (IOException e) {//error handler, no need to print error, they are too common
      return false;
    }
    return true;
  }

  /**
   * This method gets the links from the page most recently crawled
   * @return List<String> The links from the last paged
   */
  public List<String> getLinks() {
    return this.links;
  }
}
