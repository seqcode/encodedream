# Getting kmer models for PWMs.

import sys
import numpy as np
from collections import defaultdict
import itertools

#Global decision
kmersize = 8

def readPWM(fp):
	return np.genfromtxt(fp, usecols = (1,2,3,4), names= True)

def findscores(npar, ind):
	d = {}
	bigl = []
	biglsc = []
	for score in npar:
		l = []
		lsc = []
		zipscore = zip(score, ind)
		for x,y in zipscore:
			if x > 0.15:
				l.append(y)
				lsc.append(x)
		biglsc.append(lsc)
		bigl.append(l)
	return biglsc, bigl

def splitPWMs(fp):
	""" Lazy hard-coding. Must make it nice whenever there be time""" 
	L = readPWM(fp)
	return zip(L,L[1:], L[2:], L[3:], L[4:], L[5:], L[6:], L[7:])


class Makekmers:

	""" Using a bunch of fns to convert pwm to kmer score """
	def __init__(self, fp, fout):
		self.filep = fp
		self.fout = fout

	def convert(self):
		x = readPWM(self.filep)
		y = splitPWMs(self.filep)
		allkmers = []
		allscores = []
		for win in y:
			lsc, dec = findscores(win, x.dtype.names)
			kmerset = list(itertools.product(*dec))
			kmerlist = map(lambda x: ''.join(x), kmerset)

			## Getting scores here:
			kmerscore = list()
			for seq in kmerlist:
				score = 0
				for ind, val in enumerate(seq):
					#print val, ind
					poss_scores = lsc[ind]
					poss_els = dec[ind]
					score = score + poss_scores[poss_els.index(val)]
				kmerscore.append(score)
				#print kmerscore
			
			allscores.append(kmerscore)
			allkmers.append(kmerlist)

			#print len(allscores)
			#print len(allkmers)
			t1 = []
			for kmer in itertools.chain(*allkmers):
				t1.append(kmer)
			t2 =[]
			for score in itertools.chain(*allscores): 
				t2.append(score)

		#print len(t1), len(t2)
		x = zip(t1,t2)

		return x

	def writekmers(self):
		fpout = open(self.fout, "w")
		fpout.write("8mer\n")
		for kmer, score in self.convert():
			#print kmer, score
			fpout.write(kmer)
			fpout.write("\t")
			fpout.write(str(score))
			fpout.write("\n")
		fpout.close()

A = Makekmers(sys.argv[1], sys.argv[2])
A.writekmers()
