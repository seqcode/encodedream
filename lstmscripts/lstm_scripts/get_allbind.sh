#PBS -l walltime=10:00:00
#PBS -l nodes=1:ppn=4
#PBS -l mem=10gb

cd /gpfs/home/gzx103/scratch/dream/labels

tail -n+2 ATF3.train.labels.tsv > ATF3.train.labels.noheader.tsv
paste ATF3.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B" && $6=="B" && $7=="B") print $9}' > ATF3.allbind.txt

tail -n+2 E2F1.train.labels.tsv > E2F1.train.labels.noheader.tsv
paste E2F1.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B") print $7}' > E2F1.allbind.txt

tail -n+2 EGR1.train.labels.tsv > EGR1.train.labels.noheader.tsv
paste EGR1.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B" && $6=="B" && $7=="B") print $9}' > EGR1.allbind.txt

tail -n+2 CTCF.train.labels.tsv > CTCF.train.labels.noheader.tsv
paste CTCF.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B" && $6=="B" && $7=="B" && $8=="B" && $9=="B" && $10=="B") print $12}' > CTCF.allbind.txt

tail -n+2 FOXA1.train.labels.tsv > FOXA1.train.labels.noheader.tsv
paste FOXA1.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B") print $6}' | head -1 > FOXA1.allbind.txt

tail -n+2 FOXA2.train.labels.tsv > FOXA2.train.labels.noheader.tsv
paste FOXA2.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B") print $6}' | head -1 > FOXA2.allbind.txt

tail -n+2 GABPA.train.labels.tsv > GABPA.train.labels.noheader.tsv
paste GABPA.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B" && $6=="B" && $7=="B" && $8=="B" && $9=="B") print $10}' > GABPA.allbind.txt

tail -n+2 HNF4A.train.labels.tsv > HNF4A.train.labels.noheader.tsv
paste HNF4A.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B") print $6}' | head -1 > HNF4A.allbind.txt

tail -n+2 JUND.train.labels.tsv > JUND.train.labels.noheader.tsv
paste JUND.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B" && $6=="B" && $7=="B" && $8=="B") print $10}' > JUND.allbind.txt

tail -n+2 MAX.train.labels.tsv > MAX.train.labels.noheader.tsv
paste MAX.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B" && $6=="B" && $7=="B" && $8=="B" && $9=="B" && $10=="B") print $12}' > MAX.allbind.txt

tail -n+2 NANOG.train.labels.tsv > NANOG.train.labels.noheader.tsv
paste NANOG.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B") print $6}' | head -1 > NANOG.allbind.txt

tail -n+2 REST.train.labels.tsv > REST.train.labels.noheader.tsv
paste REST.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B" && $6=="B" && $7=="B" && $8=="B") print $10}' > REST.allbind.txt

tail -n+2 TAF1.train.labels.tsv > TAF1.train.labels.noheader.tsv
paste TAF1.train.labels.noheader.tsv test_coord_index_at_train.txt | awk '{if ($4=="B" && $5=="B" && $6=="B" && $7=="B") print $9}' > TAF1.allbind.txt

