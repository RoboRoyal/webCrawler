package spiders;

import java.io.File;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * <h1>Java WebCrawler</h1> Crawls the web
 *
 * @author Dakota Abernathy
 * @version 1.0.5
 * @since 2017-07-06
 */	
public class SpiderSpawner {

	protected static int maxPages = 100;// max number of pages to crawl(normally ends up scanning max number of pages)
	protected static int prePages = 100;//number of pre crawl pages
	private static  int numberOfThreads = 4;// Recommended between 1/10 and 1/20th of maxPages		
	private static final int PRIORITY = 5;//setting high increases performance(sometimes) but may lock up computer. Set between 1(lowest) and 10(max)
	private static boolean clean = true;// Whether or not to clear out all written files when done
	private static boolean limitDomains = false;//limit the crawler to one page per domain
	private static final boolean SAVE_LINKS = true;//Save URLs and emails to text file
	private static final String VERSION_NUMBER = "1.0.6";
	private static boolean saveContent = false;//to download files
	private static boolean saveJS = false;//to save embedded java script or not
	private static boolean saveImages = false;//to save found imgs
	private static boolean quiet = false;//to hide small errors
	private static int maxFiles = 100;//max number of files allowed to be downloaded, -1 for inf
	private static Logger logger = Logger.getLogger(SpiderSpawner.class.getCanonicalName());
	
	private static ArrayList<Spider> spiderArmy = new ArrayList<>();// Initializes all the spiders	

	private SpiderSpawner() {}

	public static void main(String[] args) throws InterruptedException {
		// sets parameters and handles calls to precrawl
		Exec.preCrawl(args);
	}

	/**
	 * Parses the given String array and sets the variables as needed Configures
	 * the crawler
	 * 
	 * @param args
	 *            the arguments array
	 * @return if parsing and setting of vars was successful
	 */
	public static boolean setParamteters(String[] args) {
		try {
			parse(args);
		} catch (Exception e) {
			if (!"End".equals(e.getMessage())) {
				logger.error("Problem parsing command line arguments");
				logger.trace(e);
			}
			logger.info("Usage: ");
			logger.info("-t for thread count(int)");
			logger.info("-p for max page count(int)");
			logger.info("-f for max file count(int)");
			logger.info("-c for clean(boolean)");
			logger.info("-s for save content(boolean)");
			logger.info("-j for save java script(boolean)");
			logger.info("-i for save images(boolean/int)");
			logger.info("-l for limit domain(boolean)");
			logger.info("-h for help");
			logger.info("-v to run precrawl(int)");
			return false;
		}
		return true;
	}

