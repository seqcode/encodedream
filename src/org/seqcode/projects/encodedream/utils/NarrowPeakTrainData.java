package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.seqcode.data.io.RegionFileUtilities;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Point;
import org.seqcode.genome.location.Region;
import org.seqcode.gseutils.Args;
import org.seqcode.projects.seed.features.EnrichedFeature;

/**
 * Takes a list of narrow peaks for a factor in all the train cell-lines;
 * Generates a union of  "B" and Random  "U" sites that don't overlap with any of the "B".
 * This is to be used to train the first classifier 
 * @author akshaykakumanu
 *
 */

public class NarrowPeakTrainData {
	
	protected GenomeConfig gcon;
	protected List<Region> posPeaks = new ArrayList<Region>();
	protected List<Region> negRand = new ArrayList<Region>();
	// Base pairs to jump from a positive peak to get a random peak.
	// Make sure it does not overlap with other positive peaks
	protected int randJump = 400;
	// Remove peaks that are within 50bp of each other
	protected int removeDupDistance = 50;
	protected int win = 200;
	
	
	
	
	public NarrowPeakTrainData(GenomeConfig g) {
		gcon =g;
	}
	
	// Gettors
	
	// Settors
	public void setPosPeaks(List<Region> regs){posPeaks.addAll(regs);}
	public void setRandJump(int j){randJump = j;}
	public void setwin(int w){win=w;}
	public void setRemDupDist(int d){removeDupDistance = d;}
	
	
	public void execute(){
		// First, remove redundant peaks from posPeaks
		// Sort the peaks
		Collections.sort(posPeaks);
		// Now roll down the list 
		for(int i=0; i<posPeaks.size(); i++){
			Region anchorReg = posPeaks.get(i);
			int j=i+1;
			if(j==posPeaks.size())
				break;
			if(!posPeaks.get(j).getChrom().equals(anchorReg.getChrom()))
				continue;
			while(posPeaks.get(j).getMidpoint().distance(anchorReg.getMidpoint()) < removeDupDistance && j<posPeaks.size()){
				posPeaks.remove(j);
				j++;
				if(j>=posPeaks.size())
					break;
				if(!posPeaks.get(j).getChrom().equals(anchorReg.getChrom()))
					break;
			}
		}
		// Now generate random negative regions
		for(int i=0; i<posPeaks.size(); i++){
			Point currRegionMidPoint = posPeaks.get(i).getMidpoint();
			Point JumpPointFront = currRegionMidPoint.expand(randJump).endPoint();
			Point JunPointBack = currRegionMidPoint.expand(randJump).startPoint();
			negRand.add(JumpPointFront.expand(win/2));
			negRand.add(JunPointBack.expand(win/2));
		}
		
		// now remove any neg regions that overlaps with a pos region
		// first hash pospeaks by chromosome
		HashMap<String,List<Region>> posByCHR = new HashMap<String,List<Region>>();
		for(Region p : posPeaks){
			if(posByCHR.containsKey(p.getChrom())){
				posByCHR.get(p.getChrom()).add(p);
			}else{
				posByCHR.put(p.getChrom(), new ArrayList<Region>());
				posByCHR.get(p.getChrom()).add(p);
			}
		}
		
		for(int i=0; i<negRand.size(); i++){
			if(posByCHR.containsKey(negRand.get(i).getChrom())){
				for(Region r : posByCHR.get(negRand.get(i).getChrom())){
					if(negRand.get(i).overlaps(r)){
						negRand.remove(i);
						i--;
						break;
					}
				}
			}
		}
		
		for(Region posr : posPeaks){
			System.out.println(posr.getLocationString()+"\t"+"B");
		}
		for(Region negr : negRand){
			System.out.println(negr.getLocationString()+"\t"+"U");
		}
		
	}
	
	
	public static void main(String[] args) throws IOException{
		GenomeConfig g = new GenomeConfig(args);
		NarrowPeakTrainData runner = new NarrowPeakTrainData(g);
		runner.setRandJump(Args.parseInteger(args, "randJump", 400));
		int w = Args.parseInteger(args, "win", 200);
		runner.setwin(w);
		runner.setRemDupDist(Args.parseInteger(args, "removeClosePeaksDistace", 50));
		
		// Get all positive files
		List<String> posRegFile = new ArrayList<String>();
		String fileNames = Args.parseString(args, "pos", "");
		BufferedReader br = new BufferedReader(new FileReader(fileNames));
		String line = null;
		while((line=br.readLine())!=null){
			posRegFile.add(line);
		}
		br.close();
		List<Region> posregs = new ArrayList<Region>();
		for(String fname : posRegFile){
			posregs.addAll(RegionFileUtilities.loadRegionsFromFile(fname, g.getGenome(), w));
		}
		runner.setPosPeaks(posregs);
		runner.execute();
		
	}
	
	
	
	
	
	

}
