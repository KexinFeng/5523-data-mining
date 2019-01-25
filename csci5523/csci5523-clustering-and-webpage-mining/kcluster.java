import java.util.*;
import java.io.*;

public class kcluster   {

    public static void main(String[] args){
        if (args.length > 0) {

            String inputFile = args[0];
            String critFunc = args[1];
            String classFile = args[2];
            int clusterNum = Integer.parseInt(args[3]);
            int trialNum = Integer.parseInt(args[4]);
            String outputFile = args[5];

            if (!(critFunc.equals("I2") || critFunc.equals("E1") || critFunc.equals("SSE"))) {
                System.out.println("Please check the input of your criterion function. Use I2, E1 or SSE.");
                System.exit(1);
            }


            try {
//            String inputFile = "freq.csv";
//            String critFunc = "I2";
//            String classFile = "reuters21578.class";
//            Integer clusterNum = 5;
//            Integer trialNum = 5;
//            String outputFile = "output.txt";

                IncKmeans km = new IncKmeans();
                km.doKmeans(inputFile, critFunc, classFile, clusterNum, trialNum, outputFile);

//            double obj_val = km.bestObjFun;
//            double entropy = km.globalEntropy;
//            double purity = km.globalPurity;
//            double runtime = km.runtime;

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("Please check your input!");
            System.out.println("Usage: $ kcluster input-file criterion-function class-file #cluster #trials output-file");
        }


    }

}

class IncKmeans{
    // global variables

    // To randomly select the articles:
    private Random rand = new Random();
    private int minId = 0;
    private int maxId;
    private static final int[] seeds = {1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,39};

    // To find the best trial: best objFun, Map<String, Integer> OutputCluster,
    public double bestObjFun = 0.0;
    private double currentObjFun = 0.0;
    private Map<Integer, List<Integer>> finalCluster; // clusterId -> list(ids)
    private String criterionFun;
    private Map<String, Double> D = null;
    private double threstep ;

    double currentEntropy = 0.0;
    double currentPurity = 0.0;

    double runtime = 0.0;
    double globalEntropy = 0.0;
    double globalPurity = 0.0;

    // read:
    private Map<String, Map<String, Double>> readIn(String filePath){
        Map<String, Map<String, Double>> database = new HashMap<>();
        String entree;

        try{
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            while((entree = br.readLine()) != null){
                String[] art_dim_freq = entree.split(",");

                if(database.containsKey(art_dim_freq[0])){
//                    Map<String, Double> newEntree = new HashMap<>();
//                    newEntree.put(art_dim_freq[1], Double.parseDouble(art_dim_freq[2]));
                    database.get(art_dim_freq[0]).put(art_dim_freq[1], Double.parseDouble(art_dim_freq[2]));
                }else{
                    Map<String, Double> newEntree = new HashMap<>();
                    newEntree.put(art_dim_freq[1], Double.parseDouble(art_dim_freq[2]));
                    database.put(art_dim_freq[0], newEntree);
                }
            }
            br.close();
        } catch (IOException e){
            e.printStackTrace();
        }

        return database;
    }
    // Submain:
    public void doKmeans(String inputFile, String critFun, String classFile, Integer clusterNum, Integer trialNum, String outputFile) {
        // Read data:  Map<String, Map<String, Double>> data
        Map<String, Map<String, Double>> database = readIn(inputFile);
        maxId = database.size() - 1;
        criterionFun = critFun;
        Map<String, Integer> bestResult = null;





        System.out.println("The database size = " + database.size());

        if (critFun.equals("E1")){
            // sum over all inputData;
            Map<String, Double> Dtot = new TreeMap<>();
            for (String key : database.keySet()){
                Dtot = supposition(Dtot, 1, database.get(key), 1);
            }

            double norm = 0.0;
            for(double val: Dtot.values()){
                norm += val * val;
            }
            norm = Math.sqrt(norm);

            D = new TreeMap<>();
            for (String key: Dtot.keySet()){
                D.put(key, Dtot.get(key) / norm);
            }
            Dtot = null;
        }

        switch (critFun){
            case "SSE":
                threstep = 0.008;
                threstep = 0.01;
                break;
            case "I2":
//                threstep = 0.003;
                threstep = 0.01;
                break;
            case "E1":
                threstep = 0.0008;
                threstep = 0.01;
                break;
        }

        long startTimeTot = System.nanoTime();
        for (int trial=0; trial < trialNum; trial++){
            int seed = seeds[trial];
            rand.setSeed(seed);
            System.out.println("");
            System.out.println("Trial No. " + trial);


            long startTime=System.nanoTime();   //Beginning
            Map<String, Integer> result = cluster(database, clusterNum);
            long endTime=System.nanoTime(); //Ending
            System.out.println("Running time this trial: "+(endTime-startTime)/1e9+"s");

//            double currentRuntime = (endTime-startTime)/1e9;

            if( seed == 1 || currentObjFun > bestObjFun){
                bestResult = result;
                bestObjFun = currentObjFun;
//                runtime = currentRuntime;
            }
        }

        long endTimeTot = System.nanoTime();
        runtime = (endTimeTot-startTimeTot)/1e9;

        System.out.println("");
        System.out.println("Completed.");

        writeOut(bestResult, outputFile);

        EntroPurity(bestResult, classFile);

        System.out.println("");
        System.out.println("The best objective function : " + bestObjFun);
        System.out.println("The total entropy = " + globalEntropy);
        System.out.println("The total purity = " + globalPurity);
        System.out.println("The total runtime for "+ trialNum.toString() + " trials: " + runtime +"(s)");

    }


