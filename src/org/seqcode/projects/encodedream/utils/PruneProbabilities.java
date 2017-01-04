package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class PruneProbabilities {
	
	protected double[] input_vec;
	public static final int NUM_TEST = 60519747;
	public static final int NUM_LEADERBOARD = 8843011;
	public static final int NUM_TRAIN = 51676736;
	
	protected int[] dnasePeakCalls;
	protected int[] testIndAtLeaderboard;
	protected int[] testIndAtTrain;
	
	protected int span_bin = 3;
	
	public PruneProbabilities() {
		// TODO Auto-generated constructor stub
	}
	
	//settors
	public void setDnasePeakCalls(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		dnasePeakCalls = new int[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			dnasePeakCalls[count] = Integer.parseInt(line.trim());
			count++;
		}
		br.close();
	}
	public void loadInputVecTest(String fname) throws NumberFormatException, IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		input_vec = new double[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			input_vec[count] = Double.parseDouble(line.trim());
			count++;
		}
		br.close();
	}
	public void loadInputVecLeaderboard(String fname) throws NumberFormatException, IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		input_vec = new double[NUM_LEADERBOARD];
		int count=0;
		while((line=br.readLine())!=null){
			input_vec[count] = Double.parseDouble(line.trim());
			count++;
		}
		br.close();
	}
	public void loadInputVecTrain(String fname) throws NumberFormatException, IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		input_vec = new double[NUM_TRAIN];
		int count=0;
		while((line=br.readLine())!=null){
			input_vec[count] = Double.parseDouble(line.trim());
			count++;
		}
		br.close();
	}
	public void setBinspan(int w){span_bin=w;}
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
	
	public void factorOutInaccessibleTest(){
		for(int i=0; i<NUM_TEST; i++){ // roll over the file
			if(dnasePeakCalls[i] == 0){
				System.out.println(0);
			}else{
				System.out.println(input_vec[i]);
			}
		}
	}
	
	public void factorOutInaccessibleLeaderBoard(){
		for(int i=0; i<NUM_LEADERBOARD; i++){ // roll over the file
			int testInd = testIndAtLeaderboard[i];
			if(dnasePeakCalls[testInd] == 0){
				System.out.println(0);
			}else{
				System.out.println(input_vec[i]);
			}
		}
	}
	
	public void factorOutInaccessibleTrain(){
		for(int i=0; i<NUM_TRAIN; i++){ // roll over the file
			int testInd = testIndAtTrain[i];
			if(dnasePeakCalls[testInd] == 0){
				System.out.println(0);
			}else{
				System.out.println(input_vec[i]);
			}
		}
	}
	
	public void executeTEST(){
		for(int i=0; i<NUM_TEST; i++){ // roll over the file
			int span_begin = Math.max(0, i-((span_bin-1)/2));
			int span_end =  Math.min(NUM_TEST-1, i+((span_bin-1)/2));
			double max=0.0;
			for(int s=span_begin ; s<=span_end; s++){
				if(max<input_vec[s])
					max = input_vec[s];
			}
			System.out.println(max);
		}	
	}
	
	public void executeTRAIN(){
		for(int i=0; i<NUM_TRAIN; i++){ // roll over the file
			int span_begin = Math.max(0, i-((span_bin-1)/2));
			int span_end =  Math.min(NUM_TEST-1, i+((span_bin-1)/2));
			double max=0.0;
			for(int s=span_begin ; s<=span_end; s++){
				if(max<input_vec[s])
					max = input_vec[s];
			}
			System.out.println(max);
		}	
	}
	
	public void executeLEADERBOARS(){
		for(int i=0; i<NUM_LEADERBOARD; i++){ // roll over the file
			int span_begin = Math.max(0, i-((span_bin-1)/2));
			int span_end =  Math.min(NUM_LEADERBOARD-1, i+((span_bin-1)/2));
			double max=0.0;
			for(int s=span_begin ; s<=span_end; s++){
				if(max<input_vec[s])
					max = input_vec[s];
			}
			System.out.println(max);
		}	
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		ArgParser ap = new ArgParser(args);
		PruneProbabilities runner = new PruneProbabilities();
		runner.setBinspan(Args.parseInteger(args, "binspan", 3));
		
		if(ap.hasKey("isTest")){
			runner.loadInputVecTest(Args.parseString(args,"inputVec", ""));
			if(ap.hasKey("prune"))
				runner.executeTEST();
			if(ap.hasKey("factorOutInacc")){
				runner.setDnasePeakCalls(ap.getKeyValue("dnasePeakCalls"));
				runner.factorOutInaccessibleTest();
			}
		}else if(ap.hasKey("isLBoard")){
			
			runner.loadInputVecLeaderboard(Args.parseString(args,"inputVec", ""));
			if(ap.hasKey("prune"))
				runner.executeLEADERBOARS();
			if(ap.hasKey("factorOutInacc")){
				runner.setLeaderboardTestIndMap(ap.getKeyValue("leaderboardTestMap"));
				runner.setDnasePeakCalls(ap.getKeyValue("dnasePeakCalls"));
				runner.factorOutInaccessibleLeaderBoard();
			}
				
		}else if(ap.hasKey("isTrain")){
			runner.loadInputVecTrain(Args.parseString(args,"inputVec", ""));
			if(ap.hasKey("factorOutInacc")){
				runner.setTrainTestIndMap(ap.getKeyValue("TrainTestMap"));
				runner.setDnasePeakCalls(ap.getKeyValue("dnasePeakCalls"));
				runner.factorOutInaccessibleTrain();
			}
		}
	}

}
