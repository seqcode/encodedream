package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.sequence.SequenceGenerator;
import org.seqcode.gsebricks.verbs.location.RegionParser;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class OneHotInput {
	
	private RegionParser rparser;
	private GenomeConfig gcon;
	private SequenceGenerator<Region> seqgen;
	private List<String> testRegsString = new ArrayList<String>();
	
	private HashMap<String,String> onehot = new HashMap<String,String>();
	
	public void defineOneHot(){
		onehot.put("A", "1000");
		onehot.put("T", "0100");
		onehot.put("G", "0010");
		onehot.put("C", "0001");
		onehot.put("N", "0001");
	}
	
	public void loadTestRegionString(String file) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line = null;
		while((line=br.readLine())!=null){
			testRegsString.add(line.split("\t")[0]);
		}
		br.close();
	}
	
	@SuppressWarnings("unchecked")
	public OneHotInput(GenomeConfig g) {
		gcon =g;
		seqgen = gcon.getSequenceGenerator();
		rparser = new RegionParser(gcon.getGenome());
		defineOneHot();
	}
	
	public void execute(){
		for(String s : testRegsString){
			Region reg = rparser.execute(s);
			String seq = seqgen.execute(reg).toUpperCase();
			for(int i=0; i<seq.length(); i++){
				System.out.print(onehot.get(seq.substring(i, i+1)));
			}
			System.out.println("");
		}
	}
	
	public static void main(String[] args) throws IOException{
		GenomeConfig g = new GenomeConfig(args);
		OneHotInput onehotcoder = new OneHotInput(g);
		
		onehotcoder.loadTestRegionString(Args.parseString(args, "testindex", ""));
		onehotcoder.execute();
		
	}
	
	

}
