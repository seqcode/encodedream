package org.seqcode.projects.encodedream.utils;

import cern.jet.random.Binomial;
import cern.jet.random.engine.DRand;

public class DremeSandbox {
	public static Binomial binomial = new  Binomial(100,.5, new DRand());

	/**
	 * Binomial CDF assuming scaled control. Uses COLT binomial test.
	 * Tests equality of signal & scaled control counts. 
	 * @param k = scaled control
	 * @param n = scaled control+signal
	 * @param minFoldChange = minimun fold difference between signal & control
	 * @return
	 */
	public static double binomialPValue(double k, double n, double minFoldChange){
		double pval=1;
		binomial.setNandP((int)Math.ceil(n), 1.0 / (minFoldChange + 1.0));
		pval = binomial.cdf((int) Math.ceil(k));
		return(pval);		
	}

}
