package org.seqcode.projects.encodedream;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.sequence.SequenceGenerator;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class GenerateSeq {

	protected GenomeConfig gcon;
	protected SequenceGenerator<Region> seqgen;
	protected String reg_bed_file;
	protected int win=200;

	@SuppressWarnings("unchecked")
	public GenerateSeq(GenomeConfig g) {
		gcon = g;
		seqgen = gcon.getSequenceGenerator();
	}
	
	//Settors
	public void setWin(int w){win=w;}
	public void setRegsFileName(String s){reg_bed_file =s;}
	
	public void execute() throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(reg_bed_file));
		String line = null;
		
		while((line = br.readLine()) != null){
			String[] pieces = line.split("\t");
			Region currReg = new Region(gcon.getGenome(),pieces[0].replace("chr", ""),Integer.parseInt(pieces[1]),Integer.parseInt(pieces[2]));
			String seq = seqgen.execute(currReg).toUpperCase();
			if(seq.contains("N"))
				continue;
			System.out.println(currReg.getLocationString()+"\t"+seq.toUpperCase());
		}
		br.close();
	}
	public static void main(String[] args) throws IOException{
		ArgParser ap = new ArgParser(args);
		GenomeConfig g = new GenomeConfig(args);
		GenerateSeq runner = new GenerateSeq(g);
		runner.setWin(Args.parseInteger(args, "win", 200));
		runner.setRegsFileName(ap.getKeyValue("trainbed"));
		runner.execute();
	}

}
