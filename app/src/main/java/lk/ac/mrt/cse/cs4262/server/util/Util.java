package lk.ac.mrt.cse.cs4262.server.util;

import java.util.Map;

import com.google.gson.Gson;

public class Util {
    
    public static String getJsonString(Map<String, String> map){        
        return new Gson().toJson(map);
    }
}
