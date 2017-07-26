package spiders;

import java.io.File;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * <h1>Java WebCrawler</h1> Crawls the web
 *
 * @author Dakota Abernathy
 * @version 1.0.3
 * @since 2017-07-06
 */
public class SpiderSpawner {

	private static int MAX_PAGES = 10;// max number of pages to crawl(normally ends up scanning max number of pages)
	private static  int NUMBER_OF_THREADS = 0;// Recommended between 1/10 and 1/20th of maxPages		
	private static final int PRIORITY = 5;//setting high increases performance(sometimes) but may lock up computer. Set between 1(lowest) and 10(max)
	private static boolean CLEAN = true;// Whether or not to clear out all written files when done
	private static boolean LIMIT_DOMAIN = false;//limit the crawler to one page per domain
	private static final boolean SAVE_LINKS = true;//Save URLs and emails to text file
	private static boolean SAVE_CONTENT = false;//to download files
	private static boolean SAVE_JS = false;//to save embedded java script or not
	private static boolean SAVE_IMAGES = false;//to save found imgs
	private static final boolean QUIET = false;//to hide small errors
	private static int MAX_FILES = 10;//max number of files allowed to be downloaded, -1 for inf
	private static Logger logger = Logger.getLogger(SpiderSpawner.class.getCanonicalName());
	
	private static ArrayList<Spider> spiderArmy = new ArrayList<>();// Initializes all the spiders	

	private SpiderSpawner() {}

	public static void main(String[] args) throws InterruptedException {
		
		try{
			parse(args);
		}catch(Exception e){
			logger.error("Problem parsing command line arguments",e);
			logger.info("Usage: ");
			logger.info("-t for thread count(int)");
			logger.info("-p for max page count(int)");
			logger.info("-f for max file count count(int)");
			logger.info("-c for clean(boolean)");
			logger.info("-s for save content(boolean)");
			logger.info("-j for save java script(boolean)");
			logger.info("-i for save images(boolean)");
			logger.info("-l for limit domain(boolean)");
			System.exit(1);
		}

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
			Spider.updateQuiet(QUIET);
			Spider.doDomainSearch = LIMIT_DOMAIN;//crawler settings
			SpiderTamer.fileWhite(spiderArmy.get(0));
			SpiderTamer.fileBlack(spiderArmy.get(0));
			SpiderTamer.fileAddLinks(spiderArmy.get(0));
		}else{
			logger.error("Problem initilizing the spider army");
			System.exit(1);
		}

		if(!check()){//check given parameters
			System.exit(1);}
		long startTime = System.currentTimeMillis();// used for measuring time of crawl		
		logger.info("Starting webcrawl...");

