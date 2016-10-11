package org.seqcode.projects.encodedream.classifiers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.mllib.classification.LogisticRegressionModel;
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.RandomForest;
import org.apache.spark.mllib.tree.configuration.Algo;
import org.apache.spark.mllib.tree.configuration.Strategy;
import org.apache.spark.mllib.tree.impurity.Gini;
import org.apache.spark.mllib.tree.impurity.Impurity;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.tree.model.RandomForestModel;
import org.apache.spark.mllib.util.MLUtils;
import org.seqcode.gseutils.Args;
import org.seqcode.projects.encodedream.DreamConfig;

import scala.Tuple2;

public class SparkRF {
	@SuppressWarnings({ "serial", "serial", "serial" })
	public static void main(String[] args) throws IOException{
		
		SparkConf sparkConf = new SparkConf().setAppName("JavaRandomForestClassification");
		JavaSparkContext sc = new JavaSparkContext(sparkConf);
		DreamConfig dcon = new DreamConfig(args);
		
		// Load and parse general params
		String datapath = dcon.getTrainLibSVMFilePath(); // Could be train or test data
		int minPartitions = dcon.getNumRDDPartitions();
		boolean training = dcon.trainModel();
		
		// Are you training or testing?
		if(training){
			// Setting numFeatures to -1; will be determined from the data
			JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(sc.sc(), datapath,-1,minPartitions).toJavaRDD();
			double trainFrac = dcon.getTrainFraction();
			JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[]{trainFrac, 1-trainFrac});
			JavaRDD<LabeledPoint> trainingData = splits[0];
			JavaRDD<LabeledPoint> testData = splits[1];
			
			// Load and parse random forest params
			Integer numTrees = dcon.getRFNumTrees();
			Integer maxDepth = dcon.getRFTreeDepth();

			// Train a RandomForest model.
			// Empty categoricalFeaturesInfo indicates all features are continuous.
			Integer numClasses = 2;
			HashMap<Integer, Integer> categoricalFeaturesInfo = new HashMap<Integer, Integer>();
			String featureSubsetStrategy = "auto"; // Let the algorithm choose.
			Integer maxBins = 32;
			Integer seed = 12345;
			
			// Creating the impurity criteria
			Impurity imp = Gini.instance();
			
			// Setting up our strategy
			Strategy strgy = new  Strategy(Algo.Classification(), imp, maxDepth, numClasses, maxBins, categoricalFeaturesInfo);
			strgy.setSubsamplingRate(1.0/numTrees);
			
			// Training the RF model
			final RandomForestModel model = new RandomForest(strgy,numTrees, featureSubsetStrategy,seed).run(trainingData.rdd());

			// Save and load model
			model.save(sc.sc(), dcon.getClassifierOutName());

			// Since spark doesn't return predicted class probabilities, we will need to calculate them :( :(
			Broadcast<DecisionTreeModel[]> trees = sc.broadcast(model.trees());
			JavaRDD<Tuple2<Object,Object>> predictionAndLabel = testData.map(new Function<LabeledPoint, Tuple2<Object,Object>>(){

				@Override
				public Tuple2<Object, Object> call(LabeledPoint pt) throws Exception {
					Double prob=0.0;
					for(DecisionTreeModel tr : trees.value()){
						prob=prob+tr.predict(pt.features());
					}
					prob = prob/trees.value().length;

					return new Tuple2<Object, Object>(prob,pt.label());
				}

			});

			// Evaluate on the training data
			BufferedWriter bw = new BufferedWriter(new FileWriter(dcon.getClassifierPerOutName()));
			BinaryClassificationMetrics metrics = new BinaryClassificationMetrics(predictionAndLabel.rdd(),100);

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
			bw.close();
			
			// Print the gini values for features
		
		}else{
			// Scoring a trained model
			// Just, FYI, map and collect function of spark are order preserving!!
			JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(sc.sc(), datapath,-1,minPartitions).toJavaRDD();
			// Now load the model
			
			// load a bunch of trained RF models
			List<RandomForestModel> rf_models = new ArrayList<RandomForestModel>();
			BufferedReader br = new BufferedReader(new FileReader(dcon.getRFmodelsDir()));
			String line=null;
			while((line=br.readLine())!=null){
				RandomForestModel model =  RandomForestModel.load(sc.sc(), line.trim());
				rf_models.add(model);
			}
			br.close();
			
			// Boradcast all trees //sc.broadcast(model.trees());
			
			List<Broadcast<DecisionTreeModel[]>> treesList = new ArrayList<Broadcast<DecisionTreeModel[]>>();
			for(RandomForestModel mod : rf_models){
				treesList.add(sc.broadcast(mod.trees()));
			}
					
			JavaRDD<Tuple2<Double,Double>> predictionAndLabel = data.map(new Function<LabeledPoint, Tuple2<Double,Double>>(){

				@Override
				public Tuple2<Double, Double> call(LabeledPoint pt) throws Exception {
					Double prob=0.0;
					int totTrees = 0;
					for(Broadcast<DecisionTreeModel[]> tree : treesList){
						for(DecisionTreeModel tr : tree.value()){
							prob=prob+tr.predict(pt.features());
						}
						totTrees = totTrees + tree.value().length;
					}
					
					prob = prob/totTrees;

					return new Tuple2<Double, Double>(prob,pt.label());
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
		}
		
		//RandomForestModel sameModel = RandomForestModel.load(sc.sc(), "myModelPath");
		sc.close();

	}

}
