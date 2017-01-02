package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class MakeTestDataForRF {
	
	protected double[] c1_scores;
	protected double[] c2_scores;
	protected int[] tss_dists;
	protected double[] c3_scores;
	protected double[] dnaseTgas;
	// dnase tag counts in a shorter window, eg:- 50bp;
	// This is a simple hack to learn a footprint
	protected double[] dnaseTagsShort;
	protected double[][] misc_scores;

	protected int[] testIndAtLeaderboard;
	protected int numCelllines = 1;
	protected boolean isTest = false;
	protected boolean hasC3 = false;

	public static final int NUM_TEST = 60519747;
	public static final int NUM_LEADERBOARD = 8843011;


	//settors
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
		c3_scores = new double[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			c3_scores[count] = Double.parseDouble(line);
			count++;
		}
		br.close();
	}
	public void setDnaseTags(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		dnaseTgas = new double[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			dnaseTgas[count] = Double.parseDouble(line);
			count++;
		}
		br.close();
	}
	public void setDnaseTagsShort(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		dnaseTagsShort = new double[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			dnaseTgas[count] = Double.parseDouble(line);
			count++;
		}
		br.close();
	}
	public void setLeaderboardTestIndMap(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		testIndAtLeaderboard = new int[NUM_LEADERBOARD];
		int count=0;
		while((line=br.readLine())!=null){
			testIndAtLeaderboard[count] = Integer.parseInt(line.trim().split("\t")[1]);
			count++;
		}
		br.close();
	}
	public void setIsTest(boolean b){isTest=b;}
	public void setNumCells(int c){numCelllines = c;}
	public void setHasC3(boolean b){hasC3=b;}
	
	public void executeTrain(){
		for(int te=0; te<NUM_TEST; te++){
			StringBuilder sb = new StringBuilder();
			if(hasC3){
				sb.append(0);sb.append(" "); // label
				sb.append("1:");sb.append(c1_scores[te]);sb.append(" "); // c1-score
				sb.append("2:");sb.append(c2_scores[te]);sb.append(" "); // c2-score
				sb.append("3:");sb.append(c3_scores[te]);sb.append(" "); //c3-score
				sb.append("4:");sb.append(tss_dists[te]);sb.append(" "); // tss-dist
				sb.append("5:");sb.append(dnaseTgas[te]);sb.append(" "); // dnase-tags
				if(dnaseTagsShort!=null){
					sb.append("6:");sb.append(dnaseTagsShort[te]);sb.append(" "); // dnase-tags
				}
				if(misc_scores!=null){
					int currFeatInd = (dnaseTagsShort!=null) ?  7 : 6;
					for(int f=0; f<misc_scores.length; f++){
						sb.append(currFeatInd);sb.append(":");sb.append(misc_scores[f][te]);sb.append(" ");
						currFeatInd++;
					}
				}
				sb.append("\n");
				System.out.print(sb.toString());
			}else{
				sb.append(0);sb.append(" "); // label
				sb.append("1:");sb.append(c1_scores[te]);sb.append(" "); // c1-score
				sb.append("2:");sb.append(c2_scores[te]);sb.append(" "); // c2-score
				sb.append("3:");sb.append(tss_dists[te]);sb.append(" "); // tss-dist
				sb.append("4:");sb.append(dnaseTgas[te]);sb.append(" "); // dnase-tags
				if(dnaseTagsShort!=null){
					sb.append("5:");sb.append(dnaseTagsShort[te]);sb.append(" "); // dnase-tags
				}
				if(misc_scores!=null){
					int currFeatInd = (dnaseTagsShort!=null) ?  6 : 5;
					for(int f=0; f<misc_scores.length; f++){
						sb.append(currFeatInd);sb.append(":");sb.append(misc_scores[f][te]);sb.append(" ");
						currFeatInd++;
					}
				}
				sb.append("\n");
				System.out.print(sb.toString());
			}
		}
	}
	
	public void executeLeaderBoard(){
		for(int lr = 0; lr<NUM_LEADERBOARD; lr++){ // over all train regions
			int testInd = testIndAtLeaderboard[lr];
			StringBuilder sb = new StringBuilder();
			if(hasC3){
				sb.append(0);sb.append(" "); // label
				sb.append("1:");sb.append(c1_scores[testInd]);sb.append(" "); // c1-score
				sb.append("2:");sb.append(c2_scores[testInd]);sb.append(" "); // c2-score
				sb.append("3:");sb.append(c3_scores[testInd]);sb.append(" "); //c3-score
				sb.append("4:");sb.append(tss_dists[testInd]);sb.append(" "); // tss-dist
				sb.append("5:");sb.append(dnaseTgas[testInd]);sb.append(" "); // dnase-tags
				if(dnaseTagsShort!=null){
					sb.append("6:");sb.append(dnaseTagsShort[testInd]);sb.append(" "); // dnase-tags
				}
				if(misc_scores!=null){
					int currFeatInd = (dnaseTagsShort!=null) ?  7 : 6;
					for(int f=0; f<misc_scores.length; f++){
						sb.append(currFeatInd);sb.append(":");sb.append(misc_scores[f][testInd]);sb.append(" ");
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
				sb.append("4:");sb.append(dnaseTgas[testInd]);sb.append(" "); // dnase-tags
				if(dnaseTagsShort!=null){
					sb.append("5:");sb.append(dnaseTagsShort[testInd]);sb.append(" "); // dnase-tags
				}
				if(misc_scores!=null){
					int currFeatInd = (dnaseTagsShort!=null) ?  6 : 5;
					for(int f=0; f<misc_scores.length; f++){
						sb.append(currFeatInd);sb.append(":");sb.append(misc_scores[f][testInd]);sb.append(" ");
						currFeatInd++;
					}
				}
				sb.append("\n");
				System.out.print(sb.toString());
			}
		}


	}
	
	public static void main(String[] args) throws IOException{
		ArgParser ap = new ArgParser(args);
		MakeTestDataForRF runner = new MakeTestDataForRF();
		runner.setIsTest(ap.hasKey("istest"));
		runner.setHasC3(ap.hasKey("hasC3"));
		if(ap.hasKey("miscScores")){
			runner.setMiscScores(Args.parseList(args, "miscScores"));
		}
		runner.setC1Score(ap.getKeyValue("c1scores")); // c1-scores
		runner.setC2Score(ap.getKeyValue("c2scores")); // c2-scores
		if(ap.hasKey("hasC3"))
			runner.setC3Scores(ap.getKeyValue("c3scores")); //c3-scores
		runner.setTSSdist(ap.getKeyValue("tssDistances")); // tss distances
		runner.setDnaseTags(ap.getKeyValue("dnaseTags")); // dnase tags
		if(ap.hasKey("dnaseTagsShort")){
			runner.setDnaseTagsShort(ap.getKeyValue("dnaseTagsShort"));
		}
		if(!ap.hasKey("istest")){
			runner.setLeaderboardTestIndMap(ap.getKeyValue("leaderboardTestMap"));
			runner.executeLeaderBoard();
		}else{
			runner.executeTrain();
		}
	
	}
	
	

}
