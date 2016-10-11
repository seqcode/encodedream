package org.seqcode.projects.encodedream.classifiers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.classification.LogisticRegressionWithLBFGS;
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics;
import org.apache.spark.mllib.optimization.L1Updater;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.util.MLUtils;
import org.seqcode.projects.encodedream.DreamConfig;

import scala.Tuple2;

public class SparkL1 {
	
	public static void main(String[] args) throws IOException{
		SparkConf sparkConf = new SparkConf().setAppName("JavaLogisticRegressionL1Classification");
		JavaSparkContext sc = new JavaSparkContext(sparkConf);
		
		DreamConfig dcon = new DreamConfig(args);
		
		// Load and parse general params
		String datapath = dcon.getTrainLibSVMFilePath(); // Could be train or test data
		int minPartitions = dcon.getNumRDDPartitions();
		boolean training = dcon.trainModel();
		// Are you training or testing?
		if(training){
			double trainFrac = dcon.getTrainFraction();
			int maxIters = dcon.getL1MaxItrs();
			double regParam = dcon.getL1RegParam();
			// Setting numFeatures to -1; will be determined from the data
			JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(sc.sc(), datapath,-1,minPartitions).toJavaRDD();

			// Split the data into training and test sets (30% held out for testing)
			JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{trainFrac, 1-trainFrac});
			JavaRDD<LabeledPoint> trainingData = splits[0];
			JavaRDD<LabeledPoint> testData = splits[1];

			LogisticRegressionWithLBFGS trainer = new LogisticRegressionWithLBFGS();
			trainer.optimizer().setUpdater(new L1Updater()).setRegParam(regParam);
			trainer.setFeatureScaling(true);
			trainer.setNumClasses(2);
			LogisticRegressionModel model = trainer.run(trainingData.rdd());
			model.clearThreshold();
			
			// Now save the model
			model.save(sc.sc(), dcon.getClassifierOutName());

			// Calculate raw scores on the test set.
			JavaRDD<Tuple2<Object,Object>> predictionAndLabel =
					testData.map(new Function<LabeledPoint, Tuple2<Object,Object>>() {
						@Override
						public Tuple2<Object, Object> call(LabeledPoint p) {
							return new Tuple2<Object, Object>(model.predict(p.features()), p.label());
						}
					});
			
			// Evaluate on the training data
			BufferedWriter bw = new BufferedWriter(new FileWriter(dcon.getClassifierPerOutName()));
			BinaryClassificationMetrics metrics = new BinaryClassificationMetrics(predictionAndLabel.rdd(),20);

			// Precision by threshold
			List<Tuple2<Object, Object>> precision = metrics.precisionByThreshold().toJavaRDD().collect();
			
			StringBuilder sb =  new StringBuilder();
			sb.append("######################## Begin of Precision by threshold #########################");sb.append("\n");
			sb.append("Threshold");sb.append("\t");sb.append("Precision");sb.append("\n");
			sb.append(precision);sb.append("\n");
			sb.append("######################## End of Precision by threshold #########################");sb.append("\n");
			bw.write(sb.toString());
			

			// Recall by threshold
			List<Tuple2<Object, Object>> recall = metrics.recallByThreshold().toJavaRDD().collect();
			sb =  new StringBuilder();
			sb.append("######################## Begin of Recall by threshold #########################");sb.append("\n");
			sb.append("Threshold");sb.append("\t");sb.append("Recall");sb.append("\n");
			sb.append(recall);sb.append("\n");
			sb.append("######################## End of Recall by threshold #########################");sb.append("\n");
			bw.write(sb.toString());

			// F Score by threshold
			List<Tuple2<Object, Object>> f1Score = metrics.fMeasureByThreshold().toJavaRDD().collect();
			sb =  new StringBuilder();
			sb.append("######################## Begin of F1 by threshold #########################");sb.append("\n");
			sb.append("Threshold");sb.append("\t");sb.append("F1-score");sb.append("\n");
			sb.append(f1Score);sb.append("\n");
			sb.append("######################## End of F1 by threshold #########################");sb.append("\n");
			bw.write(sb.toString());

			// Precision-recall curve
			List<Tuple2<Object, Object>> prc = metrics.pr().toJavaRDD().collect();
			sb = new StringBuilder();
			sb.append("######################## Begin of PRC curve #########################");sb.append("\n");
			sb.append(prc);sb.append("\n");
			sb.append("######################## Begin of PRC curve #########################");sb.append("\n");
			bw.write(sb.toString());

			// ROC Curve
			List<Tuple2<Object, Object>> roc = metrics.roc().toJavaRDD().collect();
			sb = new StringBuilder();
			sb.append("######################## Begin of ROC curve #########################");sb.append("\n");
			sb.append(roc);sb.append("\n");
			sb.append("######################## End of ROC curve #########################");sb.append("\n");
			bw.write(sb.toString());

			// AUPRC and AUROC
			sb = new StringBuilder();
			sb.append("Area under precision-recall curve = ");sb.append(metrics.areaUnderPR());sb.append("\n");
			sb.append("Area under ROC curve = ");sb.append(metrics.areaUnderROC());sb.append("\n");
			bw.write(sb.toString());
			
			
			// Also, print the AUPRC and AUROC for the train data
			// Calculate raw scores on the train set.
			JavaRDD<Tuple2<Object,Object>> predictionAndLabelTrain =
					trainingData.map(new Function<LabeledPoint, Tuple2<Object,Object>>() {
						@Override
						public Tuple2<Object, Object> call(LabeledPoint p) {
							return new Tuple2<Object, Object>(model.predict(p.features()), p.label());
						}
					});
			BinaryClassificationMetrics metricsTrain = new BinaryClassificationMetrics(predictionAndLabelTrain.rdd(),20);
			sb = new StringBuilder();
			sb.append("Area under precision-recall curve (train-data) = ");sb.append(metricsTrain.areaUnderPR());sb.append("\n");
			sb.append("Area under ROC curve (train-data) = ");sb.append(metricsTrain.areaUnderROC());sb.append("\n");
			bw.write(sb.toString());
			
			bw.close();
			
			// Finally, also print the model weights
			BufferedWriter bw_weights = new BufferedWriter(new FileWriter(dcon.getModWeightOutFile()));
			double[] wts = model.weights().toArray();
			
			String featureIndFileName = dcon.getFeatureIndFilename();
			BufferedReader ind_read = new BufferedReader(new FileReader(featureIndFileName));
			List<String> features_byind = new ArrayList<String>();
			String line = null;
			while((line = ind_read.readLine())!=null){
				if(line.contains("Kmer"))
					continue;
				String[] pieces = line.split("\t");
				features_byind.add(pieces[1]);
			}
			ind_read.close();
			sb = new StringBuilder();
			for(int i=0; i<wts.length; i++){
				sb.append(features_byind.get(i));sb.append("\t");sb.append(wts[i]);sb.append("\n");
				
			}
			bw_weights.write(sb.toString());
			bw_weights.close();
				
		}else{
			// Scoring a trained model
			// Just, FYI, map and collect function of spark are order preserving!!
			JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(sc.sc(), datapath,-1,minPartitions).toJavaRDD();
			// Now load the model
			LogisticRegressionModel model =  LogisticRegressionModel.load(sc.sc(), dcon.getL1mod());

			// Calculate raw scores on the test set.
			@SuppressWarnings("serial")
			JavaRDD<Tuple2<Double,Double>> predictionAndLabel =
					data.map(new Function<LabeledPoint, Tuple2<Double,Double>>() {
						@Override
						public Tuple2<Double, Double> call(LabeledPoint p) {
							return new Tuple2<Double, Double>(model.predict(p.features()), p.label());
						}
					});
			
			// Now print the probabilities
			BufferedWriter bw = new BufferedWriter(new FileWriter(dcon.getModScoreOut()));
			StringBuilder sb = new StringBuilder();
			for(Tuple2<Double,Double> tp : predictionAndLabel.collect()){
				sb.append(tp._1);sb.append("\n");
			}
			sb.deleteCharAt(sb.length()-1);
			bw.write(sb.toString());
			bw.close();
			
			JavaRDD<Tuple2<Object,Object>> predictionAndLabel_tmp =
					data.map(new Function<LabeledPoint, Tuple2<Object,Object>>() {
						@Override
						public Tuple2<Object, Object> call(LabeledPoint p) {
							return new Tuple2<Object, Object>(model.predict(p.features()), p.label());
						}
					});
			
			// Also, if we have labels for the scan instances. Calculate the ROCs
			if(dcon.getScanSetHasLabs()){
				BufferedWriter bw_scan = new BufferedWriter(new FileWriter(dcon.getClassifierPerOutName()));
				BinaryClassificationMetrics metricsScanSet = new BinaryClassificationMetrics(predictionAndLabel_tmp.rdd(),40);
				sb = new StringBuilder();
				sb.append("Area under precision-recall curve (scan-set) = ");sb.append(metricsScanSet.areaUnderPR());sb.append("\n");
				sb.append("Area under ROC curve (scan-set) = ");sb.append(metricsScanSet.areaUnderROC());sb.append("\n");
				bw_scan.write(sb.toString());
				bw_scan.close();
			}

		}
		
		sc.close();
	}

}
