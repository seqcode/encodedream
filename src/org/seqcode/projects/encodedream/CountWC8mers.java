package org.seqcode.projects.encodedream;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;
import org.seqcode.projects.encodedream.CountFeatures.CountFeaturesThread;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.sequence.SequenceGenerator;
import org.seqcode.genome.sequence.SequenceUtils;
import org.seqcode.genome.sequence.WildcardKmerUtils;
import org.seqcode.gsebricks.verbs.location.RegionParser;

public class CountWC8mers {
	
	protected GenomeConfig gcon;
	@SuppressWarnings("rawtypes")
	protected SequenceGenerator seqgen;
	protected WildcardKmerUtils wcutilsGlob;
	

	// Static final variables
	public static final int K =8; // k-mer size is fixed to 8;
	
	// Feature related params
	protected boolean biased = false;
	protected boolean exclude = false;
	// might not have reverse complements. 
	protected List<String> curated_8mers = new ArrayList<String>();
	protected HashMap<Integer,Integer> indexmap_8mers = new HashMap<Integer,Integer>();
	// List of wc 8-mers to exclude. 
	protected List<String> exclude_list = new ArrayList<String>();

	
	// Regions to scan
	protected boolean trainSubset = false;
	protected List<Region> regs = new ArrayList<Region>();
	protected List<String> labs = new ArrayList<String>();
	protected HashMap<String,Integer> testSetIndex = new HashMap<String,Integer>();
	
