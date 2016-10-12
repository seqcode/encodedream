import numpy as np
import re
import os
import sys

dream = np.genfromtxt(sys.argv[1], delimiter = "\t", filling_values = "", dtype=str, skip_header=1)

#print dream
#print dream.shape
TF = dream[:,0]
train = dream[:,1]
leaderboard = dream[:,2]
final = dream[:,3]


def iterlists(test):
	""" Get train + test types for each transcription factor """
	merge_list = list()
	train_list =list()
	for idx, val in enumerate(TF):
		## Removing quotes ayyo! 
		hold_tr = re.sub(r'^"|"$', '', train[idx])
		hold_test = re.sub(r'^"|"$', '', test[idx])
		if test[idx] == "":
			merge_list.append(None)
			train_list.append(None)
			#merge_list.append(hold_tr.split(', ',))
		else:
			train_list.append(hold_tr.split(', '))
			merge_list.append(hold_tr.split(', ') + hold_test.split(', '))
	return merge_list, train_list

tfacc, tfChIP = iterlists(final)

def do_intersect(ind, tf):
	"""Do a bedtools intersect for the train and test"""
	try:
		no_of_factors = len(tf)
	except:
		no_of_factors = 0
	try:

		cmdstring = "bedtools intersect -a %s/%s.bed -b %s/%s.bed > out.bed" % (tf[0],tf[0], tf[1],tf[1])
		print cmdstring
		#os.system(cmdstring)
		#os.system("cp out.bed in1.bed")
		count = 2
		while count < no_of_factors:
			#print count
			cmdstring = "bedtools intersect -a %s/%s.bed -b in1.bed > out.bed" % (tf[count], tf[count])
			print cmdstring
			#os.system(cmdstring)
			#os.system("cp out.bed in1.bed")
			count = count + 1
		cmd = "cp out.bed %s-acc.bed" % TF[ind]
		print cmd
		os.system(cmd)
	except:
		pass

	print "Neeext!"

#for idx, tf in enumerate(tfacc):
#	do_intersect(idx, tf)



for idx,tf in enumerate(tfChIP):
	try:
                no_of_factors = len(tf)
        except:
                no_of_factors = 0
	
	try:
		input_bedB = ''
		os.system("touch in")
		count = 0
		for x in tf:
			count += 1
			if count == 1:
				input_bedA = "conservative/" + "ChIPseq." + x + "." + TF[idx] + "." + "conservative.train.narrowPeak"
			input_bedB = input_bedB + "conservative/" + "ChIPseq." + x + "." + TF[idx] + "." + "conservative.train.narrowPeak" + " "
		
		if no_of_factors == 1:
			cmd = "cat %s | cut -f 1,2,3 >  %s.MB.bed" % (input_bedA, TF[idx])
			print cmd
			os.system(cmd)
		else:
			maj = round(0.6 * no_of_factors)  
			
			cmd = "cat %s > hold" % input_bedB
			cmd2 = "cat hold | sort -V -k1,1 -k2,2 > sorted.bed"
			cmd3 = "bedtools merge -c 1 -o count -i sorted.bed | awk '{ if($4 >= %s) print $1, $2, $3}' > %s.MB.bed" % (maj, TF[idx])
			
			print cmd
			print cmd2
			print cmd3
			
			os.system(cmd)
			os.system(cmd2)
			os.system(cmd3)
		print "Next!"
	except:
		pass


