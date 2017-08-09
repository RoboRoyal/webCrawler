package spiders;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;

public class Exec {
	private static boolean preCrawl = false;
	private static int preCrawlThreads = 10;
	private static Logger logger = Logger.getLogger(Exec.class.getCanonicalName());

	private Exec() {
	}

	/**
	 * Sets parameters and starts a prescan if told to do so Otherwise just
	 * initiates a regular scan
	 * 
	 * @param args
	 *            Command line arfuments passed in
	 * @return if crawl was successful
	 * @throws InterruptedException
	 */
	public static boolean preCrawl(String[] args) throws InterruptedException {
		int preCrawlPages;
		for (String line : args) {
			if ("-v".equals(line)) {
				preCrawl = true;
			}
		}
		logger.info("Crawl started by: " + System.getProperty("user.name"));
		StringBuilder par = new StringBuilder();
		for (String tmp : args) {
			par.append(tmp + ", ");
		}
		SpiderTamer.log(getTime() + " : " + System.getProperty("user.name") + " : " + par.toString());
		String f = "false";
		// parse gvien parameters

		if (preCrawl && !SpiderSpawner.setParamteters(args) && !SpiderSpawner.check()) {
			return false;
		}

		// if preliminary crawl
		if (preCrawl) {
			logger.info("   Starting precrawl");
			preCrawlPages = SpiderSpawner.prePages;
			String[] prePar = { "-q", "true", "-p", Integer.toString(preCrawlPages), "-t",
					Integer.toString(preCrawlThreads), "-c", f, "-s", f, "-j", f, "-i", f, "-l", "true" };
			logger.info("Parameters: " + preCrawlPages + " pages on " + preCrawlThreads + " threads");
			if (!SpiderSpawner.setParamteters(prePar)) {
				logger.error("Problem adding prameters to precrawl");
			}
			try {
				// initiate limited domain carwl
				SpiderSpawner.startSpawn();
				Set<String> tmp = Spider.getPagesVisited();
				SpiderSpawner.reset();

				// give output of prelim crawl to normal crawl
				for (String p : tmp) {
					Spider.addURL(p);
				}
			} catch (InterruptedException e) {
				logger.error(e);
				throw e;
			}
			logger.info("   Finished precrawl, starting main crawl\n");
		}

		// start normal crawl
		// return values to default
		String[] postPar = { "-q", f, "-l", f, "-p", "100", "-c", "true" };
		SpiderSpawner.setParamteters(postPar);
		if(SpiderSpawner.setParamteters(args)){
			SpiderSpawner.startSpawn();	
		}
		return true;
	}

	public static String getTime() {
		return new SimpleDateFormat("MM/dd/YYYY HH:mm:ss", Locale.US).format(new Date());
	}
}
