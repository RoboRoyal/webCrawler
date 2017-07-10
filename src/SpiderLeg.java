package spiders;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.jsoup.*;
import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

//MSI|NDL|PDF|LNK|ELI|SWF|GDL|GUI|NDLp|JAR|PDF-B|PDF_B|PDFb|APK|DMG|DLL|GUIp|PE+|DLL+|WIX|EXL|CDO|GDLp|RTF|CDLp|PPT|BMP|CON|NAT|TEX|NATp|DOS|WRD|CDL|GIF|CONp|DOCX|XLSX|PPTX|--//cant do com
public class SpiderLeg {
	private static boolean getContent = true;
	private static boolean savePics = false;

	private static final String USER_AGENT = 
			"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";// pretend we are using a browser																																							// browser
	private List<String> links = new LinkedList<String>();

	/**
	 * This method is what actually crawls individual websites
	 * 
	 * @param String
	 *            The URL to crawl
	 * @return boolean If the crawl was successful or not
	 */
	public boolean crawl(String url) {
		try {
			Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);// Connects to web pages
			connection.timeout(12000);
			Document htmlDocument = connection.get();// get contents of web page

			if (!connection.response().contentType().contains("text/html")){// print out if connection failed
				System.out.println("**Failure** Retrieved something other than HTML");
				return false;
			}
			Elements linksOnPage = htmlDocument.select("a[href]");// get all links from web page
			for (Element link : linksOnPage) {
				//System.out.println(link.absUrl("href").toString().replaceAll(".*\\.", "ending: ").replaceAll("/.*", " "));
				this.links.add(link.absUrl("href"));// save all links in the list
			}
			try{
				if (getContent) {
					getContent(htmlDocument);
				}
			}catch(Exception t){
				System.out.println(t.getMessage());
			}
		} catch (IOException e) {// error handler, no need to print error, they are too common
			return false;
		}
		return true;
	}

	/**
	 * This method gets the links from the page most recently crawled
	 * 
	 * @return List<String> The links from the last paged
	 */
	public List<String> getLinks() {
		return this.links;
	}

	/**
	 * This method tries to download content from the sites
	 * 
	 * @param htmlDocument The current web page to scan for downloadable content
	 * @return void
	 */
	private void getContent(Document htmlDocument) throws IOException {
		
		Elements linksOnPage = htmlDocument.select("a[href]");// get all links from web page
		for (Element link : linksOnPage) {
			String matchingFiles = " msi| ndl| pdf| lnk| eli| swf| gdl| gui| ndlp| jar| pdf| pdf_b| pdfb| apk| dmg| dll| guip| pe+| dll+| wix| exl| cdo| gdlp| rtf| cdlp| ppt| bmp| con| nat| tex| natp| dos| wrd| cdl| gif| conp| docx| xlsx| pptx";
			//matchingFiles = " [^b]\\w+";
			if(link.absUrl("href").replaceAll(".*\\.", " ").replaceAll("/.*", " ").matches(matchingFiles)){
				file((new URL(link.absUrl("href").toString())),"output/files/doc_"+System.currentTimeMillis()+"."+link.absUrl("href").toString().replaceAll(".*\\.", " ").replaceAll("/.*", " "));
			}
		}
		
		if (!htmlDocument.getElementsByAttribute("download").isEmpty()) {//checks if there is downloadable content
			System.out.println("docs");
			for (Element doc : htmlDocument.getElementsByAttribute("download")) {
				String docLocation = doc.absUrl("src");
				URL url2 = new URL(docLocation);
				String fileLocation = "output/files/doc_";
				if (docLocation.contains("pdf")) {
					fileLocation = "output/pdfs/pdf_";
				}
				try (InputStream in = url2.openStream();
						OutputStream out = new BufferedOutputStream(new FileOutputStream(fileLocation + System.currentTimeMillis()))) {
					for (int b; (b = in.read()) != -1;) {//write out data
						out.write(b);
					}
					out.close();
					in.close();
					// or
					file(url2,fileLocation);
				}catch(Exception e){}
			}
		}
		if (savePics && !htmlDocument.getElementsByTag("img").isEmpty()) {//checks for downloadable images
			for (Element img : htmlDocument.getElementsByTag("img")) {
				String imageLocation = img.absUrl("src");
				URL url2 = new URL(imageLocation);
				InputStream in = url2.openStream();
				OutputStream out = new BufferedOutputStream(new FileOutputStream("output/imgs/img_" + System.currentTimeMillis()));
				for(int b; (b = in.read()) != -1;) {
					out.write(b);
				}
				out.close();
				in.close();
			}
		}

	}
	
	/**
	 * This method connects to and downloads documents acting as a normal user
	 * 
	 * @param URL The download link
	 * @param fileLocation the place to save the downloaded document
	 * @return void
	 */
	public void file(URL url,String fileLocation){
		try (FileOutputStream output = new FileOutputStream(fileLocation)){
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(12000);
			connection.connect();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return;
			}
			// download the file
			InputStream input = connection.getInputStream();

			byte[] data = new byte[4096];
			int count;
			while ((count = input.read(data)) != -1) {
				output.write(data, 0, count);
			}
			input.close();
		} catch (Exception e) {}
	}
}
