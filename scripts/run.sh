#################################################################################################################################################
# Team: NittanyLions
# Contact: auk262@psu.edu
#
#
# The following is a log of all the commands that we ran for making tf binding predictions for the ENCODE-DREAM challenge (2016). All the code to 
# run these commands will be provided. However, we do not guarantee that these commands will work. We are submitting this log to comply with the
# rules of the challenge. For queries, please do not hesitate to contact Akshay at (auk262@psu.edu)
#
#################################################################################################################################################

############################# C1 classifier


###### Extract k-mers from cis-bp PWMs (Done only for factors with no PBM data)
python kmerfromPWM.py "motif.pwm" > "primaryMotifKmers.list"

###### Map primary motifs to wild-card k-mer space
java org.seqcode.projects.encodedream.utils.MapToWCspace --kmers "primaryMotifKmers.list" > "primaryMotifWCKmers.list"

###### Count wild-card 8-mers at test regions
java -Xmx40G org.seqcode.lab.akshay.encodedreme.CountWC8mers --species "Homo sapiens;hg19" --seq ~/group/genomes/hg19/ --data "training.labels" --biased "primaryMotifWCKmers.list" --featureIndOut "featureInd.list" --libsvmOut "train.libsvm" --subset --numThreads 5

#--data (making training labels)
java -Xmx20G org.seqcode.lab.akshay.encodedreme.utils.NarrowPeakTrainData --species "Homo sapiens;hg19" --seq ~/group/genomes/hg19/ --randJump 500 --win 200 --removeClosePeaksDistace 50 --pos "a file with list a list of narrow peaks" > "training.labels"

###### Run L1-logistic regression (C1-classifier)
spark-submit --class org.seqcode.lab.akshay.encodedreme.classifiers.SparkL1 --master local[30] --driver-memory 755G encodedream.jar --trainlibsvm "$fac"_train.libsvm --minPartitions 30 --trainFrac 0.7 --L1MaxItrs 10000 --L1_regParam "regularization param" --modelOutName "C1.mod" --modelPerfOutName "C1.perf" --train --weightsOutFile "C1.modweights" --featureIndex "featureInd.list"

###### Scan C1 model and score all the test regions
# First count features (wc -8mers) at all test regions
java -Xmx40G org.seqcode.lab.akshay.encodedreme.CountWC8mers --species "Homo sapiens;hg19" --seq ~/group/genomes/hg19/ --testSetIndex "test_coord_index.txt"  --biased "primaryMotifWCKmers.list" --featureIndOut "featureInd.list" --libsvmOut "test.libsvm" --numThreads 10

# scan the trained C1 model
spark-submit --class org.seqcode.lab.akshay.encodedreme.classifiers.SparkL1 --master local[30] --conf spark.driver.maxResultSize=4g --driver-memory 80G encodedream.jar --trainlibsvm "test.libsvm" --minPartitions 30 --L1mod "C1.mod" --modScoresOut "C1_score.txt"


############################# C2 classifier

###### Make train data
python createPositive.py "test-train_celltype table" > "C2.labels"

###### Count 4 to 6 mers at train sites
java -Xmx30G org.seqcode.lab.akshay.dreme.GenerateKmerLIBSVM --species "Homo sapiens;hg19" --seq ~/group/genomes/hg19/ --minK 4 --maxK 6 --win 200 --exclude "primaryMotifWCKmers.list" --trainbed "C2.labels" > "C2_train.libsvm"

##### Train L1-logistic classifier (C2-classifier)
spark-submit --class org.seqcode.lab.akshay.encodedreme.classifiers.SparkL1 --master local[5] --driver-memory 20G encodedream.jar --trainlibsvm "C2_train.libsvm" --minPartitions 5 --trainFrac 0.7 --L1MaxItrs 10000 --L1_regParam "regularization param" --modelOutName "C2.mod" --modelPerfOutName "C2.perf" --train --weightsOutFile "C2.modweights"

##### Scan C2 classifier at teset regions
# First count 4 to 6 mers at all test regions
java -Xmx30G org.seqcode.lab.akshay.dreme.GenerateKmerLIBSVM --species "Homo sapiens;hg19" --seq ~/group/genomes/hg19/ --minK 4 --maxK 6 --trainbed "test_coord_index.txt"  > "testSet_4_6.libsvm"

spark-submit --class org.seqcode.lab.akshay.encodedreme.classifiers.SparkL1 --master local[5] --conf spark.driver.maxResultSize=4g --driver-memory 650G encodedream.jar --trainlibsvm testSet_4_7.libsvm --minPartitions 5 --L1mod "C2.mod" --modScoresOut "C2_score.txt"


############################# C3 classifier

##### Generate labels for C3 classifier
## Map all the regions to test set indices
java org.seqcode.lab.akshay.encodedreme.utils.ToTestIndex --species "Homo sapiens;hg19" --seq ~/group/genomes/hg19/ --regionsList "file with list of domain calls for all cell types" --printIndices --testInd "test_coord_index.txt" > "DNaseDomains_testIndices.tab"
## Now filter domains that are within 10kbp of TSSs
java org.seqcode.lab.akshay.encodedreme.utils.FilterDNaseDomains --species "Homo sapiens;hg19" --seq ~/group/genomes/hg19/ --domains "DNaseDomains_testIndices.tab" --summits --tss "gencode.v19.annotation.tss" --tssExclude 10000 > "DNaseDomains_tssFiltered_testIndices.tab"
## Generate seed regions for training C3
perl generateSeeds.pl "C1.score" "CellType DNase Domain" 20000 0.8 3 >  "C3_train.labels"

##### Make train data 
java -Xmx30G org.seqcode.lab.akshay.dreme.GenerateKmerLIBSVM --species "Homo sapiens;hg19" --seq ~/group/genomes/hg19/ --minK 4 --maxK 6 --win 200 --exclude "primaryMotifWCKmers.list" --trainbed "C3_train.labels" > "C3_train.libsvm"

##### Scan C3 classifier at trest regions 
spark-submit --class org.seqcode.lab.akshay.encodedreme.classifiers.SparkL1 --master local[5] --conf spark.driver.maxResultSize=4g --driver-memory 650G encodedream.jar --trainlibsvm testSet_4_7.libsvm --minPartitions 5 --L1mod "C3.mod" --modScoresOut "C2_score.txt"


 
############################# Final RF classifier

##### Make train data for the Final RF classifier
java -Xmx50G org.seqcode.lab.akshay.encodedreme.utils.MakeFinalClassiferData --c1scores "C1_score.txt" --c2scores "C2_score.txt" --tssDistances "test-coord-tss-distances.txt" --dnaseTags "dnase_tagCounts.txt" --labels "train_labels.txt" --trainTestMap "test_coord_index_at_train.txt" --numCells "num of train celllines" --full > "rf.libsvm"

##### Train the RF classifier
spark-submit --class org.seqcode.lab.akshay.encodedreme.classifiers.SparkRF --master local[30] --driver-memory 400G encodedream.jar --trainlibsvm "rf.libsvm" --minPartitions 30 --trainFrac 0.7 --numTrees 500 --treeDepth 3 --modelOutName "rf.mod" --modelPerfOutName "rf.perf" --train

##### Make final predictions at test instances
spark-submit --class org.seqcode.lab.akshay.encodedreme.classifiers.SparkRF --master local[10] --conf spark.driver.maxResultSize=4g --driver-memory 70G encodedream.jar --trainlibsvm "test.libsvm"  --minPartitions 10 --RFmodelsDir "rf.mod"  --modScoresOut "final.predictions" 

