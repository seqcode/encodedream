
import tensorflow as tf
import numpy as np
#import sklearn.metrics
import os
import sys

# define reading here
# NUM_OF_EVAL_CYCLES = 27 # calculate this from the data size/batch size

# Read data into queues:
# Read data into queues:

label_bytes = 2
image_bytes = 200
record_bytes = label_bytes + image_bytes
n = 50

def read_single_example(filename, record_bytes, label_bytes, image_bytes):
    # construct a queue for all the files.
    filename_queue = tf.train.string_input_producer([filename],
                                                    num_epochs = 1,
                                                    name = "myQueue")
    # create a reader. 
    reader = tf.FixedLengthRecordReader(record_bytes=record_bytes)
    key, value = reader.read(filename_queue)
    record_bytes = tf.decode_raw(value, tf.uint8)
    label = tf.cast(
      tf.slice(record_bytes, [0], [label_bytes]), tf.float32)
    example = tf.cast(
      tf.slice(record_bytes, [2], [image_bytes]), tf.float32)
    return example, label

# get single examples
example, label = read_single_example(sys.argv[1], record_bytes, label_bytes,
    image_bytes)
# groups examples into batches randomly
examples_batch, labels_batch = tf.train.batch(
    [example, label], batch_size=60000,
    capacity=150000, allow_smaller_final_batch=True)

# create the graph here. 

#x  = tf.placeholder(tf.float32, shape = (None, n*4)) # One-hot, so n*4
#y_ = tf.placeholder(tf.float32, shape = (None, 2))   # k = no_of_classes = 2

keep_prob = tf.placeholder(tf.float32)

def variables(shapeW, shapeB, nameW, nameB):
    """ Defining the initial convolution """
    initialW = tf.truncated_normal( shapeW , stddev=0.1)
    initialB = tf.constant(0.1, shape=shapeB)
    return tf.Variable(initialW, name = nameW), tf.Variable(initialB, name = nameB)

def conv2d(x, W):
    return tf.nn.conv2d(x, W, strides=[1, 1, 1, 1], padding='SAME')

def max_pool(x):
    return tf.nn.max_pool(x, ksize=[1, 4, 50, 1],
                        strides=[1, 4, 50, 1], padding='SAME')

def nn(xd,yd,zd,bd,fc_nodes):
    """ convolve over feature matrix """

    W_conv1, b_conv1 = variables([xd,yd,zd,bd], [bd], 'W_conv1', 'b_conv1')
    x_image = tf.reshape(examples_batch, [-1, xd, n, 1])
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

# Calculating Loss

def run_conv(xd,yd,zd,bd,fc_nodes,dropout):
    """Running a CNN"""
    
    y_conv = nn( xd, yd, zd, bd, fc_nodes) # Set variables.
    y_conv_softmax = tf.nn.softmax(y_conv) # For auROC/auPRC calculations.
    
    init = tf.initialize_all_variables()
    init_local = tf.initialize_local_variables()
    saver = tf.train.Saver()

    with tf.Session() as sess:
        
        print "In session"
        sess.run(init)
        sess.run(init_local)
        saver.restore(sess, "trained_model.ckpt")
        # Start enque threads:
        coord = tf.train.Coordinator()
        threads = tf.train.start_queue_runners(sess=sess, coord = coord)
        steps = 0
        try:
          while not coord.should_stop():
            steps = steps + 1
            temp = y_conv_softmax.eval(feed_dict = { keep_prob: 1})
            if steps == 1:
                pred = temp
            else:
                pred = np.vstack((pred,temp))
        except tf.errors.OutOfRangeError:
          print "Reached End"
        finally:
          coord.request_stop()

        # Wait for the threads to finish
        coord.join(threads)
        sess.close()

    with tf.Session() as sess2:

        print "In session"
        sess2.run(init)
        sess2.run(init_local)
        coord = tf.train.Coordinator()
        threads = tf.train.start_queue_runners(sess=sess2, coord = coord)
        outf = sys.argv[2]
        steps = 0
        try:
          while not coord.should_stop():
            steps += 1
            print "Evaluating batch %s" % steps
            temp = labels_batch.eval()
            if steps == 1:
              test_labels = temp
              #print test_labels[:10]
            else:
              test_labels = np.vstack((test_labels,temp))
        except tf.errors.OutOfRangeError:
          print "Reached End"
        finally:
          coord.request_stop()

        coord.join(threads)
        sess.close()
    
    with open(outf, "a") as fout:
        #fout.write( "Fixing convolution size at 25 and # filters at 32\n")
        #fout.write( "Model Parameters: dropout: %s\n" % dropout)
        #print sklearn.metrics.roc_auc_score(test_labels,pred)
        print len(test_labels)
        print len(pred)
        np.savetxt("labels.txt",test_labels)
        np.savetxt("predictions.txt", pred)
        #fout.write( "AuROC: %s\t" % sklearn.metrics.roc_auc_score(test_labels,pred))
        #fout.write( "AuPRC: %s\n" % sklearn.metrics.average_precision_score(test_labels, pred))

run_conv(4,25,1,32, 256, 0.5)

# END
