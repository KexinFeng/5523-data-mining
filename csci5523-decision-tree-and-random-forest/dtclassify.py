from dtinduce import DecisionTree, TreeNode
# from Serialize import DeSerializer
import csv
import sys


def DeSerializer(iterator):
    pair = next(iterator, None)
    # print('pair= ', pair)
    if pair is None or len(pair) < 2:
        assert not pair is None
        Node = TreeNode(label=int(pair[0]), isleaf=True)
        return Node
    else:
        Node = TreeNode(int(pair[0]), float(pair[1]))
        Node.left = DeSerializer(iterator)
        Node.right = DeSerializer(iterator)
        return Node


def read_file(filename):

    with open(filename, "r") as fr:
        datareader = csv.reader(fr)
        for row in datareader:
            yield row
            # try:
            #     yield row
            # except:
            #     # reach the end
            #     return None



def main(model_file=None, testfile=None, pred_file=None, argv=sys.argv):
    if not len(argv) <= 1:

        if len(argv) == 4:
            model_file = argv[1]
            testfile = argv[2]
            pred_file = argv[3]

        else:
            print("Usage:" + '$ python3 dtclassify.py <model_file> <testfile>, <pred_file>')
            print("Eg:" + "$ python3 dtclassify.py demo.csv ./data/rep2/test.csv prediction.csv")
            sys.exit(1)

    elif model_file is None:
        print("Usage:" + '$ python3 dtclassify.py <model_file> <testfile>, <pred_file>')
        print("Eg:" + "$ python3 dtclassify.py demo.csv ./data/rep2/test.csv prediction.csv")
        sys.exit(1)


# Rebuild the tree
    filename = model_file
    iterator = read_file(filename)

    eff_col = next(iterator)
    col_idx = list(map(int, eff_col))

    minfreq = next(iterator)

    root_rebuilt = DeSerializer(iterator)
    assert next(iterator, None) is None

# Test
    print('testing ...')
    testfile = testfile
    dtt = DecisionTree()
    dtt.readData(testfile, minfreq)
    dtt.reduction(col_idx)

    dtt.validation(root_rebuilt, pred_file)



if __name__ == '__main__':
    model_file = 'demo.csv'
    testfile = './data/rep2/test.csv'
    pred_file = 'prediction.csv'

    main(model_file, testfile, pred_file)
