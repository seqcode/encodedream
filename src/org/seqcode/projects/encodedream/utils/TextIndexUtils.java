package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.gsebricks.verbs.location.PointParser;
import org.seqcode.gsebricks.verbs.location.RegionParser;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;
import org.seqcode.genome.location.Point;
import org.seqcode.genome.location.Region;

public class TextIndexUtils {
	
	protected GenomeConfig gcon;
	protected HashMap<String,Integer> testSetIndex = new HashMap<String,Integer>();
	protected List<String> regions = new ArrayList<String>();
	protected RegionParser rparser;
	protected PointParser pparser;
	protected boolean randomSubset = false; // extracts a random subset; when doing this excludes regions from "regions"
	protected int numRand = 100000;
	protected boolean fullSet = false; // print all the test set; treating the "regions" as positive and everything else as "U"
	protected static final int numCharsInRecord = 804;
	protected static final int NUM_TEST = 60519747;
	protected static final int TEST_JUMP_MAX = 600;
	protected Random ran = new Random();
	protected String onehotFname;
	
	public TextIndexUtils(GenomeConfig g) {
		gcon = g;
		rparser = new RegionParser(g.getGenome());
		pparser = new PointParser(g.getGenome());
	}
	
	
	// Settors
	public void setTestSetIndex(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine())!= null){
			String[] pieces = line.split("\t");
			testSetIndex.put(pieces[0], Integer.parseInt(pieces[1]));
		}
		br.close();
	}
	public void setRandMode(){randomSubset=true;}
	public void setFullMode(){fullSet = true;}
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
	public void setNumRand(int n){numRand = n;}
	public void setOneHotFname(String fname){onehotFname = fname;}
	
	
	
	public void execute() throws IOException{
		// First, map the input regions to test indices
		Map<Integer,String> inputRegsIndex = new HashMap<Integer,String>();
		for(String s : regions){
			Region r = rparser.execute(s);
			Point midpoint = r.getMidpoint();
			int reminder = midpoint.getLocation() % 50;
			Point closestMidPoint = null;
			if(reminder > 25 ){
				closestMidPoint = new Point(midpoint.getGenome(),midpoint.getChrom(),midpoint.getLocation()+50-reminder);
			}else{
				closestMidPoint = new Point(midpoint.getGenome(),midpoint.getChrom(),midpoint.getLocation()-reminder);
			}
			int currtestInd = testSetIndex.get(closestMidPoint.expand(100).getLocationString());
			inputRegsIndex.put(currtestInd, closestMidPoint.expand(100).getLocationString());
		}
		
		System.err.println(regions.size()+" mapped to "+inputRegsIndex.keySet().size()+" unique test regions!!");
		
		// Now roll down the list and print the output file
		int ind =0;
		BufferedReader br = new BufferedReader(new FileReader(onehotFname));
		String line=null;
		// First, if all we need is random subset. Randomly skip lines until you get the desired number of lines
		if(randomSubset){
			int charstojump = ran.nextInt(TEST_JUMP_MAX)*numCharsInRecord;
			long skipDump = br.skip(charstojump);
			ind += (int) (skipDump/numCharsInRecord);
			int numPrinted = 0;
			while((line=br.readLine())!=null && numPrinted < numRand){
				if(!inputRegsIndex.containsKey(ind)){ // if this region is not in "B"
					String[] outString=line.split("");
					System.out.print(StringUtils.join(outString, ","));
					System.out.println("0");
					numPrinted++;
				}
				charstojump = ran.nextInt(TEST_JUMP_MAX)*numCharsInRecord;
				skipDump = br.skip(charstojump);
				ind += (int) (skipDump/numCharsInRecord);
			}
		}else if(!fullSet){ // if not random, and not fullset, just print the postitve records
			while((line=br.readLine())!=null){
				if(inputRegsIndex.containsKey(ind)){
					String[] outString=line.split("");
					System.out.print(StringUtils.join(outString, ","));
					System.out.println(",1");
				}
				ind++;
			}
		}else{
			while((line=br.readLine())!=null){
				if(inputRegsIndex.containsKey(ind)){
					String[] outString=line.split("");
					System.out.print(StringUtils.join(outString, ","));
					System.out.println(",1");
				}else{
					String[] outString=line.split("");
					System.out.print(StringUtils.join(outString, ","));
					System.out.println(",0");
				}
				ind++;
			}
		}
		
		br.close();
	}
	
	public static void main(String[] args) throws IOException{
		GenomeConfig g = new GenomeConfig(args);
		TextIndexUtils utils = new TextIndexUtils(g);
		ArgParser ap = new ArgParser(args);
		utils.setTestSetIndex(ap.getKeyValue("testInd"));
		utils.setRegions(ap.getKeyValue("regs"));
		utils.setOneHotFname(ap.getKeyValue("data"));
		if(ap.hasKey("rand")){
			utils.setRandMode();
			utils.setNumRand(Args.parseInteger(args, "rand", 100000));
		}else if(ap.hasKey("full")){
			utils.setFullMode();
		}
		utils.execute();
		
	}
	

}