    // Initialization:
    private Map<Integer, Centroid> init_centroids(int clu_num, Map<String, Map<String, Double>> inputData, String[] artIdArray){
        Map<Integer, Centroid> centMap = new TreeMap<>();
        for(int i=0; i<clu_num; i++){
            int ord = getRandomArt();
            Map<String, Double> vec = inputData.get(artIdArray[ord]);
            centMap.put(i, new Centroid(vec));
        }
        return centMap;
    }

    public List<Integer> RandomizeArray(int a, int b){
        Random randgen = rand;  // Random number generator
        int size = b-a+1;
        List<Integer> l = new ArrayList<>();

        for(int i=0; i< size; i++){
            l.add( a + i);
        }

//        for (int i=0; i<array.length; i++) {
//            int randp = randgen.nextInt(array.length);
//            int temp = array[i];
//            array[i] = array[randp];
//            array[randp] = temp;
//        }

        Collections.shuffle(l, randgen);

        return l;
    }

    private void init_assignment(int[] centAssignment, Map<Integer, Centroid> centMap, Map<String,Map<String,Double>> inputData, String[] artIdArray){
        int clusterNum = centMap.size();
        int N = inputData.size();
        double total_obj = 0.0;

        // For termination:
        Map<Integer, Centroid> last_centMap = new TreeMap<>();
        for (int key: centMap.keySet()){
            last_centMap.put(key, new Centroid( centMap.get(key) ) );
        }
        double current_obj;
        double last_obj = 0.0;
        int stat_rate = 100;
        double stopping = threstep;


        // Loop begins:
        List<Integer> rand_ord = RandomizeArray(minId, maxId);
        for (int ord = 0; ord < N; ord ++ ){
            int art_ord = rand_ord.get(ord);
            String art_id = artIdArray[art_ord];

            double max_sim = 0.0;
            int goto_clu = 0;

//            long startTime=System.nanoTime();   //Beginning
            // scan over the clusters to get the Max_sim
            for (int clu_id=0; clu_id< centMap.keySet().size(); clu_id++){
                double sim = 0.0;

                switch (criterionFun){
                    case "SSE": sim = L2Sim(inputData.get(art_id), centMap.get(clu_id).vector);
                        break;
                    case "I2":  sim = cosSim(inputData.get(art_id), centMap.get(clu_id).getVector());
                        break;
                    case "E1":  sim = -seperation(inputData.get(art_id), centMap.get(clu_id), null);
                        break;
                }

                // Find the centroid to update:
                if (clu_id == 0 || sim > max_sim) {
                    max_sim = sim;
                    goto_clu = clu_id;
                }
            }
//            long endTime=System.nanoTime(); //Ending
//            System.out.println("scan over the clu: "+(endTime-startTime)/1e6+"ms");
//
//            startTime= System.nanoTime();//Beginning
            // update artList and assignment
            centAssignment[art_ord] = goto_clu;
            centMap.get(goto_clu).addArt(art_ord);


            // update centroid vector:
            int to_clu = goto_clu;

            Map<String, Double> centVec2 = centMap.get(to_clu).getVector();
            int num2 = centMap.get(to_clu).amount + 1;
            Map<String, Double> d0 = inputData.get(artIdArray[art_ord]);

            switch (criterionFun){
                case "SSE":
                    centVec2 = supposition(centVec2 , (double)(num2)/(num2 + 1) , d0, (double)1/(num2 + 1) );
                    centMap.get(to_clu).updateCent(centVec2);
                    break;
                case "I2":
                    centVec2 = supposition(centVec2 , 1 , d0, 1 );
                    centMap.get(to_clu).updateCent(centVec2);
                    centMap.get(to_clu).calNorm();
                    break;
                case "E1":
                    centVec2 = supposition(centVec2, 1, d0, 1);
                    centMap.get(to_clu).updateCent((centVec2));
                    centMap.get(to_clu).calNorm();
                    break;

            }
//            endTime=System.nanoTime(); //Ending
//            System.out.println("update centroid vec: "+(endTime-startTime)/1e6+"ms");
//
//            startTime= System.nanoTime();//Beginning
            // Termination Condition:
            if (Math.floorMod(ord, stat_rate) == 0) {
                double cent_change = 0.0;
                double obj_increase = 0.0;

                for (int i = 0; i<clusterNum; i++){
                    switch (criterionFun){
                        case "SSE":
                            cent_change += L2Sim(centMap.get(i).getVector(), last_centMap.get(i).getVector());
                            break;
                        case "I2":
                            cent_change += 1 - cosSim(centMap.get(i).getVector(), last_centMap.get(i).getVector());
                            break;
                        case "E1":
                            cent_change += 1 - cosSim(centMap.get(i).getVector(), last_centMap.get(i).getVector());
                            break;
                    }

                }
                cent_change = Math.abs( cent_change ) / clusterNum;

//                current_obj = objectiveFun(centMap, inputData, artIdArray);
//                obj_increase = current_obj - last_obj;

//                System.out.println("");
//                System.out.println("ave_cent_change in " + stat_rate + " iterations: " + cent_change);
//                System.out.println("Obj_change in " + stat_rate + " iterations: " + obj_increase);

                if ( ord > N || cent_change < stopping){

//                    System.out.println("cent_change < stopping? " + (cent_change < stopping));

                    break;
                }

                last_centMap = new TreeMap<>();
                for (int key: centMap.keySet()){
                    last_centMap.put(key, new Centroid( centMap.get(key) ) );  // vs  last_centMap.put(key, centMap.get(key));
                }
//                last_obj = current_obj;
            }
//            endTime=System.nanoTime(); //Ending
//            System.out.println("termination: "+(endTime-startTime)/1e6+"ms");




            // objective function
//            if (Math.floorMod(ord, 50) == 0) {
//                for (int clu : centMap.keySet()) {
//                    update_obj(clu, centMap, inputData, artIdArray);
//                    total_obj += centMap.get(clu).obj;
//                }
//                System.out.println("each iteration objective funtion: " + total_obj);
//            }

        }


        // Objective funtion
//        for (int clu : centMap.keySet()){
//            update_obj(clu, centMap, inputData, artIdArray );
//            total_obj += centMap.get(clu).obj;
//        }
//        total_obj = objectiveFun(centMap, inputData, artIdArray);
//        System.out.println("inital objective funtion: " + total_obj);
//        return total_obj;
    }


