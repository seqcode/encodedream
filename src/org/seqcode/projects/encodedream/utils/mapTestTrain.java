package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class mapTestTrain {
	
	public static final int NUM_TEST = 60519747;
	public static final int NUM_TRAIN = 51676736;
	public static final int NUM_LEADERBOARD = 8843011;
	
	protected int[] testIndAtTrain;
	protected int[] testIndAtLeaderboard;
	
	protected String[] coordinateStringsTest;
	protected HashMap<String,Double> hashedScores = new HashMap<String,Double>();
	
	protected double[] input_vec;
	
	//settors and loaders
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
	
	public void laodHashedScores(String fname) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(fname));
		String line = null;
		while((line = br.readLine())!=null){
			String[] pieces = line.trim().split("\t");
			hashedScores.put(pieces[0], Double.parseDouble(pieces[1]));
		}
		br.close();
	}
	
	
	// mappers
	public void mapTestToTrain(){
		for(int i=0; i<NUM_TRAIN; i++){
			int testInd = testIndAtTrain[i];
			System.out.println(input_vec[testInd]);
		}
	}
	public void mapHashedScoresToTrain(){
		for(int i=0; i<NUM_TRAIN; i++){
			int testInd = testIndAtTrain[i];
			String testCoorString = coordinateStringsTest[testInd];
			if(hashedScores.containsKey(testCoorString)){
				System.out.println(hashedScores.get(testCoorString));
			}else{
				System.out.println(0.0);
			}
		}
	}
	public void mapHashedScoresToTest(){
		for(int i=0; i<NUM_TEST; i++){
			
			String testCoorString = coordinateStringsTest[i];
			if(hashedScores.containsKey(testCoorString)){
				System.out.println(hashedScores.get(testCoorString));
			}else{
				System.out.println(0.0);
			}
		}
	}
	public void mapHashedScoresToLeaderBoard(){
		for(int i=0; i<NUM_LEADERBOARD; i++){
			int testInd = testIndAtLeaderboard[i];
			String testCoorString = coordinateStringsTest[testInd];
			if(hashedScores.containsKey(testCoorString)){
				System.out.println(hashedScores.get(testCoorString));
			}else{
				System.out.println(0.0);
			}
		}
	}
	
	
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		
		mapTestTrain runner = new mapTestTrain();
		ArgParser ap = new ArgParser(args);
		if(ap.hasKey("inputTestVec") && ap.hasKey("mapToTrain")){
			runner.setTrainTestIndMap(ap.getKeyValue("testIndAtTrain"));
			runner.loadInputVecTest(Args.parseString(args,"inputTestVec", ""));
			runner.mapTestToTrain();
		}
		
		if(ap.hasKey("hashedSet") && ap.hasKey("mapToTrain")){
			runner.setCoordinateStringsTest(ap.getKeyValue("testCoordInd"));
			runner.setTrainTestIndMap(ap.getKeyValue("testIndAtTrain"));
			runner.laodHashedScores(ap.getKeyValue("hashedSet"));
			runner.mapHashedScoresToTrain();
		}
		
		if(ap.hasKey("hashedSet") && ap.hasKey("mapToTest")){
			runner.setCoordinateStringsTest(ap.getKeyValue("testCoordInd"));
			runner.laodHashedScores(ap.getKeyValue("hashedSet"));
			runner.mapHashedScoresToTest();
		}
		
		if(ap.hasKey("hashedSet") && ap.hasKey("mapToLboard")){
			runner.setCoordinateStringsTest(ap.getKeyValue("testCoordInd"));
			runner.setLeaderboardTestIndMap(ap.getKeyValue("testIndAtLboard"));
			runner.laodHashedScores(ap.getKeyValue("hashedSet"));
			runner.mapHashedScoresToLeaderBoard();
		}
		
	}
	
}
