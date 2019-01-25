
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.HashSet;
import java.util.Set;




public class hcrminer {
    public static void main(String[] args) {
        int minsup = Integer.parseInt(args[0]);
        double minconf = Double.parseDouble(args[1]);
        String inputfile = args[2];
        String outputfile = args[3];
        String option = args[4];

//        String inputfile = "small";
//        String outputfile =  inputfile + "_" + petree.minsup + "_" + petree.minconf + "_" + petree.option + ".txt";

        System.out.println("minSup: "+ minsup);
        System.out.println("minConf: "+ minconf);
        System.out.println("option: "+ option);
        System.out.println("");



        PET petree = new PET();
        petree.minconf = minconf;
        petree.option = option;

        petree.minsup = minsup;

        petree.hwOutput = true;



        long startTime = System.currentTimeMillis();

//        try {
//            FileWriter fw = new FileWriter(outputfile + "tempt print.txt");
//            BufferedWriter bw = new BufferedWriter(fw);


            // petree.build: get treeRoot
            List<List<String>> db = petree.readData(inputfile);
            System.out.println("db.size= " + db.size());

            TNode treeRoot = new TNode("null");
            treeRoot.prefix = new LinkedList<>();

            petree.build(db, treeRoot);



            long endTime = System.currentTimeMillis();
            float timeTaken = (float) (endTime - startTime) / 1000;

//            System.out.println("Tree traversing ends");
//            System.out.println("Time passed: " + (float) (endTime - startTime) / 1000 + "s");
            System.out.println("Frequent itemset number= " + petree.FIS.size() );




//            bw.write("Frequent itemset number= " + petree.FIS.size() +"\r\n");
//            bw.write( "Time passed = " + timeTaken + " s\r\n");
//            bw.newLine();




            // petree.printResult: store the Accociation rules in container.
            List<Rule> container = new LinkedList<>();
            petree.ARbuilder(treeRoot, outputfile, container);
            int ARcount = container.size();

            if (minsup >20) {
                System.out.println("Association rule amount: " + ARcount);
            }

            long endTime2 = System.currentTimeMillis();
//            System.out.println("2nd taken total: " + (float) (endTime2 - endTime) / 1000 + "s");
            System.out.println("Total time taken: " + (float) (endTime2 - startTime) / 1000 + "s");

            System.out.println("");



//            bw.write("Association rules number = " + ARcount + "\r\n");
////            bw.write("2nd time taken  = " + (float) (endTime2 - endTime) / 1000 + "s\r\n");
//            bw.write("Total time taken =" + (float)(endTime2 - startTime)/1000 + "s\r\n");
//            bw.newLine();

//            bw.close();
//            fw.close();
//        }catch(IOException e){
//            System.err.println("IOException; " + e);
//        }


    }



}

 class PET {

    public int minsup;
    public double minconf;
    public String option;
    public Map<String, Integer> ordering = new HashMap<>();
    public Map<String, Integer> revOrdering = new HashMap<>();
    public List<ItemSet> FIS = new LinkedList<>();


    public boolean hwOutput = true;

    public  Comparator<String> COMPARE_BY_NAME_INT = new Comparator<String>() {
        @Override
        public int compare(String one, String other) {
            return Integer.parseInt(one) - Integer.parseInt(other);
        }
    };
    public  Comparator<String> COMPARE_BY_NAME_REVINT = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return Integer.parseInt(o2) - Integer.parseInt(o1);
        }
    };

    public Comparator<String> COMPARE_BY_NEW_ORDERING = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return ordering.get(o1) - ordering.get(o2);
        }
    };

    public Comparator<String> COMPARE_BY_ORDERING = new Comparator<String>() {
        public int compare(String one, String other) {
            return ordering.get(one) > ordering.get(other) ? 1 : ordering.get(one) > ordering.get(other) ? -1 : 0;
        }
    };

    public Comparator<String> COMPARE_BY_REVORDERING = new Comparator<String>() {
        public int compare(String one, String other) {
            return revOrdering.get(one) > revOrdering.get(other) ? 1 : revOrdering.get(one) > revOrdering.get(other) ? -1 : 0;
        }
    };


    public List<List<String>> readData(String filename) {
        final String dir = System.getProperty("user.dir");
        String path = dir;
        List<List<String>> transaction = new LinkedList<>();


        try {
            FileReader fin = new FileReader(path + "/"+filename);
            BufferedReader br = new BufferedReader(fin);
//            FileWriter fw = new FileWriter(path + "transaction.txt");
//            System.out.println(path + "."+"\"+filenam);

            String line;
            String transationNumber = "0";
            List<String> items = new LinkedList<>();

            while ((line = br.readLine()) != null) {

                String[] str = line.split(" ");
                if (str[0].equals(transationNumber)) {
                    items.add(str[1]);
                } else {
                    transaction.add(items);
                    transationNumber = str[0];
                    items = new LinkedList<>();
                    items.add(str[1]);
                }
            }

//            for (List<String> aline : transaction) {
//                fw.write(aline + "\r\n");
//            }

            fin.close();
            br.close();
//            fw.close();
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFound: " + e);
        } catch (IOException e) {
            System.err.println("IOException; " + e);
        }

        List<TNode> r = rankTheFrequent(transaction, "frequent");
//        Map<String, Integer> map = new HashMap<>();

        for (int i = 0; i < r.size(); i++) {
            ordering.put(r.get(i).name, r.size() - 1 - i);
            revOrdering.put(r.get(i).name, i);
        }

        return transaction;
    }

    public ArrayList<TNode> rankTheFrequent(List<List<String>> db, String pruningOption) {
        Map<String, TNode> dir = new HashMap<>();
        ArrayList<TNode> itemRank = new ArrayList<TNode>();

        boolean flag;
        switch (pruningOption) {
            case "all":
                flag = true;
                break;
            case "frequent":
                flag = false;
                break;
            default:
                flag = false;
        }

        if (!db.isEmpty()) {
            for (List<String> t : db) {
                for (String s : t) {
                    if (!dir.keySet().contains(s)) {
                        TNode node = new TNode(s);
                        dir.put(s, node);
                    } else {
                        dir.get(s).countIncrease();
                    }
                }
            }

            Set<String> keys = dir.keySet();
            for (String item : keys) {
                TNode node = dir.get(item);
                if (flag || node.count >= minsup) {
                    itemRank.add(node);
                }
            }
            Collections.sort(itemRank);

            return itemRank;
        } else {
            return null;
        }


    }


    public void buildFPTree(List<List<String>> db, List<TNode> can, String smallerName, boolean needToPrune) {


        TNode root = new TNode("null");
        root.count = 0;
        root.children = new LinkedList<>();


        for (List<String> t : db) {
            if (!needToPrune) {

                LinkedList<String> tt = new LinkedList<>(t);
                switch (option) {
                    case "1":
                        tt.sort(COMPARE_BY_NAME_INT);
                        break;
                    case "2":
                        tt.sort(new Comparator<String>() {
                            @Override
                            public int compare(String o1, String o2) {
                                return revOrdering.get(o1) - revOrdering.get(o2);
                            }
                        });

                        break;
                    case "3":
                        Collections.sort(tt, new Comparator<String>() {
                            @Override
                            public int compare(String o1, String o2) {
                                return ordering.get(o1) - ordering.get(o2);
                            }
                        });
                        break;
                    default:
                        Collections.sort(tt, new Comparator<String>() {
                            @Override
                            public int compare(String o1, String o2) {
                                return revOrdering.get(o1) - revOrdering.get(o2);
                            }
                        });
                }


                addFPNode(root, tt, can, smallerName);

            } else {
                LinkedList<String> tt = pruneAndSort(t);

//                Collections.reverse(tt);

                addFPNode(root, tt, can, smallerName);
            }

        }
        root = null;

        return;
    }


    public void addFPNode(TNode node, LinkedList<String> sortedItemSet, List<TNode> headsValues, String smallerName) {
        if (sortedItemSet.isEmpty()) {
            return;
        }

        String nameToAdd = sortedItemSet.poll();

        TNode index = null;
        for (TNode n : headsValues) {
            if (n.name.equals(nameToAdd)) {
                index = n;
                break;
            }
        }

        if (index != null) {

            TNode localNode = node.getChild(nameToAdd);
            if (localNode != null) {
                localNode.countIncrease();
            } else {
                localNode = new TNode(nameToAdd);
                localNode.children = new LinkedList<>();

                node.children.add(localNode);
                localNode.parent = node;


                // update the heads list, when a new node is added.
                // The beginning part guarantees that here index is not null.

                TNode pointer;
                if ((pointer = index) != null) {
                    while (pointer.next != null) {
                        pointer = pointer.next;
                    }
                    pointer.next = localNode;
                }


            }


            if (index != null && index.name.equals(smallerName)) {
                return;
            }

            addFPNode(localNode, sortedItemSet, headsValues, smallerName);
        } else {
            addFPNode(node, sortedItemSet, headsValues, smallerName);
        }

    }

    public LinkedList<String> pruneAndSort(List<String> transRecord) {
        LinkedList<String> temp = new LinkedList<>();
        for (String item : transRecord) {
            if (ordering.get(item) != null) {
                temp.add(item);
            }
        }

        switch (option) {
            case "1":
                temp.sort(COMPARE_BY_NAME_INT);
                break;
            case "2":
                Collections.sort(temp, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return revOrdering.get(o1) - revOrdering.get(o2);
                    }
                });
                break;
            case "3":
                Collections.sort(temp, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return ordering.get(o1) - ordering.get(o2);
                    }
                });
                break;
            default:
                Collections.sort(temp, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return ordering.get(o1) - ordering.get(o2);
                    }
                });
        }

        return temp;
    }

    public List<String> optionalSort(List<String> transRecord) {


        switch (option) {
            case "1":
                transRecord.sort(COMPARE_BY_NAME_INT);
                break;
            case "2":
                Collections.sort(transRecord, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return revOrdering.get(o1) - revOrdering.get(o2);
                    }
                });
                break;
            case "3":
                Collections.sort(transRecord, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return ordering.get(o1) - ordering.get(o2);
                    }
                });
                break;
            default:
                Collections.sort(transRecord, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return ordering.get(o1) - ordering.get(o2);
                    }
                });
        }

        return transRecord;
    }


    public List<List<String>> treePDB(TNode headnode) {
        List<List<String>> pdb = new LinkedList<>();


        TNode leaf = headnode.next;

        while (leaf != null) {

            TNode pointer = leaf.parent;
            int amount = leaf.count;

            List<String> tempTransaction = new LinkedList<>();
            while (pointer.parent != null) {
                // pointer.parent could be root with name "null".
                tempTransaction.add(pointer.name);
                pointer = pointer.parent;
            }

            while (amount > 0) {
                pdb.add(tempTransaction);
                amount--;
            }

            tempTransaction = null;

            leaf = leaf.next;

        }
        return pdb;
    }


    public void pet(List<List<String>> db, TNode node, List<String> nodeItems) {

        if (db.size() <= 0) {
            return;
        }

        List<TNode> candidates = rankTheFrequent(db, "frequent");
//        Map<String, TNode> heads = new HashMap<>();

//        for (TNode c : candidates){
//            heads.put(c.name, new TNode(c.name));
//        }


        node.children = candidates;


        buildFPTree(db, candidates, node.name, false);

        db = null;


        for (TNode child : node.children) {

            child.parent = node;


            List<String> nodeItemsToAdd = new LinkedList<>(nodeItems);

            nodeItemsToAdd.add(child.name);
            FIS.add(new ItemSet(nodeItemsToAdd, child.count));

            List<List<String>> pdb = treePDB(child);

            pet(pdb, child, nodeItemsToAdd);

            pdb = null;
        }

//        heads = null;
        candidates = null;

        return;
    }

    public void build(List<List<String>> db, TNode root) {

        if (db.size() <= 0) {
            return;
        }

        List<TNode> candidates = rankTheFrequent(db, "frequent");
        Map<String, TNode> heads = new HashMap<>();

        for (TNode c : candidates) {
            heads.put(c.name, new TNode(c.name));
        }


        root.children = candidates;

        int flag = 0;

        buildFPTree(db, candidates, root.name, true);

        db = null;

//        List<ItemSet> test = traversePET(tree);

        for (TNode child : candidates) {

//            child.prefix =  root.prefix;
//            child.prefix.add(root.name);
            child.parent = root;

            List<String> temp = new LinkedList<>();
            temp.add(child.name);
            FIS.add(new ItemSet(temp, child.count));
            temp = null;


            List<List<String>> pdb = treePDB(child);
            List<String> childnameToAdd = new LinkedList<String>();
            childnameToAdd.add(child.name);

            pet(pdb, child, childnameToAdd);
            pdb = null;
            childnameToAdd = null;


//            if (flag == 0) {
//                System.out.println("total root child: " + candidates.size());
//            }
//
//            flag++;
//            System.out.println("current child" + flag);

            pdb = null;
        }
    }


    public void visitTree(TNode node, List<String> prefix, List<ItemSet> output, List<ItemSet> outputDebug) {
        if (node == null) {
            return;
        }

        List<String> frequenSet = new LinkedList<>(prefix);
        frequenSet.add(0, node.name);

        ItemSet temp = new ItemSet();


        temp.setElem(frequenSet, node.count);
        outputDebug.add(temp);


        for (TNode child : node.children) {
            visitTree(child, frequenSet, output, outputDebug);
        }

        frequenSet = null;
        temp = null;

    }

    public void traversePET(TNode root, LinkedList<ItemSet> output, LinkedList<ItemSet> outputDebug) {
//        List<ItemSet> output = new LinkedList<>();
//        List<ItemSet> outputDebug = new LinkedList<>();

        List<String> prefix = new LinkedList<>();

        for (TNode child : root.children) {
            visitTree(child, prefix, output, outputDebug);
        }

        return;
    }


    public void ARbuilder(TNode root, String filename, List<Rule> container) {
        final String dir = System.getProperty("user.dir");
        String path = dir + "/";

        Path p = Paths.get(path);
        try {
            if (!Files.exists(p)) {
                Files.createDirectories(p);
            }
        } catch (IOException e) {
            System.err.println("IOException; " + e);
        }


        try {
            FileWriter fw = new FileWriter(path + filename);
            BufferedWriter bw = new BufferedWriter(fw);
//            int count = 0;

            for (ItemSet i : FIS) {

                if (i.items.size() > 1) {


                    genAsociateRule(i, root, bw, container);
//                    count++;
//                    if (count % 1e4 == 0) {
//                        System.out.println("The number of set processed: " + count / 1e4 + "e4");
//                    }


                }
            }

            bw.close();
            fw.close();
        } catch (IOException e) {
            System.err.println("IOException; " + e);
        }

    }


    public void genAsociateRule(ItemSet set, TNode tree, BufferedWriter bw, List<Rule> container) {
        int size = set.items.size();
        int nodeCount = set.count;

//        if (size > 3 ){
//            int debug = 1;
//        }

        List<List<String>> candidatesH = convertToList(set.items);

        if (minsup > 20) {


            try {

                for (int k = 1; k < size && !candidatesH.isEmpty(); k++) {

                    Iterator<List<String>> itOfH = candidatesH.iterator();

//                for (List<String> conseq : candidatesH) {
                    while (itOfH.hasNext()) {
                        List<String> conseq = itOfH.next();

                        List<String> compSet = new LinkedList<>(set.items);
                        compSet.removeAll(conseq);

                        float conf = nodeCount / supCount(compSet, tree);

                        if (conf > minconf) {

                            String toPrint = new String();

                            Iterator<String> it = compSet.iterator();
                            while (it.hasNext()) {
                                toPrint += it.next() + " ";
                            }
                            toPrint += " | ";

                            Iterator<String> it2 = conseq.iterator();
                            while (it2.hasNext()) {
                                toPrint += it2.next() + " ";
                            }
                            toPrint += " | " + nodeCount + " | " + conf;

                            container.add(new Rule(compSet, conseq));
                            if (hwOutput) {
                                bw.write(toPrint);
                                bw.newLine();
                            }
                        } else {
//                        candidatesH.remove(conseq);
                            itOfH.remove();
                        }
                    }

                    candidatesH = conseqGen(candidatesH);
//                    System.out.println("current k = " + k);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {

                String toPrint = new String();

                Set<String> frequentItemSet = set.items;
                Iterator<String> it = frequentItemSet.iterator();
                while (it.hasNext()) {
                    toPrint += it.next() + " ";
                }
                toPrint += " | ";

                //                            Iterator<String> it2 = conseq.iterator();
                //                            while (it2.hasNext()) {
                //                                toPrint += it2.next() + " ";
                //                            }
                toPrint += " {} | " + nodeCount + " | " + "-1";

                if (hwOutput) {
                    bw.write(toPrint);
                    bw.newLine();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    public List<List<String>> convertToList(Set<String> H) {
        List<List<String>> output = new LinkedList<>();
        for (String s : H) {
//            Set<String> tempt = new HashSet<String>();
//            tempt.add(s);
//            output.add(tempt);
            output.add(new LinkedList<>(Arrays.asList(s)));
        }
        return output;
    }


    public List<List<String>> conseqGen(List<List<String>> H) {   //Ordering test needed!!
        // Merging F_k * F_k to get F_k+1;

        List<List<String>> Houtput = new LinkedList<>();

        if (H.size() < 2) {
            return Houtput;
        }

        int Hscope = H.size();
        int k = H.get(0).size();

        for (int i = 0; i < Hscope; i++) {
            for (int j = i + 1; j < Hscope; j++) {
//                List<String> aa = optionalSort(H.get(i));
//                List<String> bb= optionalSort(H.get(j));

                List<String> a = H.get(i);
                List<String> b = H.get(j);

//                for (int index = 0; index <aa.size(); index++){
//                    if (aa.get(index).equals(a.get(index))){
//                        continue;
//                    }else{
//                        int debug = 1;
//                    }
//                }
//                for (int index = 0; index <bb.size(); index++){
//                    if (bb.get(index).equals(b.get(index))){
//                        continue;
//                    }else{
//                        int debug = 1;
//                    }
//                }

                int idx = 0;
                while (a.get(idx).equals(b.get(idx)) && idx < k - 1) {
                    idx++;
                }

                if (idx == k - 1) {

                    List<String> temp = new LinkedList<>();
                    temp.addAll(a);
                    temp.add(b.get(b.size() - 1));


                    temp = optionalSort(temp);
                    Houtput.add(temp);
//                    }
                }
            }
        }

        return Houtput;

    }

    public float supCount(List<String> items, TNode tree) {

        LinkedList<String> path = new LinkedList<>(items);

        switch (option) {
            case "1":
                path.sort(COMPARE_BY_NAME_REVINT);
                break;
            case "2":
                Collections.sort(path, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return ordering.get(o1) - ordering.get(o2);
                    }
                });
                break;
            case "3":
                Collections.sort(path, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return revOrdering.get(o1) - revOrdering.get(o2);
                    }
                });
                break;
            default:
                Collections.sort(path, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return revOrdering.get(o1) - revOrdering.get(o2);
                    }
                });
        }

        TNode node = tree;
        while (path.size() > 0) {
            String item = path.poll();
            node = node.getChild(item);
        }
        int supCount = node.count;

        return supCount;
    }


}

 class ItemSet {

    public Set<String> items;
    public int count;

    public ItemSet(){
    }

    public ItemSet(List<String> t, int c){
        this.items = new HashSet<String>(t);
        this.count = c;
    }

    public void setElem(List<String> t, int c){
        this.items = new HashSet<String>(t);
        this.count = c;
    }

}

 class Rule {
    public List<String> LHS;
    public List<String> RHS;

    Rule(List<String>comp, List<String> conseq){
        this.LHS = comp;
        this.RHS = conseq;
    };

}

 class TNode implements Comparable<TNode> {
    public String name;
    public int count;
    public TNode parent;
    public TNode next;

    public List<TNode> children;
    public List<String> prefix;

    public TNode( String name ){
        this.name = name;
        this.count = 1;
    }

    public void countIncrease(){
        this.count++;
    }

    public TNode getChild(String name) {

        if (children == null){
            return null;
        }

        for (TNode child : children){
            if (name.equals(child.name)){
                return child;
            }
        }
        return null;
    }

    @Override
    public int compareTo(TNode node){
        int count2 = node.count;
        return count2 - this.count;
    }

}