    // Objective function:
//    private void update_obj(int changed_clu, Map<Integer, Centroid> centMap,  Map<String,Map<String,Double>> inputData, String[] artIdArray ){
//
//        double sum = 0.0;
//
//        Centroid c = centMap.get(changed_clu);
//
//        for (int art_ord : c.getArtList()){
//            switch (criterionFun){
//                case "SSE":
//                    sum += L2Sim(c.vector, inputData.get(artIdArray[art_ord]) );
//                    break;
//                case "I2":
//                    sum += cosSim(c.vector, inputData.get(artIdArray[art_ord]) );
//                    break;
//                case "E2":
//                    break;
//            }
//
//        }
//
//        c.obj = sum;
//
//    }

    private double objectiveFun(Map<Integer, Centroid> centMap, Map<String,Map<String,Double>> inputData, String[] artIdArray){
        double sum = 0.0;
        switch (criterionFun){
            case "SSE":
                for (int clu : centMap.keySet()){
                    Centroid c = centMap.get(clu);
                    for (int art_ord : c.getArtList()){
                        sum += Math.pow(L2Sim(c.vector, inputData.get(artIdArray[art_ord]) ), 2);
                    }
//                    update_obj(clu, centMap, inputData, artIdArray );
//                    sum += centMap.get(clu).obj;
                }
                break;
            case "I2":
                for (Centroid c: centMap.values()){
//                    c.calNorm();
//                    double tmp = c.norm;
//                    c.calNorm();
//                    if (tmp != c.norm){
//                        throw new RuntimeException("c.norm not updated in time!");
//                    }
                    sum += c.norm;
                }
                break;
            case "E1":
                for (Centroid c: centMap.values()){
                    sum += c.amount*cosSim(c.vector, D);
                }

                break;
        }

        return sum;
    }


