package org.seqcode.projects.encodedream;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.seqcode.deepseq.StrandedBaseCount;
import org.seqcode.deepseq.experiments.ExperimentManager;
import org.seqcode.deepseq.experiments.ExptConfig;
import org.seqcode.deepseq.experiments.Sample;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.genome.sequence.SequenceGenerator;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

public class GenerateTrainData {
	protected GenomeConfig gcon;
	protected ExptConfig econ;
	protected SequenceGenerator<Region> seqgen;
	protected String reg_bed_file;
	protected int win=200;
	String cellline = "";

	@SuppressWarnings("unchecked")
	public GenerateTrainData(GenomeConfig g, ExptConfig e) {
		gcon = g;
		econ=e;
		seqgen = gcon.getSequenceGenerator();
	}
	
	// Settors
	public void setCellLabel(String c){cellline = c;}
	public void setWin(int w){win=w;}
	public void setRegsFileName(String s){reg_bed_file =s;}
	
	

	//keep the number in bounds
	protected final double inBounds(double x, double min, double max){
		if(x<min){return min;}
		if(x>max){return max;}
		return x;
	}
	protected final int inBounds(int x, int min, int max){
		if(x<min){return min;}
		if(x>max){return max;}
		return x;
	}

	public void execute() throws NumberFormatException, IOException{
		BufferedReader br = new BufferedReader(new FileReader(reg_bed_file));
		String line = null;
		ExperimentManager manager = new ExperimentManager(econ);
		Sample samp = manager.getSamples().get(0);

		int celllineind = 0;

		while((line = br.readLine()) != null){
			String[] pieces = line.split("\t");
			// First, set cellline index 
			if(line.contains("start")){

				for(int i=3; i<pieces.length; i++){
					if(pieces[i].equals(cellline))
						celllineind = i;
				}
			}else{


				Region currReg = new Region(gcon.getGenome(),pieces[0].replace("chr", ""),Integer.parseInt(pieces[1]),Integer.parseInt(pieces[2])-1);
				int label = 1;

				if(pieces[celllineind].equals("U") || pieces[celllineind].equals("A")){
					label = 0;
				}

				String seq = seqgen.execute(currReg).toUpperCase();
				if(seq.contains("N"))
					continue;
				double[] counts = new double[currReg.getWidth() + 1];
				List<StrandedBaseCount> hits = samp.getBases(currReg);
				for(int i=0; i<=currReg.getWidth(); i++){counts[i]=0;}

				for(StrandedBaseCount r : hits){
					if(r.getCoordinate()>=currReg.getStart() && r.getCoordinate()<=currReg.getEnd()){
						int offset = inBounds(r.getCoordinate()-currReg.getStart(),0,currReg.getWidth());
						counts[offset] += r.getCount();
					}
				}
				StringBuilder sb = new StringBuilder();
				sb.append(label);sb.append("\t");
				sb.append(seq);sb.append("\t");
				for(int c=0; c<counts.length; c++){
					sb.append(counts[c]);sb.append(",");
				}
				sb.deleteCharAt(sb.length()-1);
				System.out.println(sb.toString());

			}
		}
		br.close();

	}
	
	public static void main(String[] args) throws NumberFormatException, IOException{
		ArgParser ap = new ArgParser(args);
		GenomeConfig g = new GenomeConfig(args);
		ExptConfig e = new ExptConfig(g.getGenome(),args);
	
		GenerateTrainData runner = new GenerateTrainData(g,e);
		runner.setWin(Args.parseInteger(args, "win", 200));
		runner.setRegsFileName(ap.getKeyValue("trainbed"));
		runner.setCellLabel(ap.getKeyValue("cellline"));
	
		runner.execute();
	}
	
	
}
