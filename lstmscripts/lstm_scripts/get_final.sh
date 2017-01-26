#PBS -l walltime=10:00:00
#PBS -l nodes=1:ppn=4
#PBS -l mem=100gb

cd /gpfs/home/gzx103/scratch/dream/final_0110
### CTCF PC-3
paste CTCF.PC-3.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.PC-3.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > CTCF.PC-3.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p CTCF.PC-3 -t /gpfs/home/gzx103/scratch/dream/labels/CTCF

rm F.CTCF.PC-3.tab.gz

gzip F.CTCF.PC-3.tab

### CTCF induced_pluripotent_stem_cell
paste CTCF.induced_pluripotent_stem_cell.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.induced_pluripotent_stem_cell.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > CTCF.induced_pluripotent_stem_cell.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p CTCF.induced_pluripotent_stem_cell -t /gpfs/home/gzx103/scratch/dream/labels/CTCF

rm F.CTCF.induced_pluripotent_stem_cell.tab.gz

gzip F.CTCF.induced_pluripotent_stem_cell.tab

### E2F1 K562
paste E2F1.K562.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.K562.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > E2F1.K562.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p E2F1.K562 -t /gpfs/home/gzx103/scratch/dream/labels/E2F1

rm F.E2F1.K562.tab.gz

gzip F.E2F1.K562.tab

### EGR1 liver
paste EGR1.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > EGR1.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p EGR1.liver -t /gpfs/home/gzx103/scratch/dream/labels/EGR1

rm F.EGR1.liver.tab.gz

gzip F.EGR1.liver.tab

### FOXA1 liver
paste FOXA1.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > FOXA1.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p FOXA1.liver -t /gpfs/home/gzx103/scratch/dream/labels/FOXA1

rm F.FOXA1.liver.tab.gz

gzip F.FOXA1.liver.tab

### FOXA2 liver
paste FOXA2.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > FOXA2.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p FOXA2.liver -t /gpfs/home/gzx103/scratch/dream/labels/FOXA2

rm F.FOXA2.liver.tab.gz

gzip F.FOXA2.liver.tab

### GABPA liver
paste GABPA.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > GABPA.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p GABPA.liver -t /gpfs/home/gzx103/scratch/dream/labels/GABPA

rm F.GABPA.liver.tab.gz

gzip F.GABPA.liver.tab

### HNF4A liver
paste HNF4A.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > HNF4A.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p HNF4A.liver -t /gpfs/home/gzx103/scratch/dream/labels/HNF4A

rm F.HNF4A.liver.tab.gz

gzip F.HNF4A.liver.tab

### JUND liver
paste JUND.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > JUND.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p JUND.liver -t /gpfs/home/gzx103/scratch/dream/labels/JUND

rm F.JUND.liver.tab.gz

gzip F.JUND.liver.tab

### MAX liver
paste MAX.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > MAX.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p MAX.liver -t /gpfs/home/gzx103/scratch/dream/labels/MAX

rm F.MAX.liver.tab.gz

gzip F.MAX.liver.tab

### NANOG induced_pluripotent_stem_cell
paste NANOG.induced_pluripotent_stem_cell.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.induced_pluripotent_stem_cell.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > NANOG.induced_pluripotent_stem_cell.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p NANOG.induced_pluripotent_stem_cell -t /gpfs/home/gzx103/scratch/dream/labels/NANOG

rm F.NANOG.induced_pluripotent_stem_cell.tab.gz

gzip F.NANOG.induced_pluripotent_stem_cell.tab

### REST liver
paste REST.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > REST.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p REST.liver -t /gpfs/home/gzx103/scratch/dream/labels/REST

rm F.REST.liver.tab.gz

gzip F.REST.liver.tab

### TAF1 liver
paste TAF1.liver.y_pred.classification_y_conv_result.txt /gpfs/home/gzx103/scratch/dream/dnase_pass/DNASE.liver.conservative.narrowPeak.reg | awk '{print $1*$3,$2}' > TAF1.liver.y_pred.classification_y_conv_result.Dnase.txt

python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p TAF1.liver -t /gpfs/home/gzx103/scratch/dream/labels/TAF1

rm F.TAF1.liver.tab.gz

gzip F.TAF1.liver.tab

