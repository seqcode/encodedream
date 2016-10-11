package org.seqcode.projects.encodedream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.seqcode.deepseq.StrandedBaseCount;
import org.seqcode.deepseq.experiments.ControlledExperiment;
import org.seqcode.deepseq.experiments.ExperimentCondition;
import org.seqcode.deepseq.experiments.ExperimentManager;
import org.seqcode.deepseq.experiments.ExptConfig;
import org.seqcode.deepseq.experiments.Sample;
import org.seqcode.deepseq.stats.BackgroundCollection;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.sequence.SequenceGenerator;
import org.seqcode.genome.sequence.SequenceUtils;
import org.seqcode.genome.sequence.WildcardKmerUtils;
import org.seqcode.gsebricks.verbs.location.ChromosomeGenerator;
import org.seqcode.projects.encodedream.utils.DremeSandbox;

/**
 * Featurizes an input list of genomic regions to an accessibility aware wild-card 
 * 8-mer feature space.
 * 
 * @author akshaykakumanu
 */
public class CountFeatures {
	// Configs
	protected DreamConfig dcon;
	protected ExptConfig econ;
	protected GenomeConfig gcon;
	@SuppressWarnings("rawtypes")
	protected SequenceGenerator seqgen;
	
	// Experiments related stuff
	protected ExperimentManager manager;
	protected HashMap<ExperimentCondition,BackgroundCollection> bgmodels = new HashMap<ExperimentCondition,BackgroundCollection>();
	
	
	// Static final variables
	public static final int K =8; // k-mer size is fixed to 8;
	
	// Training/Testing data; allowed labels are "U", "B" and "?" ("?" for test data only)
	protected ConcurrentHashMap<Region,String[]> labeled_data = new ConcurrentHashMap<Region,String[]>(); // label index are same as the condition
	
	// Some feature properties
	protected boolean unbiased_8mers = false;
	protected ConcurrentHashMap<Integer,Integer> indexmap_8mers = new ConcurrentHashMap<Integer,Integer>();
	// At this point the curated list should contain both complements, its easy to deal that way.
	// I'm assuming the dreme-config does that.
	protected List<String> curated_8mers = new ArrayList<String>();
	protected HashSet<String> kmer_mapping_list = new HashSet<String>();
	protected int num_features;
	protected boolean acc_aware = false;
	protected boolean acc_aware_short = false;
	protected WildcardKmerUtils wcutils;
	
	// Output formatting params
	protected boolean print_featureIndices = false;
	protected String outfilename;
	protected String featureInd_outfilename;
	protected boolean print_regname = false;
	
