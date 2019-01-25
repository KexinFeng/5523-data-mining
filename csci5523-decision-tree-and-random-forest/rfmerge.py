import numpy as np
import csv, sys
import pandas as pd


def max_freq_static(array, keys = np.arange(10, dtype=int)):
    counts = np.zeros_like(keys)
    for key in keys:
        counts[key] = len(array[array==key])
    idx_max = np.argmax(counts)
    return keys[idx_max], counts[idx_max]



def main(pred_file_path=None, forest_pred_file=None, sample_times=3, argv=sys.argv):

    if not len(argv) == 1:

        if len(argv) == 3:
            forest_pred_file = argv[1]
            sample_times = int(argv[2])

        else:
            print("Usage:" + '$ python3 rfmerge.py <predictions> <sample_times>')
            print("Eg:" + "$ python3 rfmerge.py forest_prediction.csv 3")
            sys.exit(1)
    elif pred_file_path is None:
        print("Usage:" + '$ python3 rfmerge.py <predictions> <sample_times>')
        print("Eg:" + "$ python3 rfmerge.py forest_prediction.csv 3")
        sys.exit(1)


    votes = None
    pred = None
    initialized = False

    for ord in range(sample_times):
        pred_file = pred_file_path +'/' + 'pred{:03d}'.format(ord) + '.csv'
        # # file_iterator = read_file(pred_file)
        # with open(pred_file, 'r', newline='\n') as fr:
        #     reader = csv.reader(fr)

        pred = pd.read_csv(pred_file, header=None)

        if not initialized:
            N_test = pred.shape[0]
            votes = np.zeros((N_test, sample_times), dtype=int)
            initialized = True

        votes[:, ord] = pred.iloc[:, 1]

    labels = pred.iloc[:, 0]

    with open(forest_pred_file, 'w', newline='\n') as fw:
        print('Writing forest_pred to file: ', forest_pred_file)
        writer = csv.writer(fw, delimiter=',')
        for ord, label in enumerate(labels):
            predict_class, _ = max_freq_static(votes[ord,:])
            writer.writerow([label, predict_class])

    print('Written to file: ', forest_pred_file)



if __name__ == "__main__":

    rep = 2
    # train_file = './data/rep{}/train.csv'.format(rep)
    # test_file = './data/rep{}/test.csv'.format(rep)

    pred_file_path = './pred_files/'

    forest_pred_file = './preds/forest_pred_rep{}.csv'.format(rep)

    sample_times = 3

    main( pred_file_path, forest_pred_file, sample_times)
