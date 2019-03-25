package com.nettyrpc.test.util;

import java.util.HashMap;
import java.util.Map;

public class Test {
    public static Map<String, Object> setRelMap(Integer code, Object message){
        Map<String, Object> relMap = new HashMap<>();
        relMap.put("code", code);
        relMap.put("message", message);
        return relMap;
    }


    public static void main(String[] args) {
        Map<String, Object> map = setRelMap(100, "message");
        map.put("token", "adsfadsf");
    }
}