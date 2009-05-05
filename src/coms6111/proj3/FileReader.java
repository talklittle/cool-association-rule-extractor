package coms6111.proj3;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap; 
import java.util.TreeSet;

public class FileReader {
	// Mapping from a filename to its id
	static HashMap<String, Integer> docIds;
	// Mapping from a word (String) to its id
	static HashMap<String, Integer> wordIds=new HashMap<String, Integer>();
	// Mapping from a word's id# to the Itemset (bitmap) of docs containing it
	static HashMap<Integer, String> idWords=new HashMap<Integer, String>();
	static HashMap<Integer, Itemset> wordDocs=new HashMap<Integer, Itemset>();
	static HashMap<Integer, TreeSet<Integer>> multiwordDocs;
	static HashMap<Integer, Itemset> docWords=new HashMap<Integer, Itemset>();
	static HashMap<Itemset, Double> itemsetSupport=new HashMap<Itemset, Double>();
	static double minsup, minconf;
	static int maxWordId;
	static String specificWord;
	static Apriori apriori;
	static boolean useMultiwordDocs;

	// INSTRUMENTATION
	static long instrIndex = 0, instrCommon = 0, instrWords = 0;
	static long instrItemsetSupport = 0, instrItemsetSupportCount = 0;
	static long instrGlimpseSupport = 0, instrGlimpseSupportCount = 0;
	static long instrOutputItemsets = 0, instrOutputItemsetsCount = 0;
	static long instrAlgorithm = 0, instrLarge = 0;
	
	static Runtime rt;
	
	public static void usage() {
		System.out.println("Usage:");
		System.out.println("java FileReader <Yahoo|20newsgroups>");
	}
	
