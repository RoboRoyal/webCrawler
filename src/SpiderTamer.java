package spiders;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;


public class SpiderTamer {
  private static Logger logger = Logger.getLogger(Spider.class.getCanonicalName());

  private SpiderTamer() {}

  /**
   * This method gets the URLs/domains from black.txt and add them to the black list
   *
   * @param p The spider to change
   * @return boolean If the read and write were successful
   */
  public static void fileBlack(Spider p) {
    String blackFile = "Fileconfig/black.txt";
    try (Scanner in = new Scanner(new File((blackFile)))) {
      String line;
      while (in.hasNextLine()) {
        line = in.nextLine();
        if (line.length() >= 3 && !line.startsWith("#")) {//ignore lines shorter then 3 chars and lines that start with #
          p.addBlacklistedDomain(line);
        }
      }
    } catch (IOException e) {
      logger.error("Problem getting from blacklist: " + e.getMessage());
    }
  }

  /**
   * This method gets the URLs/domains from white.txt and add them to the white list
   *
   * @param p The spider to change
   * @return boolean If the read and write were successful
   */
  public static void fileWhite(Spider p) {
    String whiteFile = "Fileconfig/white.txt";
    try (Scanner in = new Scanner(new File((whiteFile)))) {
      String line;
      while (in.hasNextLine()) {
        line = in.nextLine();
        if (line.length() >= 3 && !line.startsWith("#")) {//ignore lines shorter then 3 chars and lines that start with #
          p.addWhitelistedDomain(line);
        }
      }
    } catch (IOException e) {
      logger.error("Problem getting from whitelist: " + e.getMessage());
    }
  }

  /**
   * This method writes to text files the URLs crawled and the email's found
   *
   * @param p The spider to get info from
   * @return boolean If the read and write were successful
   */
  public static boolean writeToFile(Spider p) {
    System.out.println("Writing to file...");
    String fileOut = "output/crawledURLS.txt";
    String mailFile = "output/foundEmails.txt";
    Set<String> emails = new HashSet<String>();

    for (String line : p.getPagesVisited()) {//Separates out all the emails
      if (line.contains("mailto")) {
        emails.add(line);
      }
    }
    try (Writer spiderJocky = new BufferedWriter(new FileWriter(new File(fileOut)));
        Writer spiderJocky2 = new BufferedWriter(new FileWriter(new File(mailFile)))) {

      //write crawledURLS
      System.out.println("Attempting to write " + p.getPagesVisited().size() + " links to file...");
      spiderJocky.write(p.getPagesVisited().toString().replaceAll("mailto.*", " ")
          .replaceFirst("\\]", " ").replaceAll(",", "\n").replaceFirst("\\[", " "));

      //write emailsFound
      System.out.println("Attempting to write " + emails.size() + " emails to file...");
      spiderJocky2.write(emails.toString().replaceFirst("\\]", " ").replaceAll(",", "\n")
          .replaceFirst("\\[", " "));
    } catch (IOException e) {
      logger.error("Problem writing to file: " + e.getMessage(), e);
      return false;
    }
    System.out.println("Written!");
    return true;
  }

  /**
   * This method gets the URLs from linksToCrawl and adds them to the list of links to be crawled
   *
   * @param p The spider to add links to
   * @return boolean If the read and write were successful
   */
  public static void fileAddLinks(Spider p) {
    String urlFile = "Fileconfig/linksToCrawl.txt";
    try (Scanner in = new Scanner(new File(urlFile))) {
      String line;
      while (in.hasNextLine()) {
        line = in.nextLine();
        if (line.length() >= 3 && !line.startsWith("#")) {//ignore lines shorter then 3 chars and lines that start with #
          p.addURL(line);
        }
      }
    } catch (IOException e) {
      logger.error("Problem reading from '" + urlFile + ": " + e.getMessage());
    }
  }
}
