package spiders;

import java.util.ArrayList;

/**
 * <h1>Java WebCrawler</h1> Crawls the web
 *
 * @author Dakota Abernathy
 * @version 1.02
 * @since 2017-07-06
 */
public class SpiderSpawner {

  private static final int MAX_PAGES = 100;//max number of pages to crawl (normally ends up scanning max number of pages)
  private static final int NUMBER_OF_THREADS = 2;//Recommended between 1/10 and 1/20th of maxPages
  private static final int PRIORITY = 5;//setting high increases performance but my lock up computer. Set between 1(lowest) and 10(max)

  private SpiderSpawner() {}

  @SuppressWarnings("unused")
  public static void main(String[] args) throws InterruptedException {

    if (NUMBER_OF_THREADS < 1) {//checks thread requirements
      System.out.println("Need at least 1 thread");
      System.exit(1);
    }
    if (PRIORITY > 10 || PRIORITY < 1) {//checks priority requirements
      System.out.println("Priority must be set between 1 and 10");
      System.exit(1);
    }
    if (MAX_PAGES < 1) {//checks pages requirements
      System.out.println("Cant crawl less then one page");
      System.exit(1);
    }

    System.out.println("Starting webcrawl");

    ArrayList<Spider> spiderArmy = new ArrayList<Spider>();//Initializes all the spiders
    for (int x = 0; x < NUMBER_OF_THREADS; x++) {
      spiderArmy.add(new Spider("Spider-" + x));
    }

    if (spiderArmy.get(0) != null) {//set parameters for all the threads
      System.out.println("Adding parameters");
      spiderArmy.get(0).setMax(MAX_PAGES);
      Spider.doDomainSearch = false;
      SpiderTamer.fileWhite(spiderArmy.get(0));
      SpiderTamer.fileBlack(spiderArmy.get(0));
      SpiderTamer.fileAddLinks(spiderArmy.get(0));

    }
    long start_time = System.currentTimeMillis();//used for measuring time of crawl

    for (Spider jock : spiderArmy) {//starts each thread crawling
      System.out.println("Starting: " + jock.name);
      jock.start();
      jock.getT().setPriority(PRIORITY);
      try {
        Thread.sleep(100);
      } catch (Throwable e) {
      }
    }

    for (Spider jock : spiderArmy) {//waits for all threads to finish crawling
      jock.getT().join();
    }
    System.out.println("Time: " + (System.currentTimeMillis() - start_time) / 1000 + " seconds");//print results of crawl
    SpiderTamer.writeToFile(spiderArmy.get(0));//save results	
  }
}
