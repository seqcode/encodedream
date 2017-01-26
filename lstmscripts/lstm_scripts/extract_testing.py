def extract_ladder(index_file,pred_file,tfname):
	print('read index')
	data00=open(index_file,'r')
	data01=[]

	for records in data00:
		data01.append(records.split())

	print('read pred')
	data10=open(pred_file+'.y_pred.classification_y_conv_result.Dnase.txt','r')
	data11=[]

	for records in data10:
		data11.append(records.split()[0])

	data_index_allB=open(tfname+'.allbind.txt','r')
	index_allB={}
	for records in data_index_allB:
		index_allB[int(records)]=' '
	data_index_allB.close()

	print('write data')
	data20=open('F.'+pred_file+'.tab','w')

	for ind in data01:
		pk=ind[0].split(':')
		start_end=pk[1].split('-')
		id_ladder=int(ind[1])
		if id_ladder in index_allB:
			pred=0.99
		else:
			pred=float(data11[id_ladder])
		data20.write(pk[0]+'\t'+start_end[0]+'\t'+start_end[1]+'\t'+str(pred)+'\n')

	data20.close()
	data10.close()
	data00.close()

############################################################################
import getopt
import sys
def main(argv):
	try:
		opts, args = getopt.getopt(argv,"hi:p:t:")
	except getopt.GetoptError:
		print 'python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p CTCF.induced_pluripotent_stem_cell -t /gpfs/home/gzx103/scratch/dream/labels/CTCF'
		sys.exit(2)

	for opt,arg in opts:
		if opt=="-h":
			print 'python /gpfs/home/gzx103/scratch/dream/test/extract_testing.py -i /gpfs/home/gzx103/scratch/dream/test/test_coord_index.txt -p CTCF.induced_pluripotent_stem_cell -t /gpfs/home/gzx103/scratch/dream/labels/CTCF'
			sys.exit()
		elif opt=="-i":
			index_file=str(arg.strip())
		elif opt=="-p":
			pred_file=str(arg.strip())
		elif opt=="-t":
			tfname=str(arg.strip())
	extract_ladder(index_file,pred_file,tfname)

if __name__=="__main__":
	main(sys.argv[1:])

