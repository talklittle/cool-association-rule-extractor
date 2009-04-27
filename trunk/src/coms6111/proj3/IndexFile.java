package coms6111.proj3;


public class IndexFile {
    public static void indexfile(String url) {
        try {
        	String cmdline[] = {"/home/gravano/Bin/glimpseindex", "-b", "-B",url };
            Process p = Runtime.getRuntime().exec(cmdline);
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        
    }

    public static void main(String url) {
    	String a="/import/html/6111/20091/Proj3-Data/yahoo/";
        indexfile(a);
    }

	//static Runtime r = Runtime.getRuntime();
	
	//public static void test(){
		//try {
			//r.exec("echo");
			//glimpseindex yahoo;
			//glimpse dolphins;
		//} catch (IOException e) {
			//System.err.println(e.getLocalizedMessage());
		//}
	//}	
	
}
