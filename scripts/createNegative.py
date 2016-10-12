import numpy as np
import re
import os

try:
	dream = np.genfromtxt("inputData.txt", delimiter = "\t", filling_values = "", dtype=str, skiprows=1)
except:
	print "Please run it with all data files."

TF = dream[:,0]
train = dream[:,1]
leaderboard = dream[:,2]
final = dream[:,3]

#print dream

def iterlists(test):
	""" Get train + test types for each transcription factor """
	merge_list = list()
	train_list =list()
	test_list = list()
	for idx, val in enumerate(TF):
		## Removing quotes ayyo! 
		hold_tr = re.sub(r'^"|"$', '', train[idx])
		hold_test = re.sub(r'^"|"$', '', test[idx])
		if test[idx] == "":
			test_list.append(None)
		else:			
			test_list.append(hold_test.split(', '))

	return test_list

test_list = iterlists(final)
#print test_list

def getnegs(idx, val):
	"""Get neg sets"""
	try:
		for x in val:
			cmd = "bedtools complement -i %s/%s.bed -g hg19.genome > %s.%s.neg.bed" % (x, x, TF[idx], x)
			print cmd
			os.system(cmd)
	except:
		pass

for idx, val in enumerate(test_list):
		getnegs(idx, val)
