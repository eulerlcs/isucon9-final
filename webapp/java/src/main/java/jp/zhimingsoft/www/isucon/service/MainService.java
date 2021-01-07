package jp.zhimingsoft.www.isucon.service;

import jp.zhimingsoft.www.isucon.domain.*;

import java.time.ZonedDateTime;
import java.util.List;

public interface MainService {

    InitializeResponse initializeHandler();

    Settings settingsHandler();

    List<StationMaster> getStationsHandler();

    /*
     列車検索
         GET /train/search?use_at=<ISO8601形式の時刻> & from=東京 & to=大阪

     return
         料金
         空席情報
         発駅と着駅の到着時刻
    */
    List<TrainSearchResponse> trainSearchHandler(
            ZonedDateTime useAt,
            String trainClass,
            String from,
            String to,
            Integer adult,
            Integer child);

    /*;
       指定した列車の座席列挙;
       GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京;
    */
    CarInformation trainSeatsHandler(ZonedDateTime date,
                                     String trainClass,
                                     String trainName,
                                     int carNumber,
                                     String from,
                                     String to);
}
