package org.seqcode.projects.encodedream;

import java.io.IOException;

public class Dream {
	
	public static void main(String[] args) throws IOException{
		DreamConfig dcon = new DreamConfig(args);
		CountFeatures cf = new CountFeatures(dcon);
		
		if(dcon.generateDnaseDomainsFeatures()){
			cf.execute();
		}
		if(dcon.gerenrateBindingLabelFeatures()){
			cf.execute();
		}
	}

}
