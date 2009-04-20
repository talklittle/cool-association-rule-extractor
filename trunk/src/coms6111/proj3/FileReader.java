package coms6111.proj3;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap; 




public class FileReader {
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
     * ��UTF-8���뷽ʽ��ȡ�ļ�����
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

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException{

        List<String> fileList=getFileList(new File("/import/html/6111/20091/Proj3-Data/yahoo/"));
        String fileContent=null;
        String[] content = null;
        for(String s:fileList){
        	//��ӡ�ļ���
            System.out.println(s);
            //�ļ�����
            fileContent=getContentByLocalFile (new File(s));
            //��ӡ�ļ�����
            System.out.println(fileContent);
            //�Զ���Ϊ��λ���в����
            content=fileContent.split(",");
            for(String c:content){
                System.out.print(c+"\t");
            }
            System.out.println();
            
        }
    }

}
