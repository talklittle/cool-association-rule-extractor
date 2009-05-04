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
	static HashMap<Integer, Itemset> docWords=new HashMap<Integer, Itemset>();
	static double minsup, minconf;
	static String specificWord;
	// INSTRUMENTATION
	static long instrIndex = 0, instrCommon = 0, instrWords = 0;
	static long instrItemsetSupport = 0, instrItemsetSupportCount = 0;
	static long instrAlgorithm = 0;
	
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
			}
		else if(args[0].equals("20newsgroups")){
			url="/import/html/6111/20091/Proj3-Data/20newsgroups/";
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
		}
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
        	// remove the word from table keys
        	wordDocs.remove(sPos);
        	wordIds.remove(s);
        	idWords.remove(sPos);
        	
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
        
        
        ///////////////////////////
        // Run Apriori algorithm
        ///////////////////////////
        
        instrAlgorithm = System.currentTimeMillis();
        
        ArrayList<SortedSet<Itemset>> largeItemset=runApriori(sortedWords);
        
        instrAlgorithm = System.currentTimeMillis() - instrAlgorithm;
        System.out.println("Finished. ("+instrAlgorithm+" ms)");
        
        ////////////////////////////////
        // Generate association rules
        ////////////////////////////////
        
        for (;;) {
	        do {
		        System.out.println("Please enter the specific word, or a period (.) to quit:");
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
	        
	        generateAssociationRule(largeItemset);
	        System.out.println();
        }
	}
	
	public static ArrayList<SortedSet<Itemset>> runApriori(TreeMap<String, Integer> sortedwords) {
		Apriori apriori = new Apriori(docIds,
									  wordIds,
									  idWords,
									  wordDocs,
									  docWords,
									  minsup,
									  minconf);
		ArrayList<SortedSet<Itemset>> largeItemsets = apriori.doApriori(sortedwords);
		return largeItemsets;
	}

	public static void generateAssociationRule(ArrayList<SortedSet<Itemset>> largeItemset){
		ArrayList<Rule> rules = new ArrayList<Rule>(); 
		
		for(int i=2;i<=3;i++){
			System.out.println("DEBUG: generateAssociationRule: i=" + i);
			if (largeItemset.size() <= i) {
				System.out.println("DEBUG: generateAssociationRule: break because largeItemset has "+largeItemset.size()+" items");
				break;
			}
			SortedSet<Itemset> aLargeItemsetSet = largeItemset.get(i);
		    
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
					System.out.println("DEBUG: itset does not contain: "+specificWord+" id="+specificId);
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
							int[] rangeId={Itemset.posToRange(id1),Itemset.posToRange(id2)};
							int[] wordId={ Itemset.posToBitmask(id1),Itemset.posToBitmask(id2)};
							Itemset wordsItem=new Itemset(rangeId, wordId);
							double wordsSupport=getItemsetSupport(wordsItem);
							double confidence=itemsetSupport/wordsSupport;
							if(confidence>minconf){
								rules.add(new Rule(confidence,
										"["+words[1]+words[2]+"] => ["+word+"] ("+"Conf:"+confidence+", Supp:"+itemsetSupport+")"));
								}else{
									break;
								}
						}else if(word.equals(words[1])){
							int id1=wordIds.get(words[0]);
							int id2=wordIds.get(words[2]);
							int[] rangeId={Itemset.posToRange(id1),Itemset.posToRange(id2)};
							int[] wordId={ Itemset.posToBitmask(id1),Itemset.posToBitmask(id2)};
							Itemset wordsItem=new Itemset(rangeId, wordId);
							double wordsSupport=getItemsetSupport(wordsItem);
							double confidence=itemsetSupport/wordsSupport;
							if(confidence>minconf){
								rules.add(new Rule(confidence,
										"["+words[0]+words[2]+"] => ["+word+"] ("+"Conf:"+confidence+", Supp:"+itemsetSupport+")"));
								}else{
									break;
								}
							
						}else if(word.equals(words[2])){
							int id1=wordIds.get(words[0]);
							int id2=wordIds.get(words[1]);
							int[] rangeId={Itemset.posToRange(id1),Itemset.posToRange(id2)};
							int[] wordId={ Itemset.posToBitmask(id1),Itemset.posToBitmask(id2)};
							Itemset wordsItem=new Itemset(rangeId, wordId);
							double wordsSupport=getItemsetSupport(wordsItem);
							double confidence=itemsetSupport/wordsSupport;
							if(confidence>minconf){
								rules.add(new Rule(confidence,
										"["+words[0]+words[1]+"] => ["+word+"] ("+"Conf:"+confidence+", Supp:"+itemsetSupport+")"));
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
							if(confidence>minconf){
								rules.add(new Rule(confidence,
										"["+words[1]+"] => ["+word+"] ("+"Conf:"+confidence+", Supp:"+itemsetSupport+")"));
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
							if(confidence>minconf){
								rules.add(new Rule(confidence,
										"["+words[0]+"] => ["+word+"] ("+"Conf:"+confidence+", Supp:"+itemsetSupport+")"));
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
    
    public static double getItemsetSupport(Itemset itset){
    	double support = 0.0;
    	
//    	instrItemsetSupport = System.currentTimeMillis() - instrItemsetSupport;
//    	instrItemsetSupportCount++;
    	
    	Set<Integer> docIdsOfThisItset = itset.getDocIdsIntersection(wordDocs);
    	if (docIdsOfThisItset == null) {
            // XXX This should not happen since COMMON words should has been removed...
            return 0.0;
    	}

    	support = ((double)docIdsOfThisItset.size()) / ((double)docIds.size()); // Ratio of containing transactions
		
//		instrItemsetSupport = System.currentTimeMillis() - instrItemsetSupport;
//		System.out.println("instrItemsetSupport millis =" + instrItemsetSupport);
		
    	return support;
    }
   

}
