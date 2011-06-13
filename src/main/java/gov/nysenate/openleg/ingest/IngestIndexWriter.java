package gov.nysenate.openleg.ingest;

import gov.nysenate.openleg.model.SenateObject;
import gov.nysenate.openleg.lucene.LuceneSerializer;
import gov.nysenate.openleg.model.bill.Bill;
import gov.nysenate.openleg.search.Result;
import gov.nysenate.openleg.search.SearchEngine;
import gov.nysenate.openleg.search.SenateResponse;
import gov.nysenate.openleg.util.EasyReader;
import gov.nysenate.openleg.util.JsonSerializer;
import gov.nysenate.openleg.util.LongSearch;
import gov.nysenate.openleg.util.SessionYear;
import gov.nysenate.openleg.util.Timer;
import gov.nysenate.openleg.util.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class IngestIndexWriter {
	private Logger logger = Logger.getLogger(IngestIndexWriter.class);
	private static final int BATCH_SIZE = 1000;
	
	JsonDao jsonDao;
	SearchEngine searchEngine;
	
	String jsonDirectory;
	String logPath;
	
	Timer timer;
	
	public IngestIndexWriter(String jsonDirectory, String logPath, SearchEngine searchEngine, JsonDao jsonDao) {
		this.searchEngine = searchEngine;
		this.jsonDao = jsonDao;
		
		this.jsonDirectory = jsonDirectory;
		this.logPath = logPath;
		
		timer = new Timer();
	}
	
	/**
	 * index documents based on what is listed in log from JsonDao write function
	 */
	public void indexBulk() {
		indexBulk(truncateLog());
	}

	/**
	 * Index BATCH_SIZE number of documents per operation
	 * @param filePaths string paths of files that must be indexed
	 */
	public void indexBulk(String[] files) {
		ArrayList<SenateObject> lst;
		Pattern p = Pattern.compile("\\d{4}/(\\w+)/.*$");
		Matcher m = null;
		
		logger.warn("Indexing " + files.length + " documents");
		
		int its = files.length/BATCH_SIZE;
		for(int i = 0; i <= its; i++) {
			lst = new ArrayList<SenateObject>();
			
			timer.start();
			for(int j = (i * BATCH_SIZE); j < (((i+1) * BATCH_SIZE)) && j < files.length; j++) {
				m = p.matcher((String)files[j]);
				if(m.find()) {
					SenateObject senObj = jsonDao.load((String)files[j], Ingest.getIngestType(m.group(1)).clazz());
					
					if(senObj != null) {
						lst.add(senObj);
					}
				}
			}
			logger.warn(timer.stop() + " - Read " + lst.size() + " Objects");
			
			timer.start();
			try {
				searchEngine.indexSenateObjects(lst, new LuceneSerializer[] {new XmlSerializer(), new JsonSerializer()});
			} catch (IOException e) {
				logger.error(e);
			}
			logger.warn(timer.stop() + " - Indexed Objects");
			
			lst.clear();
		}
	}
	
	private String[] truncateLog() {
		File file = new File(jsonDirectory + "/.log");
		
		if(!file.exists()) {
			return new String[0];
		}

		EasyReader er = new EasyReader(file).open();
		
		/*
		 * want bills to show up first in the 
		 */
		TreeSet<String> set = new TreeSet<String>();
		
		String in = null;
		while((in = er.readLine()) != null) {
			set.add(in);
		}
		er.close();
		
		String[] files = new String[set.size()];
		
		set.toArray(files);
		
		file.deleteOnExit();
		
		return files;
	}
	
	/**
	 *  The same as markInactiveBills(year) but passes on
	 *  the current session year from SessionYear.getSessionYear()
	 */
	public void markInactiveBills() {
		markInactiveBills(SessionYear.getSessionYear() + "");
	}
	
	/**
	 * scans all bills in the index looking for bills with amendments, once the latest version of
	 * a bill is found it passes it to reindexAmendedVersions(bill) where old versions
	 * of the bill are marked as inactive
	 * 
	 * @param year - the session year for bills you want to be checked
	 */
	public void markInactiveBills(String year) {
		Bill prev = null;
		
		boolean reindex = false;
		
		LongSearch<Bill> longSearch = new LongSearch<Bill>()
			.clazz(Bill.class)
			.query("otype:bill AND year:" + year);
		
		for(Bill bill:longSearch) {
			if(prev != null) {
				String billNo = bill.getSenateBillNo().split("-")[0];
				
				if(cleanBillNo(prev).equals(billNo.replaceAll("[A-Z]$", ""))) {
					reindex = true;
				}
				else {
					if(reindex) {
						logger.warn("reindexing amended versions: " + prev.getSenateBillNo());
						reindexAmendedVersions(prev);
					}
					else {
						if(Character.isLetter(billNo.charAt(billNo.length() - 1))) {
							logger.warn("reindexing amended versions: " + prev.getSenateBillNo());
							reindexAmendedVersions(bill);
						}
					}
					reindex = false;
				}
			}
			prev = bill;
		}
	}
	
	private String cleanBillNo(Bill bill) {
		return bill.getSenateBillNo().split("-")[0].replaceAll("[A-Z]$", "");
	}
	
	/**
	 * desirable to hide old versions of an amended bill from the default search
	 * this appends "active:false" as a field to any old versions of bills
	 * 
	 * to avoid constantly rewriting amended versions of bills this does a query
	 * to lucene to check if they've already been hidden, if they haven't then 
	 * they are sent to reindexInactiveBill
	 * 
	 * @param bill
	 * 
	 * returns true if current bill isn't searchable, false otherwise
	 */
	public boolean reindexAmendedVersions(Bill bill) {
		int idx = bill.getSenateBillNo().indexOf("-");
		char c = bill.getSenateBillNo().charAt(idx-1);
		String strings[] = bill.getSenateBillNo().split("-");
		
		String query = null;
		
		if(c >= 65 && c < 90)
			query = strings[0].substring(0, strings[0].length()-1);
		else 
			query = strings[0];
			
		try {
			//oid:(S418-2009 OR [S418A-2009 TO S418Z-2009]) AND year:2009
			query = "otype:bill AND oid:((" 
				+ query + "-" + strings[1] 
                    + " OR [" + query + "A-" + strings[1] 
                       + " TO " + query + "Z-" + strings[1]
                    + "]) AND " + query + "*-" + strings[1] + ")";
			
			SenateResponse sr = searchEngine.search(query,
					"json", 0,100, null, false);
						
			//if there aren't any results this is a new bill
			if(sr.getResults().isEmpty())
				return false;
								
			//create a list and store bill numbers from oldest to newest
			ArrayList<String> billNumbers = new ArrayList<String>();				
			for(Result result:sr.getResults()) {
				billNumbers.add(result.getOid());
			}
			if(!billNumbers.contains(bill))
				billNumbers.add(bill.getSenateBillNo());
			Collections.sort(billNumbers);
			
			String newest = billNumbers.get(billNumbers.size()-1);
			
			//if bill being stored isn't the newest we can assume
			//that the newest bill has already reindexed older bills
			if(!bill.getSenateBillNo().equals(newest))
				return true;
			
			billNumbers.remove(newest);
			billNumbers.remove(bill.getSenateBillNo());				
							
			for(Result result:sr.getResults()) {
				if(billNumbers.contains(result.getOid())) {
					if(result.isActive()) {
						reindexInactiveBill(result.getOid(), bill.getYear()+"");
					}
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.apache.lucene.queryParser.ParseException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private void reindexInactiveBill(String senateBillNo, String year) {
		Bill temp = (Bill) jsonDao.load(senateBillNo,
				year,
				"bill",
				Bill.class);
		
		if(temp != null) {
			temp.setLuceneActive(false);
			
			try {
				searchEngine.indexSenateObject(temp);
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}
}