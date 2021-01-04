package jp.zhimingsoft.www.isucon.service;

import jp.zhimingsoft.www.isucon.domain.InitializeResponse;
import jp.zhimingsoft.www.isucon.domain.Settings;
import jp.zhimingsoft.www.isucon.domain.StationMaster;
import jp.zhimingsoft.www.isucon.domain.TrainSearchResponse;

import java.time.ZonedDateTime;
import java.util.List;

public interface MainService {

    InitializeResponse initializeHandler();

    Settings settingsHandler();

    List<StationMaster> getStationsHandler();

    List<TrainSearchResponse> trainSearchHandler(
            ZonedDateTime useAt,
            String trainClass,
            String from,
            String to,
            Integer adult,
            Integer child);
}