	/**
	 * Set parameters with parse()
	 * Starts the actual process of spawning threads and initializes crawl
	 * 
	 * @return Success of crawl
	 * @throws InterruptedException
	 */
	public static boolean startSpawn() throws InterruptedException {
		for (int x = 0; x < numberOfThreads; x++) {
			spiderArmy.add(new Spider("Spider-" + x));
		}

		if (spiderArmy.get(0) != null) {// set parameters for all the threads
			if (!quiet) {
				logger.info("Adding parameters...");
			}
			SpiderLeg.maxFiles(maxFiles);// content settings
			SpiderLeg.saveContent(saveContent);
			SpiderLeg.savePics(saveImages);
			SpiderLeg.saveJS = saveJS;
			Spider.setMax(maxPages);
			Spider.updateQuiet(quiet);
			Spider.setLimitDomain(limitDomains);// crawler settings
			SpiderTamer.fileWhite(spiderArmy.get(0));
			SpiderTamer.fileBlack(spiderArmy.get(0));
			SpiderTamer.fileAddLinks();
		} else {
			logger.error("Problem initilizing the spider army");
			return false;
		}

		if (!check()) {// check given parameters
			return false;
		}
		long startTime = System.currentTimeMillis();// used for measuring time
													// of crawl
		logger.info("Starting webcrawl...");

		spawnSpiders();

		int pagesSoFar;
		int last = 0;
		int prog;
		do {
			try {// waits until crawl is complete or there are no pages left to crawl
				Thread.sleep(1080);// GTX
			} catch (Exception e) {
				logger.trace(e);
			}
			pagesSoFar = Spider.getPagesVisited().size();
			prog = pagesSoFar * 100 / maxPages;
			if (prog >= last + 10) {
				if (!quiet) {
					logger.info(prog + "%");
				}
				last = prog;
			}
		} while (pagesSoFar < maxPages && Spider.pagesToVisitSize() != 0 && Spider.getNumOfCurrentThreads() > 0);
		
		startTime = (System.currentTimeMillis() - startTime) / 1000;
		SpiderTamer.log(startTime+"(S), "+SpiderLeg.filesDownloaded()+" Downloads\n--------");
		if (!quiet) {
			logger.info("\n\n");
			logger.info(" *****Finished Crawling*****");// outputs results of
															// crawl
			logger.info("Visited " + Spider.getPagesVisited().size() + " web pages with an additional "
					+ Spider.pagesToVisitSize() + " links found");
			
			logger.info("Time: " + startTime / 3600 + ":" + (startTime % 3600) / 60 + ":" + (startTime % 3600) % 60
					+ " (" + (startTime) + " seconds)");
			if (SpiderLeg.filesDownloaded() > 0) {
				logger.info("Downloaded: " + SpiderLeg.filesDownloaded() + " files");
			}
			if (Spider.getProblem() > 0) {
				logger.info("Problems: " + Spider.getProblem());
			}
			if (pagesSoFar > 0 && SAVE_LINKS) {
				SpiderTamer.writeToFile();
			} // save results

			logger.info("Ending any remaining threads...");
		}
		Spider.setRunThread(false);// tells threads to stop running-don't really
									// need this
		for (Spider jock : spiderArmy) {// waits for all threads to finish
										// crawling
			jock.getT().join();
		}
		if (clean) {// Deletes all the downloaded files
			clean();
		}
		if (!quiet) {
			logger.info(" *****Ended successfully*****");
		}
		return true;
	}

	public static void reset() {
		Spider.reset();
		spiderArmy = new ArrayList<>();
	}

	/**
	 * Takes the array of spiders in spiderArmy and starts each thread, waiting
	 * for enough pages to be found before starting each
	 */

	private static void spawnSpiders() {
		for (int x = 0; x < spiderArmy.size(); x++) {// starts all the threads
			Spider jock = spiderArmy.get(x);
			if (!quiet) {
				logger.info("Starting: " + jock.name);
			}
			jock.start();
			jock.getT().setPriority(PRIORITY);
			while (Spider.pagesToVisitSize() <= x + 2 && Spider.pagesToVisitSize() != 0) {
				try {// waits for new links to crawl before starting thread
					Thread.sleep(100);
				} catch (Exception e) {
					logger.trace(e);
				}
			}
			try {
				Thread.sleep(10);// extra wait to allow thread to start
			} catch (Exception e) {
				logger.trace(e);
			}
		}
	}

	/**
	 * This method deletes all the imgs and docs in the output files
	 *
	 * @return void
	 */
	private static void clean() {
		if (!quiet) {
			logger.info("Cleaning up files...");
		}
		int del = 0;
		try {
			File folder = new File("output/imgs");// clean imgs
			File[] files = folder.listFiles();
			for (File f : files) {
				if (f.delete()) {
					del++;
				}
			}

			File folder2 = new File("output/files");// clean files
			File[] files2 = folder2.listFiles();
			for (File f : files2) {
				if (f.delete()) {
					del++;
				}
			}

			File folder3 = new File("output/js");// clean files
			File[] files3 = folder3.listFiles();
			for (File f : files3) {
				if (f.delete()) {
					del++;
				}
			}
		} catch (Exception e) {
			logger.error("Problem deleting files: " + e);
		}
		if (del != 0) {
			if (!quiet) {
				logger.info("Deleteted " + del + " files/pics/js");
			}
		} else {
			logger.info("No files to delete");
		}
	}

