import numpy as np
import pandas as pd
import time
# from TreeNode import TreeNode
# from Serialize import Serializer, DeSerializer
import csv
import sys

def Serializer(Node, fp):

    if Node.isleaf:
        output = Node.label
        writer = csv.writer(fp)
        writer.writerow([output])
        return
    else:
        assert not Node is None


    output = [Node.att_id, Node.split_val]
    writer = csv.writer(fp, delimiter=',')
    writer.writerow(output)

    Serializer(Node.left, fp)
    Serializer(Node.right, fp)


class TreeNode():

    def __init__(self, att_id=None, split_val=None, label=None, isleaf=None):
        self.att_id = att_id
        self.split_val = split_val

        self.isleaf = isleaf
        self.label = label

        self.left = None
        self.right = None


    def set_init(self, att_id, split_val):
        self.att_id = att_id
        self.split_val = split_val


class DecisionTree():
    # minfreq = 20
    image_db = None
    label_db = None
    N_db = 0
    image_unreduced = None

    image = None
    label = None
    N = 0 # the actually used size

    minfreq = None

    col_idx = None
    # boost_att_id = None
    num_att = 0


    def readData(self, filepath, minfreq_input=20):

        file = pd.read_csv(filepath, header=None)
        print('train_file: ', filepath)

        self.image_db = file.iloc[:, 1:].values
        self.label_db = file.iloc[:, 0].values
        self.N_db, self.num_att = self.image_db.shape

        # initialize the current training data
        self.minfreq = minfreq_input

        self.image = self.image_db
        self.label = self.label_db
        self.N = self.image.shape[0]

        self.col_idx = np.arange(self.num_att)
        self.image_unreduced = self.image

    def reduction(self, col_idx=None):
        print('before reduction:', self.image.shape)

        if col_idx is None:
            # rowstd = np.std(X, axis = 0)
            nonzero_row = np.sum(np.where(self.image > 0, 1, 0), axis=0)
            self.col_idx = np.argwhere(nonzero_row > 0)[:, 0]
            # self.col_idx = np.argwhere(nonzero_row > self.minfreq)[:, 0]
            # self.boost_att_id = np.argwhere(nonzero_row < self.minfreq)[:, 0]

        else:
            self.col_idx = col_idx

        self.image = self.image_unreduced[:, self.col_idx]

        self.N, self.num_att = self.image.shape

        print('after reduction:', self.image.shape)


    def find_best_split(self, E_imid, F_att):
        best_impurity = None
        best_split = None
        best_att = None
        best_att_ord = None
        # n_labl = len(E_imid)

        class_table_init = np.zeros((10, 2), dtype=np.int) # left and right
        for lab in self.label[E_imid]:
            class_table_init[int(lab), 1] += 1

        for ord, att in enumerate(F_att):
            cand_splits = self.image[E_imid, att] # 1-1 -> E_imid
            sorted_idx = np.argsort(cand_splits)

            # impurity, split = self.scan_split(E_imid[sorted_idx], cand_splits[sorted_idx])
            att_impurity = None
            att_split = None

            class_table = np.array(class_table_init, copy=True) # pointer vs copy

            last_label = self.label[E_imid[sorted_idx[0]]]
            last_split = cand_splits[sorted_idx[0]]
            # last_label = None
            # last_split = None

            # buffer_labels = {}
            # last_buffer_labels = {}
            has_split_degeneracy = False

            # print('label', 'value' )
            # print('class_table_ CHECK!')
            # print(class_table)

            for sidx in sorted_idx:

                # query!
                imid = E_imid[sidx]
                label = self.label[imid]
                # class_table[label, 0] += 1
                # class_table[label, 1] -= 1

                # do impurity calculation
                if not cand_splits[sidx] == last_split and att_impurity is None:
                    impurity = self.Gini(class_table)
                    att_impurity = impurity
                    att_split = cand_splits[sidx]


                if not cand_splits[sidx] == last_split and not label == last_label \
                    or not cand_splits[sidx] == last_split and label == last_label and has_split_degeneracy:

                    impurity = self.Gini(class_table)

                    if att_impurity is None or att_impurity > impurity:
                        att_impurity = impurity
                        att_split = cand_splits[sidx]

                # update the lasts
                if not cand_splits[sidx] == last_split:
                    last_split = cand_splits[sidx]
                    has_split_degeneracy = False
                else:
                    if not label == last_label:
                    # if split remains the same && label changes, it has split degeneracy. i.e split corresponds to 2 labels
                        has_split_degeneracy = True
                if not label == last_label:
                    last_label = label

                class_table[label, 0] += 1
                class_table[label, 1] -= 1



            # print(ord, att)
            # print('cand_split:', cand_splits, 'label:', E_imid[sorted_idx])
            # print('att_impurity:', att_impurity)
            # print('att_split:', att_split)

            if att_impurity is None:   # e.g. [1569: 0.0, 4968: 0.0]
                continue
            elif best_impurity is None or best_impurity > att_impurity:
                best_impurity = att_impurity
                best_split = att_split
                best_att = att
                best_att_ord = ord

        # print('best_impurity:', best_impurity)
        # print('best_split:', best_split)
        # print('best_att ', best_att)
        # print(ord)


            # for pair in zip(cand_splits[sorted_idx], E_imid[sorted_idx]):
            #     print(pair[0], pair[1])
        assert not best_att is None

        return best_att, best_att_ord, best_split


    def majority_vote(self, E_imid):
        labels = self.label[E_imid]
        hist = np.zeros((10,), dtype=np.int)
        for lb in labels:
            hist[lb] += 1
        return np.argmax(hist)


    def Gini(self, class_table):
        nlnr = np.sum(class_table, 0)
        assert nlnr[0] > 0
        assert nlnr[1] > 0

        class_table_norm = class_table/nlnr
        IlIr = 1 - np.sum(np.power(class_table_norm, 2), 0)
        return np.sum(nlnr * IlIr) / np.sum(nlnr)


    def branching(self, att_id, split_val, E_imid):
        right = np.where(self.image[E_imid, att_id] >= split_val, True, False)
        left = ~right

        return [E_imid[left], E_imid[right]]


    def tree_induction(self, E_imid, F_att):
        # E_imid: a list of img id
        # F_att: a list of att id
        # node: parent tree node

        stopping = False
        pure = False
        if len(E_imid) < self.minfreq:
            # below min_sup in this node
            stopping = True
            # print('below minsup!')
        elif np.all(self.label[E_imid] == self.label[E_imid[0]]):
            # pure node
            stopping = True
            pure = True
            # print('node pure')

        if len(F_att) == 0:
            # out_of_attribute
            stopping = True
            # print('out_of_attrib')


        if stopping:
            # Stopping criterion
            node = TreeNode()
            node.isleaf = True
            if pure:
                node.label = self.label[E_imid[0]]
            else:
                node.label = self.majority_vote(E_imid)
            return node

        else:
            att_id, att_ord, split_val = self.find_best_split(E_imid, F_att)
            if att_id is None:
                node = TreeNode()
                node.label = self.majority_vote(E_imid)
                return node

            F_att = np.delete(F_att, att_ord)

            # print('att_id:{}'.format(att_id), 'split_val {}'.format(split_val))
            # print('support {}'.format(len(E_imid)), '# att {}'.format(len(E_imid)))
            # print('')


            node = TreeNode(att_id, split_val)

            # branching:
            branch_cand = self.branching(att_id, split_val, E_imid)

            # for b in branch_cand:
            #     assert len(b)>0
            #     sorted_id = np.argsort(self.image[E_imid, att_id])
            #     print(self.label[E_imid[sorted_id]])
            #     print(self.image[E_imid[sorted_id], att_id])


            # for E_i in branch_cand:
            node.left = self.tree_induction(branch_cand[0], F_att)
            node.right = self.tree_induction(branch_cand[1], F_att)
            return node


    # def build_tree(self):
    #     E_init = np.arange(self.N)
    #     F_att = np.arange(self.num_att)
    #
    #     att_id, att_ord, split_val = self.find_best_split(E_init, F_att)
    #     np.delete(F_att, att_ord)
    #     # att_id : split_att
    #     # split_val: float32
    #
    #     root = TreeNode(att_id, split_val)
    #
    #     # branching:
    #     branch_cand = self.branching(att_id, split_val, E_init)
    #     # a list of [E_imid]
    #
    #     start_time = time.time()
    #
    #     # for E_i in branch_cand:
    #     self.tree_induction(branch_cand[0], F_att, root.left)
    #     print('left finished, time: {}'.format(time.time() - start_time))
    #     self.tree_induction(branch_cand[1], F_att, root.right)
    #     print('right finished, time: {}'.format(time.time() - start_time))
    #
    #     return root

    def validation(self, root, filename):
        print('Write preds to file: ', filename)

        with open(filename, 'w', newline='\n') as fw:
            writer = csv.writer(fw, delimiter=',')
            for ind in range(self.N):
                label = self.label[ind]
                pred = self.go_to_node(root, ind)
                writer.writerow([label, pred])
                # print([label, pred])



    def go_to_node(self, root, ind):
        node = root
        while node.isleaf is None:
            att = node.att_id
            if self.image[ind, att] < node.split_val:
                node = node.left
            else:
                node = node.right

        return node.label


