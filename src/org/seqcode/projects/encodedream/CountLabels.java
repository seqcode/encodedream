package org.seqcode.projects.encodedream;

import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.util.MLUtils;
import org.seqcode.gseutils.ArgParser;
import org.seqcode.gseutils.Args;

import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;


/** Some simple spark utils to manipulate the DREME data-sets */

public class CountLabels {
	
	protected JavaRDD<LabeledPoint> data;
	protected SparkConf sparkConf;
	protected JavaSparkContext sc;
	protected int minPartitions;

	public CountLabels() {
		// create the spark context
		sparkConf = new SparkConf().setAppName("JavaDremeUtils");
		sc = new JavaSparkContext(sparkConf);
	}

	public void closeContex(){sc.close();}

	// Settors
	public void setPartitions(int p){minPartitions = p;}
	public void loadData(String filename){
		data = MLUtils.loadLibSVMFile(sc.sc(), filename,-1,minPartitions).toJavaRDD();
	}
	
	
	// Manipulations
	public JavaRDD<LabeledPoint> getPosInstances(){
		return data.filter(new Function<LabeledPoint,Boolean>(){
			@Override
			public Boolean call(LabeledPoint d) throws Exception {
				return (d.label() == 1)? true:false;
			}
			
		});
	}
	
	public JavaRDD<LabeledPoint> getNegInstances(){
		return data.filter(new Function<LabeledPoint,Boolean>(){

			@Override
			public Boolean call(LabeledPoint d) throws Exception {
				return (d.label() == 0)? true:false;
			}
			
		});
	}
	
	
	
	
	public static void main(String[] args){
		ArgParser ap = new ArgParser(args);
		
		CountLabels runner = new CountLabels();
		int partitions = Args.parseInteger(args, "partitions", 10);
		runner.setPartitions(partitions);
		String dataFile = Args.parseString(args, "trainData", "");
		runner.loadData(dataFile);
		JavaRDD<LabeledPoint> bound = runner.getPosInstances();
		JavaRDD<LabeledPoint> unbound = runner.getNegInstances();
		
		if(ap.hasKey("printPos")){
			MLUtils.saveAsLibSVMFile(bound.rdd(), "bound_tmp.libsvm");
		}
		if(ap.hasKey("printNeg")){
			MLUtils.saveAsLibSVMFile(unbound.rdd(), "unbound_tmp.libsvm");
		}
		
	}
	



}
