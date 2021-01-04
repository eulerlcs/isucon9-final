package jp.zhimingsoft.www.isucon.utils;

import jp.zhimingsoft.www.isucon.domain.StationMaster;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class Utils {
    public static final int AVAILABLE_DAYS = 10;
    public static final String SESSION_NAME = "session_isutrain";
    public static final Map<String, String> TrainClassMap = Collections.unmodifiableMap(
            new HashMap<>() {{
                put("express", "最速");
                put("semi_express", "中間");
                put("local", "遅いやつ");
            }});

    public static boolean checkAvailableDate(ZonedDateTime useAt) {
        ZonedDateTime t = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        t = t.plusDays(AVAILABLE_DAYS);
        return useAt.isBefore(t);
    }


    public static List<String> getUsableTrainClassList(StationMaster fromStation , StationMaster toStation ) {
        Map<String, String>  usable =new HashMap<String,String>();

        for (Map.Entry<String, String> entry: TrainClassMap.entrySet() ){
             usable.put(entry.getKey(),entry.getValue());
        }

        if (!fromStation.getIsStopExpress()) {
            usable.remove( "express");
        }
        if (!fromStation.getIsStopSemiExpress()) {
            usable.remove( "semi_express");
        }
        if (!fromStation.getIsStopLocal()) {
            usable.remove(  "local");
        }

        if (!toStation.getIsStopExpress()) {
            usable.remove(  "express");
        }
        if (!toStation.getIsStopSemiExpress()) {
            usable.remove(  "semi_express");
        }
        if (!toStation.getIsStopLocal ()) {
            usable.remove(  "local");
        }

        return new ArrayList<>( usable.values());
    }
}
