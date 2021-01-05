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
import java.util.*;

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

    @Autowired
    private SeatMasterDao seatMasterDao;

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

        List<TrainSearchResponse> trainSearchResponseList = new ArrayList<>();
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

                trainTimetableMaster = trainTimetableMasterDao.selectOne(date.toLocalDate(), train.getTrainClass(), train.getTrainName(), toStation.getName());
                arrival = trainTimetableMaster.getArrival();

                if (!date.toLocalTime().isBefore(departure)) {
                    // 乗りたい時刻より出発時刻が前なので除外;
                    continue;
                }

                List<SeatMaster> premium_avail_seats = getAvailableSeats(train, fromStation, toStation, "premium", false);

                List<SeatMaster> premium_smoke_avail_seats = getAvailableSeats(train, fromStation, toStation, "premium", true);

                List<SeatMaster> reserved_avail_seats = getAvailableSeats(train, fromStation, toStation, "reserved", false);

                List<SeatMaster> reserved_smoke_avail_seats = getAvailableSeats(train, fromStation, toStation, "reserved", true);


                String premium_avail = "○";
                if (premium_avail_seats == null || premium_avail_seats.size() == 0) {
                    premium_avail = "×";
                } else if (premium_avail_seats.size() < 10) {
                    premium_avail = "△";
                }

                String premium_smoke_avail = "○";
                if (premium_smoke_avail_seats == null || premium_smoke_avail_seats.size() == 0) {
                    premium_smoke_avail = "×";
                } else if (premium_smoke_avail_seats.size() < 10) {
                    premium_smoke_avail = "△";
                }

                String reserved_avail = "○";
                if (reserved_avail_seats == null || reserved_avail_seats.size() == 0) {
                    reserved_avail = "×";
                } else if (reserved_avail_seats.size() < 10) {
                    reserved_avail = "△";
                }

                String reserved_smoke_avail = "○";
                if (reserved_smoke_avail_seats == null || reserved_smoke_avail_seats.size() == 0) {
                    reserved_smoke_avail = "×";
                } else if (reserved_smoke_avail_seats.size() < 10) {
                    reserved_smoke_avail = "△";
                }

                // 空席情報;
                Map<String, String> seatAvailability = Map.of(
                        "premium", premium_avail,
                        "premium_smoke", premium_smoke_avail,
                        "reserved", reserved_avail,
                        "reserved_smoke", reserved_smoke_avail,
                        "non_reserved", "○"
                );

                // 料金計算;
                int premiumFare = fareCalc(date.toLocalTime(), fromStation.getId(), toStation.getId(), train.getTrainClass(), "premium");
                premiumFare = premiumFare * adult + premiumFare / 2 * child;

                int reservedFare = fareCalc(date.toLocalTime(), fromStation.getId(), toStation.getId(), train.getTrainClass(), "reserved");
                reservedFare = reservedFare * adult + reservedFare / 2 * child;

                int nonReservedFare = fareCalc(date.toLocalTime(), fromStation.getId(), toStation.getId(), train.getTrainClass(), "non-reserved");
                nonReservedFare = nonReservedFare * adult + nonReservedFare / 2 * child;

                Map<String, Integer> fareInformation = Map.of(
                        "premium", premiumFare,
                        "premium_smoke", premiumFare,
                        "reserved", reservedFare,
                        "reserved_smoke", reservedFare,
                        "non_reserved", nonReservedFare
                );

                trainSearchResponseList.add(new TrainSearchResponse(
                        train.getTrainClass(), train.getTrainName(), train.getStartStation(), train.getLastStation(),
                        fromStation.getName(), toStation.getName(), departure.toString(), arrival.toString(), seatAvailability, fareInformation
                ));

                if (trainSearchResponseList.size() >= 10) {
                    break;
                }

            }
        }


        if (trainSearchResponseList == null || trainSearchResponseList.size() == 0) {
            throw new IsuconException("sql: no rows in result set", HttpStatus.BAD_REQUEST);
        }

        return trainSearchResponseList;
    }

    private List<SeatMaster> getAvailableSeats(TrainMaster train, StationMaster fromStation, StationMaster toStation, String seatClass, boolean isSmokingSeat) {
        // 指定種別の空き座席を返す

        // 全ての座席を取得する
        List<SeatMaster> seatList = seatMasterDao.selectSeatList(train.getTrainClass(), seatClass, isSmokingSeat);

        Map<String, SeatMaster> availableSeatMap = new HashMap<>();
        for (SeatMaster seat : seatList) {
            String key = seat.getCarNumber() + "_" + seat.getSeatRow() + seat.getSeatColumn();
            availableSeatMap.put(key, seat);
        }

        // すでに取られている予約を取得する
        List<SeatReservations> seatReservationList = seatReservationsDao.selectReservedSeatList(train.getIsNobori(), fromStation.getId(), toStation.getId());

        for (SeatReservations seatReservation : seatReservationList) {
            String key = seatReservation.getCarNumber() + "_" + seatReservation.getSeatRow() + "_" + seatReservation.getSeatColumn();
            availableSeatMap.remove(key);
        }

        return new ArrayList<>(availableSeatMap.values());
    }


    private int fareCalc(LocalTime date, Long depStation, Long destStation, String trainClass, String seatClass) {
        //;
        // 料金計算メモ;
        // 距離運賃(円) * 期間倍率(繁忙期なら2倍等) * 車両クラス倍率(急行・各停等) * 座席クラス倍率(プレミアム・指定席・自由席);
        //;


        StationMaster fromStation, toStation;

      /*
        query = "SELECT * FROM station_master WHERE id=?";

        // From;
        err = dbx.Get(&fromStation, query, depStation);
        if (err == sql.ErrNoRows ) {
            return 0, err;
        }
        if (err != nil ) {
            return 0, err;
        }

        // To;
        err = dbx.Get(&toStation, query, destStation);
        if (err == sql.ErrNoRows ) {
            return 0, err;
        }
        if (err != nil ) {
            log.Print(err);
            return 0, err;
        }

        log.info("distance", math.Abs(toStation.Distance-fromStation.Distance));
        distFare, err = getDistanceFare(math.Abs(toStation.Distance - fromStation.Distance));
        if (err != nil ) {
            return 0, err;
        }
        log.info("distFare", distFare);

        // 期間・車両・座席クラス倍率;
        fareList = []Fare{};
        query = "SELECT * FROM fare_master WHERE train_class=? AND seat_class=? ORDER BY start_date";
        err = dbx.Select(&fareList, query, trainClass, seatClass);
        if (err != nil ) {
            return 0, err;
        }

        if (len(fareList) == 0 ) {
            return 0, fmt.Errorf("fare_master does not exists");
        }

        selectedFare = fareList[0];
        date = time.Date(date.Year(), date.Month(), date.Day(), 0, 0, 0, 0, time.UTC);
        for _, fare = range fareList {
            if (!date.Before(fare.StartDate) ) {
                log.info(fare.StartDate, fare.FareMultiplier);
                selectedFare = fare;
            }
        }

        log.info("%%%%%%%%%%%%%%%%%%%");

        return int(float64(distFare) * selectedFare.FareMultiplier), nil;
    };
*/
        return 9999;
    }
}