def main(train_file=None, minfreq=20, model_file=None, argv=sys.argv):
    if not len(argv) == 1:

        if len(argv) == 4:
            train_file = argv[1]
            minfreq = int(argv[2])
            model_file = argv[3]

        else:
            print("Usage:" + '$ python3 dtinduce.py <trainFile> <minfreq> <model_file>')
            print("Eg:" + "$ python3 dtinduce.py ./data/rep2/test.csv 20 demo.csv")
            sys.exit(1)
    elif train_file is None:
        print("Usage:" + '$ python3 dtinduce.py <trainFile> <minfreq> <model_file>')
        print("Eg:" + "$ python3 dtinduce.py ./data/rep2/test.csv 20 demo.csv")
        sys.exit(1)

    start_time = time.time()
    # print(train_file)
    # print(minfreq)
    # print(model_file)

    dt = DecisionTree()
    # train_file = './data/rep2/train.csv'

    dt.readData(train_file, minfreq)
    dt.reduction()

    # dt.find_best_split(np.arange(dt.N), np.arange(dt.num_att))
    # np.random.seed(0)
    # classtable = np.random.randn(3, 2).astype(int)+10

    # classtable = np.ones((2, 2))
    # print(classtable)
    # print(dt.Gini(classtable))

    # E = np.arange(dt.N)
    # F = np.arange(dt.num_att)
    # result = dt.find_best_split(E, F[10:200])
    # print(result)

    # print(E)
    # result = dt.branching(0, dt.image[0, 0], E)
    # print(result[0])
    # print(result[1])
