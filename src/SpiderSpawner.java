


public class SpiderSpawner {
	
	private static final String start_url = "https://bitbucket.blu";
	
	


//remember headphones
	public static void main(String[] args) throws InterruptedException{
		int maxPages=500;
		
		Spider spider = new Spider("num1");
		Spider spider2 = new Spider("Num2");
		Spider spider3 = new Spider("Num3");
		Spider spider4 = new Spider("Num4");

		spider.setMax(maxPages);
		//spider2.setMax(65);
		
		
		SpiderTamer.fileWhite(spider);
		SpiderTamer.fileBlack(spider);
		SpiderTamer.fileAddLinks(spider);
		
		spider.useBlackList(true);
		
		long start_time = System.currentTimeMillis();
		//spider.startTraffic(start_url);
		//spider2.startTraffic(start_url);
		spider.start();
		spider2.start();
		spider3.start();
		spider4.start();
		//while(spider.getPagesVisited().size()<maxPages){
		//	Thread.sleep(100);
		//}
		spider.getT().join();
		spider2.getT().join();
		spider3.getT().join();
		spider4.getT().join();

		//System.out.println(spider.getSuccess()+" Successes and "+spider.getProblem()+" problems");
		System.out.println("Time: "+(System.currentTimeMillis()-start_time)/1000+" seconds");
		
		//System.out.println(spider.getPagesVisited().toString().replaceAll(",", "\n"));
		SpiderTamer.writeToFile(spider);
		
	}

	
}
