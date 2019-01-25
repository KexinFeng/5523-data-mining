import numpy as np
from bs4 import BeautifulSoup
import os, time, sys
import fnmatch
import codecs
import html
from nltk import PorterStemmer
import math


class data():
    def __init__(self, id, content):
        self.id = id
        self.content = content

class Document():
    def __init__(self, id, label, content):
        self.id = id
        self.label = label
        self.content = content


class Database():

    def __init__(self):
        self.freqmap = {} # label -> frequency
        self.articles = {} # label -> topic ie list[data( id, content)]

        self.freq_articles = {}  # label -> topic ie list[data(id, content)]
        # self.topic_finder = {} # article_id -> string

        self.article_db = []

        self.word_list = [] # list of stem, int -> string
        self.freqlabels = []  # list of string int ->string
        self.topic_id = {} # string(topic) -> int
        self.stem_id = {} # string(stem) -> int

    def reduction(self):
        # find the top 20 frequent topics
        # sort through topics

        labels = self.freqmap.keys()
        self.freqlabels = sorted(labels, key=lambda label: self.freqmap.get(label), reverse=True)[0:20]
        # self.freq_articles = {k: self.articles.get(k) for k in self.freqlabels}
        for idx, k in enumerate(self.freqlabels):
            self.freq_articles.update({k: self.articles.get(k)})
            # for id in self.articles.get(k):
            #     self.topic_finder.update({id: k})
            self.topic_id.update({k: idx})

        self.articles = None
        self.freqmap = None

    def trimer(self):
        # scan the topic and within each topic scan the articles
        # for each article trim the token and char

        token_freqmap = {}
        ps = PorterStemmer()


        with open("stoplist.txt", 'r') as fr:
            content_tmp = fr.readlines()
            l = []
            lines = [x.strip('\n') for x in content_tmp]
            for line in lines:
                terms = line.split()
                for term in terms:
                    l.append(term)
            stop_list = l
            l = None
            content_tmp = None
            lines = None

        f_class = open('reuters21578.class', 'w')
        # *.class
        # f_class maps article_id to topic_names

        for topic_name, topic in self.freq_articles.items():

            for art in topic:
                token_list = []
                char_list = []
                for c in art.content:
                    if ord(c) >= 128:
                        pass
                    elif not c.isalnum():
                        char_list.append(" ")
                    else:
                        char_list.append(c.lower())
                content = "".join(char_list)
                tokens = content.split()

                for token in tokens:
                    if not token.isdigit() and not token in stop_list:
                        # assert type(token) == str
                        # if type(token) == str:
                            # print(token)
                            # print(type(token))
                        token = html.unescape(token)
                        token = ps.stem(token)

                        token_freqmap.setdefault(token, 0)
                        token_freqmap[token] += 1

                        token_list.append(token)

                self.article_db.append(Document(art.id, self.topic_id[topic_name], token_list))
                f_class.write(str(art.id) + ',' + topic_name + '\n')

        f_class.close()

        token_frq_thrsh = 5
        self.word_list = self.cutoff_token(token_freqmap, token_frq_thrsh)
        # self.dictionaray = np.array(self.word_list)


    def word2vec(self, option='1'):
        # scan the whole articles
        # for each article scan the word

        dim = len(self.word_list)
        N = len(self.article_db)
        # doc_vec = np.empty((N, dim))
        doc_vec = []

        self.N = N
        self.dim = dim

        freq_csv = open('freq.csv', 'w')
        sqrt_csv = open('sqrtfreq.csv', 'w')
        log2_csv = open('log2freq.csv', 'w')


        for doc in self.article_db:
            stem_freq = self.histcount(doc.content, element_set=self.word_list)
            # stem: count

            # freq.csv: (art_id, stem_id, freq)



            # l1 = np.zeros(dim, dtype=float)
            # l2 = np.zeros(dim, dtype=float)
            # l3 = np.zeros(dim, dtype=float)

            l1 = []
            l2 = []
            l3 = []

            # for stem in stem_freq.keys():
            #     idx = self.stem_id[stem]
            #     count = stem_freq[stem]
            #     l1[idx] = count
            #     if count > 0:
            #         l2[idx] = (1 + count ** (1.0 / 2)) ** 2
            #         l3[idx] = (1 + math.log(count, 2)) ** 2

            for stem in stem_freq.keys():
                idx = self.stem_id[stem]
                count = stem_freq[stem]
                l1.append(count**2)
                if count > 0:
                    l2.append((1 + math.sqrt(count)) ** 2)
                    l3.append((1 + math.log(count, 2)) ** 2)

            modu1 = math.sqrt(sum(l1))
            modu2 = math.sqrt(sum(l2))
            modu3 = math.sqrt(sum(l3))

            # for idx in range(dim):
            for stem in stem_freq.keys():
                idx = self.stem_id[stem]
                count = stem_freq[stem]

                assert count != 0
                # assert l1[idx] != 0
                # artid_stemid_freq1 = str(doc.id) + ',' + str(idx) + ',' + str(l1[idx] / modu1) + '\n'
                artid_stemid_freq1 = str(doc.id) + ',' + str(idx) + ',' + str(count / modu1) + '\n'
                freq_csv.write(artid_stemid_freq1)
                # assert l2[idx] != 0
                # artid_stemid_freq2 = str(doc.id) + ',' + str(idx) + ',' + str((1 + math.sqrt(count)) / modu2) + '\n'
                artid_stemid_freq2 = str(doc.id) + ',' + str(idx) + ',' + str((1 + math.sqrt(count)) / modu2) + '\n'
                sqrt_csv.write(artid_stemid_freq2)
                # assert l3[idx] != 0
                artid_stemid_freq3 = str(doc.id) + ',' + str(idx) + ',' + str((1 + math.log2(count)) / modu3) + '\n'

                log2_csv.write(artid_stemid_freq3)

            # doc_vec.append(Document(doc.id, doc.label, tmp.tolist()))

        freq_csv.close()
        sqrt_csv.close()
        log2_csv.close()

        # doc_vec[:, :] = doc_vec[:, :] / np.linalg.norm(doc_vec[:, :], axis=1).reshape((-1, 1))
        # self.doc_vec = doc_vec


    def write2file(self):
        # with open('database.csv', 'w') as fw:
        #     for doc in self.doc_vec:
        #         fw.write(str(doc.id) + ',' + str(doc.label) + ',' + str(doc.content).strip('[]') + '\n')
        # with open('label_id2topic.csv', 'w') as fw:
        #     for idx, topic in enumerate(self.freqlabels):
        #         fw.write(str(idx) + ',' + topic + '\n')

        # reuters.class:
        # reuters21578.clabel: stem_id -> stem
        # self.word_list = [] # list of stem, int <-> string
        # self.freqlabels = [] # list of topics int <-> string



        with open('reuters21578.clabel', 'w') as f_stem:
            # *.clabel
            # f_stem: map stem_id -> stems

            for stem_id, stem in enumerate(self.word_list):
                f_stem.write(str(stem_id) + ',' + stem + '\n')

        return


    @staticmethod
    def histcount(alist, element_set=None, topcount=-1):
        freq_map = {}
        for e in alist:
            if element_set is None or e in element_set:
                freq_map.setdefault(e, 0)
                freq_map[e] += 1
            else:
                continue

        topcount = int(topcount)
        if topcount != -1:
            sorted_keys = sorted(freq_map.keys(), key=lambda k: freq_map.get(k), reverse=True)
            sorted_keys = sorted_keys[:topcount]
            output = {}
            for k in sorted_keys:
                output.update({k: freq_map[k]})
            freq_map = output

        return freq_map


    def cutoff_token(self, map, thrsh):
        word_list = []
        id = 0
        for token, frq in map.items():
            if frq < thrsh:
                pass
            else:
                word_list.append(token)
                self.stem_id[token] = id
                id += 1
        return word_list



