import java.io.*;
import java.util.*;

public class Main {
    static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    static final StringTokenizer st = null;

    public static void main(String[] args) throws Exception {
        // Your code here
        File dir = new File("/disk");
        System.out.println(dir.exists());
        File[] fileNames = dir.listFiles();
        for (int i = 0; i < fileNames.length; i++) {
            System.out.println(fileNames[i].getName());
        }
    }
}