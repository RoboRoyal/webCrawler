package spiders;

import java.util.*;
import org.apache.log4j.Logger;
import org.w3c.dom.events.EventException;
import com.google.common.hash.*;

public class Spider implements Runnable {
	private static int maxPages = 20;
	private static Set<String> pagesVisited = new HashSet<>();
	private static List<String> pagesToVisit = Collections.synchronizedList(new LinkedList<String>());
	private static Set<String> blackListDomains = new HashSet<>();
	private static Set<String> whiteListDomains = new HashSet<>();
	private static boolean quiet = false;//to not logg errors or info
	private static boolean doDomainSearch = false;//limits crawl to one scan per domain
	private static boolean runThread = true;//to continue of kill thread
	private static Logger logger = Logger.getLogger(Spider.class.getCanonicalName());
	private static BloomFilter<CharSequence> filter;//filter for bloom filter
	private static Boolean useFilter = false;//use bloom filter or brutforce check
	private int restarts = 0;//how many times the thread has restarted
	private static int num = 0;//number of threads left

	private static int problems = 0;
	private static int success = 0;
	private boolean black = true;//use black list or white list
	private Thread t;
	String name;

	public Spider() {
		name = "demo";
	}

	public Spider(String threadName) {// takes in name to set thread as
		name = threadName;
	}

	public static void reset() {
		pagesVisited = new HashSet<>();
		pagesToVisit = Collections.synchronizedList(new LinkedList<String>());
		runThread = true;
		problems = 0;
		success = 0;
	}

	/**
	 * This method searches through possible URLs and picks out one worth
	 * crawling
	 *
	 * @return String This is the next valid URL to crawl
	 */
	private String getNextURL() {
		String nextURL;
		do {
			nextURL = Spider.pagesToVisit.remove(0);// get next URL in list
			// loop through until we find an acceptable URL to crawl
		} while ((black && isBadURL(nextURL)) || (!black && !isGoodURL(nextURL)));
		pagesVisited.add(nextURL);// add new URL to the set we visited
		if (useFilter) {
			filter.put(nextURL);
		}
		return nextURL;
	}

	/**
	 * This method starts webcrawling on one thread
	 *
	 * @param url
	 *            This is the first URL to crawl
	 */
	public void startTraffic(String url) {
		pagesToVisit.add(url);
		run();
	}

	/**
	 * Primary crawling method to initiate web crawl Makes calls to find next
	 * URLs to crawl Saves new links
	 */
	private void crawlInternet() {
		while (pagesVisited.size() < maxPages && runThread) {
			String currentURL;
			SpiderLeg leg = new SpiderLeg();
			if (pagesToVisit.isEmpty()) {// end thread if its out of pages
				if (!quiet) {
					logger.warn("Out of URLS to crawl, ending thread " + this.name);
				}
				break;
			} else {
				currentURL = getNextURL();
			}
			if (leg.crawl(currentURL)) {
				if (pagesToVisit.size() <= (50 + maxPages - pagesVisited.size())) {
					addNewUrl(leg);
				}
				success++;
			} else {
				problems++;
			}
		}
	}

	/**
	 * Filters out bad URLs and adds the rest to the list of URLs to be crawled
	 * 
	 * @param leg
	 */
	private void addNewUrl(SpiderLeg leg) {
		List<String> tmp = leg.getLinks();
		for (int x = 0; x < tmp.size(); x++) {
			for (int y = x + 1; y < tmp.size(); y++) {
				if (tmp.get(x).equals(tmp.get(y))) {
					tmp.remove(x);
				}
			}
		}
		try {
			for (String newURL : tmp) {// try to add only new links
				if (!pagesToVisit.contains(newURL)) {
					pagesToVisit.add(newURL);
				}
			}
		} catch (Exception k) {
			try {
				Thread.sleep(12);
			} catch (Exception e) {
				logger.trace(e);
			}
			if (!quiet) {
				logger.error("Problem adding links on thread: " + this.name + ": " + k);
			}
			pagesToVisit.addAll(tmp);// force add all links
		}
	}

	/**
	 * This method returns if the given URL is bad using the black list
	 *
	 * @param url
	 *            The URL to test
	 * @return boolean True if the URL is bad, False if it is worth crawling
	 */
	public boolean isBadURL(String url) {
		// checks if we have visited this site before
		if ((!useFilter && pagesVisited.contains(url)) || (useFilter && filter.mightContain(url))) {
			return true;
		}
		if (url == "") {
			return true;
		}
		// checks if URL is blacklisted
		for (String blackURL : blackListDomains) {
			if (url.contains(blackURL)) {
				return true;
			}
		} // check to do domain search
		if (doDomainSearch && this.searchDomains(url)) {
			return true;
		}
		return false;
	}