	// Caution, needs thread safety 
	protected BufferedWriter bw;
	
	
	public CountFeatures(DreamConfig d) throws IOException {
		// Get all configs, and setup the expt manager and bg-models for conditions
		dcon = d;
		econ = d.getEcon();
		gcon = d.getGcon();
		manager = d.getManager();
		bgmodels.putAll(dcon.getbgmodels());
		seqgen = gcon.getSequenceGenerator();

		// Now fill feature types and other related params
		labeled_data.putAll(dcon.getLabeledData());
		unbiased_8mers = dcon.isUnbiasedKmerSet();
		acc_aware = dcon.isAccAware();
		acc_aware_short = acc_aware ? false : dcon.isAccAwareShort();
		
		//if not an unbiased list of 8-mers; 
		//map every curated k-mer index to the case specific index (i.e the input index)
		wcutils = dcon.getWCutils();
		if(!unbiased_8mers){
			curated_8mers.addAll(dcon.getCurated8mers());
			for(int i=0; i<curated_8mers.size(); i++){
				int currind = wcutils.seq2int(curated_8mers.get(i));
				int currRevind = wcutils.seq2int(SequenceUtils.reverseComplement(curated_8mers.get(i)));
				if(currind < currRevind){
					indexmap_8mers.put(currind, i);
					indexmap_8mers.put(currRevind, i);
				}
			}
		}
		kmer_mapping_list.addAll(dcon.getWCMappingList());
		
		// First, fix the number of features
		// Order is: plain, mountain, valley
		if(!unbiased_8mers){
			num_features = (acc_aware ? curated_8mers.size()*3 : (acc_aware_short ? curated_8mers.size()*2 : curated_8mers.size() ));
		}else{
			num_features = (acc_aware ? (int)Math.pow(5, 8)*3 : (acc_aware_short ? (int)Math.pow(5, 8)*2 : (int)Math.pow(5, 8) ));
		}

		// Now fill output formatting options
		
		outfilename = dcon.getTrainDataOutfilename();
		featureInd_outfilename = dcon.getFeatureIndOutFilename();
		bw = new BufferedWriter(new FileWriter(outfilename));
		print_featureIndices = dcon.printFeatureHeader();
		print_regname = dcon.printRegName();
	}
	
	
	/**
	 * Initiate different threads to print acc aware features. 
	 * @throws IOException 
	 */
	public void execute() throws IOException{
		//Split the jobs into the allowed number of threads
		@SuppressWarnings("unchecked")
		Iterator<Region> chromIter = new ChromosomeGenerator().execute(gcon.getGenome());
		//Threading divides analysis over entire chromosomes. This approach is not compatible with file caching. 
		int numThreads = econ.getCacheAllData() ? dcon.getNumThreads() : 1;

		Thread[] threads = new Thread[numThreads];
		List<Region> threadRegions[] = new ArrayList[numThreads];
		List<Region> threadChrs[] = new ArrayList[numThreads];
		
		// Store regions by chromosomes 
		HashMap<String,List<Region>> byCHR = new HashMap<String,List<Region>>();
		
		for(Region r : labeled_data.keySet()){
			if(byCHR.containsKey(r.getChrom())){
				byCHR.get(r.getChrom()).add(r);
			}else{
				byCHR.put(r.getChrom(), new ArrayList<Region>());
				byCHR.get(r.getChrom()).add(r);
			}
		}
		int i = 0;
		for (i = 0 ; i < threads.length; i++) {
			threadRegions[i] = new ArrayList<Region>();
			threadChrs[i] = new ArrayList<Region>();
		}
		
		while(chromIter.hasNext()){
			Region chr = chromIter.next();
			if(byCHR.containsKey(chr.getChrom())){
				threadChrs[(i) % numThreads].add(chr);
				threadRegions[(i++) % numThreads].addAll(byCHR.get(chr.getChrom()));
			}
		}
		
		// If, feature indices need to printed do it now
		if(print_featureIndices){
			StringBuilder sb = new StringBuilder();
			
			sb.append("Kmer Ind");sb.append("\t");sb.append("KmerName");sb.append("\n");
			int ind = 1;
			for(String s : curated_8mers){
				sb.append(ind);sb.append("\t");sb.append(s+"_p");sb.append("\n"); 
				ind++;
			}
			if(acc_aware_short || acc_aware){
				for(String s : curated_8mers){
					sb.append(ind);sb.append("\t");sb.append(s+"_m");sb.append("\n"); 
					ind++;
				}
			}
			if(acc_aware){
				for(String s : curated_8mers){
					sb.append(ind);sb.append("\t");sb.append(s+"_v");sb.append("\n"); 
					ind++;
				}
			}
			BufferedWriter ind_bw = new BufferedWriter(new FileWriter(featureInd_outfilename));
			ind_bw.write(sb.toString());
			ind_bw.close();
		}
		

		for (i = 0 ; i < threads.length; i++) {
			Thread t = new Thread(new CountFeaturesThread(threadRegions[i],threadChrs[i]));
			t.start();
			threads[i] = t;
		}

		boolean anyrunning = true;
		while (anyrunning) {
			anyrunning = false;
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) { }
			for (i = 0; i < threads.length; i++) {
				if (threads[i].isAlive()) {
					anyrunning = true;
					break;
				}
			}
		}
		
