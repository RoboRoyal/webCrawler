package spiders;

import java.io.BufferedOutputStream;

public class SpiderLeg {
  private static boolean getContent = true;//Whether or not to download anything from the sites
  private static boolean savePics = true;//will download pictures only if getConent is also set to true
  private static int filesDownloaded = 0;//max number of files to be downloaded, when limit is met will stop downloading pictures too
  private static int maxFiles = 10;//max number of files allowed to be downloaded, -1 for no limit
  private static boolean quiet = false;//to hide small errors
  protected static boolean saveJS = true;
  private static int maxImgs = 100;//max images per page
  private int imagesDownloaded = 0;
  private static Logger logger = Logger.getLogger(SpiderLeg.class.getCanonicalName());

  private static final String USER_AGENT =
  //    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36";// pretend we are using a browser	
  // "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/603.1 (KHTML, like Gecko) Version/10.0 Safari/603.1";//other
  "Mozilla/5.0 (compatible; Duckbot/"+SpiderSpawner.getVerion()+"; Sorry for crawling your site, D.A.)";

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
      connection.timeout(5916);
      Document htmlDocument = connection.get();// get contents of web page

      if (!connection.response().contentType().contains("text/html")) {// print out if connection failed
        if(!quiet){
        	logger.trace("**Failure** Retrieved something other than HTML");}
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
        	logger.error(t);}
      }
    } catch (IOException e) {// error handler
    	if(!quiet){
    		logger.trace(e);}
      return false;
    }
    return true;
  }

  /**
   * Parses out the html document to find the java script
   * 
   * @param page the HTML document for the web page
   */
  private void saveJS(String page, String url) {
	String temp;
	if(url == null){
		  url="_";}
	do{
		String saveSpace = "asdfQT";
		temp = page;
		temp = temp.replaceAll("\n|\r",saveSpace).replaceFirst(".*<script>"," ").replaceFirst("<\\/script>.*"," ").replaceAll(saveSpace,"\n");
		saveMe(temp, url);
		page = page.replaceAll("\n|\r",saveSpace).replaceFirst(".*<\\/script>"," ").replaceAll(saveSpace,"\n"); 
	}while(page.contains("<script>"));	
  }
  /**
   * Takes the extracted java script and saves in as a .js file
   * @param js The string containing javascript
   */
  private void saveMe(String js, String url){
	  try (OutputStream out =new BufferedOutputStream(new FileOutputStream("output/js/js"+url
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
	  
    Elements linksOnPage = htmlDocument.select("a[href]");// get all links from web page
    for (Element link : linksOnPage) {
      String matchingFiles =
          " msi| zip| rar| tar| pdf| lnk| swf| exe| dll| jar| pdf| apk| dmg| xls| xlsm| xlsx| ppt| pptm| pptx| rtf| doc| docm| docx| bmp| bitmap| gif| dos| bat| ps1";//cant do .com files
      //matchingFiles = " [^b]\\w+";//use this if you want to download file types that hector can't check on blu netowrk
      //matchingFiles = "(?! com| net| org| gov| info| biz| top| io| blu| edu| php| ru| html| biz| us| io| top| xxx| win| me| tv)";//use this if you want to download file types that hector can't check on Internet
      //matchingFiles = " mp4| mp3| webm| avi| wmv| mpeg4| flv| flac"//video and audio files only
      if (link.absUrl("href").replaceAll(".*\\.", " ").replaceAll("/.*", " ").matches(matchingFiles)) {
          String location = link.absUrl("href").replaceAll(".*//", " ").replaceAll("/.*", " ").replaceAll(" ", "_").replaceAll("\\.", "_");
    	filesDownloaded++;
        file(new URL(link.absUrl("href")),
            "output/files/doc" +location + System.currentTimeMillis()
                + link.absUrl("href").replaceAll(".*\\.", " ").replaceAll("/.*", " ").replaceFirst(" ", "."));
      }
    }
    String imageLocation = null;
    imagesDownloaded = 0;
		if (savePics) {// && !htmlDocument.getElementsByTag("img").isEmpty())
						// {//checks for downloadable images
			for (Element img : htmlDocument.getElementsByTag("img")) {
				imagesDownloaded++;
				imageLocation = img.absUrl("src");
				String extention = img.absUrl("src").replaceAll(".*\\.", " ").replaceAll("/.*", " ")
						.replaceFirst(" ", ".").replaceFirst("%.*", " ").replaceAll("\\?.*", " ");
				if (!extention.matches(".com.*|.main.*|.org.*|.title.*|.tv.*|.cms.*") && imagesDownloaded<maxImgs) {// filter out bad extensions
					URL url2 = new URL(imageLocation);
					InputStream in = url2.openStream();
					imageLocation = imageLocation.replaceAll(".*//", " ").replaceAll("/.*", " ").replaceAll(" ", "_")
							.replaceAll("\\.", "_");
					OutputStream out = new BufferedOutputStream(new FileOutputStream(
							"output/imgs/img" + imageLocation + System.currentTimeMillis() + extention));
					for (int b; (b = in.read()) != -1;) {
						out.write(b);
					}
					out.close();
					in.close();
				}
			}
		}
    if(saveJS && htmlDocument.body().toString().toLowerCase().contains("<script>")){
  	  saveJS(htmlDocument.body().toString().toLowerCase(), imageLocation);
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
   */
  public static void maxFiles(int newMaxFiles) {
	maxFiles = newMaxFiles;
  }
  
  /**
   * This method sets if files are to be downloaded
   * @param saveContent
   */
  public static void saveContent(boolean saveContent) {
	getContent = saveContent;
  }
  
  /**
   * This method sets if pictures are to be saved
   * @param saveImages
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
   */
  public static void updateQuiet(boolean updated) {
	quiet = updated;
  }
  /**
   * sets the max number of images to save per page
   * Default is 100
   * @param i max images per page
   */
  public static void maxImgs(int i){
	  maxImgs = i;
  }
}
