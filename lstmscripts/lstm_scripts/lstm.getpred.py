from tensorflow.python.ops import rnn, rnn_cell
import tensorflow as tf
import numpy as np

### hyper-parameters
sec_d=800
thr_d=1
for_d=5
filter1_size1=8
filter1_size1_gate=15
filter1_size2=1
first_filter_out=32
full_cn_out=16
epsilon = 0.0005
iter_num=15000
batch_size=200
training_speed=0.00001

def read_data_sep(index_file,label_file,dnase_file,seq_file,conserve_file,neg_num):
	import numpy as np
	import linecache
	index_pos=[]
	index_neg=[]
	label=[]	
	seq_pos=[]
	seq_neg=[]
	dnase_pos=[]
	dnase_neg=[]

	### get index vector
	data_index=open(index_file,'r')
	data_label=open(label_file,'r')
	data_conserve=open(conserve_file,'r')
	data_conserve0=[]
	for records in data_conserve:
		data_conserve0.append(records.split())

	for ind,la in zip(data_index,data_label):
		la_tmp=la.split()[0]
		ind_read=int(ind.split()[1])
		if int(data_conserve0[ind_read][0])==1:
			if la_tmp != 'B':
				index_neg.append(ind_read)
			elif la_tmp == 'B':
				index_pos.append(ind_read)
	data_index.close()
	data_label.close()
	data_conserve.close()
	
	### get pos seq & dnase	
	for ind in index_pos:
		seq_line = linecache.getline(seq_file, ind)
		dnase_line = linecache.getline(dnase_file, ind)
		seq_vector = list(seq_line.split()[0])
		seq_pos.append(seq_vector)
		dnase_vector=dnase_line.split()[0]
		dnase_pos.append([dnase_vector])
	seq_pos=np.array(seq_pos,dtype='float')
	dnase_pos=np.array(dnase_pos,dtype='float')
	### get neg seq & dnase 
	for ind in index_neg:
		seq_line = linecache.getline(seq_file, ind)
		dnase_line = linecache.getline(dnase_file, ind)
		seq_vector = list(seq_line.split()[0])
		seq_neg.append(seq_vector)
		dnase_vector=dnase_line.split()[0]
		dnase_neg.append([dnase_vector])
	seq_neg=np.array(seq_neg,dtype='float')
	dnase_neg=np.array(dnase_neg,dtype='float')
	return seq_pos,dnase_pos*10000000,seq_neg,dnase_neg*10000000

def LSTMstate_variable(shape):
	initial = tf.random_normal(shape, mean=0.1, stddev=0.02)
	return tf.Variable(initial)


def weight_variable(shape):
	initial = tf.truncated_normal(shape, stddev=0.1)
	return tf.Variable(initial)

def bias_variable(shape):
	initial = tf.constant(0.1, shape=shape)
	return tf.Variable(initial)

### Convolution and Pooling
def conv2d(x, W):
	return tf.nn.conv2d(x, W, strides=[1, 4, 1, 1], padding='SAME')

def conv2d_1(x, W):
	return tf.nn.conv2d(x, W, strides=[1, 1, 1, 1], padding='SAME')

def ToXGY_Res(pos,neg,pos_gate,neg_gate,pos_label,neg_label):
	X_all=np.concatenate((pos,neg))
	X_gate=np.concatenate((pos_gate,neg_gate))
	Y=np.concatenate([pos_label,neg_label])
	return X_all, X_gate, Y


######### Tensorflow model
x = tf.placeholder(tf.float32, shape=[None, sec_d])
x_gate = tf.placeholder(tf.float32, shape=[None, 1])
y_ = tf.placeholder(tf.float32, shape=[None, 2])

W_gate=weight_variable([1, first_filter_out])
x_dnase_info =tf.nn.sigmoid(tf.matmul(x_gate, W_gate))


keep_prob1 = tf.placeholder(tf.float32)
### input layer
#print('input layer')
################################
### model!!!
### input layer
x_image0 = tf.reshape(x, [-1,sec_d,1,1])

