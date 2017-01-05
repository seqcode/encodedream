package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.seqcode.gseutils.ArgParser;

public class MakeTrainForLogitRF {
	
	protected int[] dnasePeaksAtTest;
	protected int[] tfPeaksAtTrain;
	protected int[] testIndAtTrain;
	
	protected String[] coordinateStringsTest;
	
	public static final int NUM_TEST = 60519747;
	public static final int NUM_TRAIN = 51676736;
	
	public void setDnasePeaksAtTest(String fname) throws NumberFormatException, IOException{
		dnasePeaksAtTest = new int[NUM_TEST];
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		int count=0;
		while((line=br.readLine())!=null){
			dnasePeaksAtTest[count] = Integer.parseInt(line.trim());
			count++;
		}
		br.close();
	}
	
	public void setTfPeaksAtTrain(String fname) throws IOException{
		tfPeaksAtTrain = new int[NUM_TRAIN];
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		int count=0;
		while((line=br.readLine())!=null){
			if(line.contains("U")){
				tfPeaksAtTrain[count] = 0;
			}else{
				tfPeaksAtTrain[count] = 1;
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
	public void setCoordinateStringsTest(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		coordinateStringsTest = new String[NUM_TEST];
		int count=0;
		while((line=br.readLine())!=null){
			String[] pieces = line.split("\t");
			coordinateStringsTest[count] = pieces[0];
			count++;
		}
		br.close();
	}
	
	public void execute(){
		// roll down the "tfPeaksAtTrain" file
		for(int tr=0; tr<NUM_TRAIN; tr++){
			int testInd = testIndAtTrain[tr];
			if(tfPeaksAtTrain[tr] == 1){ // if peak ==> positive example 
				StringBuilder sb =new StringBuilder();
				sb.append(coordinateStringsTest[testInd]);sb.append("\t");sb.append("B");
				System.out.println(sb.toString());
			}else if(tfPeaksAtTrain[tr] == 0 && dnasePeaksAtTest[testInd] ==1){ // if no peak and accessible --> negative example
				StringBuilder sb =new StringBuilder();
				sb.append(coordinateStringsTest[testInd]);sb.append("\t");sb.append("U");
				System.out.println(sb.toString());
			}
		}
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		MakeTrainForLogitRF runner = new MakeTrainForLogitRF();
		ArgParser ap = new ArgParser(args);
		runner.setDnasePeaksAtTest(ap.getKeyValue("dnasePeaksAtTest"));
		runner.setTfPeaksAtTrain(ap.getKeyValue("tfPeaksAtTrain"));
		runner.setCoordinateStringsTest(ap.getKeyValue("testCoordInd"));
		runner.setTrainTestIndMap(ap.getKeyValue("testIndAtTrain"));
		runner.execute();
	}
	
	

}