##############################################
# Train and write the tree
    print('training ...')
    E_init = np.arange(dt.N)
    F_att = np.arange(dt.num_att)
    root = dt.tree_induction(E_init, F_att)

    # tree_file = 'demo.csv'
    tree_file = model_file
    with open(tree_file, 'w', newline='\n') as fw:
        writer = csv.writer(fw, delimiter=',')
        writer.writerow(dt.col_idx)
        writer.writerow([dt.minfreq])

        Serializer(root, fw)



# below is in dtclassify.py
##############################################
# Rebuild the tree

    # filename = 'demo.csv'
    # iterator = read_file(filename)
    #
    # eff_col = next(iterator)
    # col_idx = list(map(int, eff_col))
    # root_rebuilt = DeSerializer(iterator)
    # assert next(iterator, None) is None

# ##############################################
# # Test
#     testfile = './data/rep2/train.csv'
#     dtt = DecisionTree()
#     dtt.readData(testfile)
#     dtt.reduction(dt.col_idx)
#
#     dtt.validation(root, 'prediction.csv')
#
# ##############################################


    print('')
    print('tot_time:', time.time() - start_time)


if __name__ == '__main__':

    train_file = './data/rep2/test.csv'
    minfreq = 20
    model_file = 'demo.csv'

    main(train_file, minfreq, model_file)

# tree traverse
# diff to boost find_best_split