	public static void main(String[] args) throws IOException{
		String url=null;
		if (args.length < 1) {
			usage();
			System.exit(1);
		}
		if(args[0].equals("Yahoo")){
			url="/import/html/6111/20091/Proj3-Data/yahoo/";
			useMultiwordDocs = true;
			multiwordDocs = new HashMap<Integer, TreeSet<Integer>>();
			}
		else if(args[0].equals("20newsgroups")){
			url="/import/html/6111/20091/Proj3-Data/20newsgroups/";
			useMultiwordDocs = false;
			multiwordDocs = null;
		} else {
			usage();
			System.exit(1);
		}
		//try {
			//minsup = Double.parseDouble(args[1]);
			//minconf = Double.parseDouble(args[2]);
		//} catch (Exception e) {
			//usage();
			//System.exit(1);
		//}
		
		System.out.println("Creating indexes...");
		instrIndex = System.currentTimeMillis();
		
		// Initialize the Bits tables
		Bits.init();

		List<String> fileList=getFileList(new File(url));
		
		docIds= initDocumentsFile(fileList);
		
		String fileContent=null;
		StringBuffer content = null;
	    StringTokenizer st;
	    // Holds (word => document frequency)
		TreeMap<String,Integer> sortedWords = new TreeMap<String,Integer>();
		
		SortedSet<Integer> wordsInDoc;
		int wordsPosIndex=0, docsPosIndex=0;
		for(String aFile:fileList){
//			System.out.println("DEBUG: main: file "+aFile+" pos="+docsPosIndex);
            docIds.put(aFile, docsPosIndex++);
			fileContent=getContentByLocalFile (new File(aFile));
            content=getSplitContent(fileContent);
            st = new StringTokenizer(content.toString());
            wordsInDoc = new TreeSet<Integer>();
            while (st.hasMoreTokens()){
            	String j = st.nextToken();
            	if(sortedWords.containsKey(j)){
            		// You have seen this word before somewhere in some document
            		// wordDocs should also contain the key. update wordDocs
            		int docRange = Itemset.posToRange(docIds.get(aFile));
            		int docBitmask = Itemset.posToBitmask(docIds.get(aFile));
            		Itemset putMe = wordDocs.get(wordIds.get(j)).addAndCopy(docRange, docBitmask);
            		wordDocs.put(wordIds.get(j), putMe);
//            		// DEBUG
//            		if (j.equals("dolphins")) {
//            			System.out.println("DEBUG: add dolphins count="+wordDocs.get(wordIds.get(j)).getNumWords()
//    					+ " docId="+docIds.get(aFile));			
//            		}
//            		if (j.equals("protestant")) {
//            			System.out.println("DEBUG: add protestant count="+wordDocs.get(wordIds.get(j)).getNumWords()
//            					+ " docId="+docIds.get(aFile));
//            		}
            	}else{
            		// You have never seen this word in any document
            		sortedWords.put(j, 0); // Initialize count to 0 because it will be incremented below
            		// Give newly found word a new word position
//            		System.out.println("DEBUG: main: New word: "+j+" id="+(wordsPosIndex));
            		wordIds.put(j, wordsPosIndex++);
            		// Insert entry (word id, this doc id) to wordDocs
            		int[] docRanges = { Itemset.posToRange(docIds.get(aFile)) };
            		int[] docWords = { Itemset.posToBitmask(docIds.get(aFile)) };
            		wordDocs.put(wordIds.get(j), new Itemset(docRanges, docWords));
            	}
            	wordsInDoc.add(wordIds.get(j));
            	idWords.put(wordIds.get(j), j);

            }
            // update table of (document => set of words it contains)
			docWords.put(docIds.get(aFile), new Itemset(wordsInDoc));
			// update document frequency of words
			for (Iterator<Integer> it = wordsInDoc.iterator(); it.hasNext(); /* */) {
				String aWord = idWords.get(it.next());
				sortedWords.put(aWord, sortedWords.get(aWord) + 1);
			}
			
			if (useMultiwordDocs) {
				// Update the multiwordDocs table
				updateMultiwordDocs(wordsInDoc, docIds.get(aFile));
			}
		}
		maxWordId = wordsPosIndex - 1;
		
//		// Create glimpse index
//		File glimpseIndexDir = new File("./index/");
//		if (glimpseIndexDir.exists()) {
//			System.out.println("./index/ exists. Deleting contents.");
//			File[] glimpseFiles = glimpseIndexDir.listFiles();
//			for (File f : glimpseFiles) {
//				f.delete();
//			}
//		} else {
//			glimpseIndexDir.mkdirs();
//		}
//		rt = Runtime.getRuntime();
//		Process glimpseindex = rt.exec("/home/gravano/Bin/glimpseindex -b -B -H ./index/ "+url);
//		try {
//			int retval = glimpseindex.waitFor();
//			if (retval != 0) {
//				System.err.println("glimpseindex returned with exit code "+retval);
//				System.exit(1);
//			}
//		} catch (InterruptedException e) {
//			System.err.println("glimpseindex interrupted");
//			System.exit(1);
//		}
		instrIndex = System.currentTimeMillis() - instrIndex;
		System.out.println("Created index in memory. ("+instrIndex+" ms)");
		
		instrCommon = System.currentTimeMillis();
		// Find the COMMON words
		Map<String, Integer> resultMap = sortByValue(sortedWords,true);
		Set<String> ss = resultMap.keySet();
        TreeSet<String> sortedCommon = new TreeSet<String>();
        int i = 0;
        for(Iterator<String> it = ss.iterator(); it.hasNext() && i < 397; i++) {
        	String h=it.next();
            sortedCommon.add(h);
        }
        if (i < 397) {
            System.err.println("Did not find 397 words!");
            System.exit(1);
        }
        
        File COMMON = new File("COMMON");
        if (COMMON.exists())
        	COMMON.delete();
        COMMON.createNewFile();
        FileWriter writer = new FileWriter(COMMON);
        for (Iterator<String> it = sortedCommon.iterator(); it.hasNext(); /* */) {
        	String s = it.next();
        	int sPos = wordIds.get(s);
        	// Remove the common word from WORDS
        	if (sortedWords.remove(s) == null) {
        		System.err.println("ERROR: Could not remove COMMON word "+s+" pos="+sPos
        				+" from sortedWords");
        	}
        	
        	// remove the word from all Transactions
        	Itemset transaction;
        	for (Integer transactionId : docIds.values()) {
        		transaction = docWords.get(transactionId);
        		if (!transaction.remove(Itemset.posToRange(sPos), Itemset.posToBitmask(sPos))) {
//        			System.out.println("DEBUG: Eliminate COMMON word "+s+" pos="+sPos
//        					+" from transaction "+transactionId+" failed");
        		} else {
//        			System.out.println("DEBUG: Eliminated COMMON word "+s+" pos="+sPos
//        					+" from transaction "+transactionId);
        		}
        	}
        	
        	// remove the word from multiwordDocs
        	if (useMultiwordDocs) {
	        	for (SortedSet<Integer> pointTo : multiwordDocs.values()) {
	        		pointTo.remove(sPos);
	        	}
        	}
        	
        	// remove the word from table keys
        	wordDocs.remove(sPos);
        	wordIds.remove(s);
        	idWords.remove(sPos);
        	if (useMultiwordDocs) {
        		multiwordDocs.remove(sPos);
        	}
        	
        	// Output to file COMMON
        	writer.write(s + "\n");
		}
        writer.close();
        instrCommon = System.currentTimeMillis() - instrCommon;
        System.out.println("Created COMMON file. ("+instrCommon+" ms)");
        
		instrWords = System.currentTimeMillis();
        // Output the sorted WORDS (excluding COMMON)
        File WORDS = new File("WORDS");
        if (WORDS.exists())
        	WORDS.delete();
        WORDS.createNewFile();
        writer = new FileWriter(WORDS);
        for(Iterator<String> it = sortedWords.keySet().iterator(); it.hasNext(); /* */) {
			writer.write(it.next() + "\n");
		}
        writer.close();
        instrWords = System.currentTimeMillis() - instrWords;
        System.out.println("Created WORDS file. ("+instrWords+" ms)");
        System.out.println();
        
        for (;;) {
	        do {
		        System.out.println("Please enter the value of minsup:");
		        System.out.flush();
		        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); 
		        try{
		        	minsup=Double.parseDouble(br.readLine());
		        }catch (Exception e) {
		        	System.out.println("Error!");
		        	minsup = -1;
		        }
	        } while (minsup < 0);
	        
	        System.out.println("Using minsup of " + String.format("%.4f", minsup*100) + "%");
	        
	        ///////////////////////////
	        // Run Apriori algorithm
	        ///////////////////////////
	        
	        System.out.println("Finding large itemsets...");
	        instrAlgorithm = System.currentTimeMillis();
	        ArrayList<List<Itemset>> largeItemset=runApriori(sortedWords);
	        instrAlgorithm = System.currentTimeMillis() - instrAlgorithm;
	        System.out.println("Found large itemsets. ("+instrAlgorithm+" ms)");
	        
	        System.out.println("Outputting large itemsets to file LARGE...");
	        instrLarge = System.currentTimeMillis();
	        outputItemsets(largeItemset);
	        instrLarge = System.currentTimeMillis() - instrLarge;
	        System.out.println("Created file LARGE. ("+instrLarge+" ms)");
	        
	        ////////////////////////////////
	        // Generate association rules
	        ////////////////////////////////
	        
	        for (;;) {
		        do {
			        System.out.println("Please enter a word, or a period (.) to quit, or slash (/) to do new minsup:");
			        System.out.flush();
			        BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in)); 
			        try{
			        	specificWord=br2.readLine().toLowerCase();
			        }catch (Exception e) {
			        	System.out.println("Error!");
			        	specificWord = null;
			        }
		        } while (specificWord == null || "".equals(specificWord));
		        
