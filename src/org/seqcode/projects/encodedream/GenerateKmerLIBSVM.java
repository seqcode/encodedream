package org.seqcode.projects.encodedream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import org.seqcode.data.io.RegionFileUtilities;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.sequence.SequenceGenerator;
import org.seqcode.genome.sequence.SequenceUtils;
import org.seqcode.gsebricks.verbs.location.PointParser;
import org.seqcode.gsebricks.verbs.location.RegionParser;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class GenerateKmerLIBSVM {

	protected int minK = 4;
	protected int maxK = 8;
	protected int win = 200;
	protected GenomeConfig gcon;
	protected RegionParser rpasrser;
	protected PointParser pparser;
	protected SequenceGenerator<Region> seqgen;
	protected String reg_bed_file;
	protected List<String> labels = new ArrayList<String>();
	
	protected HashMap<Integer,String> exclude = new HashMap<Integer,String>(); // a list of k-mers indices to ignore
	
	// Settors
	public void setMinK(int k){minK = k;}
	public void setMaxK(int k){maxK = k;}
	public void setWin(int w){win = w;}
	public void setRegsFileName(String s){reg_bed_file =s;}
	public void setLabels(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line=br.readLine())!=null){
			labels.add(line.split("\t")[1]);
		}
		br.close();
	}
	public void setExclude(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line=null;
		HashSet<String> tmp = new HashSet<String>();
		while((line=br.readLine())!=null){
			if(line.contains("#") || line.contains("8mer"))
				continue;
			String[] pieces = line.split("\t");
			tmp.add(pieces[0]);
			tmp.add(SequenceUtils.reverseComplement(pieces[0]));
		}
		int numK = 0;
		for (int k = minK; k <= maxK; k++) {
			numK = numK + (int) Math.pow(4, k);
		}
		for(int i=0; i<numK; i++){
			String currKmer = getKmerName(i);
			boolean substring = false;
			for(String es : tmp){
				if(es.contains(currKmer)){
					substring = true;
					break;
				}
			}
			if(substring)
				exclude.put(i, currKmer);
		}
		br.close();
	}
	
	
	@SuppressWarnings("unchecked")
	public GenerateKmerLIBSVM(GenomeConfig g) {
		gcon = g;
		seqgen = gcon.getSequenceGenerator();
		rpasrser = new RegionParser(g.getGenome());
		pparser = new PointParser(g.getGenome());
	}
	
	public int getKmerBaseInd(int s){
		int baseInd = 0;
		for(int k=minK; k<s; k++){
			baseInd += (int)Math.pow(4, k);
		}
		return baseInd;
	}
	
	public String getKmerName(int ind){
		int currKmerLen = 0;
		ArrayList<Integer> baseinds = new ArrayList<Integer>();
		for(int k=minK; k <= maxK; k++){
			baseinds.add(getKmerBaseInd(k));
		}
		int search = Collections.binarySearch(baseinds, ind);

		currKmerLen = search >= 0 ? minK + search : -1*(search + 1) - 1 + minK;
		String kmerName = RegionFileUtilities.int2seq(ind- getKmerBaseInd(currKmerLen), currKmerLen);

		return kmerName;
	}
	
	public void execute() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(reg_bed_file));
		String line = null;
		int numK = 0;
		for (int k = minK; k <= maxK; k++) {
			numK = numK + (int) Math.pow(4, k);
		}
		int loc=0;
		while((line = br.readLine()) != null){
			int[] kmerCounts = new int[numK];
			String[] pieces = line.split("\t");
			Region currReg = null;
			if(pieces[0].contains("-")){
				currReg = rpasrser.execute(pieces[0]).getMidpoint().expand(win/2);
			}else{
				currReg = pparser.execute(pieces[0]).expand(win/2);
			}
			String seq = seqgen.execute(currReg).toUpperCase();
			int ind = 0;
			for (int k = minK; k <= maxK; k++) {
				for (int i = 0; i < (seq.length() - k + 1); i++) {
					String currK = seq.substring(i, i + k);
					if(currK.contains("N"))
						continue;
					String revCurrK = SequenceUtils.reverseComplement(currK);
					int currKInt = RegionFileUtilities.seq2int(currK);
					int revCurrKInt = RegionFileUtilities.seq2int(revCurrK);
					int kmer = currKInt < revCurrKInt ? currKInt : revCurrKInt;
					kmerCounts[ind + kmer]++;
				}
				ind = ind + (int) Math.pow(4, k);
			} 
			
			StringBuilder sb = new StringBuilder();
			if(labels.get(loc).equals("B")){
				sb.append(1);sb.append(" ");
			}else{
				sb.append(0);sb.append(" ");
			}
			
			for(int i=0; i<kmerCounts.length; i++){				
				if(exclude.containsKey(i)){
					kmerCounts[i] = 0;
				}
				if(kmerCounts[i]>0){
					sb.append(i+1);sb.append(":");sb.append(kmerCounts[i]);sb.append(" ");
				}
			}
			sb.deleteCharAt(sb.length()-1);
			System.out.println(sb.toString());
			loc++;
			
		}
		br.close();
	}
	
	public static void main(String[] args) throws IOException{
		ArgParser ap = new ArgParser(args);
		GenomeConfig g = new GenomeConfig(args);
		GenerateKmerLIBSVM runner = new GenerateKmerLIBSVM(g);
		runner.setMinK(Args.parseInteger(args, "minK", 4));
		runner.setMaxK(Args.parseInteger(args, "maxK", 8));
		runner.setWin(Args.parseInteger(args, "win", 200));
		runner.setRegsFileName(ap.getKeyValue("trainbed"));
		runner.setLabels(ap.getKeyValue("trainbed"));
		runner.setExclude(ap.getKeyValue("exclude"));
		runner.execute();
	}
	
}