class DataSelect():
    def __init__(self):
        self.db = Database()

    def sgmreader(self, path):
        if not os.path.exists(path):
            raise ValueError('The folder ' + path + " doesn't exists")

        self.data_path = path
        count = 0
        tot_topcount = 0
        for root, dirs, files in os.walk(path):
            for file in fnmatch.filter(files, '*.sgm'):
                tot_topcount += self.doc_reader(root + '/' + file)
                count += 1
        print('')
        print('File parsed: ', count)
        # print('Using BeautifulSoup, ')
        # print('total of article' , tot_topcount)

    def doc_reader(self, file):
        # read file
        with codecs.open(file, 'r', encoding='utf-8', errors='ignore') as fr:
            str = fr.read()
            str = str.replace("BODY>", "CONTENT>")
            soup = BeautifulSoup(str, features='lxml') # The default is 'lxml'
            topic_count = 0
            # for article in soup.find_all("reuters", topics='YES'):
            for article in soup.find_all('reuters'):

                if article.topics is None or len(article.topics) != 1:
                    pass
                else:
                    # if article.get('topics') == 'NO' or article.get('topics') is None:
                    #     # query on those whose topics!='Yes' but article.topics has length == 1
                    #     dbstop = 1;

                    if article.content == None:
                        # There is pathological event whose text tag has attribute 'type' = 'BRIEF' or 'UNRPOC'
                        # They have no dateline tag, no body tag, ('BRIEF' has title tag, 'UNRPOC' doesn't), but just a bunch of string.
                        # 'BRIEF' can be discarded. 'UNPROC' may be retained.

                        # printout = article.find('text').get('type')
                        # print('The text type =', printout)
                        pass
                    else:
                        topic_count += 1

                        label = article.topics.d.string

                        self.db.freqmap.setdefault(label, 0)
                        self.db.freqmap[label] += 1

                        self.db.articles.setdefault(label, [])
                        d = data(int(article.get('newid')), article.content.string)
                        self.db.articles[label].append(d)

            print('topic_count= ', topic_count)

        return topic_count





def main(argv=sys.argv):

    start = time.time()

    print('Begin preproc.')
    preproc = DataSelect()
    path = './reuters21578'
    preproc.sgmreader(path)

    # print('begin reduction')
    preproc.db.reduction()

    end = time.time()
    # print("reduction time:", end - start, 's')
    # print("")


    # time.sleep(0.001)

    start = time.time()
    preproc.db.trimer()
    # print('len of word_list:', len(preproc.db.word_list))
    # items = preproc.db.article_db
    # for n, item in enumerate(items):
    #     if n > 10:
    #         break
    #     else:
    #         print(item.content)
    #         print('')

    end = time.time()
    # print("trimer time:", end - start, 's')
    # print("")


    # time.sleep(0.001)

    start = time.time()

    preproc.db.word2vec(option= '1')

    # print(preproc.db.doc_vec[0].content)
    # assert np.allclose(np.linalg.norm(preproc.db.doc_vec[:, 1:], axis=1), np.ones(preproc.db.N, dtype=float) )

    end = time.time()
    # print("word2vec time:", end - start, 's')

    preproc.db.write2file()


    # dbstop = 0





if __name__ == '__main__':
    main()