### First Convolutional Layer
### The First layer will have 32 features for each 6x1 patch.
W_conv1 = weight_variable([filter1_size1, 1, 1, first_filter_out])
b_conv1 = bias_variable([first_filter_out])
x_image0_info = tf.nn.relu(conv2d(x_image0, W_conv1) + b_conv1)

x_dnase_info_reshape=tf.reshape(x_dnase_info,[-1,1,1,first_filter_out])
x_dnase_info_reshape_tile=tf.tile(x_dnase_info_reshape,[1,sec_d/4,1,1])

x_image0_info_gate = x_image0_info * x_dnase_info_reshape_tile

W_conv2 = weight_variable([filter1_size1, 1, first_filter_out, first_filter_out])
b_conv2 = bias_variable([first_filter_out])
x_image0_info_2 = tf.nn.relu(conv2d_1(x_image0_info_gate, W_conv2) + b_conv2)

x_image0_info_gated = x_image0_info_2 

h_conv1_drop = tf.nn.dropout(x_image0_info_gated, keep_prob1)
# Permuting batch_size and n_steps
h_conv1_out = tf.transpose(h_conv1_drop, [1, 0, 2, 3])
# Reshaping to (n_steps*batch_size, n_input)
h_conv1_out = tf.reshape(h_conv1_out, [-1, first_filter_out])
# Split to get a list of 'n_steps' tensors of shape (batch_size, n_input)
h_conv1_out = tf.split(0, sec_d/4/max_pool1, h_conv1_out)
lstm_cell = rnn_cell.BasicLSTMCell(1, forget_bias=1.0)

outputs, states = rnn.rnn(lstm_cell, h_conv1_out, dtype=tf.float32)

W_atten=weight_variable([5, 2])
lstm_tail=tf.concat(1,outputs[-5:])


lstm_tail_gate=lstm_tail
y_conv = tf.matmul(lstm_tail_gate, W_atten)
y_conv_softmax=tf.nn.softmax(y_conv)

#print(y_conv)
### Densely Connected Layer
### a fully-connected layer with 1024 neurons to allow processing on the entire seq. 
cross_entropy = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(y_conv, y_))
train_step = tf.train.AdamOptimizer(training_speed).minimize(cross_entropy)
correct_prediction = tf.equal(tf.argmax(y_conv,1), tf.argmax(y_,1))
accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))



sess = tf.InteractiveSession()
sess.run(tf.initialize_all_variables())
sess.run(tf.initialize_local_variables())


print('Start!!!')

def test(filename):
	saver = tf.train.Saver()
	saver.restore(sess, "trained_lstm_lstm0a.ckpt")
	all_file_matrix=open(filename,'r')
	for files in all_file_matrix:
		file_vector=files.split()[0]
		index_file='/pylon2/mc4s9pp/shaunm/group/dream/index/test_coord_index_at_train.txt'
		seq_file='/pylon2/mc4s9pp/shaunm/group/dream/seq_matrix/win200_split/'+file_vector
		dnase_file='/pylon2/mc4s9pp/shaunm/group/dream/dnase/win200/K562_split/'+file_vector
		data_seq_pos,data_dnase_pos=read_data_sep(index_file,dnase_file,seq_file)
		print('start testing')
		classification=sess.run(y_conv_softmax, feed_dict={x: data_seq_pos, x_gate: data_dnase_pos, keep_prob1: 1.0})
		np.savetxt(file_vector+'.classification_y_conv_result.txt', classification,delimiter='\t')

############################################################################
import getopt
import sys
def main(argv):
	try:
		opts, args = getopt.getopt(argv,"hf:")
	except getopt.GetoptError:
		print('python lstm.test.py')
		sys.exit(2)

	for opt,arg in opts:
		if opt=="-h":
			print('python lstm.test.py')
			sys.exit()
		elif opt=="-f":
			filename=str(arg.strip())
	test(filename)
if __name__=="__main__":
	main(sys.argv[1:])


