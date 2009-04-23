package coms6111.proj3;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap; 
import java.util.TreeSet;



public class FileReader {
	public static void main(String[] args) throws IOException{
		List<String> fileList=getFileList(new File("/import/html/6111/20091/Proj3-Data/yahoo/"));
		String fileContent=null;
		StringBuffer content = null;
	    StringTokenizer st;
		TreeMap<String,Integer> tm=new TreeMap<String,Integer>();
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
		TreeMap<String,Integer> resultMap=(TreeMap<String, Integer>) sortByValue(tm,true);
		for(Map.Entry<String,Integer> entry :resultMap.entrySet()) {
			System.out.println(entry.getKey()+"  "+entry.getValue());
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
     * 以UTF-8编码方式读取文件内容
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
    public static Map sortByValue(Map map , final boolean reverse){   
        List list = new LinkedList(map.entrySet());   
        Collections.sort(list, new Comparator() {   
            public int compare(Object o1, Object o2) {   
                if(reverse){   
                    return -((Comparable) ((Map.Entry) (o1)).getValue())   
                    .compareTo(((Map.Entry) (o2)).getValue());   
                }   
                return ((Comparable) ((Map.Entry) (o1)).getValue())   
                        .compareTo(((Map.Entry) (o2)).getValue());   
            }   
        });   
        Map result = new LinkedHashMap();   
        for (Iterator it = list.iterator(); it.hasNext();) {   
            Map.Entry entry = (Map.Entry) it.next();   
            result.put(entry.getKey(), entry.getValue());   
        }   
        return result;   
  
    }  

}
