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
	private static Logger logger = Logger.getLogger(SpiderTamer.class.getCanonicalName());

	private SpiderTamer() {
	}

	/**
	 * This method gets the URLs/domains from black.txt and add them to the
	 * black list
	 *
	 * @param p
	 *            The spider to change
	 */
	public static void fileBlack(Spider p) {
		String blackFile = "Fileconfig/black.txt";
		try (Scanner in = new Scanner(new File(blackFile))) {
			String line;
			while (in.hasNextLine()) {
				line = in.nextLine();
				// ignore lines shorter then 3 chars and lines that start with #
				if (line.length() >= 3 && !line.startsWith("#")) {
					p.addBlacklistedDomain(line);
				}
			}
		} catch (IOException e) {
			logger.error("Problem getting from blacklist: " + e);
		}
	}

	/**
	 * This method gets the URLs/domains from white.txt and add them to the
	 * white list
	 *
	 * @param p
	 *            The spider to change
	 */
	public static void fileWhite(Spider p) {
		String whiteFile = "Fileconfig/white.txt";
		try (Scanner in = new Scanner(new File(whiteFile))) {
			String line;
			while (in.hasNextLine()) {
				line = in.nextLine();
				// ignore lines shorter then 3 chars and lines that start with #
				if (line.length() >= 3 && !line.startsWith("#")) {
					p.addWhitelistedDomain(line);
				}
			}
		} catch (IOException e) {
			logger.error("Problem getting from whitelist: " + e);
		}
	}

	/**
	 * This method writes to text files the URLs crawled and the email's found
	 *
	 * @param p
	 *            The spider to get info from
	 * @return boolean If the read and write were successful
	 */
	public static boolean writeToFile() {
		logger.info("  Writing to file...");
		String fileOut = "output/crawledURLS.txt";
		String mailFile = "output/foundEmails.txt";
		Set<String> emails = new HashSet<>();
		// Separates out all the emails
		for (String line : Spider.getPagesVisited()) {
			if (line.contains("mailto")) {
				emails.add(line);
			}
		}
		try (Writer spiderJocky = new BufferedWriter(new FileWriter(new File(fileOut)));
				Writer spiderJocky2 = new BufferedWriter(new FileWriter(new File(mailFile)))) {

			// write crawledURLS
			logger.info("  Attempting to write " + Spider.getPagesVisited().size() + " links to file...");
			spiderJocky.write(Spider.getPagesVisited().toString().replaceAll(",", "\n").replaceAll("mailto.*", " ")
					.replaceFirst("\\]", " ").replaceFirst("\\[", " "));

			// write emailsFound
			int emailCount = emails.size();
			if (emailCount > 0) {
				logger.info("  Attempting to write " + emailCount + " emails to file...");
				spiderJocky2.write(
						emails.toString().replaceFirst("\\]", " ").replaceAll(",", "\n").replaceFirst("\\[", " "));
			}
		} catch (IOException e) {
			logger.error("Problem writing to file: " + e.getMessage(), e);
			return false;
		}
		logger.info("  Written!");
		return true;
	}

	/**
	 * This method gets the URLs from linksToCrawl and adds them to the list of
	 * links to be crawled
	 *
	 * @param p
	 *            The spider to add links to
	 */
	public static void fileAddLinks() {
		String urlFile = "Fileconfig/linksToCrawl.txt";
		try (Scanner in = new Scanner(new File(urlFile))) {
			String line;
			while (in.hasNextLine()) {
				line = in.nextLine();
				// ignore lines shorter then 3 chars and lines that start with #
				if (line.length() >= 3 && !line.startsWith("#")) {
					Spider.addURL(line);
				}
			}
		} catch (IOException e) {
			logger.error("Problem reading from '" + urlFile + ": " + e);
		}
	}
}