    // Similarities:
    private double cosSim(Map<String, Double> v1, Map<String, Double> v2){
        Set<String> dimensions = new HashSet<>(v1.keySet());
        dimensions.addAll(v2.keySet());

        double product = 0;
        double norm1 = 0;
        double norm2 = 0;

        double val1;
        double val2;
        for(String key : dimensions) {
//            val1 = v1.containsKey(key) ? v1.get(key) : 0;
            if (v1.containsKey(key)){
                val1 = v1.get(key);
            }else {
                val1 = 0;
            }
            val2 = v2.containsKey(key) ? v2.get(key) : 0;
            product += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }
        return product / (Math.sqrt(norm1 * norm2));
    }

    private double L2Sim(Map<String, Double> v1, Map<String, Double> v2){
        Set<String> dimensions = new HashSet<>(v1.keySet());
        dimensions.addAll(v2.keySet());
        double result = 0;

        for(String key : dimensions) {
            double val1 = v1.containsKey(key) ? v1.get(key) : 0;
            double val2 = v2.containsKey(key) ? v2.get(key) : 0;

            result += Math.pow(val1 - val2, 2);
        }

        return -Math.sqrt(result);

    }

    private double seperation(Map<String, Double> d, Centroid C2, Centroid C1){
        double seperation;
//        if (C1 == null){
//            Map<String, Double> D2 = C2.vector;
//            seperation = C2.amount * cosSim(supposition(D2, 1, d, 1), D) + C2.amount * cosSim(D2, D);
//        }else{
//            if (C1 == C2){
//                int n = C1.amount;
//                Map<String, Double> D1 = C1.vector;
//                Map<String, Double> D2 = C2.vector;
//                if (D1 != D2) {
//                    throw new RuntimeException("C1 == C2 but D1 != D2");
//                }
//                seperation = n*cosSim(D1, D) - (n-1) * cosSim(supposition(D1,1,d,-1), D);
//            }else {
//                Map<String, Double> D2 = C2.vector;
//                seperation = C2.amount * cosSim(supposition(D2, 1, d, 1), D) + C2.amount * cosSim(D2, D);
//            }
//        }
//        return seperation;


        Map<String, Double> D2 = C2.vector;
        int n2 = C2.amount;
        double D2Norm = C2.norm;

        Set<String> dimensions = new HashSet<>(d.keySet());
        dimensions.addAll(D2.keySet());
        dimensions.addAll(D.keySet());

        if (C1 == null || C1 != C2) {

//            seperation = C2.amount * cosSim(supposition(D2, 1, d, 1), D) + C2.amount * cosSim(D2, D);

            double term1 = 0;
            double term2 = 0;
            double newNorm = 0;

            double vald;
            double valD2;
            double valD;
            for (String key : dimensions) {
                vald = d.containsKey(key) ? d.get(key) : 0;
                valD2 = D2.containsKey(key) ? D2.get(key) : 0;
                valD = D.containsKey(key) ? D.get(key) : 0;

                term1 += (n2 + 1) * valD2 * valD + vald * valD;
                term2 += n2 * valD2 * valD;

                newNorm += (valD2 + vald) * (valD2 + vald);

            }
            newNorm = Math.sqrt(newNorm);
            seperation = term1/newNorm - term2/D2Norm;

            return seperation;
        }else{

            double term1 = 0;
            double term2 = 0;
            double newNorm = 0;

            double vald;
            double valD2;
            double valD;
            for (String key : dimensions) {
                vald = d.containsKey(key) ? d.get(key) : 0;
                valD2 = D2.containsKey(key) ? D2.get(key) : 0;
                valD = D.containsKey(key) ? D.get(key) : 0;

                term1 += n2 * valD2 * valD;
                term2 += (n2 - 1) * valD2 * valD - vald * valD;


                newNorm += (valD2 - vald) * (valD2 - vald);

            }
            newNorm = Math.sqrt(newNorm);
            seperation = term1/D2Norm - term2/newNorm;

            return seperation;
        }

    }



