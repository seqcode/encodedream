package org.seqcode.projects.encodedream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.seqcode.data.io.RegionFileUtilities;
import org.seqcode.deepseq.experiments.ExperimentCondition;
import org.seqcode.deepseq.experiments.ExperimentManager;
import org.seqcode.deepseq.experiments.ExptConfig;
import org.seqcode.deepseq.stats.BackgroundCollection;
import org.seqcode.deepseq.stats.PoissonBackgroundModel;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.sequence.SequenceUtils;
import org.seqcode.genome.sequence.WildcardKmerUtils;
import org.seqcode.gsebricks.verbs.location.RegionParser;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class DreamConfig {
	
	/////////
	//Configs
	/////////
	protected GenomeConfig gcon;
	protected ExptConfig econ; 
	protected ExperimentManager manager;
	// Poisson genome-wide back-ground models at condition level
	protected HashMap<ExperimentCondition,BackgroundCollection> conditionBackgrounds = new HashMap<ExperimentCondition,BackgroundCollection>();
	protected String tf_name;
	// Indexed based on the design file
	protected String[] celllines;
	// WC utils
	protected WildcardKmerUtils wcutils;
	
	/////////
	//Different kinds of training regions and related params
	////////
	protected HashMap<String,List<Region>> dnase_domains_pos = new HashMap<String,List<Region>>();
	protected HashMap<String,List<Region>> dnase_adj_neg = new HashMap<String,List<Region>>();
	// train/test data, the labels are indexed based on the ExperimentConditions
	protected HashMap<Region,String[]> labeled_data = new HashMap<Region,String[]>();
	/** The training window around "B" labels; fixed to 200 challenge
	 but, might have to look a broader window, 1000bp?  */
	protected int tf_win = 800;
	protected int dnase_win = 300;

	////////
	// Kmer accessibility aware params
	////////
	protected int acc_features_binwidth = 20;
	protected int acc_features_binstep = 20;
	protected int acc_features_numWins = 5;
	protected int acc_features_global_win = 1000;
	protected double acc_features_globenrich_thresh = 0.01;
	protected int acc_features_tagShift=0;			//Shift tags this length in the 3' direction
	protected int acc_features_tag3PrimeExtension=0; //Extend tags this number of bp in the 3' direction (starting from 5' position)
	protected int acc_features_tag5PrimeExtension=0; //Extend tags this number of bp in the 5' direction (starting from 5' position)
	public final int ACC_FEATURES_MAXSECTION = 50000000; //Size of genomic sections that each thread analyzes
	protected double acc_features_adjEnrichment_thresh = 0.01;
	protected double minSigCtrlFoldDifference = 1; //minimum signal/control difference to use in Binomial testing (per bin & per event)
	protected double minAdjSigFoldDifference = 1.5; 
	protected double perBinPoissonLogPThres=-7; //Log base 10 confidence threshold for use with Poisson background models when scoring bins
	protected int acc_aware_short_kmerAroundWin = 500;
	protected boolean usePoissonBgModel = false;

	/////////
	//biased or unbiased k-mers related params
	/////////
	protected List<String> curated_8mers = new ArrayList<String>();
	protected boolean unbiased_kmers = false; // are we dealing with a biased k-mer list
	protected boolean acc_aware =  false;
	protected boolean acc_aware_short = false;
	protected HashSet<String> curated_8mers_mapping_set = new HashSet<String>();
	
	
	////////
	// Output formatting options
	////////
	protected boolean print_featureIndices = false; //
	protected boolean print_regName = false;
	protected String featureInd_outfilename;
	protected String traindata_outfilename;

	
	////////
	//Actions
	///////
	protected boolean generateDnaseDomainsFeatures = false;
	protected boolean gerenrateBindingLabelFeatures = false;
	
	////////
	//Misc params
	protected int numThreads = 1;
	
	/////////
	//Classifier options
	////////
	protected String trainDataLibSVMFile;
	// no. of partitions needed for the javaRDD object
	protected int RDD_partitions = 10;
	protected double trainFrac = 0.7;
	protected int L1_maxItrs = 100;
	protected double L1_regularization = 0.1;
	protected String classifierOutName;
	protected String classifierPerfOutname;
	protected int RF_numTrees=100;
	protected int RF_treeDepth = 5;
	protected boolean training = false;
	protected String trained_L1_model;
	protected String trained_RF_models_dir;
	protected String trained_MLP_model;
	protected String model_scores_outfile;
	protected String model_weights_outfile;
	protected String featureIndexFile;
	protected boolean scanSetHasLabs=false;
	
	
	
	// Settors

	// gettors
	public int getAccFeaturesBinwidth(){return acc_features_binwidth;}
	public int getAccFeaturesBinstep(){return acc_features_binstep;}
	public int getAccFeaturesNumwins(){return acc_features_numWins;}
	public int getAccFeaturesTagShift(){return acc_features_tagShift;}
	public int getAccFeatures3primeExt(){return acc_features_tagShift;}
	public int getAccFeatures5primeExt(){return acc_features_tag5PrimeExtension;}
	public int getAccFeaturesGlobalWin(){return acc_features_global_win;}
	public double getAccFeaturesGlobalThresh(){return acc_features_globenrich_thresh;}
	public double getAccFeaturesAdjEnrichThresh(){return acc_features_adjEnrichment_thresh;}
	public HashMap<ExperimentCondition,BackgroundCollection> getbgmodels(){return conditionBackgrounds;}
	public double getMinSigCtrlFoldDifference(){return minSigCtrlFoldDifference;}
	public double getMinAdjSigCtrlFoldDifference(){return minAdjSigFoldDifference;}
	public GenomeConfig getGcon(){return gcon;}
	public ExptConfig getEcon(){return econ;}
	public boolean isUnbiasedKmerSet(){return unbiased_kmers;}
	public boolean isAccAware(){return acc_aware;}
	public boolean isAccAwareShort(){return acc_aware_short;}
	public boolean printFeatureHeader(){return print_featureIndices;}
	public boolean printRegName(){return print_regName;}
	public HashMap<Region,String[]> getLabeledData(){return labeled_data;}
	public int getNumThreads(){return numThreads;}
	public double getPerBinPoissonLogPThres(){return perBinPoissonLogPThres;}
	public String getFeatureIndOutFilename(){return featureInd_outfilename;}
	public String getTrainDataOutfilename(){return traindata_outfilename;}
	public List<String> getCurated8mers(){return curated_8mers;}
	public boolean generateDnaseDomainsFeatures(){return generateDnaseDomainsFeatures;}
	public boolean gerenrateBindingLabelFeatures(){return gerenrateBindingLabelFeatures;}
	public String getTrainLibSVMFilePath(){return trainDataLibSVMFile;}
	public int getNumRDDPartitions(){return RDD_partitions;}
	public double getTrainFraction(){return trainFrac;}
	public int getL1MaxItrs(){return L1_maxItrs;}
	public double getL1RegParam(){return L1_regularization;}
	public String getClassifierOutName(){return classifierOutName;}
	public String getClassifierPerOutName(){return classifierPerfOutname;}
	public int getRFNumTrees(){return RF_numTrees;}
	public int getRFTreeDepth(){return RF_treeDepth;}
	public boolean trainModel(){return training;}
	public String getL1mod(){return trained_L1_model;}
	public String getRFmodelsDir(){return trained_RF_models_dir;}
	public String getMLPmod(){return trained_MLP_model;}
	public String getModScoreOut(){return model_scores_outfile;}
	public String getModWeightOutFile(){return model_weights_outfile;}
	public ExperimentManager getManager(){return manager;}
	public int getAccAwareShortKmerArndWin(){return acc_aware_short_kmerAroundWin;}
	public boolean usePoissonBgMod(){return usePoissonBgModel;}
	public String getFeatureIndFilename(){return featureIndexFile;}
	public WildcardKmerUtils getWCutils(){return wcutils;}
	public HashSet<String> getWCMappingList(){ return curated_8mers_mapping_set;}
	public boolean getScanSetHasLabs(){return scanSetHasLabs;}

	
	// Initializers
	public void initializeBackgrounds(){
		for(ExperimentCondition c : manager.getConditions())
			conditionBackgrounds.put(c, new BackgroundCollection());
		//Condition-level genomic backgrounds
    	for(ExperimentCondition c : manager.getConditions())
    		conditionBackgrounds.get(c).addBackgroundModel(new PoissonBackgroundModel(-1, perBinPoissonLogPThres, c.getTotalSignalCount(), gcon.getGenome().getGenomeLength(), econ.getMappableGenomeProp(), (double)acc_features_binwidth, '.', 1, true));	
	}     


	public DreamConfig(String[] arguments) throws IOException {
		String[] args = arguments;
		ArgParser ap = new ArgParser(args);
		
		gcon = new GenomeConfig(args);
		econ = new ExptConfig(gcon.getGenome(),args);
		manager = new ExperimentManager(econ);
		// Initialize the background models
		initializeBackgrounds();
		// Initialize the wildcard utils
		wcutils = new WildcardKmerUtils(8);
		
		// Fill the training data; and names
		tf_name = ap.getKeyValue("tfname");
		// Read the test OR train cell-lines from the design file
		celllines = new String[manager.getNumConditions()];
		for(ExperimentCondition ec : manager.getConditions()){
			celllines[ec.getIndex()] = ec.getName();
		}
		
		// Set the window size for tf labels to train on
		tf_win = Args.parseInteger(args, "tfwin", 800); 
		dnase_win = Args.parseInteger(args, "dnasewin", 300);
		numThreads = Args.parseInteger(args, "threads", 1);
		
		// Now load the train labels
		// I'm assuming the dreme-format here
		// chr	start	end	cell-line-1	cell-line-2
		
		if(ap.hasKey("labels")){
			String labels_file = ap.getKeyValue("labels");
			BufferedReader br = new BufferedReader(new FileReader(labels_file));
			String line;
			// indices of cell-lines in the labels file
			Map<String,Integer> cellIndMap = new HashMap<String,Integer>();
			while((line = br.readLine()) != null){
				String[] pieces = line.split("\t");

				if(pieces[0].contains("Region")){
					for(int i=1; i<pieces.length; i++){
						cellIndMap.put(pieces[i], i);
					}
					continue;
				}
				// Take the mid-point and then expand the region to the given window size
				RegionParser parser = new RegionParser(gcon.getGenome());
				Region currReg = parser.execute(pieces[0]);
				String[] labs = new String[manager.getNumConditions()];
				for(int i=1; i< pieces.length; i++){
					// get the manage cell-line name
					String cellname = manager.getConditions().get(i-1).getName();
					labs[i-1] = pieces[cellIndMap.get(cellname)];
				}
				labeled_data.put(currReg.getMidpoint().expand(tf_win/2), labs);
			}
			br.close();
		}
		
		// Now. if enriched domains have been given load them
		// Domains should be given in standard peak/region format
		for(String s : args){
			String cellname = "";
			if(s.contains("posDnase")){
				String[] pieces = s.split("_");
				cellname = pieces[1];
				dnase_domains_pos.put(cellname, RegionFileUtilities.loadRegionsFromFile(ap.getKeyValue(s), gcon.getGenome(),dnase_win/2));
				// Now also load the neg domains
				if(!s.contains(cellname+"_"+cellname)){
					System.err.println("Provide neg regions, i.e adj to dnase domains for all cell-lines");
					System.exit(1);
				}else{
					dnase_adj_neg.put(cellname, RegionFileUtilities.loadRegionsFromFile(ap.getKeyValue(cellname+"_"+cellname), gcon.getGenome(),dnase_win/2));
				}
			}
		}
		
		// Now load other analysis params needed for the dnase data
		
		acc_features_binwidth = Args.parseInteger(args, "binwidth", 20);
		acc_features_binstep = Args.parseInteger(args, "binstep", 20);
		acc_features_numWins = Args.parseInteger(args, "adjNumberOfWinsForValley", 5);
		acc_features_global_win = Args.parseInteger(args, "dnaseWindowGlobal", 1000);
		acc_features_globenrich_thresh = Args.parseDouble(args, "globalThresh", 0.01);
		acc_features_tagShift=Args.parseInteger(args, "tagShift", 0);		
		acc_features_tag3PrimeExtension=Args.parseInteger(args, "3pExt", 0); 
		acc_features_tag5PrimeExtension=Args.parseInteger(args, "5pExt", 0);
		acc_features_adjEnrichment_thresh = Args.parseDouble(args, "valleyTestPvalThresh", 0.01);
		acc_aware_short_kmerAroundWin =  Args.parseInteger(args, "accShortKmerArndWin", 500);
		
		
		// Now load cognate motif features if given
		// Make sure both the complements are present, its easier to deal that way.
		if(ap.hasKey("selected8mers")){
			String filename  = ap.getKeyValue("selected8mers");
			unbiased_kmers = false;
			Set<String> curated_8mers_set = new HashSet<String>();
			BufferedReader br_8mers = new BufferedReader(new FileReader(filename));
			String line;
			while((line = br_8mers.readLine()) != null){
				if(line.contains("8mer"))
					continue;
				String[] pieces = line.split("\t");
				curated_8mers_set.add(pieces[0]);
				curated_8mers_set.add(SequenceUtils.reverseComplement(pieces[0]));
			}
			curated_8mers.addAll(curated_8mers_set);
			
			for(String curr8mer : curated_8mers){
				for(String kmap : wcutils.map(curr8mer)){
					curated_8mers_mapping_set.add(kmap);
				}
			}
			
			br_8mers.close();
		}else{
			unbiased_kmers = true;
		}
		
		
		acc_aware = ap.hasKey("accaware");
		acc_aware_short = ap.hasKey("accawareshort");
		usePoissonBgModel = ap.hasKey("usePoissonBgModel");
		print_featureIndices = ap.hasKey("printFeatureIndices");
		print_regName = ap.hasKey("printRegionName");
		featureInd_outfilename = ap.getKeyValue("featureIndOut");
		traindata_outfilename = ap.getKeyValue("trainDataOut");
		
		generateDnaseDomainsFeatures = ap.hasKey("generateDnaseDomainsFeatures");
		gerenrateBindingLabelFeatures = ap.hasKey("gerenrateBindingLabelFeatures");
		
		// Now load classifier options
		trainDataLibSVMFile = Args.parseString(args, "trainlibsvm", "");
		RDD_partitions = Args.parseInteger(args, "minPartitions", 10);
		trainFrac = Args.parseDouble(args, "trainFrac", 0.7);
		L1_maxItrs = Args.parseInteger(args, "L1MaxItrs", 100);
		L1_regularization = Args.parseDouble(args, "L1_regParam", 0.1);
		classifierOutName = Args.parseString(args, "modelOutName", "out.mod");
		classifierPerfOutname = Args.parseString(args, "modelPerfOutName", "out.log");
		RF_numTrees = Args.parseInteger(args, "numTrees", 100);
		RF_treeDepth = Args.parseInteger(args, "treeDepth", 5);
		training = ap.hasKey("train");
		trained_L1_model = Args.parseString(args, "L1mod", "");
		trained_RF_models_dir = Args.parseString(args, "RFmodelsDir", "");
		trained_MLP_model = Args.parseString(args, "MLPmod", "");
		model_scores_outfile = Args.parseString(args, "modScoresOut", "");
		model_weights_outfile = Args.parseString(args, "weightsOutFile", "");
		featureIndexFile = Args.parseString(args, "featureIndex", "");
		scanSetHasLabs = ap.hasKey("scanSetHasLabs");
		
	}







}
