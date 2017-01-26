""" Train amd save a single layer convolutional neural network """

# Run as:
# python train_network.py INPUT_FILE SEQLEN

#execfile('/opt/packages/TensorFlow/TensorFlow_0.8.0/TensorFlowEnv/bin/activate_this.py', dict(__file__='/opt/packages/TensorFlow/TensorFlow_0.8.0/TensorFlowEnv/bin/activate_this.py'))

import sys
import tensorflow as tf
import numpy as np
#import sklearn.metrics
import read

# Assuming that these features are in their one-hot representations. 

train_features, train_labels = read.get_matrix(sys.argv[1])
n = int(sys.argv[2])

# Creating placeholders for the features (x) , and the labels (y).

x  = tf.placeholder(tf.float32, shape = (None, n*4)) # One-hot, so n*4
y_ = tf.placeholder(tf.float32, shape = (None, 2))   # k = no_of_classes = 2
keep_prob = tf.placeholder(tf.float32)

def variables(shapeW, shapeB, nameW, nameB):
    """ Defining the initial convolution """
    initialW = tf.truncated_normal( shapeW , stddev=0.1)
    initialB = tf.constant(0.1, shape=shapeB)
    return tf.Variable(initialW, name = nameW), tf.Variable(initialB, name = nameB)

def conv2d(x, W):
    return tf.nn.conv2d(x, W, strides=[1, 1, 1, 1], padding='SAME')

def max_pool(x):
    return tf.nn.max_pool(x, ksize=[1, 4, n, 1],
                        strides=[1, 4, n, 1], padding='SAME')

def nn(xd,yd,zd,bd,fc_nodes):
    """ convolve over feature matrix """

    W_conv1, b_conv1 = variables([xd,yd,zd,bd], [bd], 'W_conv1', 'b_conv1')
    x_image = tf.reshape(x, [-1, xd, n, 1])
    h_conv1 = tf.nn.relu(conv2d(x_image, W_conv1) + b_conv1)    
    h_pool1 = max_pool(h_conv1)

    # do a fully connected layer
    W_fc1, b_fc1 = variables( [1 * 1 * bd,fc_nodes], [fc_nodes], 'W_fc1', 'b_fc1') 
    h_pool1_flat = tf.reshape(h_pool1, [-1, 1 * 1 * bd])
    h_fc1 = tf.nn.relu(tf.matmul(h_pool1_flat, W_fc1) + b_fc1)

    # dropout
    h_fc1_drop = tf.nn.dropout(h_fc1, keep_prob)

    # final layer
    W_fc2, b_fc2 = variables([fc_nodes, 2], [2], 'W_fc2', 'b_fc2')
    y_conv = tf.matmul(h_fc1_drop, W_fc2) + b_fc2

    return y_conv


def run_conv(xd,yd,zd,bd,fc_nodes,dropout):
    """ calculating loss & training """
    y_conv = nn( xd, yd, zd, bd, fc_nodes)
    y_conv_softmax = tf.nn.softmax(y_conv) # for auROC/auPRC calculations.
    
    cross_entropy = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(y_conv, y_))
    train_step = tf.train.AdamOptimizer(1e-4).minimize(cross_entropy)
    correct_prediction = tf.equal(tf.argmax(y_conv,1), tf.argmax(y_,1))
    accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

    init = tf.initialize_all_variables()
    saver = tf.train.Saver()

    # running the tensorflow graph - training

    with tf.Session() as sess:
        
        print "In session"
        sess.run(init)
        batch_size = 64
    
        for i in range(20000):

            idx = np.random.choice(np.arange(len(train_labels)), batch_size, replace = False)
            batch_xs, batch_ys = train_features[idx], train_labels[idx]
            
            if i%100 == 0:

                train_accuracy = accuracy.eval(feed_dict={ x: batch_xs, y_: batch_ys, keep_prob: 1})
                print("step %d, training accuracy %g"%(i, train_accuracy))
            
            # Training step.
            sess.run(train_step, feed_dict = { x: batch_xs, y_ : batch_ys, keep_prob: dropout})
        save_path = saver.save(sess, "trained_model.ckpt")

run_conv(4,25,1,32, 256, 0.5)
