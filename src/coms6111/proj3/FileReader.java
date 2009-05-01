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
	static HashMap<String, Integer> documentsPosition;
	// Mapping from a word (String) to its id
	static HashMap<String, Integer> wordsPosition=new HashMap<String, Integer>();
	// Mapping from a word's id# to the Itemset (bitmap) of docs containing it
	static HashMap<Integer, Itemset> wordDocs=new HashMap<Integer, Itemset>();
	static HashMap<Integer, Itemset> docsWord=new HashMap<Integer, Itemset>();
	static int minconf, minsup;
	
	public static void main(String[] args) throws IOException{
		String url=null;
		if(args[0].equals("Yahoo")){
			url="/import/html/6111/20091/Proj3-Data/yahoo/";
			}
		else if(args[0].equals("20newsgroups")){
			url="/import/html/6111/20091/Proj3-Data/20newsgroups/";
		} else {
			System.err.println("argument must be 'Yahoo' or '20newsgroups'");
			System.exit(1);
		}
		
		List<String> fileList=getFileList(new File(url));
		
		documentsPosition= initDocumentsFile(fileList);
		
		String fileContent=null;
		StringBuffer content = null;
	    StringTokenizer st;
		TreeMap<String,Integer> sortedWords=new TreeMap<String,Integer>();
		Map<String, Integer> resultMap=null;
		SortedSet<Integer> wordsInDoc;
		SortedSet<Integer> docInWords;
		int wordsPosIndex=0;
		for(String aFile:fileList){
	            fileContent=getContentByLocalFile (new File(aFile));
	            content=getSplitContent(fileContent);
	            st = new StringTokenizer(content.toString());
	            wordsInDoc = new TreeSet<Integer>();
	            docInWords = new TreeSet<Integer>();
	            while (st.hasMoreTokens()){
	            	String j = st.nextToken();
	            	if(sortedWords.containsKey(j)){
	            		sortedWords.put(j,sortedWords.get(j)+1);
	            	}else{
	            		sortedWords.put(j, 1);
	            		// Give newly found word a new word position
	            		wordsPosition.put(j, wordsPosIndex);
	            	    wordsPosIndex++;
	            	}
	            	wordsInDoc.add(documentsPosition.get(aFile));
            		if (wordDocs.containsKey(wordsPosition.get(j))) {
            			
            		} else {
            			wordDocs.put(wordsPosition.get(j), new Itemset(wordsInDoc));
            		}
            		docInWords.add(wordsPosition.get(st));
	            }
	            docsWord.put(documentsPosition.get(aFile), new Itemset(docInWords));
	           
		}
		
		// Find the COMMON words
		resultMap=sortByValue(sortedWords,true);
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
        	// Remove the common word from WORDS
        	sortedWords.remove(s);
        	
        	// TODO remove the word from all Transactions
        	
        	// Output to file COMMON
        	writer.write(s + "\n");
		}
        writer.close();
        
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
		
        runApriori();
	}
	
	public static void runApriori() {
		Apriori apriori = new Apriori(documentsPosition,
									  wordsPosition,
									  wordDocs,
									  minconf,
									  minsup);
		HashSet<Itemset> largeItemsets = apriori.doApriori();
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
        return builder.toString();    
    }
    public static StringBuffer getSplitContent(String filecontent){
    	StringBuffer output = new StringBuffer(filecontent.length());
    	for(int i=0;i<filecontent.length();i++){
    		if (Character.isLetter(filecontent.charAt(i)) && filecontent.charAt(i)<128) {
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

}
