import java.util.ArrayList;

public class SpiderSpawner {
	
	//private static final String start_url = "https://bitbucket.blu";
	
	


//remember headphones
	public static void main(String[] args) throws InterruptedException{
		int maxPages=20000;
		int numberOfThreads = 12;
		System.out.println("Starting webcrawl");
		if(numberOfThreads<1){
			System.out.println("Need at least 1 thread");
		}
		
		ArrayList<Spider> spiderArmy = new ArrayList<Spider>();
		for(int x = 0;x<numberOfThreads;x++){
			spiderArmy.add(new Spider("Spider-"+x));
		}
		if(spiderArmy.get(0) != null){
			System.out.println("Adding parameters");
			spiderArmy.get(0).setMax(maxPages);
			SpiderTamer.fileWhite(spiderArmy.get(0));
			SpiderTamer.fileBlack(spiderArmy.get(0));
			SpiderTamer.fileAddLinks(spiderArmy.get(0));
			
		}
		long start_time = System.currentTimeMillis();
		for(Spider jock:spiderArmy){
			System.out.println("Starting: "+jock.name);
			jock.start();
		}
		for(Spider jock:spiderArmy){
			//System.out.println("Waiting....: "+jock.getT().getName());
			jock.getT().join();
		}
		

		
		
		//System.out.println(spider.getSuccess()+" Successes and "+spider.getProblem()+" problems");
		System.out.println("Time: "+(System.currentTimeMillis()-start_time)/1000+" seconds");
		
		//System.out.println(spider.getPagesVisited().toString().replaceAll(",", "\n"));
		SpiderTamer.writeToFile(spiderArmy.get(0));
		
	}

	
}
