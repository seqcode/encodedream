package org.seqcode.projects.encodedream.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.seqcode.genome.GenomeConfig;
import org.seqcode.genome.location.Region;
import org.seqcode.gseutils.Args;

/**
 * Generates a subset of binding site labels. Undersamples the negative set by 
 * picking regions arund the bound sites 
 * @author akshaykakumanu
 *
 */
public class SubsetLabels {
	
	protected GenomeConfig gcon;
	protected Map<Region,String[]> labels = new HashMap<Region,String[]>();
	protected String header;
	protected int numCelllines;
	
	public SubsetLabels(GenomeConfig g) {
		gcon = g;
	}
	
	//Settors
	public void setLabs(Map<Region,String[]> ls){labels.putAll(ls);}
	public void setHeader(String s){header = s;}
	public void setNumCellLines(int c){numCelllines = c;}
	
	
	
	public void execute(){
		// First sort the regs
		List<Region> regs = new ArrayList<Region>();
		regs.addAll(labels.keySet());
		Collections.sort(regs);
 		int jump_val = 10;
 		
 		System.out.println(header);
 		
 		for(int r=0; r<regs.size(); r++){
 			// Is this region "B" in any of the cellline ??
 			boolean bound = false;
 			for(int c=0; c<numCelllines; c++){
 				if(labels.get(regs.get(r))[c].equals("B")){
 					bound = true;
 					break;
 				}
 			}
 			
 			if(bound){
 				// First print his region
 				System.out.println(regs.get(r).getLocationString()+"\t"+String.join("\t", labels.get(regs.get(r))));
 				
 				// Jump down the list and check if its bound or not
 				int jump_loc = (r+jump_val >= regs.size() ? regs.size()-1 : r+jump_val);
 				// Make sure not bound in any of the celllines
 				boolean alsoBound = false;
 				while(!alsoBound && jump_loc < regs.size()){
 					for(int c=0; c< numCelllines; c++){
 						if(labels.get(regs.get(jump_loc))[c].equals("B")){
 							alsoBound = true;
 							break;
 						}
 					}
 					if(!alsoBound){
 						System.out.println(regs.get(jump_loc).getLocationString()+"\t"+String.join("\t", labels.get(regs.get(jump_loc))));
 						break;
 					}
 					// If not run over the loop again
 					alsoBound = false;
 					jump_loc++;
 				}
 			} // End of bound
 		}// End of regions
	}// End of execute
	
	public static void main(String[] args) throws IOException{
		GenomeConfig gcon = new GenomeConfig(args);
		SubsetLabels runner = new SubsetLabels(gcon);
		
		// Now load labs;
		Map<Region,String[]> labs = new HashMap<Region,String[]>();
		BufferedReader br = new BufferedReader(new FileReader(Args.parseString(args, "labels", "")));
		String line;
		int numCells = 0;
		while((line = br.readLine()) != null){
			String[] pieces = line.split("\t");
			if(line.contains("start")){
				runner.setHeader(line);
				runner.setNumCellLines(pieces.length-3);
				numCells = pieces.length-3;
			}else{
				Region tmpReg = new Region(gcon.getGenome(),pieces[0],Integer.parseInt(pieces[1]),Integer.parseInt(pieces[2]));
				String[] val = new String[numCells];
				for(int c=3; c<pieces.length; c++){
					val[c-3] = pieces[c];
				}
				
				labs.put(tmpReg, val);
			}
		}
		br.close();
		
		runner.setLabs(labs);
		runner.execute();
	}
	
	

}
