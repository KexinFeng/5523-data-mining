import numpy as np
from dtinduce import DecisionTree, Serializer
import time, os, sys
import csv
# from Serialize import Serializer

class RForest(DecisionTree):

    def boostrap(self, testfile, pred_file_path, percent=.4, sample_times = 3, minfreq = 20):
        sample_size = int(self.N_db * percent)
        print('testfile: ', testfile)


        dtt = DecisionTree()
        dtt.readData(testfile)

        N_db = self.N_db
        N_att_db = self.num_att

        N_test = dtt.N_db
        num_att_unreduced_test = dtt.num_att

        start_time = time.time()
        tot_start_time = time.time()

        for ord in range(sample_times):
            suffix = 'pred{:03d}'.format(ord) + '.csv'
            print('No. ', ord)

            sample_id = np.random.choice(self.N_db, sample_size ,replace=True)


            # initialize the current training data

            print('before sampling:', self.image_db.shape)
            self.image = self.image_db[sample_id, :]
            self.image_unreduced = self.image

            self.label = self.label_db[sample_id]
            self.N = self.image.shape[0]
            self.minfreq = minfreq
            assert self.image_db.shape[0] == N_db

            self.reduction()
            assert self.image_db.shape[1] == N_att_db

            print('after sampling:', self.image.shape)


            # Train and write the tree
            print('training ...')

            E_init = np.arange(self.N)
            F_att = np.arange(self.num_att)
            print('In use: ', (self.N, self.num_att))

            root = self.tree_induction(E_init, F_att)


            print('time:', time.time() - start_time)
            print('')
            start_time = time.time()

            # Write tree to file
            model_file = pred_file_path + 'Serilized_tree_for_' + suffix
            tree_file = model_file
            with open(tree_file, 'w', newline='\n') as fw:
                writer = csv.writer(fw, delimiter=',')
                writer.writerow(self.col_idx)
                writer.writerow([self.minfreq])

                Serializer(root, fw)


            # Make prediction and write to file
            print('testing ...')
            dtt.reduction(self.col_idx)
            assert dtt.image_unreduced.shape[0] == N_test
            assert dtt.image_unreduced.shape[1] == num_att_unreduced_test
            dtt.validation(root, pred_file_path + '/' + suffix)


            print('time:', time.time() - start_time)
            print('')

            start_time = time.time()

        print('\n total time: ', time.time() - tot_start_time)


def main(train_file=None, test_file=None, sample_times=10, minfreq=20, argv = sys.argv, pred_file_path='./pred_files/'):

    if not len(argv) == 1:

        if len(argv) == 5:
            train_file = argv[1]
            test_file = argv[2]
            sample_times = int(argv[3])
            minfreq = int(argv[4])

        else:
            print("Usage:" + '$ python3 rf.py <trainFile> <testFile> <sample_times> <minfreq>')
            print("Eg:" + "$ python3 rf.py ./data/rep2/test.csv ./data/rep2/test.csv 3 20")
            sys.exit(1)
    elif train_file is None:
        print("Usage:" + '$ python3 rf.py <trainFile> <testFile> <sample_times> <minfreq>')
        print("Eg:" + "$ python3 rf.py ./data/rep2/test.csv ./data/rep2/test.csv 3 20")
        sys.exit(1)

    try:
        os.mkdir(pred_file_path)
    except:
        pass

    RF = RForest()
    RF.readData(train_file)

    RF.boostrap(test_file, pred_file_path, sample_times=sample_times, minfreq=minfreq)

if __name__ == '__main__':

    rep = 2
    test_file ='./data/rep{}/test.csv'.format(rep)
    train_file = './data/rep{}/test.csv'.format(rep)

    # output_path = './preds/'
    pred_file_path = './pred_files/'

    try:
        # os.mkdir(output_path)
        os.mkdir(pred_file_path)
    except:
        pass


    main(train_file, test_file, pred_file_path=pred_file_path)