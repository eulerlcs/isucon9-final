package jp.zhimingsoft.www.isucon.service.impl;

import jp.zhimingsoft.www.isucon.dao.*;
import jp.zhimingsoft.www.isucon.domain.*;
import jp.zhimingsoft.www.isucon.exception.IsuconException;
import jp.zhimingsoft.www.isucon.service.MainService;
import jp.zhimingsoft.www.isucon.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class MainServiceImpl implements MainService {
    @Autowired
    private SeatReservationsDao seatReservationsDao;
    @Autowired
    private ReservationsDao reservationsDao;
    @Autowired
    private UsersDao usersDao;

    @Autowired
    private StationMasterDao stationMasterDao;
    @Autowired
    private TrainMasterDao trainMasterDao;

    @Autowired
    private TrainTimetableMasterDao trainTimetableMasterDao;

    @Override
    public InitializeResponse initializeHandler() {
        /*
            initialize
        */

        seatReservationsDao.truncate();
        reservationsDao.truncate();
        usersDao.truncate();

        InitializeResponse initializeResponse = new InitializeResponse();

        initializeResponse.setLanguage("golang");
        initializeResponse.setAvailableDays(Utils.AVAILABLE_DAYS);

        return initializeResponse;
    }

    @Override
    public Settings settingsHandler() {
        String paymentApi = System.getenv().getOrDefault("PAYMENT_API", "http://localhost:5000");

        Settings settings = new Settings();
        settings.setPaymentAPI(paymentApi);

        return settings;
    }

    @Override
    public List<StationMaster> getStationsHandler() {
        List<StationMaster> list = stationMasterDao.selectAllByDistanceAsc();

        return list;
    }

    @Override
    public List<TrainSearchResponse> trainSearchHandler(
            ZonedDateTime use_at,
            String trainClass,
            String from,
            String to,
            Integer adult,
            Integer child) {

        /*
            列車検索
                GET /train/search?use_at=<ISO8601形式の時刻> & from=東京 & to=大阪

            return
                料金
                空席情報
                発駅と着駅の到着時刻

        */

        List<TrainSearchResponse> list = new ArrayList<>();


        if (!Utils.checkAvailableDate(use_at)) {
            throw new IsuconException("予約可能期間外です", HttpStatus.NOT_FOUND);
        }

        LocalDateTime date = use_at.withZoneSameInstant(ZoneOffset.ofHours(9)).toLocalDateTime();

        StationMaster fromStation = stationMasterDao.selectByName(from);
        if (fromStation == null) {
            throw new IsuconException("fromStation: no rows", HttpStatus.BAD_REQUEST);
        }

        StationMaster toStation = stationMasterDao.selectByName(to);
        if (toStation == null) {
            throw new IsuconException("ToStation: no rows", HttpStatus.BAD_REQUEST);
        }

        log.info("From {}", fromStation);
        log.info("To {}", toStation);

        boolean isNobori = false;
        if (fromStation.getDistance() > toStation.getDistance()) {
            isNobori = true;
        }


        List<StationMaster> stations = null;
        if (isNobori) {
            stations = stationMasterDao.selectAllByDistanceDesc();
        } else {
            stations = stationMasterDao.selectAllByDistanceAsc();
        }

        List<String> usableTrainClassList = null;
        if (trainClass == null || trainClass.isEmpty()) {
            usableTrainClassList = Utils.getUsableTrainClassList(fromStation, toStation);
        } else {
            usableTrainClassList = new ArrayList<>() {{
                add(trainClass);
            }};
        }
        List<TrainMaster> trainList = trainMasterDao.selectByDateClassNobori(date.toLocalDate(), usableTrainClassList, isNobori);


        List<TrainSearchResponse> trainSearchResponseList = null;

        for (TrainMaster train : trainList) {
            boolean isSeekedToFirstStation = false;
            boolean isContainsOriginStation = false;
            boolean isContainsDestStation = false;
            int i = 0;

            for (StationMaster station : stations) {
                if (!isSeekedToFirstStation) {
                    // 駅リストを列車の発駅まで読み飛ばして頭出しをする;
                    // 列車の発駅以前は止まらないので無視して良い;
                    if (Objects.equals(station.getName(), train.getStartStation())) {
                        isSeekedToFirstStation = true;
                    } else {
                        continue;
                    }
                }

                if (Objects.equals(station.getId(), fromStation.getId())) {
                    // 発駅を経路中に持つ編成の場合フラグを立てる;
                    isContainsOriginStation = true;
                }

                if (Objects.equals(station.getId(), toStation.getId())) {
                    if (isContainsOriginStation) {
                        // 発駅と着駅を経路中に持つ編成の場合;
                        isContainsDestStation = true;
                        break;
                    } else {
                        // 出発駅より先に終点が見つかったとき;
                        log.info("なんかおかしい");
                        break;
                    }
                }

                if (Objects.equals(station.getName(), train.getLastStation())) {
                    // 駅が見つからないまま当該編成の終点に着いてしまったとき;
                    break;
                }

                i++;
            }

            if (isContainsOriginStation && isContainsDestStation) {
                // 列車情報;

                // 所要時間;
                LocalTime departure, arrival;

                TrainTimetableMaster trainTimetableMaster = trainTimetableMasterDao.selectOne(date.toLocalDate(), train.getTrainClass(), train.getTrainName(), fromStation.getName());
                departure = trainTimetableMaster.getDeparture();
                arrival = trainTimetableMaster.getArrival();

                if (!date.toLocalTime().isBefore(departure) ) {
                    // 乗りたい時刻より出発時刻が前なので除外;
                    continue;
                }

                  /*

                premium_avail_seats, err = train.getAvailableSeats(fromStation, toStation, "premium", false);
                if (err != nil ) {
                    errorResponse(w, http.StatusBadRequest, err.Error());
                    return;
                }
                premium_smoke_avail_seats, err = train.getAvailableSeats(fromStation, toStation, "premium", true);
                if (err != nil ) {
                    errorResponse(w, http.StatusBadRequest, err.Error());
                    return;
                }

                reserved_avail_seats, err = train.getAvailableSeats(fromStation, toStation, "reserved", false);
                if (err != nil ) {
                    errorResponse(w, http.StatusBadRequest, err.Error());
                    return;
                }
                reserved_smoke_avail_seats, err = train.getAvailableSeats(fromStation, toStation, "reserved", true);
                if (err != nil ) {
                    errorResponse(w, http.StatusBadRequest, err.Error());
                    return;
                }

                premium_avail = "○";
                if (len(premium_avail_seats) == 0 ) {
                    premium_avail = "×";
                } else if (len(premium_avail_seats) < 10 ) {
                    premium_avail = "△";
                }

                premium_smoke_avail = "○";
                if (len(premium_smoke_avail_seats) == 0 ) {
                    premium_smoke_avail = "×";
                } else if (len(premium_smoke_avail_seats) < 10 ) {
                    premium_smoke_avail = "△";
                }

                reserved_avail = "○";
                if (len(reserved_avail_seats) == 0 ) {
                    reserved_avail = "×";
                } else if (len(reserved_avail_seats) < 10 ) {
                    reserved_avail = "△";
                }

                reserved_smoke_avail = "○";
                if (len(reserved_smoke_avail_seats) == 0 ) {
                    reserved_smoke_avail = "×";
                } else if (len(reserved_smoke_avail_seats) < 10 ) {
                    reserved_smoke_avail = "△";
                }

                // 空席情報;
                seatAvailability = map[string]string{
                    "premium":        premium_avail,;
                    "premium_smoke":  premium_smoke_avail,;
                    "reserved":       reserved_avail,;
                    "reserved_smoke": reserved_smoke_avail,;
                    "non_reserved":   "○",;
                }

                // 料金計算;
                premiumFare, err = fareCalc(date, fromStation.ID, toStation.ID, train.TrainClass, "premium");
                if (err != nil ) {
                    errorResponse(w, http.StatusBadRequest, err.Error());
                    return;
                }
                premiumFare = premiumFare*adult + premiumFare/2*child;

                reservedFare, err = fareCalc(date, fromStation.ID, toStation.ID, train.TrainClass, "reserved");
                if (err != nil ) {
                    errorResponse(w, http.StatusBadRequest, err.Error());
                    return;
                }
                reservedFare = reservedFare*adult + reservedFare/2*child;

                nonReservedFare, err = fareCalc(date, fromStation.ID, toStation.ID, train.TrainClass, "non-reserved");
                if (err != nil ) {
                    errorResponse(w, http.StatusBadRequest, err.Error());
                    return;
                }
                nonReservedFare = nonReservedFare*adult + nonReservedFare/2*child;

                fareInformation = map[string]int{
                    "premium":        premiumFare,;
                    "premium_smoke":  premiumFare,;
                    "reserved":       reservedFare,;
                    "reserved_smoke": reservedFare,;
                    "non_reserved":   nonReservedFare,;
                }

                trainSearchResponseList = append(trainSearchResponseList, TrainSearchResponse{
                    train.TrainClass, train.TrainName, train.StartStation, train.LastStation,;
                    fromStation.Name, toStation.Name, departure, arrival, seatAvailability, fareInformation,;
                });

                if (len(trainSearchResponseList) >= 10 ) {
                    break;
                }

                 */
            }
        }


        if (list == null || list.size() == 0) {
            throw new IsuconException("sql: no rows in result set", HttpStatus.BAD_REQUEST);
        }

        return list;
    }
}
