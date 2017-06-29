import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class SpiderTamer {
	
	public static void fileBlack(Spider p){
		String blackFile = "black.txt";
		try{
			Scanner in = new Scanner(new File((blackFile)));
			String line;
			while(in.hasNextLine()){
				line = in.nextLine();
				if(line.length() >= 3){
					p.addBlacklistedDomain(line);
				}
			}
			in.close();
		}catch(IOException e){
			System.out.println("Prob: "+e.getMessage());
		}

	}
	
	public static void fileWhite(Spider p){
		String whiteFile = "white.txt";
		try{
			Scanner in = new Scanner(new File((whiteFile)));
			String line;
			while(in.hasNextLine()){
				line = in.nextLine();
				if(line.length() >= 3){
					p.addWhitelistedDomain(line);
				}
			}
			in.close();
		}catch(IOException e){
			System.out.println("Prob: "+e.getMessage());
		}
	}
	public static boolean writeToFile(Spider p){
		System.out.println("Writing to file...");
		String fileOut = "crawledURLS.txt";
		try{

			BufferedWriter SpiderJocky = new BufferedWriter(new FileWriter(new File(fileOut)));
			System.out.println("Attempting to write "+p.getPagesVisited().size()+" links to file...");
			SpiderJocky.write(p.getPagesVisited().toString().replaceFirst("\\]", " ").replaceAll(",", "\n").replaceFirst("\\[", " "));
			SpiderJocky.close();
		}catch(IOException e){
			System.out.println("Problem writing to file: "+e.getMessage());
			return false;
		}
		System.out.println("Written!");
		return true;
	}
	public static void fileAddLinks(Spider p) {
		String urlFile = "linksToCrawl.txt";
		try{
			Scanner in = new Scanner(new File((urlFile)));
			String line;
			while(in.hasNextLine()){
				line = in.nextLine();
				if(line.length() >= 3){
					p.addURL(line);
				}
			}
			in.close();
		}catch(IOException e){
			System.out.println("Prob: "+e.getMessage());
		}
		
	}

}