    //Perform clustering:
    public Map<String, Integer> cluster(Map<String, Map<String, Double>> inputData, Integer clusterNum) {
        // Initialization: Centroid Map<Integer, Map<String, Double>>
        int N = inputData.size();
        List<String> artIdList = new ArrayList<>(inputData.keySet());
        String[] artIdArray = new String[N];

        for (int i=0; i<N; i++){
            artIdArray[i] = artIdList.get(i);
        }

//        TreeMap<Integer, List<Integer>> clu2ArtList = new TreeMap<>();
//        Map<Integer, Map<String, Double>> cent = new TreeMap<>();

        Map<Integer, Centroid> centMap = null;
        Map<String, Integer> result = null;
        int[] centroidAssignment = new int[N] ;
        for (int i =0; i<N; i++){
            centroidAssignment[i] = -1;
        }


        centMap = init_centroids(clusterNum, inputData, artIdArray);

//        for (int clu : centMap.keySet()){
//
//            centMap.get(clu).calNorm();
//            double norm_test = centMap.get(clu).norm;
//            System.out.println("norm = "+ norm_test+ " clu: "+ clu  );
//            System.out.println("vector: " + centMap.get(clu).vector);
//        }

        init_assignment(centroidAssignment, centMap, inputData, artIdArray);

//        // debug:
//        int sum_centMap = 0;
//        int sum_centroiAssi = 0;
//        int count_clu;
//        for (int key : centMap.keySet()){
//            count_clu= centMap.get(key).artIdList.size();
//            for (int i = 0; i< N; i++){
//                if (centroidAssignment[i] == key) {
//                    sum_centroiAssi += 1;
//                }
//            }
//            if (count_clu != sum_centroiAssi){
//                int dbstop = 0;
//            }
//            sum_centroiAssi = 0;
//            sum_centMap += count_clu;
//        }




//        System.out.println("");
//        for (int clu : centMap.keySet()){
//            centMap.get(clu).calNorm();
//            double norm_test = centMap.get(clu).norm;
//            System.out.println("norm = "+ norm_test+ " clu: "+ clu  );
//        }

//
//        double obj_fun = 0.0;
//        double last_obj_fun = 0.0;
//        double last_stat_obj_fun = 0.0;

        // Termination initials:
        Map<Integer, Centroid> last_centMap = new TreeMap<>();
        for (int key: centMap.keySet()){
            last_centMap.put(key, new Centroid( centMap.get(key) ) );
        }
        double current_obj = 0.0;

        int it = 0;
        int stat_rate = 100;
        int max_step = 100;
        double stopping = threstep;


        while(true){
            it ++;
//            System.out.println("iteration: " + it);

            // radomly choose a point;
            int art_ord = getRandomArt();


            // Update the assignment;
            boolean updated = update_assignment(art_ord, centroidAssignment, centMap, inputData, artIdArray, "interm");


//            if(updated){
//                System.out.println("Attension! updated! ");
//                System.out.println("");
//            }


            // termination condition:
            if (Math.floorMod(it, stat_rate) == 0) {
                double cent_change = 0.0;
                double obj_increase = 0.0;

                for (int i = 0; i<clusterNum; i++){
                    switch (criterionFun){
                        case "SSE":
                            cent_change += L2Sim(centMap.get(i).getVector(), last_centMap.get(i).getVector());
                            break;
                        case "I2":
                            cent_change += 1 - cosSim(centMap.get(i).getVector(), last_centMap.get(i).getVector());
                            break;
                        case "E1":
                            cent_change += 1 - cosSim(centMap.get(i).getVector(), last_centMap.get(i).getVector());
                            break;
                    }

                }
                cent_change = Math.abs( cent_change ) / clusterNum;
//                current_obj = objectiveFun(centMap, inputData, artIdArray);
//                obj_increase = current_obj - last_obj;

//                System.out.println("");
//                System.out.println("ave_cent_change in " + stat_rate + " iterations: " + cent_change);
//                System.out.println("Obj_change in " + stat_rate + " iterations: " + obj_increase);

                if ( it > max_step || cent_change < stopping){

//                    System.out.println("cent_change < stopping? " + (cent_change < stopping));
//                    System.out.println("Converged!");
//                    System.out.println("");
                    break;
                }

//                last_centMap = new TreeMap<>();
//                for (int key: centMap.keySet()){
//                    last_centMap.put(key, centMap.get(key));
//                }

                last_centMap = new TreeMap<>();
                for (int key: centMap.keySet()){
                    last_centMap.put(key, new Centroid( centMap.get(key) ) );
                }

//                last_obj = current_obj;

            }

        }

//        FinalAssignment(centroidAssignment, centMap, criterionFun);
        for (int i=0; i<N; i++){
            update_assignment(i, centroidAssignment, centMap, inputData, artIdArray, "fin");
        }

//        // debug:
//        int tot_count = 0;
//        for(int key : centMap.keySet()){
//            tot_count += centMap.get(key).artIdList.size();
//        }
//        if (tot_count != N){
//            int dbstop2 = 2;
//        }


        currentObjFun = objectiveFun(centMap, inputData, artIdArray);
        System.out.println("final objective function: " + currentObjFun);

        // find the best objective function:
        if (finalCluster == null || (currentObjFun > bestObjFun)) {
            TreeMap<Integer, List<Integer>> clu2ArtList = new TreeMap<>();
            for (int clu_id=0; clu_id < clusterNum; clu_id ++){
//                clu2ArtList.put(clu_id, centMap.get(clu_id).getArtList());
                clu2ArtList.put(clu_id, new ArrayList<>(centMap.get(clu_id).getArtList()));
            }
            finalCluster = clu2ArtList;
        }

        result = new TreeMap<>();
        for (int i=0; i < N; i++){
            result.put(artIdArray[i], centroidAssignment[i]);
        }

        return result;
    }

