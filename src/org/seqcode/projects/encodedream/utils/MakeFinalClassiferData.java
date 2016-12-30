package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class MakeFinalClassiferData {
	
	protected double[] c1_scores;
	protected double[] c2_scores;
	//any number of other scores will also be added to feature set 
	// these scores must be specified using the tag "--miscScores"
	// dims are num_miscScores x NUM_TEST
	protected double[][] misc_scores; 
	protected int[] tss_dists;
	protected double[][] c3_scores;
	protected double[][] dnaseTgas;
	
	protected int[][] labels; // based on train-index
	protected int[] testIndAtTrain;
	
	
	protected boolean genNeg = false;
	protected boolean hasC3 = false;
	protected int numRand = 10000;
	protected int numCelllines = 1;
	
	protected int posWeight = 10; // weight for positive examples; 
	
	public static final int NUM_TEST = 60519747;
	public static final int NUM_TRAIN = 51676736;
	
	
	// setteros
	public void setPosWeight(int w){posWeight=w;}
	public void setC1Score(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		c1_scores = new double[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			c1_scores[count] = Double.parseDouble(line.trim());
			count++;
		}
		br.close();
	}
	public void setMiscScores(List<String> fnames) throws IOException{
		misc_scores = new double[fnames.size()][NUM_TEST];
		for(int f=0;f<fnames.size(); f++){
			BufferedReader br = new BufferedReader(new FileReader(fnames.get(f)));
			String line = null;
			int count=0;
			while((line=br.readLine())!=null){
				misc_scores[f][count] = Double.parseDouble(line.trim());
				count++;
			}
			br.close();
		}
	}
	public void setC2Score(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		c2_scores = new double[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			c2_scores[count] = Double.parseDouble(line.trim());
			count++;
		}
		br.close();
	}
	public void setTSSdist(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		tss_dists = new int[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			tss_dists[count] = Integer.parseInt(line.trim());
			count++;
		}
		br.close();
	}
	public void setC3Scores(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		c3_scores = new double[NUM_TEST][numCelllines];
		int count=0;
		while((line=br.readLine())!=null){
			String[] pieces = line.split("\t");
			for(int p=0; p<pieces.length; p++){
				c3_scores[count][p] = Double.parseDouble(pieces[p]);
			}
			count++;
		}
		br.close();
	}
	public void setDnaseTags(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		dnaseTgas = new double[NUM_TEST][numCelllines];
		int count=0;
		while((line=br.readLine())!=null){
			String[] pieces = line.split("\t");
			for(int p=0; p<pieces.length; p++){
				dnaseTgas[count][p] = Double.parseDouble(pieces[p]);
			}
			count++;
		}
		br.close();
	}
	public void setLabels(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		labels = new int[NUM_TRAIN][numCelllines];
		int count=0;
		while((line=br.readLine())!=null){
			String[] pieces = line.split("\t");
			for(int p=0; p<pieces.length; p++){
				labels[count][p] = pieces[p].equals("B") ? 1 : 0;
			}
			count++;
		}
		br.close();
	}
	public void setTrainTestIndMap(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		testIndAtTrain = new int[NUM_TRAIN];
		int count=0;
		while((line=br.readLine())!=null){
			testIndAtTrain[count] = Integer.parseInt(line.trim().split("\t")[1]);
			count++;
		}
		br.close();
	}
	public void genRand(boolean b){genNeg=b;}
	public void setNumRand(int n){numRand = n;}
	public void setNumCells(int c){numCelllines = c;}
	public void setHasC3(boolean b){hasC3 = b;}
	
	
	public MakeFinalClassiferData() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Order of features is :
	 * C1-score
	 * C2-score
	 * C3-score
	 * TSS-dist
	 * DNase-tag count
	 */
	public void executePos(){
		for(int tr = 0; tr<labels.length; tr++){ // over all train regions
			int testInd = testIndAtTrain[tr];
			for(int c=0; c<labels[0].length; c++){ // over all cell-lines
				StringBuilder sb = new StringBuilder();
				if(labels[tr][c] == 1){ // if this is bound "B"
					if(hasC3){
						sb.append(1);sb.append(" "); // label
						sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
						sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
						sb.append("3:");sb.append(c3_scores[testInd][c]);sb.append(" "); //c3-score
						sb.append("4:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
						sb.append("5:");sb.append(dnaseTgas[testInd][c]);sb.append(" "); // dnase-tags
						if(misc_scores!=null){
							int currFeatInd = 6;
							for(int f=0; f<misc_scores.length; f++){
								sb.append(currFeatInd);sb.append(";");sb.append(misc_scores[f][testInd]);sb.append(" ");
								currFeatInd++;
							}
						}
						sb.append("\n");
						System.out.print(sb.toString());
					}else{
						sb.append(1);sb.append(" "); // label
						sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
						sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
						sb.append("3:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
						sb.append("4:");sb.append(dnaseTgas[testInd][c]);sb.append(" "); // dnase-tags
						if(misc_scores!=null){
							int currFeatInd = 5;
							for(int f=0; f<misc_scores.length; f++){
								sb.append(currFeatInd);sb.append(";");sb.append(misc_scores[f][testInd]);sb.append(" ");
								currFeatInd++;
							}
						}
						sb.append("\n");
						System.out.print(sb.toString());
					}
				}
			}
		}
	}
	
	public void executeFull(){
		for(int tr = 0; tr<labels.length; tr++){ // over all train regions
			int testInd = testIndAtTrain[tr];
			boolean boundinNone = true;
			for(int c=0; c<labels[0].length; c++){ // over all cell-lines
				StringBuilder sb = new StringBuilder();
				if(labels[tr][c] == 1){ // if this is bound "B"
					boundinNone = false;
					if(hasC3){
						for(int pw=0; pw<posWeight; pw++){
							sb.append(1);sb.append(" "); // label
							sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
							sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
							sb.append("3:");sb.append(c3_scores[testInd][c]);sb.append(" "); //c3-score
							sb.append("4:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
							sb.append("5:");sb.append(dnaseTgas[testInd][c]);sb.append(" "); // dnase-tags
							if(misc_scores!=null){
								int currFeatInd = 6;
								for(int f=0; f<misc_scores.length; f++){
									sb.append(currFeatInd);sb.append(";");sb.append(misc_scores[f][testInd]);sb.append(" ");
									currFeatInd++;
								}
							}
							sb.append("\n");
							System.out.print(sb.toString());
						}
					}else{
						for(int pw=0; pw<posWeight; pw++){
							sb.append(1);sb.append(" "); // label
							sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
							sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
							sb.append("3:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
							sb.append("4:");sb.append(dnaseTgas[testInd][c]);sb.append(" "); // dnase-tags
							if(misc_scores!=null){
								int currFeatInd = 5;
								for(int f=0; f<misc_scores.length; f++){
									sb.append(currFeatInd);sb.append(";");sb.append(misc_scores[f][testInd]);sb.append(" ");
									currFeatInd++;
								}
							}
							sb.append("\n");
							System.out.print(sb.toString());
						}
					}
				}
			}
			if(boundinNone){
				StringBuilder sb = new StringBuilder();
				int randCellInd = 0 + (int)(Math.random()*(labels[0].length));
				if(hasC3){
					sb.append(0);sb.append(" "); // label
					sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
					sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
					sb.append("3:");sb.append(c3_scores[testInd][randCellInd]);sb.append(" "); //c3-score
					sb.append("4:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
					sb.append("5:");sb.append(dnaseTgas[testInd][randCellInd]);sb.append(" "); // dnase-tags
					if(misc_scores!=null){
						int currFeatInd = 6;
						for(int f=0; f<misc_scores.length; f++){
							sb.append(currFeatInd);sb.append(";");sb.append(misc_scores[f][testInd]);sb.append(" ");
							currFeatInd++;
						}
					}
					sb.append("\n");
					System.out.print(sb.toString());
				}else{
					sb.append(0);sb.append(" "); // label
					sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
					sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
					sb.append("3:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
					sb.append("4:");sb.append(dnaseTgas[testInd][randCellInd]);sb.append(" "); // dnase-tags
					if(misc_scores!=null){
						int currFeatInd = 5;
						for(int f=0; f<misc_scores.length; f++){
							sb.append(currFeatInd);sb.append(";");sb.append(misc_scores[f][testInd]);sb.append(" ");
							currFeatInd++;
						}
					}
					sb.append("\n");
					System.out.print(sb.toString());
				}
			}
		}
		
	}
	
	
	public void executeNeg(){
		// Random integer generator; for generating train index
		int min = 0;
		int max = testIndAtTrain.length-1;
		int count = 0;
		while(count <= numRand){
			int trainInd = min+(int)(Math.random()*(max));
			int testInd = testIndAtTrain[trainInd];
			boolean unbound = true;
			for(int c=0; c<labels[0].length; c++){
				if(labels[trainInd][c] == 1){
					unbound = false;
					break;
				}
			}
			if(unbound){
				StringBuilder sb = new StringBuilder();
				int randCellInd = 0 + (int)(Math.random()*(labels[0].length));
				if(hasC3){
					sb.append(0);sb.append(" "); // label
					sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
					sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
					sb.append("3:");sb.append(c3_scores[testInd][randCellInd]);sb.append(" "); //c3-score
					sb.append("4:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
					sb.append("5:");sb.append(dnaseTgas[testInd][randCellInd]);sb.append(" "); // dnase-tags
					if(misc_scores!=null){
						int currFeatInd = 6;
						for(int f=0; f<misc_scores.length; f++){
							sb.append(currFeatInd);sb.append(";");sb.append(misc_scores[f][testInd]);sb.append(" ");
							currFeatInd++;
						}
					}
					sb.append("\n");
					System.out.print(sb.toString());
				}else{
					sb.append(0);sb.append(" "); // label
					sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
					sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
					sb.append("3:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
					sb.append("4:");sb.append(dnaseTgas[testInd][randCellInd]);sb.append(" "); // dnase-tags
					if(misc_scores!=null){
						int currFeatInd = 5;
						for(int f=0; f<misc_scores.length; f++){
							sb.append(currFeatInd);sb.append(";");sb.append(misc_scores[f][testInd]);sb.append(" ");
							currFeatInd++;
						}
					}
					sb.append("\n");
					System.out.print(sb.toString());
				}
				count++;
			}
			
		}
		
	}
	
	
	public static void main(String[] args) throws IOException{
		ArgParser ap = new ArgParser(args);
		MakeFinalClassiferData runner = new MakeFinalClassiferData();
		runner.setPosWeight(Args.parseInteger(args, "posW", 10));
		runner.setNumCells(Args.parseInteger(args, "numCells", 1));
		runner.setHasC3(ap.hasKey("hasC3"));
		runner.setC1Score(ap.getKeyValue("c1scores")); // c1-scores
		runner.setC2Score(ap.getKeyValue("c2scores")); // c2-scores
		if(ap.hasKey("hasC3")){
			runner.setC3Scores(ap.getKeyValue("c3scores")); //c3-scores
		}
		if(ap.hasKey("miscScores")){
			runner.setMiscScores(Args.parseList(args, "miscScores"));
		}
		runner.setTSSdist(ap.getKeyValue("tssDistances")); // tss distances
		runner.setDnaseTags(ap.getKeyValue("dnaseTags")); // dnase tags
		runner.setLabels(ap.getKeyValue("labels")); //labels
		runner.setTrainTestIndMap(ap.getKeyValue("trainTestMap"));
		runner.setNumRand(Args.parseInteger(args, "numRand", 10000)); //num
		runner.genRand(ap.hasKey("numRand"));
		
		if(ap.hasKey("numRand")){
			runner.executeNeg();
		}else if(ap.hasKey("full")){
			runner.executeFull();
		}else{
			runner.executePos();
		}
	}
	
	
	
	
	
	
	
	

}
