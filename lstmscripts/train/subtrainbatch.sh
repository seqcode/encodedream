#!/bin/bash
#SBATCH --gres=gpu:1
#SBATCH --time 3:00:00
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --gres gpu:1

# echo commands to stdout
set -x

# run GPU program
module load cuda/7.5
#module load anaconda/4.0
module load tensorflow/0.10

# move to the working directory
cd /cstor/xsede/users/xs-divyas/data-balanced/

# train basic kmer models for each of the TFs for submission. 
for folder in *
do
    cd $folder
    echo $folder
    ls
    for input in *.txt
    do
        python /home/xsede/users/xs-divyas/projects/encode-dream/code/train/train_network.py $input 50 
    done
    cd ..
done
