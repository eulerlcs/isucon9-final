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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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

    @Autowired
    private DistanceFareMasterDao distanceFareMasterDao;
    @Autowired
    private FareMasterDao fareMasterDao;

    /*
         initialize
     */
    @Override
    public InitializeResponse initializeHandler() {
        seatReservationsDao.truncate();
        reservationsDao.truncate();
        usersDao.truncate();

        InitializeResponse initializeResponse = new InitializeResponse();

        initializeResponse.setLanguage("java");
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
        List<StationMaster> list = stationMasterDao.selectOrderByDistance(false);

        return list;
    }

    /*
         列車検索
             GET /train/search?use_at=<ISO8601形式の時刻> & from=東京 & to=大阪

         return
             料金
             空席情報
             発駅と着駅の到着時刻
     */
    @Override
    public List<TrainSearchResponse> trainSearchHandler(
            ZonedDateTime use_at,
            String trainClass,
            String from,
            String to,
            Integer adult,
            Integer child) {

        ZonedDateTime date = use_at.withZoneSameInstant(ZoneOffset.ofHours(9));
        if (!Utils.checkAvailableDate(date)) {
            throw new IsuconException("予約可能期間外です", HttpStatus.NOT_FOUND);
        }

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
            stations = stationMasterDao.selectOrderByDistance(true);
        } else {
            stations = stationMasterDao.selectOrderByDistance(false);
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
                TrainTimetableMaster trainTimetableMaster = trainTimetableMasterDao.selectOne(date.toLocalDate(), train.getTrainClass(), train.getTrainName(), fromStation.getName());
                LocalTime departure = trainTimetableMaster.getDeparture();

                trainTimetableMaster = trainTimetableMasterDao.selectOne(date.toLocalDate(), train.getTrainClass(), train.getTrainName(), toStation.getName());
                LocalTime arrival = trainTimetableMaster.getArrival();

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
                int premiumFare = fareCalc(date.toLocalDate(), fromStation.getId(), toStation.getId(), train.getTrainClass(), "premium");
                premiumFare = premiumFare * adult + premiumFare / 2 * child;

                int reservedFare = fareCalc(date.toLocalDate(), fromStation.getId(), toStation.getId(), train.getTrainClass(), "reserved");
                reservedFare = reservedFare * adult + reservedFare / 2 * child;

                int nonReservedFare = fareCalc(date.toLocalDate(), fromStation.getId(), toStation.getId(), train.getTrainClass(), "non-reserved");
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
                        fromStation.getName(), toStation.getName(), departure, arrival, seatAvailability, fareInformation
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

    // 指定種別の空き座席を返す
    private List<SeatMaster> getAvailableSeats(TrainMaster train, StationMaster fromStation, StationMaster toStation, String seatClass, boolean isSmokingSeat) {
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

    private int getDistanceFare(double origToDestDistance) {
        List<DistanceFareMaster> distanceFareList = distanceFareMasterDao.selectOrderByDistance();

        double lastDistance = 0.0;
        int lastFare = 0;
        for (DistanceFareMaster distanceFare : distanceFareList) {
            log.info("{} {} {}", origToDestDistance, distanceFare.getDistance(), distanceFare.getFare());

            if (lastDistance < origToDestDistance && origToDestDistance < distanceFare.getDistance()) {
                break;
            }

            lastDistance = distanceFare.getDistance();
            lastFare = distanceFare.getFare();
        }

        return lastFare;
    }

    //;
    // 料金計算メモ;
    // 距離運賃(円) * 期間倍率(繁忙期なら2倍等) * 車両クラス倍率(急行・各停等) * 座席クラス倍率(プレミアム・指定席・自由席);
    //;
    private int fareCalc(LocalDate date, Long depStation, Long destStation, String trainClass, String seatClass) {
        StationMaster fromStation = stationMasterDao.selectById(depStation);
        StationMaster toStation = stationMasterDao.selectById(destStation);
        log.info("distance {}", Math.abs(toStation.getDistance() - fromStation.getDistance()));

        int distFare = getDistanceFare(Math.abs(toStation.getDistance() - fromStation.getDistance()));
        log.info("distFare {}", distFare);

        // 期間・車両・座席クラス倍率;
        List<FareMaster> fareList = fareMasterDao.selectByTrainSeat(trainClass, seatClass);
        if (fareList == null || fareList.size() == 0) {
            log.error("fare_master does not exists");
            return 0;
        }

        FareMaster selectedFare = fareList.get(0);
        for (FareMaster fare : fareList) {
            if (!date.isBefore(fare.getStartDate())) {
                log.info("{} {}", fare.getStartDate(), fare.getFareMultiplier());
                selectedFare = fare;
            }
        }

        log.info("%%%%%%%%%%%%%%%%%%%");

        Double fare = distFare * selectedFare.getFareMultiplier();
        return fare.intValue();
    }

    /*;
       指定した列車の座席列挙;
       GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京;
   */
    @Override
    public CarInformation trainSeatsHandler(ZonedDateTime use_at,
                                            String trainClass,
                                            String trainName,
                                            int carNumber,
                                            String fromName,
                                            String toName) {

        ZonedDateTime date = use_at.withZoneSameInstant(ZoneOffset.ofHours(9));

        if (!Utils.checkAvailableDate(date)) {
            throw new IsuconException("予約可能期間外です", HttpStatus.NOT_FOUND);
        }

        // 対象列車の取得;
        TrainMaster train = trainMasterDao.selectByDateClassName(date.toLocalDate(), trainClass, trainName);
        if (train == null) {
            throw new IsuconException("列車が存在しません", HttpStatus.NOT_FOUND);
        }

        StationMaster fromStation = stationMasterDao.selectByName(fromName);
        if (fromStation == null) {
            throw new IsuconException("fromStation: no rows", HttpStatus.BAD_REQUEST);
        }

        StationMaster toStation = stationMasterDao.selectByName(toName);
        if (toStation == null) {
            throw new IsuconException("ToStation: no rows", HttpStatus.BAD_REQUEST);
        }

        List<String> usableTrainClassList = Utils.getUsableTrainClassList(fromStation, toStation);
        boolean usable = usableTrainClassList.stream().anyMatch(it -> Objects.equals(it, trainClass));
        if (!usable) {
            throw new IsuconException("invalid train_class", HttpStatus.BAD_REQUEST);
        }

        List<SeatMaster> seatList = seatMasterDao.selectByClassNumber(trainClass, carNumber);

        List<SeatInformation> seatInformationList = new ArrayList<>();
        for (SeatMaster seat : seatList) {
            SeatInformation s = new SeatInformation(seat.getSeatRow(), seat.getSeatColumn(), seat.getSeatClass(), seat.getIsSmokingSeat(), false);
            List<SeatReservations> seatReservationList = seatReservationsDao.selectSeatReservationList(
                    date.toLocalDate(),
                    seat.getTrainClass(),
                    trainName,
                    seat.getCarNumber(),
                    seat.getSeatRow(),
                    seat.getSeatColumn()
            );
            log.info(seatReservationList.toString());

            for (SeatReservations seatReservation : seatReservationList) {
                Reservations reservation = reservationsDao.selectById(seatReservation.getReservationId());

                StationMaster departureStation = stationMasterDao.selectByName(reservation.getDeparture());
                if (fromStation == null) {
                    throw new IsuconException("reservation departureStation: no row", HttpStatus.BAD_REQUEST);
                }

                StationMaster arrivalStation = stationMasterDao.selectByName(reservation.getArrival());
                if (toStation == null) {
                    throw new IsuconException("reservation arrivalStation: no row", HttpStatus.BAD_REQUEST);
                }

                if (train.getIsNobori()) {
                    // 上り;
                    if (toStation.getId() < arrivalStation.getId() && fromStation.getId() <= arrivalStation.getId()) {
                        // pass;
                    } else if (toStation.getId() >= departureStation.getId() && fromStation.getId() > departureStation.getId()) {
                        // pass;
                    } else {
                        s.setIsOccupied(true);
                    }
                } else {
                    // 下り;
                    if (fromStation.getId() < departureStation.getId() && toStation.getId() <= departureStation.getId()) {
                        // pass;
                    } else if (fromStation.getId() >= arrivalStation.getId() && toStation.getId() > arrivalStation.getId()) {
                        // pass;
                    } else {
                        s.setIsOccupied(true);
                    }
                }
            }
            log.info("{}", s.getIsOccupied());
            seatInformationList.add(s);
        }
        // 各号車の情報;
        List<SimpleCarInformation> simpleCarInformationList = new ArrayList<>();

        int i = 1;
        for (; ; i++) {
            SeatMaster seat = seatMasterDao.selectOneByClassNumber(trainClass, i);
            if (seat == null) {
                break;
            }

            simpleCarInformationList.add(new SimpleCarInformation(i, seat.getSeatClass()));
        }

        CarInformation c = new CarInformation(date.toLocalDate(), trainClass, trainName,
                carNumber, seatInformationList, simpleCarInformationList);

        return c;
    }
}