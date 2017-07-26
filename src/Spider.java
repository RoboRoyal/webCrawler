package spiders;

import java.util.*;
import org.apache.log4j.Logger;
import org.w3c.dom.events.EventException;

public class Spider implements Runnable {
  private static int maxPages = 20;
  private static Set<String> pagesVisited = new HashSet<>();
  private static List<String> pagesToVisit = Collections.synchronizedList(new LinkedList<String>());
  private static Set<String> blackListDomains = new HashSet<>();
  private static Set<String> whiteListDomains = new HashSet<>();
  private static boolean quiet = false;
  public static boolean doDomainSearch = false;
  public static boolean run = true;
  private static Logger logger = Logger.getLogger(Spider.class.getCanonicalName());

  private int problems;
  private int success;
  private boolean black = true;
  private Thread t;
  String name;

  public Spider() {
    problems = 0;
    success = 0;
    name = "demo";
  }

  public Spider(String threadName) {//takes in name to set thread as
    problems = 0;
    success = 0;
    name = threadName;
  }

  /**
   * This method searches through possible URLs and picks out one worth crawling
   *
   * @return String This is the next valid URL to crawl
   */
  private String getNextURL() {
    String nextURL;
    do {
      nextURL = Spider.pagesToVisit.remove(0);//get next URL in list
    } while ((black && isBadURL(nextURL)) || (!black && !isGoodURL(nextURL)));//loop through list until we find a good URL
    pagesVisited.add(nextURL);//add new URL to the set we visited
    return nextURL;
  }

  /**
   * This method starts webcrawling on one thread
   *
   * @param url This is the first URL to crawl
   */
  public void startTraffic(String url) {
    pagesToVisit.add(url);
    run();
  }

  /**
   * Primary crawling method
   */
  private void crawlInternet() {
		while (pagesVisited.size() < maxPages && run) {
			String currentURL;
			SpiderLeg leg = new SpiderLeg();
			if (pagesToVisit.isEmpty()) {// end thread if its out of pages
				logger.warn("Out of URLS to crawl, ending thread " + this.name);
				break;
			} else {
				currentURL = getNextURL();
			}
			if (leg.crawl(currentURL)) {
				try {
					for (String newURL : leg.getLinks()) {// try to add only new links
						if (!pagesToVisit.contains(newURL)) {
							pagesToVisit.add(newURL);
						}
					}
				} catch (Exception k) {
					try {
						Thread.sleep(12);
					} catch (Exception e) {
						logger.trace(e);}
					if (!quiet) {
						logger.error("Problem adding links on thread: " + this.name + ": " + k);}
					pagesToVisit.addAll(leg.getLinks());// force add all links
				}
				success++;
			} else {
				problems++;
			}
		}
	}

  /**
   * This method returns if the given URL is bad using the black list
   *
   * @param url The URL to test
   * @return boolean True if the URL is bad, False if it is worth crawling
   */
  public boolean isBadURL(String url) {

    if (pagesVisited.contains(url)) {//checks if this site has been visited before
      return true;
    }
    if(url == ""){
    	return true;
    }
    for (String blackURL : blackListDomains) {//checks if URL is blacklisted
      if (url.contains(blackURL)) {
        return true;
      }
    }//check to do domain search
    if (doDomainSearch && this.searchDomains(url)) {
      return true;
    }
    return false;
  }

  /**
   * This method returns if a URL is good using the white list
   *
   * @param url The URL to test
   * @return boolean False if the URL is bad, True if it is worth crawling
   */
  public boolean isGoodURL(String url) {
    if (pagesVisited.contains(url)) {//checks if this is a new site
      return false;
    }
    for (String white : whiteListDomains) {//check if next URL is part of whitelist domain
      if (url.contains(white)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This method is used to set weather or not to use black list or white list
   *
   * @param useBlack Set True to use black list or False to use white list
   */
  public void useBlackList(boolean useBlack) {
    black = useBlack;
  }

  /**
   * This method is used to add URLs/domains to the black list
   *
   * @param domain The domain to add to black list
   */
  public void addBlacklistedDomain(String domain) {
    blackListDomains.add(domain);
  }

  /**
   * This method is used to add URLs/domains to the white list
   *
   * @param domain The domain to add to white list
   */
  public void addWhitelistedDomain(String domain) {
    whiteListDomains.add(domain);
  }

  /**
   * This method is used to set the max number of pages to crawl
   *
   * @param new_max The new max number of pages to crawl
   */
  public static void setMax(int newMax) {
    maxPages = newMax;
  }

  /**
   * This method is used to add to the URLs to be crawled
   *
   * @param new_url Another URL to be crawled
   */
  public void addURL(String newUrl) {
    pagesToVisit.add(newUrl);
  }

  /**
   * This method is used to get the number of successful page crawls
   *
   * @return int The number of pages successfully crawled
   */
  public int getSuccess() {
    return success;
  }

  /**
   * This method is used to get the number of pages unsuccessfully crawled
   *
   * @return int The number of pages unsuccessfully crawled
   */
  public int getProblem() {
    return problems;
  }

  /**
   * This method is used to get the pages crawls
   *
   * @return Set<String> The pages that were crawled
   */
  public Set<String> getPagesVisited() {
    return pagesVisited;
  }

  /**
   * This initializes the thread
   */
  public void start() {
    if (t == null) {
      t = new Thread(this, name);
      t.start();
    }
  }

  /**
   * This method is used to check if the given URL is part of a domain that has already been visited
   *
   * @param new_url The domain to be searched against visited URLs
   *        @ return boolean If the domain has already been visited
   */
  public boolean searchDomains(String newUrl) {
	    try {
	      for (String URL : pagesVisited) {//searches through lists
	        if (URL.replaceAll("//", " ").replaceAll("/.*", " ")
	            .contains(newUrl.replaceAll("//", " ").replaceAll("/.*", " "))) {
	          return true;
	        }
	      }
	    } catch (Exception e) {//on fail, try one more time, otherwise return false
	      logger.trace("Problem in search domain; thread: " + this.name + ";Error: " + e);
	      try {
	        for (String URL : pagesVisited) {
	          if (URL.replaceAll("//", " ").replaceAll("/.*", " ")
	              .contains(newUrl.replaceAll("//", " ").replaceAll("/.*", " "))) {
	            return true;
	          }
	        }
	      } catch (Exception dlv) {
	        if(!quiet) {
	        	logger.error("Problem in search domain, forcing true; thread: " + this.name + "; Error: "
	            + dlv);}
	        return true;
	      }
	    }
	   return false;
  }
  
  /**
   * Overloaded method to implement mulithreading, starts the thread
   */
  @Override
  public void run() {
    try {
      crawlInternet();
      if (pagesVisited.size() < maxPages && pagesToVisit.size() > 1) {
        throw new EventException((short) 12, "Missed number of pages");
      }
    } catch (Exception dlv) {
      logger.error("Problem in thread: " + this.name + "; Error: " + dlv);
      logger.info("Attempting to restart thread: " + this.name + "...");
      run();
    }
  }
  
  /**
   * Returns the number of pages left in the que to visit
   *
   * @return pagesToVisit size
   */
  public static int pagesToVisitSize(){
	  return pagesToVisit.size();
  }
  
  /**
   * This method sets if to hide errors
   * @param boolean updates status
   * @return void
   */
  public static void updateQuiet(boolean updated) {
	quiet = updated;
	SpiderLeg.updateQuiet(updated);
  }
  
  /**
   * Returns the active thread
   *
   * @return t The thread this instance of Spider is running on
   */
  public Thread getT() {
    return t;
  }
}
