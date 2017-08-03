package spiders;

import java.util.Set;

import org.apache.log4j.Logger;

public class Exec {
	private static boolean preCrawl = false;
	private static int preCrawlPages = 100;
	private static Logger logger = Logger.getLogger(Exec.class.getCanonicalName());
	
	private Exec(){}

	public static boolean preCrawl(String[] args) throws InterruptedException {
		for(String line:args){
			if("-v".equals(line)){
				preCrawl = true;
			}
		}
		logger.info(System.getProperty("user.name"));
		String f = "false";
		// parse gvien parameters

		if(!SpiderSpawner.setParamteters(args)){
			return false;
		}

		// if prelim crawl
		if (preCrawl) {
			logger.info("   Starting precrawl");
			String[] prePar = { "-q", "true", "-p", Integer.toString(preCrawlPages), "-c", f, "-s", f, "-j", f,"-i",f,"-l", "true" };
			if (!SpiderSpawner.setParamteters(prePar)) {
				logger.error("P");
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
		String[] postPar = { "-q", f, "-l", f, "-p", "100", "-c", "true" };
		SpiderSpawner.setParamteters(postPar);
		SpiderSpawner.setParamteters(args);
		SpiderSpawner.startSpawn();
		return true;
	}

}