	/**
	 * This method parses the command line input and sets the parameters as
	 * needed
	 *
	 * @param arg
	 *            Passed in arguments from command line
	 */
	private static void parse(String[] arg) {
		String t = "true";
		for (int x = 0; x < arg.length; x += 2) {
			switch (arg[x]) {
			case "-p":
				maxPages = Integer.valueOf(arg[x + 1]);
				break;
			case "-t":
				numberOfThreads = Integer.valueOf(arg[x + 1]);
				break;
			case "-f":
				maxFiles = Integer.valueOf(arg[x + 1]);
				break;
			case "-c":
				clean = arg[x + 1].equals(t);
				break;
			case "-l":
				limitDomains = arg[x + 1].equals(t);
				break;
			case "-s":
				saveContent = arg[x + 1].equals(t);
				break;
			case "-j":
				saveJS = arg[x + 1].equals(t);
				break;
			case "-i":
				saveImages = arg[x + 1].equals(t);
				try{ 
					int ilv = Integer.parseInt(arg[x + 1]);
					if(ilv > 1){
						saveImages = true;
						SpiderLeg.maxImgs(ilv);
					}
				}catch(Exception e){logger.trace(e);}
				break;
			case "-q":
				quiet = arg[x + 1].equals(t);
				break;
			case "-h":
				logger.info("For any integer parameter, enter a number after the switch");
				logger.info("For any boolean, enter 'true' or 'false' after the switch");
				logger.info("To run a prescan, include '-v'");
				logger.info("'-a' for about");
				throw new IllegalArgumentException("End");
			case "-a":
				logger.info("WebCrawlerA version: " + VERSION_NUMBER);
				logger.info("Crawler is identified as DuckBot");
				logger.info("Writen by Dakota A. Summer 2017, BluVector");
				throw new IllegalArgumentException("End");
			case "-v":
				try{ 
					prePages = Integer.valueOf(arg[x + 1]);
				}catch(Exception e){logger.trace(e);}
				break;
			default:
				logger.error("Invalid: " + arg[x]);
				throw new IllegalArgumentException("Invalid statment");
			}
		}
	}

	/**
	 * This method checks to make sure all the parameters set by user are valid
	 *
	 * @return boolean If the current settings are valid for crawling
	 */
	@SuppressWarnings("unused")
	public static boolean check() {
		if (numberOfThreads < 1) {// checks thread requirements
			logger.error("Need at least 1 thread");
			return false;
		}
		if (numberOfThreads > maxPages) {// checks thread requirements
			logger.error("Don't want to have more threads then pages to crawl");
			return false;
		}
		if (PRIORITY > 10 || PRIORITY < 1) {// checks priority requirements
			logger.error("Priority must be set between 1 and 10");
			return false;
		}
		if (maxPages < 1) {// checks pages requirements
			logger.error("Cant crawl less then one page");
			return false;
		}
		if (Spider.pagesToVisitSize() < 1) {
			logger.error("Need at least one starting URLs");
			return false;
		}
		if (Spider.pagesToVisitSize() == 1) {
			SpiderTamer.fileAddLinks();
		}
		if(prePages<1){
			logger.error("Can't have lett than one prescan page");
			return false;
		}else if(prePages<Spider.pagesToVisitSize()){
			logger.error("You shouldnt have the number of prepages less than the number of starting pages");
			logger.info("Please increase prescan size");
		}
		if (!saveContent && (saveImages || saveJS)) {
			logger.warn("Can't save picuters or java script without having SAVE_CONTENT (-s) set to true");
		}
		if (maxPages >= 100 && numberOfThreads == 1) {
			logger.warn("Crawler will be slow with only one thread");
		} else if (numberOfThreads > 50) {
			logger.warn("Don't go too crazy with that many threads...");
		} else if (maxPages > 1000 && maxPages / numberOfThreads > 200 && numberOfThreads <= 40) {
			logger.warn("Crawler may be slow with so few threads");
		}
		return true;
	}

	public static String getVerion() {
		return VERSION_NUMBER;
	}
}
