import numpy as np
# from dtclassify import read_file
import sys, csv

def read_file(filename):

    with open(filename, "r") as fr:
        datareader = csv.reader(fr)
        for row in datareader:
            yield row

def ConfusionM(pred_file):
    confM = np.zeros((10, 10), dtype=np.int)

    file_iterator = read_file(pred_file)
    while 1:
        # (true label, prediction)
        pair = next(file_iterator, None)
        if pair is None:
            break
        else:
            confM[int(pair[0]), int(pair[1])] += 1

    total = np.sum(confM)
    # accuracies = np.zeros(10)
    # for i in range(10):
    #     accuracies[i] = confM[i, i]/totals[i]
    # tot_accuracy = np.sum(accuracies * totals) / np.sum(totals)

    tot_accuracy = np.sum(np.diag(confM)) / total
    # print('totals:', totals)
    # print('N=', np.sum(totals))
    # print(confM)

    print('Total number: ', total)

    print('The confusion matrix:')
    print('        Predictions')
    firstline = ['Label']+ [str(x) for x in range(10)]
    print('\t'.join('{:^4}'.format(x) for x in firstline))
    for idx in range(10):
        stream = [str(idx)]
        for x in confM[idx, :]:
            stream.append(x)
        print('\t'.join('{:^4}'.format(x) for x in stream))

    print("The total accuracy: {:.2f}".format(tot_accuracy))


    return tot_accuracy


def main(pred_file=None, argv=sys.argv):
    if not len(argv) <= 1:

        if len(argv) == 2:
            pred_file = argv[1]

        else:
            print("Usage:" + '$ python3 showconfmatrix.py <pred_file>')
            print("Eg:" + "$ python3 showconfmatrix.py prediction.csv")
            sys.exit(1)
    elif pred_file is None:
        print("Usage:" + '$ python3 showconfmatrix.py <pred_file>')
        print("Eg:" + "$ python3 showconfmatrix.py prediction.csv")
        sys.exit(1)


# Confusion Matrix and accuracy

    pred_file = pred_file
    return ConfusionM(pred_file)



if __name__ == '__main__':
    rep = 2
    forest_pred_file = './preds/forest_pred_rep{}.csv'.format(rep)

    # for ind in range(3):
    #     print('\n', ind)
    #     file = './preds/rep{}/pred00{}.csv'.format(rep, ind)
    #
    #     main(pred_file=file)

    main(forest_pred_file)