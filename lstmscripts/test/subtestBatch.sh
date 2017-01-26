#!/bin/bash
#SBATCH --gres=gpu:1
#SBATCH --time 1:00:00
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --gres gpu:1

# echo commands to stdout
set -x

# run GPU program
module load CUDA
#module load anaconda/4.0
module load tensorflow/0.10

# move to the working directory
cd /cstor/xsede/users/xs-divyas/data-balanced/CTCF2/

python /home/xsede/users/xs-divyas/projects/encode-dream/code/test/scan.py ../../test-binaries/balanced.bin tb.metrics

#python /home/xsede/users/xs-divyas/projects/encode-dream/code/test/scancsv.py balanced.txt
