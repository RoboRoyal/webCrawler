package spiders;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.apache.log4j.Logger;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SpiderLeg {
  private static boolean getContent = true;//wether or not to download anything from the sites
  private static boolean savePics = true;//will download pictures only if getConent is also set to true
  private static int filesDownloaded = 0;//max number of files to be downloaded, when limit is met will stop downloading pictures too
  private static int maxFiles = 10;//max number of files allowed to be downloaded, -1 for no limit
  private static boolean quiet = false;//to hide small errors
  public static boolean saveJS = true;
  private static Logger logger = Logger.getLogger(SpiderLeg.class.getCanonicalName());

  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";// pretend we are using a browser																																							// browser

  private List<String> links = new LinkedList<>();


  /**
   * This method is what actually crawls individual websites
   *
   * @param String
   *        The URL to crawl
   * @return boolean If the crawl was successful or not
   */
  public boolean crawl(String url) {
    try {
      Connection connection = Jsoup.connect(url).userAgent(USER_AGENT);// Connects to web pages
      connection.timeout(8112);
      Document htmlDocument = connection.get();// get contents of web page

      if (!connection.response().contentType().contains("text/html")) {// print out if connection failed
        if(!quiet){
        	logger.warn("**Failure** Retrieved something other than HTML");}
        return false;
      }
      Elements linksOnPage = htmlDocument.select("a[href]");// get all links from web page
      for (Element link : linksOnPage) {
        this.links.add(link.absUrl("href"));// save all links in the list
      }
      try {
        if (getContent && (filesDownloaded < maxFiles || maxFiles == -1) ) {
          getContent(htmlDocument);
        }
      } catch (Exception t) {
        if(!quiet){
        	logger.trace(t);}
      }
    } catch (IOException e) {// error handler
    	if(!quiet){
    		logger.trace(e);}
      return false;
    }
    return true;
  }

  private void saveJS(String page) {
	String temp = page;
	do{
		temp = temp.replaceAll("\n","!@#@!").replaceFirst(".*<script>", " ").replaceFirst("</script>.*", " ").replaceAll("!@#@!","\n");
		saveMe(temp);
		//logger.info(temp);
	}while(temp.contains("<script>"));
	
}
  private void saveMe(String js){
	  try (OutputStream out =new BufferedOutputStream(new FileOutputStream("output/js/js_"
	                    + System.currentTimeMillis()+".js"))) {
		  out.write(js.getBytes());
	  }catch(Exception e){logger.trace(e);}
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
	  if(saveJS && htmlDocument.body().toString().toLowerCase().contains("<script>")){
    	  logger.info("Found js!!");
    	  saveJS(htmlDocument.body().toString().toLowerCase());
      }

    Elements linksOnPage = htmlDocument.select("a[href]");// get all links from web page
    for (Element link : linksOnPage) {
      String matchingFiles =
          " msi| zip| rar| tar| pdf| lnk| swf| exe| dll| jar| pdf| apk| dmg| xls| xlsm| xlsx| ppt| pptm| pptx| rtf| doc| docm| docx| bmp| bitmap| gif| dos| bat| js";//cant do .com files
      //matchingFiles = " [^b]\\w+";//use this if you want to download file types that hector can't check on blu netowrk
      //matchingFiles = "(?! com| net| org| gov| info| biz| top| io| blu| edu| php| ru| html| biz| us| io| top| xxx| win| me| tv)";//use this if you want to download file types that hector can't check on Internet
      //matchingFiles = " mp4| mp3| webm| avi| wmv| mpeg4| flv| flac"//video and audio files only
      if (link.absUrl("href").replaceAll(".*\\.", " ").replaceAll("/.*", " ").matches(matchingFiles)) {
          String location = link.absUrl("href").replaceAll(".*//", " ").replaceAll("/.*", " ").replaceAll(" ", "_").replaceAll("\\.", "_");
    	filesDownloaded++;
        file(new URL(link.absUrl("href").toString()),
            "output/files/doc" +location + System.currentTimeMillis()
                + link.absUrl("href").replaceAll(".*\\.", " ").replaceAll("/.*", " ").replaceFirst(" ", "."));
      }
    }

    if (!htmlDocument.getElementsByAttribute("download").isEmpty()) {//checks if there is downloadable content
      for (Element doc : htmlDocument.getElementsByAttribute("download")) {
    	filesDownloaded++;
        String docLocation = doc.absUrl("src");
        URL url2 = new URL(docLocation);
        String fileLocation = "output/files/doc_";
        if (docLocation.contains("pdf")) {
          fileLocation = "output/pdfs/pdf_";
        }
        try (InputStream in = url2.openStream();
            OutputStream out =
                new BufferedOutputStream(new FileOutputStream(fileLocation
                    + System.currentTimeMillis()))) {
          for (int b; (b = in.read()) != -1;) {//write out data
            out.write(b);
          }
          file(url2, fileLocation);
        } catch (Exception e) {logger.trace(e);}
      }
    }
    if (savePics && !htmlDocument.getElementsByTag("img").isEmpty()) {//checks for downloadable images
        for (Element img : htmlDocument.getElementsByTag("img")) {
          String imageLocation = img.absUrl("src");
          String extention = img.absUrl("src").replaceAll(".*\\.", " ").replaceAll("/.*", " ").replaceFirst(" ", ".").replaceFirst("%.*", " ");
          URL url2 = new URL(imageLocation);
          extention = extention.replaceAll("\\?.*", " ");
          InputStream in = url2.openStream();
          imageLocation = imageLocation.replaceAll(".*//", " ").replaceAll("/.*", " ").replaceAll(" ", "_").replaceAll("\\.", "_");
          if (!extention.matches(".com.*|.main.*|.org.*|.title.*|.tv.*|.cms.*")) {//filter out bad extentions
        	  OutputStream out =
                      new BufferedOutputStream(new FileOutputStream("output/imgs/img"
                    		  +imageLocation+ System.currentTimeMillis()+extention));
                  for (int b; (b = in.read()) != -1;) {
                    out.write(b);
                  }
                  out.close();
                  in.close();  
          }
        }
      }
    
  }

  /**
   * This method connects to and downloads documents acting as a normal user
   *
   * @param URL
   *        The download link
   * @param fileLocation
   *        the place to save the downloaded document
   * @return void
   */
  public void file(URL url, String fileLocation) {
    try (FileOutputStream output = new FileOutputStream(fileLocation)) {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();// opens new connection
      connection.setConnectTimeout(12000);
      connection.connect();// Connects directly to download link
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return;
      }
      // download the file
      try (InputStream input = connection.getInputStream()) {

        byte[] data = new byte[4096];
        int count;
        while ((count = input.read(data)) != -1) {
          output.write(data, 0, count);
        }
        input.close();
      } catch (Exception ilv) {
        logger.trace(ilv);
      }
    } catch (Exception e) {
      logger.trace(e);
    }
  }
  /**
   * This method sets the maximum number of files allowed to be downloaded
   * @param newMaxFiles
   * @return void
   */
  public static void maxFiles(int newMaxFiles) {
	maxFiles = newMaxFiles;
  }
  /**
   * This method sets if files are to be downloaded
   * @param saveContent
   * @return void
   */
  public static void saveContent(boolean saveContent) {
	getContent = saveContent;
  }
  /**
   * This method sets if pictures are to be saved
   * @param saveImages
   * @return void
   */
  public static void savePics(boolean saveImages) {
	savePics = saveImages;
  }
  /**
   * This method returns the number of files that have been downloaded
   * @return filesDownloaded
   */
  public static int filesDownloaded() {
	return filesDownloaded;
  }
  /**
   * This method sets if to hide errors
   * @param boolean updates status
   * @return void
   */
  public static void updateQuiet(boolean updated) {
	quiet = updated;
  }
}