		        if (".".equals(specificWord))
		        	break;
		        if ("/".equals(specificWord))
		        	break;
		        
		        do {
			        System.out.println("Please enter the value of minconf:");
			        System.out.flush();
			        BufferedReader br1 = new BufferedReader(new InputStreamReader(System.in)); 
			        try{
			        	minconf=Double.parseDouble(br1.readLine());
			        }catch (Exception e) {
			        	System.out.println("Error!");
			        	minconf = -1;
			        }
		        } while (minconf < 0);
		        
		        System.out.println("Using minconf of " + String.format("%.4f", minconf*100) + "%");
		        generateAssociationRule(largeItemset);
		        System.out.println();
	        }
	        if (".".equals(specificWord))
	        	break;
		}
        
//        System.out.println("instrItemsetSupport: "+instrItemsetSupport+" ms "+instrItemsetSupportCount);
//        System.out.println("instrGlimpseSupport: "+instrGlimpseSupport+" ms "+instrGlimpseSupportCount);
//        System.out.println("instrOutputItemsets: "+instrOutputItemsets+" ms "+instrOutputItemsetsCount);
	}
	
	public static ArrayList<List<Itemset>> runApriori(TreeMap<String, Integer> sortedwords) {
		apriori = new Apriori(docIds,
							  wordIds,
							  idWords,
							  wordDocs,
							  multiwordDocs,
							  docWords,
							  maxWordId,
							  minsup,
							  minconf);
		ArrayList<List<Itemset>> largeItemsets = apriori.doApriori(sortedwords);
		return largeItemsets;
	}

	public static void generateAssociationRule(List<List<Itemset>> largeItemset){
		ArrayList<Rule> rules = new ArrayList<Rule>(); 
		
		for(int i=2;i<=3;i++){
//			System.out.println("DEBUG: generateAssociationRule: i=" + i);
			if (largeItemset.size() <= i) {
//				System.out.println("DEBUG: generateAssociationRule: break because largeItemset has "+largeItemset.size()+" items");
				break;
			}
			List<Itemset> aLargeItemsetSet = largeItemset.get(i);
		    
			for(Iterator<Itemset> it=aLargeItemsetSet.iterator(); it.hasNext(); /* */){
				Integer specificId = wordIds.get(specificWord);
				if (specificId == null) {
					System.out.println("No transactions contain the word: "+specificWord);
					return;
				}
				int[] rangeIdSpecific= { Itemset.posToRange(specificId) };
				int[] wordIdSpecific = { Itemset.posToBitmask(specificId) };
				Itemset specificItem = new Itemset(rangeIdSpecific, wordIdSpecific);
//				System.out.println("DEBUG: generateAssociationRule: looking at next itemset");
				Itemset itset = it.next();
				if(!itset.contains(specificItem) ) {
//					System.out.println("DEBUG: itset does not contain: "+specificWord+" id="+specificId);
					continue;
				}
				
				String[] words=new String[i];
				List<Integer> ids = itset.getIds();
				for(int j=0; j<ids.size(); j++){
					words[j]=idWords.get(ids.get(j));
				}
				
				double itemsetSupport=getItemsetSupport(itset);
//				System.out.println("DEBUG: generateAssociationRule: itset (next line) supp:"+itemsetSupport);
//				itset.debugPrintWords(idWords);
				for (Integer wId : ids){
//					System.out.println("DEBUG: generateAssociationRule: looking at next word");
					
					String word=idWords.get(wId);
					
//					System.out.println("DEBUG: generateAssociationRule: word: "+word+" wordId="+wId+" wordsupp:"+wordSupport+" conf:"+confidence
//							+" wordItem.size="+wordItem.getNumWords());
//					System.out.println("DEBUG: generateAssociationRule: docCount="+wordDocs.get(wId).getNumWords());
					if(ids.size()==3){
						if(word.equals(words[0])){
							int id1=wordIds.get(words[1]);
							int id2=wordIds.get(words[2]);
							int[] rangeId1={Itemset.posToRange(id1)};
							int[] wordId1={Itemset.posToBitmask(id1)};
							int rangeId2=Itemset.posToRange(id2);
							int wordId2=Itemset.posToBitmask(id2);
							Itemset wordsItem=new Itemset(rangeId1, wordId1);
							wordsItem=wordsItem.addAndCopy(rangeId2,wordId2);
							double wordsSupport=getItemsetSupport(wordsItem);
							double confidence=itemsetSupport/wordsSupport;
							String confStr = String.format("%.4f", confidence * 100);
							String supStr = String.format("%.4f", itemsetSupport * 100);
							if(confidence>=minconf){
								rules.add(new Rule(confidence,
										"["+words[1]+", "+words[2]+"] => ["+word+"] ("+"Conf:"+confStr+"%, Supp:"+supStr+"%)"));
								}else{
									break;
								}
						}else if(word.equals(words[1])){
							int id1=wordIds.get(words[0]);
							int id2=wordIds.get(words[2]);
							int[] rangeId1={Itemset.posToRange(id1)};
							int[] wordId1={Itemset.posToBitmask(id1)};
							int rangeId2=Itemset.posToRange(id2);
							int wordId2=Itemset.posToBitmask(id2);
							Itemset wordsItem=new Itemset(rangeId1, wordId1);
							wordsItem=wordsItem.addAndCopy(rangeId2,wordId2);
							double wordsSupport=getItemsetSupport(wordsItem);
							double confidence=itemsetSupport/wordsSupport;
							String confStr = String.format("%.4f", confidence * 100);
							String supStr = String.format("%.4f", itemsetSupport * 100);
							if(confidence>=minconf){
								rules.add(new Rule(confidence,
										"["+words[0]+", "+words[2]+"] => ["+word+"] ("+"Conf:"+confStr+"%, Supp:"+supStr+"%)"));
								}else{
									break;
								}
							
						}else if(word.equals(words[2])){
							int id1=wordIds.get(words[0]);
							int id2=wordIds.get(words[1]);
							int[] rangeId1={Itemset.posToRange(id1)};
							int[] wordId1={Itemset.posToBitmask(id1)};
							int rangeId2=Itemset.posToRange(id2);
							int wordId2=Itemset.posToBitmask(id2);
							Itemset wordsItem=new Itemset(rangeId1, wordId1);
							wordsItem=wordsItem.addAndCopy(rangeId2,wordId2);
							double wordsSupport=getItemsetSupport(wordsItem);
							double confidence=itemsetSupport/wordsSupport;
							String confStr = String.format("%.4f", confidence * 100);
							String supStr = String.format("%.4f", itemsetSupport * 100);
							if(confidence>=minconf){
								rules.add(new Rule(confidence,
										"["+words[0]+", "+words[1]+"] => ["+word+"] ("+"Conf:"+confStr+"%, Supp:"+supStr+"%)"));
								}else{
									break;
								}
						}
					}else if(ids.size()==2){
						if(word.equals(words[0])){
							int id1=wordIds.get(words[1]);
							int[] rangeId={Itemset.posToRange(id1)};
							int[] wordId={ Itemset.posToBitmask(id1)};
							Itemset wordsItem=new Itemset(rangeId, wordId);
							double wordsSupport=getItemsetSupport(wordsItem);
							double confidence=itemsetSupport/wordsSupport;
							String confStr = String.format("%.4f", confidence * 100);
							String supStr = String.format("%.4f", itemsetSupport * 100);
							if(confidence>=minconf){
								rules.add(new Rule(confidence,
										"["+words[1]+"] => ["+word+"] ("+"Conf:"+confStr+"%, Supp:"+supStr+"%)"));
								}else{
									break;
								}
						}else if(word.equals(words[1])){
							int id1=wordIds.get(words[0]);
							int[] rangeId={Itemset.posToRange(id1)};
							int[] wordId={ Itemset.posToBitmask(id1)};
							Itemset wordsItem=new Itemset(rangeId, wordId);
							double wordsSupport=getItemsetSupport(wordsItem);
							double confidence=itemsetSupport/wordsSupport;
							String confStr = String.format("%.4f", confidence * 100);
							String supStr = String.format("%.4f", itemsetSupport * 100);
							if(confidence>=minconf){
								rules.add(new Rule(confidence,
										"["+words[0]+"] => ["+word+"] ("+"Conf:"+confStr+"%, Supp:"+supStr+"%)"));
								}else{
									break;
								}
							
						}
					}
					
					
				}
				
		}
		}
		
		// Print in decreasing order of confidence
		Rule[] outputRules = rules.toArray(new Rule[0]);
		Arrays.sort(outputRules);
		for (int i = outputRules.length-1; i >= 0; i--) {
			System.out.println(outputRules[i].toString());
		}
	}
	
	/**
     * get all documents of one given directory
     * @param file
     * @return
     */
    public static List<String> getFileList(File file) {
        List<String> result = new ArrayList<String>();
        if (!file.isDirectory()) {
//            System.out.println(file.getAbsolutePath());
            result.add(file.getAbsolutePath());
        } else {
//        	System.out.println("dir: " + file.getAbsolutePath());
            File[] directoryList=file.listFiles(new FileFilter(){
                    public boolean accept(File f) {
//                        if (f.isFile() && f.getName().indexOf("txt") > -1) {
//                            return true;
//                        } else {
//                            return false;
//                        }
                    	return true;
                    }
            });  
            for(int i=0;i<directoryList.length;i++){ 
                result.addAll(getFileList(directoryList[i]));
            }
        }
        return result;
    }
    
    /**
     * @param path
     * @return
     * @throws IOException
     */
    public static String  getContentByLocalFile (File path) throws IOException{
        InputStream input=new FileInputStream(path);
        InputStreamReader reader=new InputStreamReader(input,"utf-8");
        BufferedReader br = new BufferedReader(reader);
        StringBuilder builder = new StringBuilder();
        String temp = null;
        while((temp = br.readLine())!=null){
             builder.append(temp);
        }
        input.close();
        return builder.toString();    
    }
    
    public static StringBuffer getSplitContent(String filecontent){
    	StringBuffer output = new StringBuffer(filecontent.length());
    	for(int i=0;i<filecontent.length();i++){
    		if (Character.isLetter(filecontent.charAt(i))) {
                output.append(Character.toLowerCase(filecontent.charAt(i)));
            }else{
            	output.append(' ');
            }
        }
    	return output;
    }
    
    public static Map<String, Integer> sortByValue(Map<String, Integer> map , final boolean reverse){   
        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(map.entrySet());   
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {   
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {   
                if(reverse){   
                    return -o1.getValue().compareTo(o2.getValue());   
                }   
                return o1.getValue().compareTo(o2.getValue());   
            }   
        });   
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();   
        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {   
            Map.Entry<String, Integer> entry = it.next();   
            result.put(entry.getKey(), entry.getValue());   
        }   
        return result;   
  
    } 

    /**
     * Initialize the Map for associating a filename with an id.
     * @param fileList
     * @return
     */
    public static HashMap<String, Integer> initDocumentsFile(List<String> fileList){
    	HashMap<String, Integer> documentsPosition = new HashMap<String, Integer>();
    	for(int i=0;i<fileList.size();i++){
    		documentsPosition.put(fileList.get(i), i);
    	}
    	return documentsPosition;
    }
    
    public static void updateMultiwordDocs(SortedSet<Integer> wordsInDoc, int docId) {
		Integer a = null, afterA = null;
    	for (Iterator<Integer> ita = wordsInDoc.iterator(); ita.hasNext(); /* */) {
    		if (a == null) {
    			a = ita.next();
    			continue;
    		} else if (afterA == null) {
    			afterA = ita.next();
    		} else {
    			a = afterA;
    			afterA = ita.next();
    		}
    		// Combine id a with all the ids after it
    		for (Iterator<Integer> itb = wordsInDoc.tailSet(afterA).iterator(); itb.hasNext(); /* */) {
    			Integer b = itb.next();
//    			Integer[] ab = {a, b};
//    			Arrays.sort(ab); // Just in case
    			if (a < b) {
	    			if (multiwordDocs.containsKey(a)) {
	    				TreeSet<Integer> tmpa = multiwordDocs.get(a);
	    				tmpa.add(b);
//	    				System.out.println("DEBUG: updateMultiwordDocs: a="+a+" add b="+b);
	    			} else {
	    				TreeSet<Integer> tmpa = new TreeSet<Integer>();
	    				tmpa.add(b);
//	    				System.out.println("DEBUG: updateMultiwordDocs: a="+a+" add b="+b);
	    				multiwordDocs.put(a, tmpa);
	    			}
    			} else if (b < a) {
	    			if (multiwordDocs.containsKey(b)) {
	    				TreeSet<Integer> tmpa = multiwordDocs.get(b);
	    				tmpa.add(a);
//	    				System.out.println("DEBUG: updateMultiwordDocs: b="+b+" add a="+a);
	    			} else {
	    				TreeSet<Integer> tmpa = new TreeSet<Integer>();
	    				tmpa.add(a);
	    				multiwordDocs.put(b, tmpa);
//	    				System.out.println("DEBUG: updateMultiwordDocs: b="+b+" add a="+a);
	    			}
    			}
//    			System.out.println("DEBUG: updateMultiwordDocs: added ab={"+a+","+b+"} new numbits="
//    					+multiwordDocs.get(a).get(b).getNumBits());
    		}
    	}
    }
    
    /**
     * Output large itemsets to a file LARGE in decreasing order of support
     * @param itemsets
     */
    public static void outputItemsets(List<List<Itemset>> itemsets) {
    	
    	instrOutputItemsets = System.currentTimeMillis() - instrOutputItemsets;
    	instrOutputItemsetsCount++;
    	
    	Itemset[] allLargeItemsets;
    	HashSet<Itemset> tmpLargeItemsets = new HashSet<Itemset>();
//    	long debugCounter = 0;
    	
    	for (List<Itemset> ss : itemsets) {
//    		debugCounter += ss.size();
    		tmpLargeItemsets.addAll(ss);
    	}
    	//     System.out.println("DEBUG: outputItemsets: itemsets.size()="+itemsets.size()
    	//                     +" total ss should be "+debugCounter+" tmpLargeItemsets.size()="+tmpLargeItemsets.size());
    	allLargeItemsets = tmpLargeItemsets.toArray(new Itemset[0]);
    	Arrays.sort(allLargeItemsets, new ItemsetSupportComparator());

    	
    	File LARGE = new File("LARGE");
        if (LARGE.exists())
        	LARGE.delete();
    	try {
            LARGE.createNewFile();
	    	FileWriter writer = new FileWriter(LARGE);
	    	// Output in decreasing order of support
	    	for (int i = allLargeItemsets.length - 1; i >= 0; i--) {
	    		List<Integer> myWordIds = allLargeItemsets[i].getIds();
	    		String[] words = new String[myWordIds.size()];
	    		for (int j = 0; j < words.length; j++) {
	    			words[j] = idWords.get(myWordIds.get(j));
	    		}
	    		// On each line, print the words of itemset in alphabetical order
	    		Arrays.sort(words);
	    		writer.write("[");
	    		for (int k = 0; k < words.length - 1; k++) {
	    			writer.write(words[k] + ",");
	    		}
	    		writer.write(words[words.length-1] + "], ");
	    		writer.write(String.format("%.4f", getItemsetSupport(allLargeItemsets[i])*100));
	    		writer.write("%\n");
    		}
	    	writer.close();
    	} catch (IOException e) {
    		System.err.println(e.getLocalizedMessage());
    	}
    	
    	instrOutputItemsets = System.currentTimeMillis() - instrOutputItemsets;
    }
    
    public static double getItemsetSupport(Itemset itset){
    	double support = 0.0;
    	
    	instrItemsetSupport = System.currentTimeMillis() - instrItemsetSupport;
    	instrItemsetSupportCount++;
    	
    	if (itemsetSupport.containsKey(itset)) {
//			System.out.println("DEBUG: itemsetSupport contains key (next line)");
//			itset.debugPrintWords(idWords);
    		instrItemsetSupport = System.currentTimeMillis() - instrItemsetSupport;
			return itemsetSupport.get(itset);
		}
//		System.out.println("DEBUG: itemsetSupport does not contain key (next line)");
//		itset.debugPrintWords(idWords);
		
    	Itemset docIdsOfThisItset = itset.getDocIdsIntersection(wordDocs);
    	
    	if (itset.getNumBits() == 0) {
    		support = 0.0;
    	} else {
	    	support = ((double)docIdsOfThisItset.getNumBits()) / ((double)docIds.size()); // Ratio of containing transactions
    	}
		itemsetSupport.put(itset, support);
		
		instrItemsetSupport = System.currentTimeMillis() - instrItemsetSupport;
//		System.out.println("instrItemsetSupport millis =" + instrItemsetSupport);
		
    	return support;
    }

