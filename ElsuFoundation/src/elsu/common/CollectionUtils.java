package elsu.common;

import java.util.*;

/**
 *
 * @author ss.dhaliwal_admin
 */
public class CollectionUtils {

    public static Map<String, Object> getMapValueSubset(Map<String, Object> list, String pattern) {
    	HashMap<String, Object> result = new HashMap<>();
    	
    	for(String key : list.keySet()) {
    		if (key.matches(pattern)) {
    			result.put(key, list.get(key));
    		}
    	}
    		
        return result;
    }

    public static String getMapValueAsString(Map<String, Object> list, String key) {
        return list.get(key).toString();
    }

    public static int getMapValueAsInteger(Map<String, Object> list, String key) {
        return Integer.parseInt(getMapValueAsString(list, key));
    }
    
    public static String ArrayListToString(ArrayList<String> values) {
        return CollectionUtils.ArrayToString(values.toArray(new String[values.size()]));
    }
    public static String ArrayListToString(ArrayList<String> values, char delimiter) {
        return CollectionUtils.ArrayToString(values.toArray(new String[values.size()]), delimiter);
    }
    
    public static String ArrayToString(String[] values) {
        return ArrayToString(values, ',');
    }
    public static String ArrayToString(String[] values, char delimiter) {
        String result = "";
        int offset = 0;
        
        for(String value : values) {
            if (value.indexOf(delimiter) >= 0) {
                result += "\"" + value + "\"";
            } else result += value;
            
            offset++;

            if (offset < values.length) {
                result += delimiter;
            }
        }
        
        return result;
    }
    
    public static Object[] ArrayToObject(byte[] values) {
        Object[] objects = new Object[values.length];
        
        for(int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        
        return objects;
    }
    
    public static Object[] ArrayToObject(char[] values) {
        Object[] objects = new Object[values.length];
        
        for(int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        
        return objects;
    }
    
    public static Object[] ArrayToObject(short[] values) {
        Object[] objects = new Object[values.length];
        
        for(int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        
        return objects;
    }
    
    public static Object[] ArrayToObject(int[] values) {
        Object[] objects = new Object[values.length];
        
        for(int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        
        return objects;
    }
    
    public static Object[] ArrayToObject(long[] values) {
        Object[] objects = new Object[values.length];
        
        for(int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        
        return objects;
    }
    
    public static Object[] ArrayToObject(float[] values) {
        Object[] objects = new Object[values.length];
        
        for(int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        
        return objects;
    }
    
    public static Object[] ArrayToObject(double[] values) {
        Object[] objects = new Object[values.length];
        
        for(int i = 0; i < values.length; i++) {
            objects[i] = values[i];
        }
        
        return objects;
    }
}
