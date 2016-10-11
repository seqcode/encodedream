package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.seqcode.data.io.RegionFileUtilities;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.location.StrandedPoint;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class FilterDNaseDomains {
	
	protected GenomeConfig gcon;
	protected List<Region> inputDNaseDomains = new ArrayList<Region>();
	public Map<String,List<Region>> exclude = new HashMap<String,List<Region>>();
	public List<StrandedPoint> tss = new ArrayList<StrandedPoint>();
	public int tssExcludeWin = 10000; // 10kbp
	
	public boolean summits = false;
	
	
	
	public void setInputDomains(String fname) throws IOException{
		if(summits){
			inputDNaseDomains.addAll(RegionFileUtilities.loadRegionsFromFile(fname, gcon.getGenome(), 6));
		}else{
			inputDNaseDomains.addAll(RegionFileUtilities.loadRegionsFromFile(fname, gcon.getGenome(), -1));
		}
	}
	public void setTSS(String fname){
		tss.addAll(RegionFileUtilities.loadStrandedPointsFromFile(gcon.getGenome(), fname));
	}
	public void setTssExcludeWin(int w){tssExcludeWin = w;}
	public void setSummits(boolean b){summits = b;}
	
	
	public FilterDNaseDomains(GenomeConfig g) {
		gcon = g;
	}
	
	public void execute(){
		
		// first expand tss in regions
		Map<String,List<Region>> tssExcludeByCHR = new HashMap<String,List<Region>>();
		for(StrandedPoint sp : tss){
			if(tssExcludeByCHR.containsKey(sp.getChrom())){
				tssExcludeByCHR.get(sp.getChrom()).add(sp.expand(tssExcludeWin));
			}else{
				tssExcludeByCHR.put(sp.getChrom(), new ArrayList<Region>());
				tssExcludeByCHR.get(sp.getChrom()).add(sp.expand(tssExcludeWin));
			}
		}
		
		for(Region dom : inputDNaseDomains){
			String currChrom = dom.getChrom();
			if(tssExcludeByCHR.containsKey(currChrom) || tssExcludeByCHR.containsKey(currChrom.replace("chr", ""))){
				currChrom = tssExcludeByCHR.containsKey(currChrom) ? currChrom : currChrom.replace("chr", "");
				boolean overlap = false;
				for(Region tssR : tssExcludeByCHR.get(currChrom)){
					if(dom.overlaps(tssR)){
						overlap = true;
						break;
					}
				}
				if(!overlap){
					if(summits){
						System.out.println(dom.getMidpoint().getLocationString());
					}else{
						System.out.println(dom.getLocationString());
					}
				}
			}
		}
		
	
	}
	
	
	public static void main(String[] args) throws IOException{
		ArgParser ap = new ArgParser(args);
		GenomeConfig g = new GenomeConfig(args);
		FilterDNaseDomains runner = new FilterDNaseDomains(g);
		runner.setSummits(ap.hasKey("summits"));
		runner.setInputDomains(ap.getKeyValue("domains"));
		runner.setTSS(ap.getKeyValue("tss"));
		runner.setTssExcludeWin(Args.parseInteger(args, "tssExclude", 10000));
		runner.execute();
	}
	
	
	
	
	
	
	

}
