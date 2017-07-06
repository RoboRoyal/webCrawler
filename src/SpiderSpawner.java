import java.util.ArrayList;

public class SpiderSpawner {

	public static void main(String[] args) throws InterruptedException{
		int maxPages = 20;//max number of pages to crawl (normally ends up scanning max number of pages)
		int numberOfThreads = 1;//Recommended between 1/10 and 1/20th of maxPages
		int priority = 5;//setting high increases performance but my lock up computer. Set between 1(lowest) and 10(max)
		
		if(numberOfThreads<1){//checks thread requirements
			System.out.println("Need at least 1 thread");
			System.exit(1);
		}
		if(priority>10 || priority<1){//checks priority requirements
			System.out.println("Priority must be set between 1 and 10");
			System.exit(1);
		}
		
		System.out.println("Starting webcrawl");
		
		ArrayList<Spider> spiderArmy = new ArrayList<Spider>();//Initializes all the spiders
		for(int x = 0;x<numberOfThreads;x++){
			spiderArmy.add(new Spider("Spider-"+x));
		}
		
		if(spiderArmy.get(0) != null){//set parameters for all the threads
			System.out.println("Adding parameters");
			spiderArmy.get(0).setMax(maxPages);
			spiderArmy.get(0).doDomainSearch = false;
			SpiderTamer.fileWhite(spiderArmy.get(0));
			SpiderTamer.fileBlack(spiderArmy.get(0));
			SpiderTamer.fileAddLinks(spiderArmy.get(0));
			
		}
		long start_time = System.currentTimeMillis();//used for measuring time of crawl
		
		for(Spider jock:spiderArmy){//starts each thread crawling
			System.out.println("Starting: "+jock.name);
			jock.start();
			jock.getT().setPriority(priority);
			try{
				Thread.sleep(2);
			}catch(Throwable e){}
		}
		
		for(Spider jock:spiderArmy){//waits for all threads to finish crawling
			jock.getT().join();
		}

		System.out.println("Time: "+(System.currentTimeMillis()-start_time)/1000+" seconds");//print results of crawl
	
		SpiderTamer.writeToFile(spiderArmy.get(0));//save results	
	}
}