    private boolean update_assignment(int art_ord, int[] centAssignment, Map<Integer, Centroid> centMap, Map<String,Map<String,Double>> inputData, String[] artIdArray, String fin_interm){
        int clusterNum = centMap.size();
        int N = inputData.size();
        String art_id = artIdArray[art_ord];
//        double[] similarity = new double[clusterNum];
        int current_clu = centAssignment[art_ord];

//        double currensim = L2Sim(inputData.get(art_id), centMap.get(current_clu).getVector());
//        System.out.println("current sim = " + currensim);



        double max_sim = 0.0;
        int goto_clu = 0;


        // scan over the clusters to get the Max_sim
        for (int clu_id = 0; clu_id< centMap.keySet().size(); clu_id++){

            double sim;

            switch (criterionFun){
                case "SSE":
                    sim = L2Sim(inputData.get(art_id), centMap.get(clu_id).vector);
//                    System.out.println("sim = " + sim);
                    break;
                case "I2":
                    sim = cosSim(inputData.get(art_id), centMap.get(clu_id).getVector());
//                    System.out.println("sim = " + sim);
                    break;
                case "E1":
                    sim = - seperation(inputData.get(art_id), centMap.get(clu_id), centMap.get(centAssignment[art_ord]) );
                    break;
                default: sim = cosSim(inputData.get(art_id), centMap.get(clu_id).getVector());
            }


            // Find the centroid to update:
            if (clu_id == 0 || sim > max_sim) {
                max_sim = sim;
                goto_clu = clu_id;
            }
        }

        if (goto_clu == current_clu && fin_interm.equals("interm")){
//            System.out.println(currensim + " vs " + max_sim);
            return false; // don't need to update;

        } else{
            centAssignment[art_ord] = goto_clu;

            // update centroids:
            updateCent(art_ord, current_clu, goto_clu, centMap,  inputData, artIdArray );

            return true;
        }

//        }
    }
    private void updateCent(int art_ord, int from_clu, int to_clu, Map<Integer, Centroid> centMap,  Map<String,Map<String,Double>> inputData, String[] artIdArray ){
        //Update artList and amount
        boolean existence;
        if (from_clu < 0){
            existence = false;
        }else{
            existence = centMap.get(from_clu).removeArt(art_ord);
        }

//        if(!existence){
//            // no such article in the artList to remove
//            throw new RuntimeException("No such article: art_id " + Integer.toString(art_ord) + "in cluster_id  " + Integer.toString(from_clu));
//        };

        centMap.get(to_clu).addArt(art_ord);

        // Update centroid vectors  //and norm if necessary.
        if (existence) {
            Map<String, Double> centVec1 = centMap.get(from_clu).getVector();
            Map<String, Double> centVec2 = centMap.get(to_clu).getVector();
            int num1 = centMap.get(from_clu).amount - 1;
            int num2 = centMap.get(to_clu).amount + 1;
            Map<String, Double> d0 = inputData.get(artIdArray[art_ord]);

//        if (num1 + 1 == num2 - 1){
//            throw new RuntimeException(" Warning: num1 == num2");
//        }

            switch (criterionFun) {
                case "SSE":
                    centVec1 = supposition(centVec1, (double) (num1) / (num1 + 1), d0, (double) 1 / (num1 + 1));
                    centVec2 = supposition(centVec2, (double) (num2) / (num2 + 1), d0, (double) 1 / (num2 + 1));
                    centMap.get(from_clu).updateCent(centVec1);
                    centMap.get(to_clu).updateCent(centVec2);
                    break;
                case "I2":
                    centVec1 = supposition(centVec1, 1, d0, 1);
                    centVec2 = supposition(centVec2, 1, d0, 1);

                    centMap.get(from_clu).updateCent(centVec1);
                    centMap.get(to_clu).updateCent(centVec2);
                    centMap.get(from_clu).calNorm();
                    centMap.get(to_clu).calNorm();
                    break;
                case "E1":
                    centVec1 = supposition(centVec1, 1, d0, 1);
                    centVec2 = supposition(centVec2, 1, d0, 1);

                    centMap.get(from_clu).updateCent(centVec1);
                    centMap.get(to_clu).updateCent(centVec2);
                    centMap.get(from_clu).calNorm();
                    centMap.get(to_clu).calNorm();
                    break;
            }
        }else {

            Map<String, Double> centVec2 = centMap.get(to_clu).getVector();
            int num2 = centMap.get(to_clu).amount + 1;
            Map<String, Double> d0 = inputData.get(artIdArray[art_ord]);


            switch (criterionFun) {
                case "SSE":
                    centVec2 = supposition(centVec2, (double) (num2) / (num2 + 1), d0, (double) 1 / (num2 + 1));
                    centMap.get(to_clu).updateCent(centVec2);
                    break;
                case "I2":
                    centVec2 = supposition(centVec2, 1, d0, 1);

                    centMap.get(to_clu).updateCent(centVec2);
                    centMap.get(to_clu).calNorm();
                    break;
                case "E1":
                    centVec2 = supposition(centVec2, 1, d0, 1);

                    centMap.get(to_clu).updateCent(centVec2);
                    centMap.get(to_clu).calNorm();
                    break;
            }

        }
    }

