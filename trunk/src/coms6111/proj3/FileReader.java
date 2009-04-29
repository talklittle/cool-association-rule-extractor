package coms6111.proj3;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
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
import java.util.StringTokenizer;
import java.util.TreeMap; 



public class FileReader {
	public static void main(String[] args) throws IOException{
		String url=null;
		if(args[0].equals("Yahoo")){
			url="/import/html/6111/20091/Proj3-Data/yahoo/";
			}
		else if(args[0].equals("20newsgroups")){
			url="/import/html/6111/20091/Proj3-Data/20newsgroups/";
		} else {
			System.err.println("argument must be 'Yahoo' or '20newsgroups'");
		}
		List<String> fileList=getFileList(new File(url));
		String fileContent=null;
		StringBuffer content = null;
	    StringTokenizer st;
		TreeMap<String,Integer> tm=new TreeMap<String,Integer>();
		Map<String, Integer> resultMap=null;
		for(String s:fileList){
	            fileContent=getContentByLocalFile (new File(s));
	            content=getSplitContent(fileContent);
	            st = new StringTokenizer(content.toString());
	            while (st.hasMoreTokens()){
	            	String j = st.nextToken();
	            	if(tm.containsKey(j)){
	            		tm.put(j,tm.get(j)+1);
	            	}else{
	            		tm.put(j, 1);
	            	}
	            }
		}
		resultMap=sortByValue(tm,true);
		Set<String> ss=resultMap.keySet();
        String[] sortedCommon = new String[397];
        int i = 0;
        for(Iterator<String> it = ss.iterator(); it.hasNext() && i < 397; i++) {
        	String h=it.next();
            sortedCommon[i] = h;
        }
        if (i < 397) {
            System.err.println("Did not find 397 words!");
            System.exit(1);
        }
               
        Arrays.sort(sortedCommon);

		for (String s : sortedCommon) {
			System.out.println(s);
		}
		int x= resultMap.size()-397;
		String[] sortedWords = new String[x];
		for(Iterator<String> it = ss.iterator();it.hasNext()&& i<x;i++){
			String l=it.next();
			while(!sortedCommon.equals(l)){
				sortedWords[i]=l;
			}
		}
		Arrays.sort(sortedWords);
		for(String g:sortedWords){
			System.out.println(g);
		}
		HashMap<String, Integer> wordsPosition = wordsFile(sortedWords);
		HashMap<String, Integer> documentsPosition= documentsFile(fileList);
		//HashMap<Integer, List<Integer>> wordsDocPosition = wordDocumentsFile(sortedWords,fileList);
		HashMap<Integer, List<Integer>> wdp= new HashMap<Integer, List<Integer>>();
		List<Integer> result= null;
    	for(int b=0;b<sortedWords.length;b++){
    		for(String s: fileList){
    			while(s.contains(sortedWords[b])){
    				result.add(documentsPosition.get(s));
    				}
    			}
    		wdp.put(wordsPosition.get(sortedWords[b]),result);
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
            System.out.println(file.getAbsolutePath());
            result.add(file.getAbsolutePath());
        } else {
             File[] directoryList=file.listFiles(new FileFilter(){
                    public boolean accept(File file) {
                        if (file.isFile() && file.getName().indexOf("txt") > -1) {
                            return true;
                        } else {
                            return false;
                        }
                    }
             });  
             for(int i=0;i<directoryList.length;i++){ 
                 result.add(directoryList[i].getAbsolutePath());
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
    public static HashMap<String, Integer> wordsFile(String[] wordFile){
    	HashMap<String, Integer> wordsPosition = new HashMap<String, Integer>();
    	for(int i=0;i<wordFile.length;i++){
    		wordsPosition.put(wordFile[i], i);
    	}
    	return wordsPosition;
    	}
    public static HashMap<String, Integer> documentsFile(List<String> fileList){
    	HashMap<String, Integer> documentsPosition = new HashMap<String, Integer>();
    	for(int i=0;i<fileList.size();i++){
    		documentsPosition.put(fileList.get(i), i);
    	}
    	return documentsPosition;
    	
    }
   

}
