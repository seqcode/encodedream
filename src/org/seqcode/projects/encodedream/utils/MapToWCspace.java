package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.sequence.WildcardKmerUtils;
import org.seqcode.gseutils.Args;

/** Maps a given list of 8-mers to Wild-card space */

public class MapToWCspace {
	protected final int K = 8;
	protected WildcardKmerUtils wcutils;
	protected GenomeConfig gcon;
	protected List<String> input_8mers = new ArrayList<String>();
	
	public MapToWCspace(GenomeConfig g) {
		gcon = g;
	}
	
	public void set8mers(List<String> mers){input_8mers.addAll(mers);}
	
	public void execute() throws IOException{
		
		// Initiate the wc utils
		wcutils = new WildcardKmerUtils(K);
		
		Set<String> mapped8mers = new HashSet<String>();
		
		for(String s: input_8mers){
			for(String wcMap : wcutils.map(s)){
				mapped8mers.add(wcMap);
			}
		}
		
		for(String s : mapped8mers){
			System.out.println(s);
		}
	}
	
	public static void main(String[] args) throws IOException{
		
		GenomeConfig g = new GenomeConfig(args);
		MapToWCspace mapper = new MapToWCspace(g);
		
		// Now load the input 8mers to map
		String kmers_filename = Args.parseString(args, "kmers", "");
		BufferedReader br = new BufferedReader(new FileReader(kmers_filename));
		List<String> kmers = new ArrayList<String>();
		String line;
		while((line = br.readLine())!=null){
			String[] pieces = line.split("\t");
			if(!line.contains("8mer"))
				kmers.add(pieces[0]);
		}
		br.close();
		
		mapper.set8mers(kmers);
		mapper.execute();
	}
	
	
}