	// misc params
	protected int numThreads = 5;
	protected BufferedWriter bw_libsvm;
	protected String featureIndFname;
	
	
	// Setters
	public void setTestSetInd(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line=null;
		while((line=br.readLine())!=null){
			String[] pieces = line.split("\t");
			testSetIndex.put(pieces[0], Integer.parseInt(pieces[1]));
		}
		br.close();
	}
	public void setOuputWriter(String fname) throws IOException{
		bw_libsvm = new BufferedWriter(new FileWriter(fname));
	}
	public void setisSubset(boolean b){trainSubset = b;}
	public void setFeatureIndFname(String f){featureIndFname=f;}
	public void setRegs(List<Region> rs){regs.addAll(rs);}
	public void setLabs(List<String> ls){labs.addAll(ls);}
	public void setBiased(boolean b){biased = b;}
	public void setExclude(boolean b){exclude = b;}
	public void setCurated8mers(String fname) throws IOException{
		Set<String> tmp = new HashSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine())!=null){
			if(line.contains("8mer"))
				continue;
			String[] pieces = line.split("\t");
			tmp.add(pieces[0]);
			tmp.add(SequenceUtils.reverseComplement(pieces[0]));
		}
		curated_8mers.addAll(tmp);
		br.close();
		wcutilsGlob = new WildcardKmerUtils(K);
		
		// make an index map
		for(int i=0; i<curated_8mers.size(); i++){
			int currind = wcutilsGlob.seq2int(curated_8mers.get(i));
			int currRevind = wcutilsGlob.seq2int(SequenceUtils.reverseComplement(curated_8mers.get(i)));
			if(currind < currRevind){
				indexmap_8mers.put(currind, i);
				indexmap_8mers.put(currRevind, i);
			}
		}
		wcutilsGlob = null;
		
	}
	public void setNumThreads(int n){numThreads = n;}
	
	public void setExcludeSet(String fname) throws IOException{
		Set<String> tmp = new HashSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine())!=null){
			if(line.contains("8mer"))
				continue;
			String[] pieces = line.split("\t");
			tmp.add(pieces[0]);
			tmp.add(SequenceUtils.reverseComplement(pieces[0]));
		}
		exclude_list.addAll(tmp);
		br.close();
	}
	
	public CountWC8mers(GenomeConfig g) throws IOException {
		gcon = g;
		seqgen = g.getSequenceGenerator();
	}
	
	public void execute() throws IOException{
		Thread[] threads = new Thread[numThreads];
		List<String> threadRegions[] = new ArrayList[numThreads];
		List<String> threadLabs[] = new ArrayList[numThreads];
		for (int i = 0 ; i < threads.length; i++) {
			threadRegions[i] = new ArrayList<String>();
			threadLabs[i] = new ArrayList<String>();
		}
		
		// write feature indicies and names to file
		if(trainSubset){
			BufferedWriter bw_ind = new BufferedWriter(new FileWriter(featureIndFname)); 
			StringBuilder sb_ind = new StringBuilder();
			for(int j=0; j< curated_8mers.size(); j++){
				sb_ind.append(j+1);sb_ind.append("\t");sb_ind.append(curated_8mers.get(j));sb_ind.append("\n");
			}
			bw_ind.write(sb_ind.toString());
			bw_ind.close();
			
			int ind= 0;
			while(ind<regs.size()){
				Region currreg = regs.get(ind);
				String lab = labs.get(ind);
				threadRegions[(ind) % numThreads].add(currreg.getLocationString());
				threadLabs[(ind++) % numThreads].add(lab);
			}
		}else{
			List<String> locs = new ArrayList<String>();
			locs.addAll(testSetIndex.keySet());
			int ind =0;
			while(ind<locs.size()){
				String currregString = locs.get(ind);
				String lab = "?";
				threadRegions[(ind) % numThreads].add(currregString);
				threadLabs[(ind++) % numThreads].add(lab);
			}	
		}
		
		for (int i = 0 ; i < threads.length; i++) {
			Thread t = new Thread(new CountWC8mersThread(threadRegions[i],threadLabs[i]));
			t.start();
			threads[i] = t;
		}
		
		boolean anyrunning = true;
		while (anyrunning) {
			anyrunning = false;
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) { }
			for (int i = 0; i < threads.length; i++) {
				if (threads[i].isAlive()) {
					anyrunning = true;
					break;
				}
			}
		}
		synchronized(bw_libsvm){
			bw_libsvm.close();
		}
		
	}
	
	
	public class CountWC8mersThread implements Runnable{

		protected List<String> regStrings = new ArrayList<String>();
		protected List<String> labStrings = new ArrayList<String>();
		protected RegionParser regParser = new RegionParser(gcon.getGenome());
		protected WildcardKmerUtils wcutils;
		
		
		public CountWC8mersThread(List<String> rs, List<String> ls) throws IOException {
			regStrings.addAll(rs);
			labStrings.addAll(ls);
			wcutils = new WildcardKmerUtils(K);
			
		}
		
		public String reverseComplement(String str) { 
			StringBuilder sb = new StringBuilder();
			for(int i = str.length()-1; i>= 0; i--) { 
				sb.append(SequenceUtils.complementChar(str.charAt(i)));
			}
			return sb.toString();
		}

		@Override
		public void run() {
			//BufferedWriter bw_libsvm = new BufferedWriter(new FileWriter(libsvmFname));
			int ind = 0;
			for(String currRegString : regStrings){
				// Get reg
				Region currReg = regParser.execute(currRegString);
				// Get seq
				
				String seq = "";
				synchronized(seqgen){
					seq = seqgen.execute(currReg).toUpperCase();
				}

				// Make feature vector
				int[] features = (biased ? new int[curated_8mers.size()] : new int[(int)Math.pow(5, 8)]);

				for (int i = 0; i < (seq.length() - K + 1); i++) {
					String currK = seq.substring(i, i + K);
					if(currK.contains("N"))
						continue;
					if(exclude && exclude_list.contains(currK))
						continue;

					for(String wcMap : wcutils.map(currK)){
						String wcMapRev = reverseComplement(wcMap);
						if(exclude && exclude_list.contains(wcMap)) // if have to exclude 
							continue;
						if(biased && !curated_8mers.contains(wcMap)) // if not in the curated k-mer
							continue;

						int wcMapInd = wcutils.seq2int(wcMap); 
						int wcMapRevInd = wcutils.seq2int(wcMapRev);

						if(wcMapInd < wcMapRevInd){
							if(biased){
								features[indexmap_8mers.get(wcMapInd)]++;
							}else{
								features[wcMapInd]++;
							}
						}

					}

				}
				
				StringBuilder sb = new StringBuilder();
				sb.append(currReg.getLocationString());sb.append("\t");

				// Replace "B" with 1's, remove "A", and replace "U" and "?" with 0's
				if(labStrings.get(ind).equals("B")){
					sb.append(1);sb.append(" ");
				}else if(labStrings.get(ind).equals("U")){
					sb.append(0);sb.append(" ");
				}else if(labStrings.get(ind).equals("?")){
					sb.append(0);sb.append(" ");
				}

				for(int i=0; i<features.length; i++){
					if(features[i]>0){
						sb.append(i+1);sb.append(":");sb.append(features[i]);sb.append(" ");
					}
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append("\n");
				synchronized(bw_libsvm){
					try {
						bw_libsvm.write(sb.toString());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				ind++;
			} // end of loop over threaded regions
		}// end of thread's run

	} // end of class


	public static void main(String[] args) throws IOException{

		ArgParser ap = new ArgParser(args);
		GenomeConfig g = new GenomeConfig(args);
		CountWC8mers counter = new CountWC8mers(g);
		String dataFname = Args.parseString(args, "data", "");
		
		boolean subset = ap.hasKey("subset");
		counter.setisSubset(subset);
		if(subset){
			List<Region> rs =new ArrayList<Region>();
			List<String> ls = new ArrayList<String>();
		
			BufferedReader br = new BufferedReader(new FileReader(dataFname));
			String line = null;
			while((line=br.readLine())!=null){
				String[] pieces = line.split("\t");
				RegionParser rpaser = new RegionParser(g.getGenome());
				rs.add(rpaser.execute(pieces[0]));
				ls.add(pieces[1]);
			}
			br.close();
			counter.setRegs(rs);
			counter.setLabs(ls);
		}else{ // load the testSet index
			counter.setTestSetInd(ap.getKeyValue("testSetIndex"));
		}
		
		
		
		boolean bias = ap.hasKey("biased");
		counter.setBiased(bias);
		boolean excl = ap.hasKey("exclude");
		counter.setExclude(excl);
		if(bias){
			String curFname = ap.getKeyValue("biased");
			counter.setCurated8mers(curFname);
		}
		if(excl){
			String exclFname = ap.getKeyValue("exclude");
			counter.setExcludeSet(exclFname);
		}
		
		String fInd_out = ap.getKeyValue("featureIndOut");
		String libsvm_out = ap.getKeyValue("libsvmOut");
		counter.setFeatureIndFname(fInd_out);
		counter.setOuputWriter(libsvm_out);
		counter.setNumThreads(Args.parseInteger(args, "numThreads", 5));
		
		counter.execute();
		
	}

}