		bw.close();
		
	}




	public class CountFeaturesThread implements Runnable{
		/** The chrs and the regions are sorted*/
		protected List<Region> chrs = new ArrayList<Region>();
		protected List<Region> regs = new ArrayList<Region>();
		/** float[sample index][numbins][strand]*/
		protected float[][][] landscape=null;
		protected int shift;
		protected int hit3Extend;
		protected int hit5Extend;
		int global_enrichment_win = 1000;
		protected double global_thredh = 0.01;
		protected int binwidth;
		protected int binstep;
		// no.of. adjacent bins to look for to check if a 8-mer is in a valley? 
		protected int num_adjBins;

		// The chr index (chrs) of the current loaded landscape
		protected int currChunkChrId = 0;
		// The current start of the landscape chunk
		protected int x = 0;
		protected Region currChunk;
		protected HashMap<Sample,List<StrandedBaseCount>> curr_hitsPos;
		protected HashMap<Sample,List<StrandedBaseCount>> curr_hitsNeg; 
		protected int curr_totNumBins;
		protected Map<ExperimentCondition,float[]> curr_condition_counts;

		/**
		 * 
		 * @param posRegs : positive labeled regions
		 * @param negRegs
		 * @param cs
		 */
		public CountFeaturesThread(List<Region> ps, List<Region> cs) {
			regs.addAll(ps);
			chrs.addAll(cs);
			synchronized(Region.class){
				Collections.sort(regs); // sort the regions 
				Collections.sort(chrs); // sort the chrs
			}
			shift=dcon.getAccFeaturesTagShift();
			hit3Extend=dcon.getAccFeatures3primeExt();
			hit5Extend=dcon.getAccFeatures5primeExt();
			global_enrichment_win = dcon.getAccFeaturesGlobalWin();
			global_thredh = dcon.getAccFeaturesGlobalThresh();
			binwidth = dcon.getAccFeaturesBinwidth();
			binstep = dcon.getAccFeaturesBinstep();
			num_adjBins = dcon.getAccFeaturesNumwins();
			
		}
		
		@Override
		public void run() {

			// First, update the landscape
			updateLandscape();

			for(Region currP : regs){
				Map<ExperimentCondition,int[]> features = new HashMap<ExperimentCondition,int[]>();
				for(ExperimentCondition ec : manager.getConditions()){
					features.put(ec, new int[num_features]);
				}

				while(!currP.getChrom().equals(currChunk.getChrom()) || currP.getEnd() > currChunk.getEnd()){
					updateLandscape();
					if(landscape == null)
						return;
				}

				// Now get the sequence!!
				@SuppressWarnings("unchecked")
				String seq = "";
				synchronized(seqgen){
					seq = seqgen.execute(currP).toUpperCase();
				}

				for (int i = 0; i < (seq.length() - K + 1); i++) {
					String currK = seq.substring(i, i + K);
					if(currK.contains("N"))
						continue;
					if(!kmer_mapping_list.contains(currK))
						continue;

					// Is this k-mer in a DNase enriched region
					HashMap<ExperimentCondition,Boolean> enriched = new HashMap<ExperimentCondition,Boolean>();
					for(ExperimentCondition ec : manager.getConditions()){
						enriched.put(ec, false);
					}
					int adjWinStartBin = inBounds((currP.getStart()+i-(dcon.getAccFeaturesGlobalWin()/2)-currChunk.getStart())/binstep, 0, curr_totNumBins);
					int adjWinEndBin = inBounds((currP.getStart()+i+(dcon.getAccFeaturesGlobalWin()/2)-currChunk.getStart())/binstep, 0, curr_totNumBins);


					for(ExperimentCondition ec : manager.getConditions()){
						float totAdjCount = 0;
						float bg_expected_counts = 0;
						for(int b=adjWinStartBin; b<=adjWinEndBin;b++){
							totAdjCount += curr_condition_counts.get(ec)[b];
							if(dcon.usePoissonBgMod()){
								synchronized(bgmodels){
									bg_expected_counts += bgmodels.get(ec).getGenomicExpectedCount();
								}
							}
						}

						if(!dcon.usePoissonBgMod()){
							bg_expected_counts = (float)(dcon.getAccFeaturesGlobalWin()*ec.getTotalSignalCount()/econ.getMappableGenomeLength());
						}

						double pval = 0.0;
						synchronized(DremeSandbox.class){
							pval = DremeSandbox.binomialPValue(bg_expected_counts, (totAdjCount+bg_expected_counts), dcon.getMinSigCtrlFoldDifference());
						}

						if(pval < dcon.getAccFeaturesGlobalThresh()){
							enriched.put(ec, true);
						}
					}

					for(String wcMap : wcutils.map(currK)){
						String wcMapRev = "";
						synchronized(SequenceUtils.class){
							wcMapRev = SequenceUtils.reverseComplement(wcMap);
						} 
						int wcMapInd =0;
						int wcMapRevInd =0;
						synchronized(wcutils){
							wcMapInd = wcutils.seq2int(wcMap);
							wcMapRevInd = wcutils.seq2int(wcMapRev);
						}

						if(!(indexmap_8mers.containsKey(wcMapInd) || indexmap_8mers.containsKey(wcMapRevInd)))
							continue;
						if(wcMapInd < wcMapRevInd){
							for(ExperimentCondition ec : manager.getConditions()){ // over all conds
								if(enriched.get(ec)){
									if(!unbiased_8mers){
										features.get(ec)[curated_8mers.size()*1+indexmap_8mers.get(wcMapInd)]++;
									}else{
										features.get(ec)[(int)Math.pow(5, 8)*1+wcMapInd]++;
									}
								}else{
									if(!unbiased_8mers){
										features.get(ec)[indexmap_8mers.get(wcMapInd)]++;
									}else{
										features.get(ec)[wcMapInd]++;
									}
								}
							}
						}	

						
					} // Over all cell-lines

				} // if this 8-mer needs to be added



				for(ExperimentCondition ec : manager.getConditions()){
					if(IntStream.of(features.get(ec)).sum() > 0){
						StringBuilder sb = new StringBuilder();
						if(print_regname){
							sb.append(currP.getLocationString());sb.append("_");sb.append(ec.getName());sb.append("\t");
						}
						// Replace "B" with 1's, remove "A", and replace "U" and "?" with 0's
						if(labeled_data.get(currP)[ec.getIndex()].equals("B")){
							sb.append(1);sb.append(" ");
						}else if(labeled_data.get(currP)[ec.getIndex()].equals("A")){
							continue;
						}else if(labeled_data.get(currP)[ec.getIndex()].equals("U")){
							sb.append(0);sb.append(" ");
						}else if(labeled_data.get(currP)[ec.getIndex()].equals("?")){
							sb.append(0);sb.append(" ");
						}
						for(int i=0; i<features.get(ec).length; i++){
							if(features.get(ec)[i]>0){
								sb.append(i+1);sb.append(":");sb.append(features.get(ec)[i]);sb.append(" ");
							}
						}
						sb.deleteCharAt(sb.length()-1);
						sb.append("\n");
						synchronized(bw){
							try {
								bw.write(sb.toString());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							};
						}
					}
				}	

			}
		}

		protected void updateLandscape(){
			if(!(x < chrs.get(currChunkChrId).getEnd())){
				if(++currChunkChrId < chrs.size()){
					x=0;
				}else{
					landscape =null;
					return;
				}
			}

			int y = (int) (x+dcon.ACC_FEATURES_MAXSECTION);
			if(y>chrs.get(currChunkChrId).getEnd()){y=chrs.get(currChunkChrId).getEnd();}
			currChunk = new Region(gcon.getGenome(), chrs.get(currChunkChrId).getChrom(), x, y);
			curr_hitsPos = new HashMap<Sample,List<StrandedBaseCount>>();
			curr_hitsNeg = new HashMap<Sample,List<StrandedBaseCount>>();

			// fill hits
			//Initialize & sort the read lists per Sample
			for(Sample samp : manager.getSamples()){
				synchronized(manager){//hitCache requires thread safety
					List<StrandedBaseCount> sampHitsP = samp.getStrandedBases(currChunk, '+'); 
					List<StrandedBaseCount> sampHitsN = samp.getStrandedBases(currChunk, '-');
					Collections.sort(sampHitsP); Collections.sort(sampHitsN); //This might be pointless - the hits should be sorted in the cache already
					curr_hitsPos.put(samp, sampHitsP);
					curr_hitsNeg.put(samp, sampHitsN);
				}
			}
			curr_totNumBins = (int)(currChunk.getWidth()/binstep);
			// Make the landscape
			makeHitLandscape(curr_hitsPos,curr_hitsNeg,currChunk, dcon.getAccFeaturesBinwidth(), dcon.getAccFeaturesBinstep()); 

			curr_condition_counts = new HashMap<ExperimentCondition,float[]>();
			for(ExperimentCondition ec : manager.getConditions()){
				curr_condition_counts.put(ec, this.getConditionCounts(ec, landscape, '.', true));
			}

			x = y;

		}



		/**
		 * This code chunk is similar to what "seed" does
		 * Makes integer arrays corresponding to the read landscape over the current region.
		 * Tags are semi-extended by half the binWidth to account for the step, 
		 * 	  and may also be shifted or extended here, depending on the event detection strategy
		 * No needlefiltering here as that is taken care of during tag loading (i.e. in Sample)
		 * 
		 * @param hits  : Lists of StrandedBaseCounts, indexed by Sample (sorted within each sample)
		 * @param currReg
		 * @param binWidth
		 * @param binStep
		 */
		protected void makeHitLandscape(Map<Sample, List<StrandedBaseCount>> hitsPos, Map<Sample, List<StrandedBaseCount>> hitsNeg, Region currReg, int binWidth, int binStep){
			int numBins = (int)(currReg.getWidth()/binStep);
			landscape = new float[hitsPos.size()][numBins+1][2];
			int halfWidth = binWidth/2;

			for(Sample samp : manager.getSamples()){
				for(int i=0; i<=numBins; i++)
					for(int s=0; s<=1; s++)
						landscape[samp.getIndex()][i][s]=0;

				for(int strand=0; strand<=1; strand++){
					List<StrandedBaseCount> currHits = strand==0 ? hitsPos.get(samp) : hitsNeg.get(samp);

					for(StrandedBaseCount h : currHits){

						//landscape array
						int left = getLeft(h);
						int right = getRight(h);
						if(left <= currReg.getEnd() && right>=currReg.getStart()){
							int offsetL=inBounds(left-currReg.getStart(),0,currReg.getWidth());
							int offsetR=inBounds(right-currReg.getStart(),0,currReg.getWidth());

							int binstart = inBounds(((offsetL-halfWidth)/binStep), 0, numBins);
							int binend = inBounds(((offsetR/binStep)), 0, numBins);
							for(int b=binstart; b<=binend; b++)
								landscape[samp.getIndex()][b][strand]+=h.getCount();
						}
					}
				}
			}
		}

		protected final int inBounds(int x, int min, int max){
			if(x<min){return min;}
			if(x>max){return max;}
			return x;
		}
		protected final int getShifted5Prime(StrandedBaseCount h){
			return(h.getStrand()=='+' ? 
					h.getCoordinate()+shift : 
						h.getCoordinate()-shift);
		}
		protected final int getLeft(StrandedBaseCount h){
			return(h.getStrand()=='+' ? 
					h.getCoordinate()+shift-hit5Extend : 
						h.getCoordinate()-shift-hit3Extend);
		}
		protected final int getRight(StrandedBaseCount h){
			return(h.getStrand()=='+' ? 
					h.getCoordinate()+shift+hit3Extend : 
						h.getCoordinate()-shift+hit5Extend);
		}

		/**
		 * Parses the landscape arrays to get a per-condition count array 
		 * 
		 * @param cond : ExperimentCondition of interest
		 * @param data : data structure to parse. Should be landscape. Assumes indexed by Sample, base, strand
		 * @param strand : +/-/.
		 * @param signal : true to count condition's signal samples, false to count condition's control samples. 
		 * @return
		 */
		protected float[]  getConditionCounts(ExperimentCondition cond, float[][][] data, char strand, boolean signal){
			int clength = data[0].length;
			float[] counts = new float[clength];
			for(int c=0; c<clength; c++){
				float currCount=0;
				List<Sample> currSamples = signal ? cond.getSignalSamples() : cond.getControlSamples();
				for(Sample samp : currSamples){
					if(strand=='.' || strand=='+')
						currCount+=data[samp.getIndex()][c][0];
					if(strand=='.' || strand=='-')
						currCount+=data[samp.getIndex()][c][1];
				}
				counts[c]=currCount;
			}
			return counts;
		}




	}


}