	/**
	 * This method returns if a URL is good using the white list
	 *
	 * @param url
	 *            The URL to test
	 * @return boolean False if the URL is bad, True if it is worth crawling
	 */
	public boolean isGoodURL(String url) {
		if (pagesVisited.contains(url)) {// checks if this is a new site
			return false;
		}
		// check if next URL is part of white list domains
		for (String white : whiteListDomains) {
			if (url.contains(white)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method is used to set weather or not to use black list or white list
	 *
	 * @param useBlack
	 *            Set True to use black list or False to use white list
	 */
	public void useBlackList(boolean useBlack) {
		black = useBlack;
	}

	/**
	 * This method is used to add URLs/domains to the black list
	 *
	 * @param domain
	 *            The domain to add to black list
	 */
	public void addBlacklistedDomain(String domain) {
		blackListDomains.add(domain);
	}

	/**
	 * This method is used to add URLs/domains to the white list
	 *
	 * @param domain
	 *            The domain to add to white list
	 */
	public void addWhitelistedDomain(String domain) {
		whiteListDomains.add(domain);
	}

	/**
	 * This method is used to set the max number of pages to crawl
	 *
	 * @param new_max
	 *            The new max number of pages to crawl
	 */
	public static void setMax(int newMax) {
		maxPages = newMax;
		if (newMax > 8000) {
			useFilter = true;
			filter = BloomFilter.create(Funnels.stringFunnel(), newMax);
		}
	}

	/**
	 * This method is used to add to the URLs to be crawled
	 *
	 * @param new_url
	 *            Another URL to be crawled
	 */
	public static void addURL(String newUrl) {
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
	public static int getProblem() {
		return problems;
	}

	/**
	 * This method is used to get the pages crawls
	 *
	 * @return Set<String> The pages that were crawled
	 */
	public static Set<String> getPagesVisited() {
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
	 * This method is used to check if the given URL is part of a domain that
	 * has already been visited
	 *
	 * @param new_url
	 *            The domain to be searched against visited URLs @ return
	 *            boolean If the domain has already been visited
	 */
	public boolean searchDomains(String newUrl) {
		try {
			for (String URL : pagesVisited) {// searches through lists
				if (URL.replaceAll("//", " ").replaceAll("/.*", " ")
						.contains(newUrl.replaceAll("//", " ").replaceAll("/.*", " "))) {
					return true;
				}
			}
		} catch (Exception e) {// on fail, try one more time, otherwise return
								// false
			logger.trace("Problem in search domain; thread: " + this.name + ";Error: " + e);
			try {
				for (String URL : pagesVisited) {
					if (URL.replaceAll("//", " ").replaceAll("/.*", " ")
							.contains(newUrl.replaceAll("//", " ").replaceAll("/.*", " "))) {
						return true;
					}
				}
			} catch (Exception dlv) {
				if (!quiet) {
					logger.error("Problem in search domain, forcing true; thread: " + this.name + "; Error: " + dlv);
				}
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
			num++;
			crawlInternet();
			if (pagesVisited.size() < maxPages && pagesToVisit.size() > 1) {
				Thread.sleep(100);
				throw new EventException((short) 12, "Missed number of pages");
			}
		} catch (Exception dlv) {
			if (!quiet) {
				logger.info("Visited: " + pagesVisited.size() + " Pages to visit: " + pagesToVisit.size());
				logger.error("Problem in thread: " + this.name + "; Error: ", dlv);
			}
			//only let a thread restart up to 10 times
			if (restarts <= 10) {
				restarts++;
				if (!quiet) {
					logger.info("Attempting to restart thread: " + this.name+" restart: "+restarts + "...");
				}
				run();
				//if the thread has been restarted more the the max allowed, shut it down
			}else{
				num--;
				logger.info("Thread "+this.name+" is shutting down, "+num+" threads left");
			}
		}
	}

	/**
	 * Returns the number of pages left in the que to visit
	 *
	 * @return pagesToVisit size
	 */
	public static int pagesToVisitSize() {
		return pagesToVisit.size();
	}

	/**
	 * This method sets if to hide errors
	 * 
	 * @param boolean
	 *            updates status
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

	public static void setRunThread(boolean run) {
		runThread = run;
	}

	public static void setLimitDomain(boolean limitDomains) {
		doDomainSearch = limitDomains;
	}
	public static int getNumOfCurrentThreads(){
		return num;
	}
}
