package lk.ac.mrt.cse.cs4262.server.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


import lk.ac.mrt.cse.cs4262.server.objects.ServerConfigObj;

public class ConfigUtil {

    
    public static Map<String, ServerConfigObj> loadSystemConfig(String fileName, String serverName)
             throws FileNotFoundException{
        Map<String, ServerConfigObj> map = new HashMap<>();
        File file = new File(fileName);
        Scanner myReader = new Scanner(file);
        while (myReader.hasNextLine()) {
            String[] values = myReader.nextLine().strip().split("    ");
            System.out.print(values);
            map.put(values[0], new ServerConfigObj(
                values[0], values[1], values[2], values[3], values[4], serverName
            ));
        }
        myReader.close();
        return map;
    }
}
