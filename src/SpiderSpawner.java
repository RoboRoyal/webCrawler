package spiders;

import java.io.File;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * <h1>Java WebCrawler</h1> Crawls the web
 *
 * @author Dakota Abernathy
 * @version 1.02
 * @since 2017-07-06
 */
public class SpiderSpawner {

	private static final int MAX_PAGES = 10;// max number of pages to crawl(normally ends up scanning max number of pages)
	private static final int NUMBER_OF_THREADS = 2;// Recommended between 1/10 and 1/20th of maxPages		
	private static final int PRIORITY = 5;//setting high increases performance(sometimes) but may lock up computer. Set between 1(lowest) and 10(max)
	private static final boolean CLEAN = true;// Whether or not to clear out all written files when done
	private static Logger logger = Logger.getLogger(SpiderSpawner.class.getCanonicalName());

	private SpiderSpawner() {}

	public static void main(String[] args) throws InterruptedException {

		check();//check given parameters
		
		ArrayList<Spider> spiderArmy = new ArrayList<Spider>();// Initializes all the spiders	
		for (int x = 0; x < NUMBER_OF_THREADS; x++) {
			spiderArmy.add(new Spider("Spider-" + x));
		}

		if (spiderArmy.get(0) != null) {// set parameters for all the threads
			logger.info("Adding parameters...");
			spiderArmy.get(0).setMax(MAX_PAGES);
			Spider.doDomainSearch = false;
			SpiderTamer.fileWhite(spiderArmy.get(0));
			SpiderTamer.fileBlack(spiderArmy.get(0));
			SpiderTamer.fileAddLinks(spiderArmy.get(0));
		}

		long startTime = System.currentTimeMillis();// used for measuring time of crawl		
		logger.info("Starting webcrawl...");

		for(int x = 0;x<spiderArmy.size();x++){//starts all the threads
			Spider jock = spiderArmy.get(x);
			logger.info("Starting: " + jock.name);
			jock.start();
			jock.getT().setPriority(PRIORITY);
			while (Spider.pagesToVisitSize() < x+3 && Spider.pagesToVisitSize() != 0) { 
				try {//waits for new links to crawl before starting thread
					Thread.sleep(100);
				} catch (Throwable e) {}
			}
			try {
				Thread.sleep(10);//extra wait to allow thread to start
			} catch (Throwable e) {}
		}

		while (spiderArmy.get(0).getPagesVisited().size() < MAX_PAGES && Spider.pagesToVisitSize() != 0) {
			try {//waits until crawl is compleat or there are no pages left to crawl
				Thread.sleep(500);
			} catch (Throwable e) {}
		}
		logger.info("***Finished Crawling***");//outputs results of crawl
		logger.info("Visited " + spiderArmy.get(0).getPagesVisited().size() + " web pages with an additional "
				+ Spider.pagesToVisitSize() + " links found.");
		logger.info("Downloaded: "+SpiderLeg.filesDownloaded+" files");
		logger.info("Time: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");// prints time of crawl
		SpiderTamer.writeToFile(spiderArmy.get(0));// save results

		logger.info("Ending any remaining threads...");
		Spider.run = false;//tells threads to stop running
		for (Spider jock : spiderArmy) {// waits for all threads to finish crawling
			jock.getT().join();
		}
		if (CLEAN) {//Deletes all the downloaded files
			clean();
		}
		logger.info("Ended successfully!");
	}

	/**
	 * This method deletes all the imgs and docs in the output files
	 *
	 * @return void
	 */
	private static void clean() {
		logger.info("Cleaning up files...");
		try {
			File folder = new File("output/imgs");//clean imgs
			File[] files = folder.listFiles();
			if (files != null) { // checks to make sure folder is there
				for (File f : files) {
					f.delete();
				}
			}
			File folder2 = new File("output/files");//clean files
			File[] files2 = folder2.listFiles();
			if (files2 != null) { /// checks to make sure folder is there
				for (File f : files2) {
					f.delete();
				}
			}
		} catch (Throwable e) {
			logger.error("Problem deleting files: "+e);
		}
	}
	/**
	 * This method checks to make sure all the parameters set by user are valid
	 *
	 * @return void
	 */
	@SuppressWarnings("unused")
	private static void check() {
		if (NUMBER_OF_THREADS < 1) {// checks thread requirements
			logger.error("Need at least 1 thread");
			System.exit(1);
		}
		if (NUMBER_OF_THREADS > MAX_PAGES) {// checks thread requirements
			logger.error("Can't have more threads then pages to crawl");
			System.exit(1);
		}
		if (PRIORITY > 10 || PRIORITY < 1) {// checks priority requirements
			logger.error("Priority must be set between 1 and 10");
			System.exit(1);
		}
		if (MAX_PAGES < 1) {// checks pages requirements
			logger.error("Cant crawl less then one page");
			System.exit(1);
		}
	}
	
}
