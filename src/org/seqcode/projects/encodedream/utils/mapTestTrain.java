package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class mapTestTrain {
	
	public static final int NUM_TEST = 60519747;
	public static final int NUM_TRAIN = 51676736;
	protected int[] testIndAtTrain;
	
	protected double[] input_vec;
	
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
	
	public void mapTestToTrain(){
		for(int i=0; i<NUM_TRAIN; i++){
			int testInd = testIndAtTrain[i];
			System.out.println(input_vec[testInd]);
		}
	}
	
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		
		mapTestTrain runner = new mapTestTrain();
		ArgParser ap = new ArgParser(args);
		if(ap.hasKey("isTest")){
			runner.loadInputVecTest(Args.parseString(args,"inputVec", ""));
			if(ap.hasKey("mapToTrain")){
				runner.mapTestToTrain();
			}
		}
		
	}
	
}
