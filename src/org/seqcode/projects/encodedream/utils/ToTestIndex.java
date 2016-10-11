package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Point;
import org.seqcode.genome.location.Region;
import org.seqcode.gsebricks.verbs.location.PointParser;
import org.seqcode.gsebricks.verbs.location.RegionParser;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

/**
 * Takes a given set of regions and scores indexed by test-set
 * Outputs the scores for the input regions in the order they were given
 * @author akshaykakumanu
 *
 */

public class ToTestIndex {
	protected GenomeConfig gcon;
	protected HashMap<String,Integer> testSetIndex = new HashMap<String,Integer>();
	protected List<Double> scores = new ArrayList<Double>();
	protected List<String> regions = new ArrayList<String>();
	protected RegionParser rparser;
	protected PointParser pparser;
	protected boolean printRegion = false; // if false prints the mid-point location string
	
	
	public ToTestIndex(GenomeConfig g) {
		gcon=g;
		rparser = new RegionParser(g.getGenome());
		pparser = new PointParser(g.getGenome());
	}
	
	// Settors
	public void setPrintRegion(boolean b){printRegion = b;}
	public void setTestSetIndex(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine())!= null){
			String[] pieces = line.split("\t");
			testSetIndex.put(pieces[0], Integer.parseInt(pieces[1]));
		}
		br.close();
	}
	public void setScores(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine())!=null){
			scores.add(Double.parseDouble(line));
		}
		br.close();
	}
	public void setRegions(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine())!= null){
			if(line.startsWith("#"))
				continue;
			String[] pieces = line.split("\t");
			if(pieces[0].contains("-")){
				Region r = rparser.execute(pieces[0]);
				regions.add(r.getLocationString());
			}else if(pieces[0].contains(":") && !pieces[0].contains("-")){
				Point p = pparser.execute(pieces[0]);
				regions.add(p.expand(5).getLocationString());
			}
		}
		br.close();
	}
	public void setRegionsList(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine()) != null){
			if(line.startsWith("#"))
				continue;
			String[] pieces = line.split("\t");
			setRegions(pieces[0]);
		}
		br.close();
	}
	
	
	public void printTestIndices(){
		for(String s : regions){
			int testInd = 0;
			if(testSetIndex.containsKey(s)){
				testInd = testSetIndex.get(s);
			}else{
				Region r = rparser.execute(s);
				Point midpoint = r.getMidpoint();
				int reminder = midpoint.getLocation() % 50;
				Point closestMidPoint = null;
				if(reminder > 25 ){
					closestMidPoint = new Point(midpoint.getGenome(),midpoint.getChrom(),midpoint.getLocation()+50-reminder);
				}else{
					closestMidPoint = new Point(midpoint.getGenome(),midpoint.getChrom(),midpoint.getLocation()-reminder);
				}
				if(testSetIndex.containsKey(closestMidPoint.expand(100).getLocationString())){
					testInd = testSetIndex.get(closestMidPoint.expand(100).getLocationString());
				}else{
					continue;
				}
			}
			if(printRegion){
				System.out.println(s+"\t"+Integer.toString(testInd));
			}else{
				Region r = rparser.execute(s);
				System.out.println(r.getMidpoint().getLocationString()+"\t"+Integer.toString(testInd));
			}
		}
	}
	
	
	// Simple hack to look at the test-regions closest to a given region.
	public void execute(){
		for(String s : regions){
			int testInd = 0;
			if(testSetIndex.containsKey(s)){
				testInd = testSetIndex.get(s);
			}else{ 
				Region r = rparser.execute(s);
				Point midpoint = r.getMidpoint();
				int reminder = midpoint.getLocation() % 50;
				Point closestMidPoint = null;
				if(reminder > 25 ){
					closestMidPoint = new Point(midpoint.getGenome(),midpoint.getChrom(),midpoint.getLocation()+50-reminder);
				}else{
					closestMidPoint = new Point(midpoint.getGenome(),midpoint.getChrom(),midpoint.getLocation()-reminder);
				}
				testInd = testSetIndex.get(closestMidPoint.expand(100).getLocationString());
			}
			
			System.out.println(scores.get(testInd));
			
		}
	}

	
	public void executeFull(){
		// Hash the test indexes by chromosomes
		HashMap<String, List<String>> byCHR = new HashMap<String,List<String>>();
		for(String s : testSetIndex.keySet()){
			String[] pieces = s.split(":");
			if(byCHR.containsKey(pieces[0])){
				byCHR.get(pieces[0]).add(s);
			}else{
				byCHR.put(pieces[0], new ArrayList<String>());
				byCHR.get(pieces[0]).add(s);
			}
		}
		
		// Now sort the lists
		for(String s : byCHR.keySet()){
			Collections.sort(byCHR.get(s), new Comparator<String>(){

				@Override
				public int compare(String o1, String o2) {
					Region r1 = rparser.execute(o1);
					Region r2 = rparser.execute(o2);
					return r1.compareTo(r2);
				}
				
			});
		}
		
		for(String s : regions){
			int testInd = 0;
			if(testSetIndex.containsKey(s)){
				testInd = testSetIndex.get(s);
			}else{
				// now binary serach for the test regions
				String currChr = s.split(":")[0];
				int srchInd = Collections.binarySearch(byCHR.get(currChr), s, new Comparator<String>(){

					@Override
					public int compare(String o1, String o2) {
						Region r1 = rparser.execute(o1);
						Region r2 = rparser.execute(o2);
						return r1.compareTo(r2);
					}
					
				});
				int instn_pt = -1*srchInd-1;
				Region cuurReg = rparser.execute(s);
				Region leftToInsrtPoint = rparser.execute(byCHR.get(currChr).get(Math.min(0,instn_pt-1)));
				Region rightToInsrtPoint = rparser.execute(byCHR.get(currChr).get(Math.min(byCHR.get(currChr).size()-1,instn_pt+1)));
				if(cuurReg.getMidpoint().distance(leftToInsrtPoint.getMidpoint()) < 
						cuurReg.getMidpoint().distance(rightToInsrtPoint.getMidpoint())){
					testInd = testSetIndex.get(byCHR.get(currChr).get(Math.min(0,instn_pt-1)));
				}else{
					testInd = testSetIndex.get(byCHR.get(currChr).get(Math.min(byCHR.get(currChr).size()-1,instn_pt+1)));
				}
			}
			System.out.println(scores.get(testInd));
		}
	}
	
	
	public static void main(String[] args) throws IOException{
		ArgParser ap = new ArgParser(args);
		GenomeConfig g = new GenomeConfig(args);
		ToTestIndex runner = new ToTestIndex(g);
		runner.setPrintRegion(ap.hasKey("printRegion"));
			
		if(ap.hasKey("regions")){
			runner.setRegions(ap.getKeyValue("regions"));
		}else if(ap.hasKey("regionsList")){
			runner.setRegionsList(ap.getKeyValue("regionsList"));
		}
		runner.setTestSetIndex(ap.getKeyValue("testInd"));
		if(ap.hasKey("scores"))
			runner.setScores(ap.getKeyValue("scores"));
		if(ap.hasKey("scores"))
			runner.execute();
		if(ap.hasKey("printIndices"))
			runner.printTestIndices();
	}
	
	
	
	
}
