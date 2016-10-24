package org.seqcode.projects.encodedream;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.seqcode.data.io.RegionFileUtilities;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.sequence.SequenceGenerator;
import org.seqcode.genome.sequence.SequenceUtils;
import org.seqcode.genome.sequence.WildcardKmerUtils;
import org.seqcode.gsebricks.verbs.location.PointParser;
import org.seqcode.gsebricks.verbs.location.RegionParser;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class GenerateWCKmerLIBSVM {
	
	protected int K = 8;
	protected GenomeConfig gcon;
	protected SequenceGenerator<Region> seqgen;
	protected String reg_bed_file;
	protected WildcardKmerUtils wcutils;
	protected RegionParser rparser;
	protected PointParser pparser;
	protected List<String> labels = new ArrayList<String>();
	protected Map<Integer,Integer> exclude_inds = new HashMap<Integer,Integer>();
	protected int win=200;
	
	// Settors
	public void setK(int k) throws IOException{K = k;wcutils = new WildcardKmerUtils(K);}
	public void setWin(int w){win=w;}
	public void setRegsFileName(String s){reg_bed_file =s;}
	public void setLabels(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line=br.readLine())!=null){
			labels.add(line.split("\t")[1]);
		}
		br.close();
	}
	public void setExcludeInds(String fname) throws IOException{ // assumes a file name with wc/norma kmers given
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine())!=null){
			if(line.contains("8mer"))
				continue;
			String[] pieces = line.split("\t");
			exclude_inds.put(wcutils.seq2int(pieces[0]), 1);
			exclude_inds.put(wcutils.seq2int(SequenceUtils.reverseComplement(pieces[0])), 1);
		}

		br.close();
	}
	
	
	@SuppressWarnings("unchecked")
	public GenerateWCKmerLIBSVM(GenomeConfig g) {
		gcon = g;
		seqgen = gcon.getSequenceGenerator();
		rparser = new RegionParser(gcon.getGenome());
		pparser = new PointParser(gcon.getGenome());
	}
	
	public void execute() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(reg_bed_file));
		String line = null;
		int numK = (int)Math.pow(5, K);
		int ind=0;
		while((line = br.readLine()) != null){
			int[] kmerCounts = new int[numK];
			String[] pieces = line.split("\t");
			Region currReg = null;
			if(pieces[0].contains("-")){
				currReg = rparser.execute(pieces[0]);
			}else{
				currReg = pparser.execute(pieces[0]).expand(win/2);
			}
			String seq = seqgen.execute(currReg).toUpperCase();
			
			for (int i = 0; i < (seq.length() - K + 1); i++) {
				String currK = seq.substring(i, i + K);
				if(currK.contains("N"))
					continue;
				
				for(String wcMap : wcutils.map(currK)){
					String wcMapRev = SequenceUtils.reverseComplement(wcMap);
					int wcMapInd = wcutils.seq2int(wcMap);
					int wcMapRevInd = wcutils.seq2int(wcMapRev);
					if(wcMapInd < wcMapRevInd)
						kmerCounts[wcMapInd]++;
				}
			} 
			
			StringBuilder sb = new StringBuilder();
			if(labels.get(ind).equals("B")){
				sb.append(1);sb.append(" ");
			}else{
				sb.append(0);sb.append(" ");
			}
			for(int i=0; i<kmerCounts.length; i++){
				if(kmerCounts[i]>0 && !exclude_inds.containsKey(i) ){
					sb.append(i+1);sb.append(":");sb.append(kmerCounts[i]);sb.append(" ");
				}
			}
			sb.deleteCharAt(sb.length()-1);
			System.out.println(sb.toString());
			ind++;
		}
		br.close();
	}
	
	public static void main(String[] args) throws IOException{
		ArgParser ap = new ArgParser(args);
		GenomeConfig g = new GenomeConfig(args);
		GenerateWCKmerLIBSVM runner = new GenerateWCKmerLIBSVM(g);
		runner.setK(Args.parseInteger(args, "K", 8));
		runner.setWin(Args.parseInteger(args, "win", 200));
		
		runner.setLabels(ap.getKeyValue("trainreg"));
		runner.setRegsFileName(ap.getKeyValue("trainreg"));
		
		if(ap.hasKey("exclude")){
			runner.setExcludeInds(ap.getKeyValue("exclude"));
		}
		
		runner.execute();
	}

}