//    public static double getGlimpseSupport(String joinedWords) {
//    	instrGlimpseSupport = System.currentTimeMillis() - instrGlimpseSupport;
//    	instrGlimpseSupportCount++;
//    	try {
//    		Process glimpse = rt.exec("/home/gravano/Bin/glimpse -i -w -N -y '"+joinedWords+"'");
//    		BufferedReader in = new BufferedReader(new InputStreamReader(glimpse.getInputStream()));
//    		String line = in.readLine();
//    		in.close();
//    		if ("".equals(line)) {
////    			glimpse.destroy();
//    			instrGlimpseSupport = System.currentTimeMillis() - instrGlimpseSupport;
//    			return 0.0;
//    		}
//    		StringTokenizer st = new StringTokenizer(line);
//    		for (int i = 0; i < 4; i++)
//    			st.nextToken();
//    		Double numerator = Double.parseDouble(st.nextToken()); // 5th word
//    		for (int i = 5; i < 7; i++)
//    			st.nextToken();
//    		Double denominator = Double.parseDouble(st.nextToken()); // 8th word
////    		glimpse.destroy();
//    		instrGlimpseSupport = System.currentTimeMillis() - instrGlimpseSupport;
//    		return numerator / denominator;
//		} catch (IOException e) {
//			System.err.println(e.getLocalizedMessage());
//			System.exit(1);
//		}
//		instrGlimpseSupport = System.currentTimeMillis() - instrGlimpseSupport;
//		return 0.0;
//    }
//
//    public static double getGlimpseSupport(String word1, String word2, String word3) {
//    	return getGlimpseSupport(word1+";"+word2+";"+word3);
//    }
//    
//    public static double getGlimpseSupport(String[] words) {
//    	String joined = "";
//    	for (int i = 0; i < words.length - 1; i++) {
//    		joined += words[i] + ";";
//    	}
//    	joined += words[words.length-1];
//    	return getGlimpseSupport(joined);
//    }
    
    public static class ItemsetSupportComparator implements Comparator<Itemset> {
    	public int compare(Itemset it1, Itemset it2) {
    		double it1Sup = getItemsetSupport(it1);
    		double it2Sup = getItemsetSupport(it2);
    		if (it1Sup < it2Sup)
    			return -1;
    		if (it1Sup > it2Sup)
    			return 1;
    		return 0;
    	}
    }
}