    private Map<String, Double> supposition(Map<String, Double> v1, double a, Map<String, Double> v2, double b){
        // a * v1 + b * v2
        Map<String,Double> result = new TreeMap<>();

        Set<String> dimensions = new HashSet<>(v1.keySet());
        dimensions.addAll(v2.keySet());

        for(String key: dimensions){
            double val1 = v1.containsKey(key) ? v1.get(key) : 0;
            double val2 = v2.containsKey(key) ? v2.get(key) : 0;
            result.put(key, val1 * a + val2 * b);
        }
        return result;
    }


    // Finalize:
    private int getRandomArt() {
        return rand.nextInt((maxId - minId) + 1) + minId;
    }


    private void EntroPurity(Map<String, Integer> result, String classFile){

        int N = maxId;

        try{
            FileReader fr = new FileReader(classFile);
            BufferedReader br = new BufferedReader(fr);
            Set topicSet = new TreeSet<String>();

            Map<Integer, Map<String, Integer>> cluster_topic = new HashMap<>(finalCluster.size());

            String aline;
            String[] pair; // art_id -> class, result: art_id -> cluster
            Integer cluster;
            while((aline = br.readLine()) != null){
                // scan over the topics
                pair = aline.split(",");
                topicSet.add(pair[1]);
                cluster = result.get(pair[0]);

                if (cluster_topic.containsKey(cluster)){
                    Map<String,Integer> topG = cluster_topic.get(cluster);
                    int count = topG.containsKey(pair[1]) ? topG.get(pair[1]) : 0;
                    topG.put(pair[1], count + 1);
                }else{
                    Map<String, Integer> topG = new HashMap<>();
                    topG.put(pair[1], 1);
                    cluster_topic.put(cluster, topG);
                }
            }
            br.close();
            fr.close();

            System.out.println("");

            System.out.format("%10s", "cluster_id)");
            Iterator<String> iter = topicSet.iterator();

            while (iter.hasNext()) {
                System.out.format("%10s", iter.next());
            }
            System.out.format("%10s", "Purity");
            System.out.format("%10s", "Entropy");
            System.out.println("");

            String top;

            for (Map.Entry<Integer, Map<String, Integer>> entry : cluster_topic.entrySet()) {
                // iterate over entry.key ie cluster:

                int clu = entry.getKey();
//                if (finalCluster.get(clu).size() == 0){
//
//                }

                System.out.format("%10d", entry.getKey());
                Iterator<String> topIter = topicSet.iterator();
                double entropy = 0;
                double pij ;
                double logpij ;
                double purity = 0;
                while (topIter.hasNext()) {
                    Map<String, Integer> mytopics = entry.getValue();
                    top = topIter.next();


                    if (mytopics!=null && top!=null && mytopics.containsKey(top)) {

                        pij = mytopics.get(top)/(double) finalCluster.get(entry.getKey()).size();
                        if(Double.isInfinite(pij) || Double.isNaN(pij)){
                            int dbstop = 0;
                        }
                        purity = (pij > purity) ? pij : purity;

                        if(Double.isInfinite(purity) || Double.isNaN(purity)){
                            int dbstop = 0;
                        }

                        logpij = Math.log10(pij)/Math.log10(2);
                        entropy -= ( (pij==0) ? 0 : (pij * (logpij)) );

                        if(Double.isInfinite(entropy) || Double.isNaN(entropy)){
                            int dbstop = 0;
                        }

                        if (entropy<0){
                            int dbstop2 = 2;
                        }

                        System.out.format("%10d", mytopics.get(top));
                    } else {
                        System.out.format("%10d", 0);
                    }
                }
                System.out.format("%10f", purity);
                System.out.format("%10f", entropy);
                System.out.println("");

                double mi = (double) finalCluster.get(entry.getKey()).size();
                globalEntropy += (entropy) * mi/ N;
                globalPurity += purity *mi/N;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void writeOut(Map<String, Integer> result, String resultFile)
    {
        try {
            FileWriter fw = new FileWriter(resultFile);
            Set<Map.Entry<String, Integer>> resultSet = result.entrySet();

            for (Map.Entry<String, Integer> entry : resultSet) {
                fw.append(entry.getKey());
                fw.append(",");
                fw.append(entry.getValue().toString());
                fw.append("\n");
//                fw.append(entry.getKey() + "," + entry.getValue() + "\n");
            }

            fw.flush();
            fw.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

}

class Centroid{
    public List<Integer> artIdList = null;
    public int amount = 0 ;
    public Map<String, Double> vector = null;
    public double norm = 0.0;
//    public double obj = 0.0;

    public Centroid(){}

    public Centroid(Map<String, Double> vector){

//        this.vector = new TreeMap<String, Double>(vector);
        this.vector = new TreeMap<String, Double>();

//        Iterator iter = vector.entrySet().iterator();
//        Map.Entry entry = (Map.Entry) iter.next();
//        Object test = entry.getKey();

//        Set key = vector.keySet();
//        Object test = key.iterator().next();
//        vector.get(test);

        for (String key: vector.keySet()){
            this.vector.put(key, vector.get(key));
        }

        if (this.vector == vector){
            throw new RuntimeException("shallow copy didn't work for me! ");
        }
        this.artIdList = new ArrayList<>();
        this.calNorm();

    }

    public Centroid(Centroid c){
        this.vector = new TreeMap<>();
        for (String key: c.vector.keySet()){
            this.vector.put(key, c.vector.get(key));
        }

        if (c.artIdList != null){
            this.artIdList = new ArrayList<>();
            for (int i : c.artIdList){
                this.artIdList.add(i);
            }
        }
        this.amount = c.amount ;
        this.norm = c.norm;
//        this.obj = c.obj;

    }


    public void updateCent(Map<String, Double> vector){
        this.vector = new TreeMap<>(vector);
    }

    public double calNorm(){
        double norm = 0.0;
        for(Double val : this.vector.values()){
            norm += val * val;
        }
        this.norm = Math.sqrt(norm);
        return this.norm;
    }

//    public void calObj(String criterion){
//        switch (criterion){
//            case "SSE":
//                this.obj
//        }
//
//    }
//
//    public double calObj(String criterion, Map<String, Double> D){
//
//    }


    public void addArt(int id){
        this.artIdList.add(id);
        this.amount ++;
    }


    public boolean removeArt(int id){
        boolean removeResult = this.artIdList.remove(new Integer(id));
        this.amount --;
        return removeResult;
    }


    public Map<String, Double> getVector(){
        return this.vector;
    }


    public List<Integer> getArtList(){
        if (this.artIdList == null){
            System.out.println("Not created yet. Return null!");
        }
        return this.artIdList;
    }



}






