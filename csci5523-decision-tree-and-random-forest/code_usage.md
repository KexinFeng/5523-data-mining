Info of running the code:

Training rep1 generally takes 750s. Training rep2 generally takes 75s. The other steps are done on a fly.

The usage of each code:

$ python dtinduce.py <trainFile> <minfreq> <model_file>  
$ python dtclassify.py <model_file> <testfile> <pred_file>  
$ python3 showconfmatrix.py <pred_file>


Note that there is a reduction process. So the columns with all zeros are abandoned. So in the model file that dtinduce.py produces, the attribute numbers don¡¯t correspond to original unreduced attributes. So in the written out tree file, you may see that the best-split attribute-id may not be the same as the original unreduced attribute ids.


#Random forest:
Usage:

$ python3 rf.py <trainFile> <testFile> <sample_times> <minfreq>  
$ python3 rfmerge.py <predictions> <sample_times>  
$ python3 showconfmatrix.py <predictions>

rf.py ouputs the predictions made by each tree and they are stored in a created path "./pred_files"

rfmerge.py output the forest predictions to the file of <predictions>

showconfmatrix.py takes in <predictions> and calculate the confusion matrix and test accuracy.


The code has been well tested, but in case there is anything unexpected happening to the code, please contact fengx463@umn.edu. Thank you!