//import java.io.EOFException;
import java.util.*;


class Spider implements Runnable{
	private static int Max_Pages = 20;
	private static Set<String> pagesVisited = new HashSet<String>();
	private static List<String> pagesToVisit = Collections.synchronizedList(new LinkedList<String>());
	//private Set<String> problemSites = new HashSet<String>();
	private static Set<String> blackListDomains = new HashSet<String>();
	private static Set<String> whiteListDomains = new HashSet<String>();
	private boolean doDomainSearch = false;
	
	private int problems;
	private int success;
	private boolean black = true;
	private Thread t;
	public String name;

	public Spider(){
		problems = 0;
		success = 0;
		name="demo";
	}
	public Spider(String i){//takes in name to set thread as
		problems = 0;
		success = 0;
		name=i;
	}
	
	private String getNextURL(){//looks through the list of urls found and selects the next valid one
		String nextURL;

		do{
				nextURL = Spider.pagesToVisit.remove(0);//get next URL in list
		}while((black && badURL(nextURL)) || (!black && !goodURL(nextURL)));//loop through list until we find a good URL
		pagesVisited.add(nextURL);//add new URL to the set we visited
		return nextURL;
	}
	
	public void startTraffic(String url){//initiates crawl
		pagesToVisit.add(url);
		run();
		
	}
	
	private void crawlInternet(){//gets next url and initiates crawling of pages-loops through crawls
		while(pagesVisited.size()<Max_Pages){
			String currentURL;
			SpiderLeg leg = new SpiderLeg();
			if(pagesToVisit.isEmpty()){
				System.out.println("Out of URLS to crawl, ending thread "+this.name);
				break;
			}else{

					currentURL = getNextURL();
			}

			if(leg.crawl(currentURL)){
				try{

					for(String newURL:leg.getLinks()){//try to add only new links
						if(!pagesToVisit.contains(newURL)){
							pagesToVisit.add(newURL);
						}
					}
				}catch(Throwable k){
					try {
						Thread.sleep(12);
					} catch (InterruptedException e) {};
					System.out.println("Problem adding links: "+k.getMessage());
					pagesToVisit.addAll(leg.getLinks());//force add all links
				}
				success++;
			}else{
				problems++;
			}
		}
		System.out.println("\n\t***Finished Crawling***\n\nVisited "+pagesVisited.size()+" web pages with an additional "+pagesToVisit.size()+" links found.");
	}
	
public boolean badURL(String url){//returns if URL givin is a bad one and should be ignored
		
		if(pagesVisited.contains(url)){//checks if this site has been visited before
			return true;
		}
		if(url==""){
			return true;
		}
		for(String blackURL:blackListDomains){//checks if URL is blacklisted
			if(url.contains(blackURL)){
				return true;
			}
		}

		if(this.doDomainSearch && this.searchDomains(url)){return true;}//check to do domain search
		return false;
	}
	
	public boolean goodURL(String url){//checks if given URL should be used, relying on whitelist
		if(pagesVisited.contains(url)){//checks if this is a new site
			return false;
		}
		for(String white:whiteListDomains){//check if next URL is part of whitelist domain
			if(url.contains(white)){
				return true;
			}
		}
		return false;
	}
	
	public void useBlackList(boolean useBlack){//sets to use black list over whitelist
		black = useBlack;
	}
	
	public void addBlacklistedDomain(String domain){//add URLS to black list
		blackListDomains.add(domain);
	}
	
	public void addWhitelistedDomain(String domain){//add URLS to whitelist
		whiteListDomains.add(domain);
	}
	
	public void setMax(int new_max){//set max number of pages to crawl
		Max_Pages = new_max;
	}
	
	public void addURL(String new_url){//add links to cawl
		pagesToVisit.add(new_url);
	}
	
	public int getSuccess(){//returns the number of successful pages crawled
		return success;
	}
	
	public int getProblem(){//get hiow many crawls failed
		return problems;
	}
	
	public Set<String> getPagesVisited(){//get the number of pages in total visited
		return pagesVisited;
	}

	public void start(){//initiate a thread
			if(t==null){
				t = new Thread(this,name);
				t.start();
			}
	}

	public boolean searchDomains(String new_url){//search if the link is in the same domain as any other link previusly crawled
		try{
			for(String URL:pagesVisited){//searches through lists
				if(URL.replaceAll("//", " ").replaceAll("/.*", " ").contains(new_url.replaceAll("//", " ").replaceAll("/.*", " "))){
					return true;
				}
			}
		}catch(Throwable e){//on fail, try one more time, otherwise return false
			System.out.println("Problem in search domain: "+e.getMessage());
			try{
				for(String URL:pagesVisited){
					if(URL.replaceAll("//", " ").replaceAll("/.*", " ").contains(new_url.replaceAll("//", " ").replaceAll("/.*", " "))){
						return true;
					}
				}
			}catch(Throwable lkl){System.out.println("Problem in search domain, froce return true: "+lkl.getMessage()); return true;}
		}
		return false;
	}

	@Override
	public void run() {//overloaded run for multithreading
		try{
			crawlInternet();
			if(pagesVisited.size()<Max_Pages && pagesToVisit.size()>1){
				throw new Exception("Missed number of pages");
			}
		}catch(Throwable lkl){
				System.out.println("Problem with thread "+name+": "+lkl.getMessage());
				System.out.println("Attempting to restart....");
				crawlInternet();
		}
		
		
	}
	public Thread getT(){//get the thread
		return t;
	}
	
}
