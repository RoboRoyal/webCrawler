import java.util.*;


class Spider implements Runnable{
	private static int Max_Pages = 20;
	private static Set<String> pagesVisited = new HashSet<String>();
	private static List<String> pagesToVisit = new LinkedList<String>();
	//private Set<String> problemSites = new HashSet<String>();
	private static Set<String> blackListDomains = new HashSet<String>();
	private static Set<String> whiteListDomains = new HashSet<String>();
	private boolean doDomainSearch = false;
	private int problems;
	private int success;
	private boolean black = true;
	private Thread t;
	private String name;
	
	public Spider(){
		problems = 0;
		success = 0;
		name="demo";
	}
	public Spider(String i){
		problems = 0;
		success = 0;
		name=i;
	}
	
	
	
	private String getNextURL(){//looks through the list of urls found and selects the next valid one
		String nextURL;

		do{
			try{
				nextURL = Spider.pagesToVisit.remove(0);//get next URL in list
			}catch(Throwable e){
				System.out.println("Problem removing link from pagesToVisit: "+e.getMessage());
				try{
					nextURL = Spider.pagesToVisit.remove(0);//get next URL in list
				}catch(Throwable k){
					System.out.println("Problem removing link from pagesToVisit, returning null");
					return null;
				}
			}
			//System.out.println("check");
		}while((black && badURL(nextURL)) || (!black && !goodURL(nextURL)));//loop through list until we find a good URL
		pagesVisited.add(nextURL);//add new URL to the set we visited
		return nextURL;
	}
	
	public void startTraffic(String url){//initiates crawl
		pagesToVisit.add(url);
		//crawlInternet();
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
				//try{
					currentURL = getNextURL();
				/*}catch(Throwable k){
					currentURL=null;
					System.out.println("Error trying to get next URL, ["+k.getMessage()+"] trying again");
					while(pagesToVisit.size()>1){
						try{
							try{
								Thread.sleep(2);
								currentURL = getNextURL();
								System.out.println("Success! Got next URL");
							}catch(Throwable l){};	
						}catch(Throwable q){};
					}
				}*/
			}
			if(currentURL == null){
				System.out.println("Received a null URL from getNextURL()");
				if(pagesVisited.size()<Max_Pages){
					System.out.println("Trying to get next url again...");
					try{
						Thread.sleep(12);
						currentURL = getNextURL();
						if(currentURL == null){
							throw new EOFException();
						}
						System.out.println("Success! Got next URL");
					}catch(Throwable l){
						System.out.println("This is: "+name+" and I have failed you. I am sorry.");
					}	
				}
				break;
			}
			//System.out.println("This is: "+name);

			if(leg.crawl(currentURL)){
				try{
					pagesToVisit.addAll(leg.getLinks());
				}catch(Throwable k){
					try {
						Thread.sleep(2);
					} catch (InterruptedException e) {};
					System.out.println("Problem adding links, trying again");
					pagesToVisit.addAll(leg.getLinks());
				}
				success++;
			}else{
				problems++;
			}
		}
		System.out.println("\n\t***Finished Crawling***\n\nVisited "+pagesVisited.size()+" web pages with an additional "+pagesToVisit.size()+" links found.");
	}
	
	public boolean badURL(String url){
		
		if(pagesVisited.contains(url)){//checks if this site has been visited before
			return true;
		}
		if(url==""){
			return true;
		}
			try{
			for(String blackURL:blackListDomains){//checks if URL is blacklisted
				if(url.contains(blackURL)){
					return true;
				}
			}
		}catch(Throwable e){
			System.out.println("Problem searching in blackListDomains: "+e.getMessage());
			if(url == null){
				System.out.println("Url was null, problem resolved");
				return true;
			}
			System.out.println("*INFO* Name: "+name+" size of blackListDomains: "+blackListDomains.size()+" URL searching: "+url);
			System.out.println("Trying one more time...");
			for(String blackURL:blackListDomains){//checks if URL is blacklisted
				if(url.contains(blackURL)){
					return true;
				}
			
			}
		}
		if(this.doDomainSearch && this.searchDomains(url)){return true;}
		return false;
	}
	
	public boolean goodURL(String url){
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
	
	public void useBlackList(boolean useBlack){
		black = useBlack;
	}
	
	public void addBlacklistedDomain(String domain){
		blackListDomains.add(domain);
	}
	
	public void addWhitelistedDomain(String domain){
		whiteListDomains.add(domain);
	}
	
	public void setMax(int new_max){
		Max_Pages = new_max;
	}
	
	public void addURL(String new_url){
		pagesToVisit.add(new_url);
	}
	
	public int getSuccess(){
		return success;
	}
	
	public int getProblem(){
		return problems;
	}
	public Set<String> getPagesVisited(){
		return pagesVisited;
	}

	public void start(){
			if(t==null){
				t = new Thread(this,name);
				t.start();
			}
	}

	public boolean searchDomains(String new_url){
		try{
			for(String URL:pagesVisited){//checks if URL is part of a domain already crawled
				if(URL.replaceAll("//", " ").replaceAll("/.*", " ").contains(new_url.replaceAll("//", " ").replaceAll("/.*", " "))){
					return true;
				}
			}
		}catch(Throwable e){
			System.out.println("Problem in search domain: "+e.getMessage());
			for(String URL:pagesVisited){//tries one more time
				if(URL.replaceAll("//", " ").replaceAll("/.*", " ").contains(new_url.replaceAll("//", " ").replaceAll("/.*", " "))){
					return true;
				}
			}
		}
			
		return false;
	}

	@Override
	public void run() {
		crawlInternet();
		
		
	}
	public Thread getT(){
		return t;
	}
	
}
