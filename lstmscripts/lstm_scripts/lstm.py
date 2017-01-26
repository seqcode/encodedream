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

training_accuracy=open('training_accuracy.txt','w')

all_file_matrix=open('all_file_matrix.txt','r')
file_vector=all_file_matrix.readline().split()#[x.strip() for x in all_file_matrix.readline().split('\t')]
index_file=file_vector[0]
label_file=file_vector[1]
seq_file=file_vector[2]
dnase_file=file_vector[3]
conserve_file=file_vector[4]

print('read data')
data_seq_pos,data_dnase_pos,data_seq_neg,data_dnase_neg=read_data_sep(index_file,label_file,dnase_file,seq_file,conserve_file,5000000)


for records in all_file_matrix:
	try:
		file_vector=all_file_matrix.readline().split()
		index_file=file_vector[0]
		label_file=file_vector[1]
		seq_file=file_vector[2]
		dnase_file=file_vector[3]
		conserve_file=file_vector[4]
		data_seq_pos_1,data_dnase_pos_1,data_seq_neg_1,data_dnase_neg_1=read_data_sep(index_file,label_file,dnase_file,seq_file,conserve_file,500000)
		data_seq_pos=np.concatenate((data_seq_pos,data_seq_pos_1),axis=0)
		data_dnase_pos=np.concatenate((data_dnase_pos,data_dnase_pos_1),axis=0)
		data_seq_neg=np.concatenate((data_seq_neg,data_seq_neg_1),axis=0)
		data_dnase_neg=np.concatenate((data_dnase_neg,data_dnase_neg_1),axis=0)
	except:
		print('Error')
		pass

print('Start!!!')
for i in range(iter_num):
	print(i)
	### mini batch postive part
	index_array_p=np.arange(data_dnase_pos.shape[0])
	np.random.shuffle(index_array_p)
	index_array_p=index_array_p[0:batch_size]
	batch_xs1_p=data_seq_pos[index_array_p,:]
	batch_xs1_gate_p=data_dnase_pos[index_array_p]
	data_label_p=np.repeat([[1,0]],batch_size,axis=0)
	### mini batch negative part
	index_array_n=np.arange(data_dnase_neg.shape[0])
	np.random.shuffle(index_array_n)
	index_array_n=index_array_n[0:batch_size]
	batch_xs1_n=data_seq_neg[index_array_n,:]
	batch_xs1_gate_n=data_dnase_neg[index_array_n]
	data_label_n=np.repeat([[0,1]],batch_size,axis=0)

	print(batch_xs1_p)
	batch_xs1, batch_xs1_gate, batch_ys1 = ToXGY_Res(batch_xs1_p,batch_xs1_n,batch_xs1_gate_p,batch_xs1_gate_n,data_label_p,data_label_n)
	sess.run(train_step, feed_dict={x: batch_xs1, x_gate: batch_xs1_gate, y_: batch_ys1, keep_prob1: 0.5})

	if i%100 == 0:
		train_accuracy1 = accuracy.eval(feed_dict={x:batch_xs1, x_gate: batch_xs1_gate, y_: batch_ys1, keep_prob1: 1.0})
		print("step %d, training accuracy same cell: %g"%(i, train_accuracy1))
		training_accuracy.write(str(i)+':\t'+str(train_accuracy1)+'\n')
		with open("tmp_accurate.txt0a", "a") as tmp_accurate:
			tmp_accurate.write("step %d, training accuracy same cell: %g"%(i, train_accuracy1))
			tmp_accurate.write('\n')

		batch_ys1_indice=np.argsort(batch_ys1)
		y_conv_softmax_result=sess.run(y_conv_softmax, feed_dict={x: batch_xs1, x_gate: batch_xs1_gate, keep_prob1: 1.0})
	train_step.run(feed_dict={x: batch_xs1, x_gate: batch_xs1_gate, y_: batch_ys1, keep_prob1: 0.5})
	### save model every 500 steps
	if i%500 == 0:
		saver = tf.train.Saver()
		save_path = saver.save(sess, "trained_lstm_lstm0a.ckpt")
### save model
saver = tf.train.Saver()
save_path = saver.save(sess, "trained_lstm_lstm0a.ckpt")

