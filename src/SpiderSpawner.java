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

	private static final int MAX_PAGES = 200;// max number of pages to crawl(normally ends up scanning max number of pages)
	private static final int NUMBER_OF_THREADS = 8;// Recommended between 1/10 and 1/20th of maxPages		
	private static final int PRIORITY = 5;//setting high increases performance(sometimes) but may lock up computer. Set between 1(lowest) and 10(max)
	private static final boolean CLEAN = false;// Whether or not to clear out all written files when done
	private static final boolean LIMIT_DOMAIN = false;//limit the crawler to one page per domain
	private static final boolean SAVE_LINKS = true;//Save URLs and emails to text file
	private static final boolean SAVE_CONTENT = true;//to download files
	private static final boolean SAVE_JS = false;//to save embedded java script or not
	private static final boolean SAVE_IMAGES = true;//to save found imgs
	private static final boolean QUIET = false;//to hide small errors
	private static final int MAX_FILES = 10;//max number of files allowed to be downloaded, -1 for inf
	private static Logger logger = Logger.getLogger(SpiderSpawner.class.getCanonicalName());
	
	private static ArrayList<Spider> spiderArmy = new ArrayList<Spider>();// Initializes all the spiders	

	private SpiderSpawner() {}

	public static void main(String[] args) throws InterruptedException {
		for (int x = 0; x < NUMBER_OF_THREADS; x++) {
			spiderArmy.add(new Spider("Spider-" + x));
		}

		if (spiderArmy.get(0) != null) {// set parameters for all the threads
			logger.info("Adding parameters...");
			SpiderLeg.maxFiles(MAX_FILES);//content settings
			SpiderLeg.saveContent(SAVE_CONTENT);
			SpiderLeg.savePics(SAVE_IMAGES);
			SpiderLeg.saveJS = SAVE_JS;
			Spider.setMax(MAX_PAGES);
			Spider.updateQUIET(QUIET);
			Spider.doDomainSearch = LIMIT_DOMAIN;//crawler settings
			SpiderTamer.fileWhite(spiderArmy.get(0));
			SpiderTamer.fileBlack(spiderArmy.get(0));
			SpiderTamer.fileAddLinks(spiderArmy.get(0));
		}else{
			logger.error("Problem initilizing the spider army");
			System.exit(1);
		}

		check();//check given parameters
		long startTime = System.currentTimeMillis();// used for measuring time of crawl		
		logger.info("Starting webcrawl...");

		for(int x = 0;x<spiderArmy.size();x++){//starts all the threads
			Spider jock = spiderArmy.get(x);
			if(!QUIET){
				logger.info("Starting: " + jock.name);}
			jock.start();
			jock.getT().setPriority(PRIORITY);
			while (Spider.pagesToVisitSize() <= x*2 && Spider.pagesToVisitSize() != 0) {
				try {//waits for new links to crawl before starting thread
					Thread.sleep(100);
				} catch (Exception e) {logger.trace(e);}
			}
			try {
				Thread.sleep(10);//extra wait to allow thread to start
			} catch (Exception e) {logger.trace(e);}
		}
		
		int pagesSoFar;
		int last = 0;
		do{
			try {//waits until crawl is compleat or there are no pages left to crawl
				Thread.sleep(1080);//GTX
			} catch (Exception e) {logger.trace(e);}
			pagesSoFar = spiderArmy.get(0).getPagesVisited().size();
			if((pagesSoFar*100/MAX_PAGES)>=last+10){
				logger.info(pagesSoFar*100/MAX_PAGES+"%");
				last = pagesSoFar*100/MAX_PAGES;}
		}while (pagesSoFar < MAX_PAGES && Spider.pagesToVisitSize() != 0) ;
		
		logger.info("***Finished Crawling***");//outputs results of crawl
		logger.info("Visited " + spiderArmy.get(0).getPagesVisited().size() + " web pages with an additional "
				+ Spider.pagesToVisitSize() + " links found.");
		logger.info("Downloaded: "+SpiderLeg.filesDownloaded()+" files");
		logger.info("Time: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");// prints time of crawl
		if(pagesSoFar > 0 && SAVE_LINKS){
			SpiderTamer.writeToFile(spiderArmy.get(0));}// save results

		logger.info("Ending any remaining threads...");
		Spider.run = false;//tells threads to stop running-don't really need this
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
			File folder3 = new File("output/js");//clean files
			File[] files3 = folder3.listFiles();
			if (files3 != null) { /// checks to make sure folder is there
				for (File f : files3) {
					f.delete();
				}
			}
		} catch (Exception e) {
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
			logger.error("Don't want to have more threads then pages to crawl");
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
		if(Spider.pagesToVisitSize() < 1){
			logger.error("Need at least one starting URLs");
			System.exit(1);	
		}
		if(Spider.pagesToVisitSize() < 2){
			SpiderTamer.fileAddLinks(spiderArmy.get(0));
		}
		if (MAX_PAGES >= 100 && NUMBER_OF_THREADS == 1) {
			logger.warn("Crawler will be slow with only one thread");
		} else if (NUMBER_OF_THREADS > 50) {
			logger.warn("Don't go too crazy with that many threads...");
		} else if (MAX_PAGES > 1000 && MAX_PAGES / NUMBER_OF_THREADS > 100 && NUMBER_OF_THREADS < 50) {
			logger.warn("Crawler may be slow with so few threads");
		}
	}
}