		for(int x = 0;x<spiderArmy.size();x++){//starts all the threads
			Spider jock = spiderArmy.get(x);
			if(!QUIET){
				logger.info("Starting: " + jock.name);}
			jock.start();
			jock.getT().setPriority(PRIORITY);
			while (Spider.pagesToVisitSize() <= x+2 && Spider.pagesToVisitSize() != 0) {
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
		int prog;
		do{
			try {//waits until crawl is compleat or there are no pages left to crawl
				Thread.sleep(1080);//GTX
			} catch (Exception e) {logger.trace(e);}
			pagesSoFar = spiderArmy.get(0).getPagesVisited().size();
			prog = pagesSoFar*100/MAX_PAGES;
			if(prog>=last+10){
				logger.info(prog+"%");
				last = prog;}
		}while (pagesSoFar < MAX_PAGES && Spider.pagesToVisitSize() != 0) ;
		
		logger.info("***Finished Crawling***");//outputs results of crawl
		logger.info("Visited " + spiderArmy.get(0).getPagesVisited().size() + " web pages with an additional "
				+ Spider.pagesToVisitSize() + " links found.");
		logger.info("Downloaded: "+SpiderLeg.filesDownloaded()+" files");
		logger.info("Time: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");// prints time of crawl
		startTime=(System.currentTimeMillis() - startTime)/1000;
		logger.info("Time: "+startTime/3600+":"+(startTime%3600)/60+":"+(startTime%3600)%60);
		if(pagesSoFar > 0 && SAVE_LINKS){
			SpiderTamer.writeToFile(spiderArmy.get(0));}// save results

		logger.info("Ending any remaining threads...");
		Spider.run = false;//tells threads to stop running-don't really need this
		for (Spider jock : spiderArmy) {// waits for all threads to finish crawling
			jock.getT().join();
		}
		if (CLEAN) {//Deletes all the downloaded files
			clean();}
		logger.info("Ended successfully!");
	}

	/**
	 * This method deletes all the imgs and docs in the output files
	 *
	 * @return void
	 */
	private static void clean() {
		logger.info("Cleaning up files...");
		int del = 0;
		try {
			File folder = new File("output/imgs");//clean imgs
			File[] files = folder.listFiles();
			if (files != null) { // checks to make sure folder is there
				for (File f : files) {
					if(f.delete()){
					del++;}
				}
			}
			File folder2 = new File("output/files");//clean files
			File[] files2 = folder2.listFiles();
			if (files2 != null) { /// checks to make sure folder is there
				for (File f : files2) {
					if(f.delete()){
						del++;}
				}
			}
			File folder3 = new File("output/js");//clean files
			File[] files3 = folder3.listFiles();
			if (files3 != null) { /// checks to make sure folder is there
				for (File f : files3) {
					if(f.delete()){
						del++;}
				}
			}
		} catch (Exception e) {
			logger.error("Problem deleting files: "+e);
		}
		if(del == 0){
			logger.info("Deleteted "+del+ " files");
		}else{
			logger.info("No files to delete files");
		}
	}
	/**
	 * This method parses the command line input and sets the parameters as needed
	 *
	 * @param arg Passed in arguments from command line
	 */
	private static void parse(String[] arg) throws Exception{
		for(int x = 0;x<arg.length;x+=2){
			switch(arg[x]){
			case"-p":
				MAX_PAGES = Integer.valueOf(arg[x+1]);
				break;
			case"-t":
				NUMBER_OF_THREADS = Integer.valueOf(arg[x+1]);
				break;
			case"-f":
				MAX_FILES = Integer.valueOf(arg[x+1]);
				break;
			case"-c":
				CLEAN = arg[x+1].equals("true");
				break;
			case"-l":
				LIMIT_DOMAIN = arg[x+1].equals("true");
				break;
			case"-s":
				SAVE_CONTENT = arg[x+1].equals("true");
				break;
			case"-j":
				SAVE_JS = arg[x+1].equals("true");
				break;
			case"-i":
				SAVE_IMAGES = arg[x+1].equals("true");
				break;
			default:
				throw new Exception("Invalid statment");
			}
		}
	}
	
	/**
	 * This method checks to make sure all the parameters set by user are valid
	 *
	 * @return boolean If the current settings are valid for crawling
	 */
	@SuppressWarnings("unused")
	private static boolean check() {
		if (NUMBER_OF_THREADS < 1) {// checks thread requirements
			logger.error("Need at least 1 thread");
			return false;
		}
		if (NUMBER_OF_THREADS > MAX_PAGES) {// checks thread requirements
			logger.error("Don't want to have more threads then pages to crawl");
			return false;
		}
		if (PRIORITY > 10 || PRIORITY < 1) {// checks priority requirements
			logger.error("Priority must be set between 1 and 10");
			return false;
		}
		if (MAX_PAGES < 1) {// checks pages requirements
			logger.error("Cant crawl less then one page");
			return false;
		}
		if(Spider.pagesToVisitSize() < 1){
			logger.error("Need at least one starting URLs");
			return false;	
		}
		if(Spider.pagesToVisitSize() == 1){
			SpiderTamer.fileAddLinks(spiderArmy.get(0));
		}
		if(!SAVE_CONTENT && (SAVE_IMAGES || SAVE_JS)){
			logger.warn("Can't save picuters or java script without having SAVE_CONTENT set to true");
		}
		if (MAX_PAGES >= 100 && NUMBER_OF_THREADS == 1) {
			logger.warn("Crawler will be slow with only one thread");
		} else if (NUMBER_OF_THREADS > 50) {
			logger.warn("Don't go too crazy with that many threads...");
		} else if (MAX_PAGES > 1000 && MAX_PAGES / NUMBER_OF_THREADS > 200 && NUMBER_OF_THREADS <= 40) {
			logger.warn("Crawler may be slow with so few threads");
		}
		return true;
	}
}
