# encodedream

##Overview
Our approach to the ENCODE/DREAM challenge assumes that TF binding in a given cell type is determined by an unknown mixture of two types of genomic features: 1) features that represent invariant properties of the TF itself; and 2) features that represent cell-specific interactions with other regulatory actors. An example of a TF property that we assume to be (relatively) invariant is the inherent DNA binding preference of the TF itself, while cell-specific features could include binding motifs of co-factor/cooperating TFs. The ENCODE/DREAM challenge provides TF-binding peak calls for a small set of training cell types, and we could conceivably use this data and public motif repositories to estimate invariant TF features. However, since we are not provided with TF-binding data for the test cell type, there is no clear way to estimate the cell-specific features that are important for specifying binding in the test cell type. 

We aim to take a transfer learning approach to the challenge. We first train three separate types of classifiers to recognize: 1) the primary binding motif of the TF of interest; 2) properties of TF binding sites that are bound in all training cell types; and 3) general cell-specific motif features associated with accessible regions in each training and test cell type. A fourth classifier is trained to integrate the outputs of each of the three preceding classifiers along with other genomic features (DNase tag counts and distance to TSS) to predict bound/unbound labels. The parameters of this final classifier are trained using the training datasets and then directly applied (i.e. transferred) to make predictions in the test cell type(s). The underlying idea is that we train the final classifier to recognize how important the various invariant and cell-specific features are in determining binding sites in the training cell types, and then these general parameters are assumed to also apply to the test cell type(s). 

While it is not yet performing with high accuracy, the potential benefit of our approach is that it encapsulates a principled way to learn and incorporate both invariant and cell-specific features in scenarios with limited training data. 

##Approach details
###General training data definitions
Definition of accessible regions from DNase-seq data: We used a custom domain finder ("SEED") to call DNase-seq-enriched domains for each cell type. Our domain-finder used a sliding window of 200bp (100bp step), a Poisson threshold of p<10^-5, and a Binomial test threshold of p<0.05 to call enriched regions. 
Normalized DNase tag count features: For each cell type, we counted DNase-seq tags in 600bp windows around all test coordinates along the genome. Tag counts are normalized using total tag counts. 600bp was chosen as the window size as it gave the best naive discrimination between bound and unbound labels (AUROC).
Distances to TSS: Distances between a site and annotated TSSs were calculated using the GENCODE (v19) release. 

###Classifier C1: Primary TF binding motif features
The purpose of this classifier is to represent the inherent binding preference of the TF of interest. We train a L1-regularized logistic classifier using counts of a restricted set of wildcard k-mers (8-mers with 2 wildcard positions) in 200bp windows to classify bound/unbound labels in the training cell types. The restricted set of k-mers are chosen to represent top-scoring 8-mers in PBM data for the TF of interest (using data downloaded from the cis-bp database). If no PBM data exists, we generate k-mers from appropriate PWM models for the TF of interest. Since we assume that the TF's binding preference is invariant, only one C1 classifier is trained per TF. 

###Classifier C2: Features of shared sites
The purpose of this classifier is to recognize general features of TF binding sites that we expect to be bound in many/all cell types. We first extract "shared" TF binding peaks that are present in all training cell types. We further restrict to shared sites that are accessible in the test cell type. We then train a L1-regularized logistic classifier using 4-mers, 5-mers and 6-mers (excluding k-mers that are substrings of features used to train C1) counts within 200bp windows using the shared sites as the positive set and unbound sequences 500bp away as the negative set. We train one C2 classifier for each training and test cell type (since the accessibility filter can change the training set in each cell type).

###Classifier C3: Cell-specific motif features
The purpose of this classifier is to recognize features associated with distal accessible regions of chromatin in each cell type. Beginning with our defined DNase-seq-enriched domains for a given cell type, we filter out domains that are within 10Kbp of an annotated TSS. We then rank these distal domains by scores obtained by scanning the C1 classifier. We then train a L1-regularized logistic classifier using 4-mers, 5-mers and 6-mers (excluding k-mers that are substrings of features used to train C1) counts within 200bp windows using the top ranked distal accessible regions as the positive set and random regions as the negative set. We train one C3 classifier for each training and test cell type.

###Final classifier: Predicting Bound/Unbound labels
This classifier takes the following as input features for each test coordinate along the genome: 
 - C1 classifier score (representing primary motif features)
 - C2 classifier score (representing features of shared sites)
 - C3 classifier score (representing cell-specific motifs)
 - Normalized DNase-seq tag counts
 - Distance to TSS
We train a random forest classifier using these features to predict observed Bound/Unbound labels in each training cell type. Input features (other than the C1 score and distance to TSS) are defined using cell-type appropriate sources for each training example. The trained random forest classifier is used to make predictions on Bound/Unbound labels along the genome for the test cell type(s), where we again substitute cell-type appropriate sources of input features for the C2, C3, and DNase tag count scores. 


##Outcome & future directions
We do not expect that the current version of our approach is performing well on the challenge data, as we did not have sufficient time to optimize and extend the component classifiers. However, we plan to continue working on this challenge for the January deadline. Future directions for improving the model will focus on changing the form and complexity of each of the component classifiers. 


##Suggestions for improving the challenge
Firstly, we'd like to congratulate the organizers for putting together this timely challenge. Challenges like this not only allow us to assess the state of the art within a particular application domain, they also have a lasting impact on the direction of future computational biology research. Bearing this in mind, it may be useful for the organizers to justify or discuss some of the following challenge design choices in the planned manuscript describing the challenge results:
1) The challenge itself is framed as predicting TF binding, when in fact it asks us to predict the outcome of a peak-finder when run on ChIP-seq data. This distinction matters when we consider the exact nature of the requested predictions: we are asked to predict binary labels for 200bp segments over a relatively wide window around binding events. The window of consecutive "B" (bound) labels around predicted binding peaks is relatively large (often 800bp+) compared with the biological TF-DNA binding event. In practice, then, most of the "B" labeled 200bp segments do not in fact contain the TF-bound DNA bases. This makes the challenge exceedingly difficult; we could in theory train a highly specific model that would perfectly predict which 200bp sequences are bound by a TF, but this model would still not correctly predict most of the "B" labels in the challenge. 
2) It would have been useful for some approaches to have access to the raw ChIP-seq signals for training instead of peak calls. 
3) This challenge focuses on predicting TF binding between cell types, which may lead some to assume that the complementary within cell type TF binding challenge has been solved (i.e. training on ChIP-seq and DNase-seq for a subset of chromosomes and predicting TF binding in the other chromosomes). It's not obvious that this is true, and directly testing the ability of current approaches to solve this within-cell problem would serve as a useful baseline. 


##Provided code
We have provided a code repository containing all of our code and scripts for this project. An example run-through of our procedure is provided in scripts/run.sh in this repo. Note that this shell script is for illustration only. We have not tested that the script runs for a new dataset off of our local computing environment. If this is necessary, we are happy to debug and modify the codes to run on your systems. Please contact Shaun (mahony@psu.edu) or Akshay (auk262@psu.edu). 


