package jp.zhimingsoft.www.isucon.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.zhimingsoft.www.isucon.dao.*;
import jp.zhimingsoft.www.isucon.domain.*;
import jp.zhimingsoft.www.isucon.exception.IsuconException;
import jp.zhimingsoft.www.isucon.service.MainService;
import jp.zhimingsoft.www.isucon.utils.MessageResponse;
import jp.zhimingsoft.www.isucon.utils.SecureUtil;
import jp.zhimingsoft.www.isucon.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class MainServiceImpl implements MainService {
    @Autowired
    HttpSession session;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RestTemplate restTemplate;

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
        List<SeatReservations> seatReservationList = seatReservationsDao.selectReservedSeatList(train.isNobori(), fromStation.getId(), toStation.getId());

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

    //
    // 料金計算メモ
    // 距離運賃(円) * 期間倍率(繁忙期なら2倍等) * 車両クラス倍率(急行・各停等) * 座席クラス倍率(プレミアム・指定席・自由席)
    //
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

    /*
       指定した列車の座席列挙
       GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京
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
            SeatInformation s = new SeatInformation(seat.getSeatRow(), seat.getSeatColumn(), seat.getSeatClass(), seat.isSmokingSeat(), false);
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
                Reservations reservation = reservationsDao.selectByReservationId(seatReservation.getReservationId());

                StationMaster departureStation = stationMasterDao.selectByName(reservation.getDeparture());
                if (fromStation == null) {
                    throw new IsuconException("reservation departureStation: no row", HttpStatus.BAD_REQUEST);
                }

                StationMaster arrivalStation = stationMasterDao.selectByName(reservation.getArrival());
                if (toStation == null) {
                    throw new IsuconException("reservation arrivalStation: no row", HttpStatus.BAD_REQUEST);
                }

                if (train.isNobori()) {
                    // 上り;
                    if (toStation.getId() < arrivalStation.getId() && fromStation.getId() <= arrivalStation.getId()) {
                        // pass;
                    } else if (toStation.getId() >= departureStation.getId() && fromStation.getId() > departureStation.getId()) {
                        // pass;
                    } else {
                        s.setOccupied(true);
                    }
                } else {
                    // 下り;
                    if (fromStation.getId() < departureStation.getId() && toStation.getId() <= departureStation.getId()) {
                        // pass;
                    } else if (fromStation.getId() >= arrivalStation.getId() && toStation.getId() > arrivalStation.getId()) {
                        // pass;
                    } else {
                        s.setOccupied(true);
                    }
                }
            }
            log.info("{}", s.isOccupied());
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

    /*
		列車の席予約API　支払いはまだ;
		POST /api/train/reserve;
			{
				"date": "2020-12-31T07:57:00+09:00",;
				"train_name": "183",;
				"train_class": "中間",;
				"car_number": 7,;
				"is_smoking_seat": false,;
				"seat_class": "reserved",;
				"departure": "東京",;
				"arrival": "名古屋",;
				"child": 2,;
				"adult": 1,;
				"column": "A",;
				"seats": [;
					{
					"row": 3,;
					"column": "B";
					},;
						{
					"row": 4,;
					"column": "C";
					}
				];
		}
		レスポンスで予約IDを返す;
		reservationResponse(w http.ResponseWriter, errCode int, id int, ok bool, message string);
	*/
    @Override
    @Transactional
    public TrainReservationResponse trainReservationHandler(TrainReservationRequest req) {
        // 乗車日の日付表記統一;
        ZonedDateTime date = req.getDate().withZoneSameInstant(ZoneOffset.ofHours(9));
        if (!Utils.checkAvailableDate(date)) {
            throw new IsuconException("予約可能期間外です", HttpStatus.NOT_FOUND);
        }

        // 止まらない駅の予約を取ろうとしていないかチェックする;
        // 列車データを取得;
        TrainMaster tmas = trainMasterDao.selectByDateClassName(date.toLocalDate(), req.getTrainClass(), req.getTrainName());
        if (tmas == null) {
            throw new IsuconException("列車データがみつかりません", HttpStatus.NOT_FOUND);
        }

        // 列車自体の駅IDを求める;
        // Departure;
        StationMaster departureStation = stationMasterDao.selectByName(tmas.getStartStation());
        if (departureStation == null) {
            throw new IsuconException("リクエストされた列車の始発駅データがみつかりません", HttpStatus.NOT_FOUND);
        }

        // Arrive;
        StationMaster arrivalStation = stationMasterDao.selectByName(tmas.getLastStation());
        if (arrivalStation == null) {
            throw new IsuconException("リクエストされた列車の終着駅データがみつかりません", HttpStatus.NOT_FOUND);
        }

        // リクエストされた乗車区間の駅IDを求める;
        // From;
        StationMaster fromStation = stationMasterDao.selectByName(req.getDeparture());
        if (fromStation == null) {
            throw new IsuconException("乗車駅データがみつかりません " + req.getDeparture(), HttpStatus.NOT_FOUND);
        }

        // To;
        StationMaster toStation = stationMasterDao.selectByName(req.getArrival());
        if (toStation == null) {
            throw new IsuconException("降車駅データがみつかりません " + req.getArrival(), HttpStatus.NOT_FOUND);
        }


        switch (req.getTrainClass()) {
            case "最速":
                if (!fromStation.isStopExpress() || !toStation.isStopExpress()) {
                    throw new IsuconException("最速の止まらない駅です", HttpStatus.BAD_REQUEST);
                }
                break;
            case "中間":
                if (!fromStation.isStopSemiExpress() || !toStation.isStopSemiExpress()) {
                    throw new IsuconException("中間の止まらない駅です", HttpStatus.BAD_REQUEST);
                }
                break;
            case "遅いやつ":
                if (!fromStation.isStopLocal() || !toStation.isStopLocal()) {
                    throw new IsuconException("遅いやつの止まらない駅です", HttpStatus.BAD_REQUEST);
                }
                break;
            default:
                throw new IsuconException("リクエストされた列車クラスが不明です", HttpStatus.BAD_REQUEST);
        }


        // 運行していない区間を予約していないかチェックする;
        if (tmas.isNobori()) {
            if (fromStation.getId() > departureStation.getId() || toStation.getId() > departureStation.getId()) {
                throw new IsuconException("リクエストされた区間に列車が運行していない区間が含まれています", HttpStatus.BAD_REQUEST);
            }
            if (arrivalStation.getId() >= fromStation.getId() || arrivalStation.getId() > toStation.getId()) {
                throw new IsuconException("リクエストされた区間に列車が運行していない区間が含まれています", HttpStatus.BAD_REQUEST);
            }
        } else {
            if (fromStation.getId() < departureStation.getId() || toStation.getId() < departureStation.getId()) {
                throw new IsuconException("リクエストされた区間に列車が運行していない区間が含まれています", HttpStatus.BAD_REQUEST);
            }
            if (arrivalStation.getId() <= fromStation.getId() || arrivalStation.getId() < toStation.getId()) {
                throw new IsuconException("リクエストされた区間に列車が運行していない区間が含まれています", HttpStatus.BAD_REQUEST);
            }
        }

        /*
            あいまい座席検索;
            seatsが空白の時に発動する;
        */
        switch (req.getSeats().size()) {
            case 0:
                ;
                if (Objects.equals(req.getSeatClass(), "non-reserved")) {
                    // non-reservedはそもそもあいまい検索もせずダミーのRow/Columnで予約を確定させる。
                    break;
                }
                //当該列車・号車中の空き座席検索;
                TrainMaster train = trainMasterDao.selectByDateClassName(date.toLocalDate(), req.getTrainClass(), req.getTrainName());
                if (train == null) {
                    throw new IsuconException("列車が存在しません", HttpStatus.NOT_FOUND);
                }

                List<String> usableTrainClassList = Utils.getUsableTrainClassList(fromStation, toStation);
                boolean usable = usableTrainClassList.stream().anyMatch(it -> Objects.equals(it, train.getTrainClass()));

                if (!usable) {
                    throw new IsuconException("invalid train_class", HttpStatus.BAD_REQUEST);
                }

                // 座席リクエスト情報は空に
                req.getSeats().clear();

                for (int carnum = 1; carnum <= 16; carnum++) {
                    List<SeatMaster> seatList = seatMasterDao.selectOne4(req.getTrainClass(), carnum, req.getSeatClass(), req.isSmokingSeat());
                    if (seatList == null) {
                        throw new IsuconException("error", HttpStatus.BAD_REQUEST);
                    }

                    List<SeatInformation> seatInformationList = new ArrayList<>();
                    for (SeatMaster seat : seatList) {
                        SeatInformation s = new SeatInformation(seat.getSeatRow(), seat.getSeatColumn(), seat.getSeatClass(), seat.isSmokingSeat(), false);
                        List<SeatReservations> seatReservationList = seatReservationsDao.selectSeatReservationListForUpdate(
                                date.toLocalDate(),
                                seat.getTrainClass(),
                                req.getTrainName(),
                                seat.getCarNumber(),
                                seat.getSeatRow(),
                                seat.getSeatColumn()
                        );


                        if (seatReservationList == null) {
                            throw new IsuconException("error", HttpStatus.BAD_REQUEST);
                        }

                        for (SeatReservations seatReservation : seatReservationList) {
                            Reservations reservation = reservationsDao.selectByIdForUpdate(seatReservation.getReservationId());

                            departureStation = stationMasterDao.selectByName(reservation.getDeparture());
                            if (departureStation == null) {
                                throw new IsuconException("error", HttpStatus.NOT_FOUND);
                            }
                            arrivalStation = stationMasterDao.selectByName(reservation.getArrival());
                            if (arrivalStation == null) {
                                throw new IsuconException("error", HttpStatus.NOT_FOUND);
                            }


                            if (train.isNobori()) {
                                // 上り
                                if (toStation.getId() < arrivalStation.getId() && fromStation.getId() <= arrivalStation.getId()) {
                                    // pass
                                } else if (toStation.getId() >= departureStation.getId() && fromStation.getId() > departureStation.getId()) {
                                    // pass
                                } else {
                                    s.setOccupied(true);
                                }
                            } else {
                                // 下り
                                if (fromStation.getId() < departureStation.getId() && toStation.getId() <= departureStation.getId()) {
                                    // pass
                                } else if (fromStation.getId() >= arrivalStation.getId() && toStation.getId() > arrivalStation.getId()) {
                                    // pass
                                } else {
                                    s.setOccupied(true);
                                }
                            }

                        }

                        seatInformationList.add(s);
                    }

                    // 曖昧予約席とその他の候補席を選出;
                    int seatnum;            // 予約する座席の合計数;
                    boolean reserved;          // あいまい指定席確保済フラグ;
                    boolean vargue;           // あいまい検索フラグ;
                    RequestSeat VagueSeat = new RequestSeat(); // あいまい指定席保存用;
                    reserved = false;
                    vargue = true;
                    seatnum = (req.getAdult() + req.getChild() - 1); // 全体の人数からあいまい指定席分を引いておく;
                    if (!StringUtils.hasLength(req.getColumn())) {                 // A/B/C/D/Eを指定しなければ、空いている適当な指定席を取るあいまいモード;
                        seatnum = req.getAdult() + req.getChild(); // あいまい指定せず大人＋小人分の座席を取る;
                        reserved = true;                   // dummy;
                        vargue = false;                    // dummy;
                    }
                    RequestSeat CandidateSeat = null;
                    List<RequestSeat> CandidateSeats = new ArrayList<>();

                    // シート分だけ回して予約できる席を検索;
                    int i = 0;
                    for (SeatInformation seat : seatInformationList) {
                        if (Objects.equals(seat.getColumn(), req.getColumn()) && !seat.isOccupied() && !reserved && vargue) { // あいまい席があいてる;
                            VagueSeat.setRow(seat.getRow());
                            VagueSeat.setColumn(seat.getColumn());
                            reserved = true;
                        } else if (!seat.isOccupied() && i < seatnum) { // 単に席があいてる;
                            CandidateSeat = new RequestSeat();
                            CandidateSeat.setRow(seat.getRow());
                            CandidateSeat.setColumn(seat.getColumn());
                            CandidateSeats.add(CandidateSeat);
                            i++;
                        }
                    }

                    if (vargue && reserved) { // あいまい席が見つかり、予約できそうだった;
                        req.getSeats().add(VagueSeat); // あいまい予約席を追加;
                    }
                    if (i > 0) { // 候補席があった;
                        req.getSeats().addAll(CandidateSeats); // 予約候補席追加;
                    }

                    if (req.getSeats().size() < req.getAdult() + req.getChild()) {
                        // リクエストに対して席数が足りてない;
                        // 次の号車にうつしたい;
                        log.info("-----------------");
                        log.info("現在検索中の車両: {}号車, リクエスト座席数: {}, 予約できそうな座席数: {}, 不足数: {}", carnum, req.getAdult() + req.getChild(), req.getSeats().size(), req.getAdult() + req.getChild() - req.getSeats().size());
                        log.info("リクエストに対して座席数が不足しているため、次の車両を検索します。");
                        req.setSeats(new ArrayList<>());
                        ;
                        if (carnum == 16) {
                            log.info("この新幹線にまとめて予約できる席数がなかったから検索をやめるよ");
                            req.setSeats(new ArrayList<>());

                            break;
                        }
                    }
                    log.info("空き実績: {}号車 シート:{} 席数:{}\n", carnum, req.getSeats(), req.getSeats().size());
                    if (req.getSeats().size() >= req.getAdult() + req.getChild()) {
                        log.info("予約情報に追加したよ");
                        req.setSeats(req.getSeats().subList(0, req.getAdult() + req.getChild()));
                        req.setCarNumber(carnum);
                        break;
                    }
                }
                if (req.getSeats().size() == 0) {
                    throw new IsuconException("あいまい座席予約ができませんでした。指定した席、もしくは1車両内に希望の席数をご用意できませんでした。", HttpStatus.NOT_FOUND);
                }
                break;
            default:
                // 座席情報のValidate;
                SeatMaster seatList = null;
                for (RequestSeat z : req.getSeats()) {
                    log.info("XXXX", z);

                    seatList = seatMasterDao.selectOne5(
                            req.getTrainClass(),
                            req.getCarNumber(),
                            z.getColumn(),
                            z.getRow(),
                            req.getSeatClass()
                    );
                    if (seatList == null) {
                        throw new IsuconException("リクエストされた座席情報は存在しません。号車・喫煙席・座席クラスなど組み合わせを見直してください", HttpStatus.NOT_FOUND);
                    }
                }

                break;
        }

        // 当該列車・列車名の予約一覧取得;
        List<Reservations> reservations = reservationsDao.selectByDateClassNameForUpdate(
                date.toLocalDate(),
                req.getTrainClass(),
                req.getTrainName()
        );
        if (reservations == null) {
            throw new IsuconException("列車予約情報の取得に失敗しました", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        for (Reservations reservation : reservations) {
            if (Objects.equals(req.getSeatClass(), "non-reserved")) {
                break;
            }

            // train_masterから列車情報を取得(上り・下りが分かる);
            tmas = trainMasterDao.selectByDateClassName(date.toLocalDate(), req.getTrainClass(), req.getTrainName());
            if (tmas == null) {
                throw new IsuconException("列車データがみつかりません", HttpStatus.NOT_FOUND);
            }

            // 予約情報の乗車区間の駅IDを求める;
            // From;
            StationMaster reservedfromStation = stationMasterDao.selectByName(reservation.getDeparture());
            if (reservedfromStation == null) {
                throw new IsuconException("予約情報に記載された列車の乗車駅データがみつかりません", HttpStatus.NOT_FOUND);
            }
            // To;
            StationMaster reservedtoStation = stationMasterDao.selectByName(reservation.getArrival());
            if (reservedfromStation == null) {
                throw new IsuconException("予約情報に記載された列車の降車駅データがみつかりません", HttpStatus.NOT_FOUND);
            }

            // 予約の区間重複判定;
            boolean secdup = false;
            if (tmas.isNobori()) {
                // 上り;
                if (toStation.getId() < reservedtoStation.getId() && fromStation.getId() <= reservedtoStation.getId()) {
                    // pass;
                } else if (toStation.getId() >= reservedfromStation.getId() && fromStation.getId() > reservedfromStation.getId()) {
                    // pass;
                } else {
                    secdup = true;
                }
            } else {
                // 下り;
                if (fromStation.getId() < reservedfromStation.getId() && toStation.getId() <= reservedfromStation.getId()) {
                    // pass;
                } else if (fromStation.getId() >= reservedtoStation.getId() && toStation.getId() > reservedtoStation.getId()) {
                    // pass;
                } else {
                    secdup = true;
                }
            }

            if (secdup) {
                // 区間重複の場合は更に座席の重複をチェックする;
                List<SeatReservations> seatReservations = seatReservationsDao.selectByIdForUpdate(reservation.getReservationId());

                if (seatReservations == null) {
                    throw new IsuconException("座席予約情報の取得に失敗しました", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                for (SeatReservations v : seatReservations) {
                    for (RequestSeat seat : req.getSeats()) {
                        if (Objects.equals(v.getCarNumber(), req.getCarNumber()) && Objects.equals(v.getSeatRow(), seat.getRow()) && Objects.equals(v.getSeatColumn(), seat.getColumn())) {
                            log.info("Duplicated ", reservation);
                            throw new IsuconException("リクエストに既に予約された席が含まれています", HttpStatus.BAD_REQUEST);
                        }
                    }
                }
            }
        }
        // 3段階の予約前チェック終わり

        // 自由席は強制的にSeats情報をダミーにする（自由席なのに席指定予約は不可）
        if (Objects.equals(req.getSeatClass(), "non-reserved")) {
            req.setSeats(new ArrayList<RequestSeat>());
            RequestSeat dummySeat = new RequestSeat();
            req.setCarNumber(0);
            for (int num = 0; num < req.getAdult() + req.getChild(); num++) {
                dummySeat.setRow(0);
                dummySeat.setColumn("");
                req.getSeats().add(dummySeat);
            }
        }


        // 運賃計算;
        int fare;
        switch (req.getSeatClass()) {
            case "premium":
                ;
                fare = fareCalc(date.toLocalDate(), fromStation.getId(), toStation.getId(), req.getTrainClass(), "premium");
                break;
            case "reserved":
                ;
                fare = fareCalc(date.toLocalDate(), fromStation.getId(), toStation.getId(), req.getTrainClass(), "reserved");
                break;
            case "non-reserved":
                ;
                fare = fareCalc(date.toLocalDate(), fromStation.getId(), toStation.getId(), req.getTrainClass(), "non-reserved");
                break;

            default:
                throw new IsuconException("リクエストされた座席クラスが不明です", HttpStatus.BAD_REQUEST);
        }
        int sumFare = (req.getAdult() * fare) + (req.getChild() * fare) / 2;
        log.info("SUMFARE");

        // userID取得。ログインしてないと怒られる。
        Users user = getUser();

        //予約ID発行と予約情報登録;
        Reservations result = new Reservations(
                -1L,
                user.getId(),
                date.toLocalDate(),
                req.getTrainClass(),
                req.getTrainName(),
                req.getDeparture(),
                req.getArrival(),
                "requesting",
                "a",
                req.getAdult(),
                req.getChild(),
                Long.valueOf(sumFare)
        );
        int ret = reservationsDao.insert(result);
        if (ret != 1) {
            throw new IsuconException("予約の保存に失敗しました。", HttpStatus.BAD_REQUEST);
        }


        Long id = result.getReservationId(); //予約ID;
        if (id <= 0) {
            throw new IsuconException("予約IDの取得に失敗しました", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        //席の予約情報登録
        //reservationsレコード1に対してseat_reservationstが1以上登録される
        for (RequestSeat v : req.getSeats()) {
            SeatReservations seatReservations = new SeatReservations(id, req.getCarNumber(), v.getRow(), v.getColumn());
            ret = seatReservationsDao.insert(seatReservations);
            if (ret != 1) {
                throw new IsuconException("座席予約の登録に失敗しました", HttpStatus.BAD_REQUEST);
            }
        }

        TrainReservationResponse rr = new TrainReservationResponse(id, sumFare, true);
        return rr;
    }

    /*
		支払い及び予約確定API
		POST /api/train/reservation/commit
		{
			"card_token": "161b2f8f-791b-4798-42a5-ca95339b852b",
			"reservation_id": "1"
		}

		前段でフロントがクレカ非保持化対応用のpayment-APIを叩き、card_tokenを手に入れている必要がある
		レスポンスは成功か否かのみ返す
	*/
    @Override
    @Transactional
    public ReservationPaymentResponse reservationPaymentHandler(ReservationPaymentRequest req) {
        // 予約IDで検索;
        Reservations reservation = reservationsDao.selectByReservationId(req.getReservationId().longValue());
        if (reservation == null) {
            throw new IsuconException("予約情報がみつかりません", HttpStatus.NOT_FOUND);
        }

        // 支払い前のユーザチェック。本人以外のユーザの予約を支払ったりキャンセルできてはいけない。
        Users user = getUser();

        if (!Objects.equals(reservation.getUserId(), user.getId())) {
            throw new IsuconException("他のユーザIDの支払いはできません", HttpStatus.FORBIDDEN);
        }

        // 予約情報の支払いステータス確認
        switch (reservation.getStatus()) {
            case "done":
                throw new IsuconException("既に支払いが完了している予約IDです", HttpStatus.FORBIDDEN);
            default:
                break;
        }

        // 決済する
        PaymentInformationRequest paymentInformationRequest = new PaymentInformationRequest(req.getCardToken(), req.getReservationId(), reservation.getAmount().intValue());
        PaymentInformation paymentInfo = new PaymentInformation(paymentInformationRequest);
        String payment_api = System.getenv("PAYMENT_API");
        if (!StringUtils.hasLength(payment_api)) {
            payment_api = "http://payment:5000";
        }

        ResponseEntity<PaymentResponse> resp = null;
        try {
            resp = restTemplate.postForEntity(payment_api + "/payment", paymentInfo, PaymentResponse.class);
        } catch (RestClientException e) {
            HttpStatus httpStatus = resp == null ? HttpStatus.INTERNAL_SERVER_ERROR : resp.getStatusCode();
            throw new IsuconException("HTTP POSTに失敗しました", httpStatus);
        }

        // リクエスト失敗
        if (!Objects.equals(resp.getStatusCode(), HttpStatus.OK)) {
            throw new IsuconException("決済に失敗しました。カードトークンや支払いIDが間違っている可能性があります", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // リクエスト取り出し
        PaymentResponse output = resp.getBody();
        if (output == null) {
            throw new IsuconException("JSON parseに失敗しました", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 予約情報の更新
        Reservations record = new Reservations();
        record.setReservationId(req.getReservationId().longValue());
        record.setStatus("done");
        record.setPaymentId(output.getPaymentId());

        int ret = reservationsDao.update(record);
        if (ret != 1) {
            throw new IsuconException("予約情報の更新に失敗しました", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ReservationPaymentResponse rr = new ReservationPaymentResponse(true);

        return rr;
    }


    private Users getUser() {
        // userID取得
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            throw new IsuconException("no session", HttpStatus.UNAUTHORIZED);
        }
        Users user = usersDao.selectById(userId);
        if (user == null) {
            throw new IsuconException("user not found", HttpStatus.UNAUTHORIZED);
        }

        return user;
    }


    /*
        ログイン
        POST /auth/login
    */
    @Override
    public MessageResponse loginHandler(Users postUser) {
        Users user = usersDao.selectByEmail(postUser.getEmail());
        if (user == null) {
            throw new IsuconException("authentication failed", HttpStatus.FORBIDDEN);
        }

        byte[] challengePassword = SecureUtil.getSecurePassword(postUser.getPassword(), user.getSalt());
        if (challengePassword == null) {
            throw new IsuconException("authentication failed", HttpStatus.FORBIDDEN);
        }
        if (!Arrays.equals(user.getSuperSecurePassword(), challengePassword)) {
            throw new IsuconException("authentication failed", HttpStatus.FORBIDDEN);
        }

        session.setAttribute("user_id", user.getId());

        return new MessageResponse("autheticated");
    }

    /*
       ユーザー登録
       POST /auth/signup
   */
    @Override
    @Transactional
    public MessageResponse signUpHandler(Users postUser) {
        // TODO: validation;
        byte[] salt = SecureUtil.generateSalt(1024);
        if (salt == null) {
            throw new IsuconException("salt generator error", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        byte[] superSecurePassword = SecureUtil.getSecurePassword(postUser.getPassword(), salt);

        Users user = new Users();
        user.setEmail(postUser.getEmail());
        user.setSalt(salt);
        user.setSuperSecurePassword(superSecurePassword);

        int ret = usersDao.insert(user);
        if (ret != 1) {
            throw new IsuconException("user registration failed", HttpStatus.BAD_REQUEST);
        }

        return new MessageResponse("registration complete");
    }

    /*
       認証情報取得
       GET /auth
    */
    @Override
    public AuthResponse getAuthHandler() {
        Users user = getUser();

        AuthResponse resp = new AuthResponse(user.getEmail());
        return resp;
    }

    /*
        ログアウト
        POST /auth/logout
    */
    @Override
    public MessageResponse logoutHandler() {
        session.setAttribute("user_id", 0L);
        return new MessageResponse("logged out");
    }

    /*
        予約取得
        GET /user/reservations
    */
    @Override
    public List<ReservationResponse> userReservationsHandler() {
        Users user = getUser();

        List<Reservations> reservationList = reservationsDao.selectByUserId(user.getId());
        if (reservationList == null) {
            throw new IsuconException("error", HttpStatus.BAD_REQUEST);
        }

        List<ReservationResponse> reservationResponseList = new ArrayList<>();

        for (Reservations r : reservationList) {
            ReservationResponse res = makeReservationResponse(r);
            if (res == null) {
                throw new IsuconException("error", HttpStatus.BAD_REQUEST);
            }
            reservationResponseList.add(res);
        }

        return reservationResponseList;
    }

    private ReservationResponse makeReservationResponse(Reservations reservation) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

        TrainTimetableMaster ttm = trainTimetableMasterDao.selectOne(reservation.getDate(), reservation.getTrainClass(), reservation.getTrainName(), reservation.getDeparture());
        if (ttm == null) {
            return null;
        }
        String departure = ttm.getDeparture().format(dtf);

        ttm = trainTimetableMasterDao.selectOne(reservation.getDate(), reservation.getTrainClass(), reservation.getTrainName(), reservation.getArrival());
        if (ttm == null) {
            return null;
        }
        String arrival = ttm.getArrival().format(dtf);

        ReservationResponse reservationResponse = new ReservationResponse();

        reservationResponse.setReservationId(reservation.getReservationId());
        reservationResponse.setDate(reservation.getDate());
        reservationResponse.setAmount(reservation.getAmount().intValue());
        reservationResponse.setAdult(reservation.getAdult());
        reservationResponse.setChild(reservation.getChild());
        reservationResponse.setDeparture(reservation.getDeparture());
        reservationResponse.setArrival(reservation.getArrival());
        reservationResponse.setTrainClass(reservation.getTrainClass());
        reservationResponse.setTrainName(reservation.getTrainName());
        reservationResponse.setDepartureTime(departure);
        reservationResponse.setArrivalTime(arrival);

        List<SeatReservations> seatReservations = seatReservationsDao.selectById(reservation.getReservationId());
        reservationResponse.setSeats(seatReservations);

        // 1つの予約内で車両番号は全席同じ;
        reservationResponse.setCarNumber(reservationResponse.getSeats().get(0).getCarNumber());

        if (reservationResponse.getSeats().get(0).getCarNumber() == 0) {
            reservationResponse.setSeatClass("non-reserved");
        } else {
            // 座席種別を取得;
            SeatMaster seat = seatMasterDao.selectOne6(
                    reservation.getTrainClass(),
                    reservationResponse.getCarNumber(),
                    reservationResponse.getSeats().get(0).getSeatColumn(),
                    reservationResponse.getSeats().get(0).getSeatRow()
            );

            if (seat == null) {
                return null;
            }

            reservationResponse.setSeatClass(seat.getSeatClass());
        }

        for (int i = 0; i < reservationResponse.getSeats().size(); i++) {
            SeatReservations v = reservationResponse.getSeats().get(i);
            // omit;
            v.setReservationId(0L);
            v.setCarNumber(0);
            reservationResponse.getSeats().set(i, v);
        }
        return reservationResponse;
    }


    /*
        個別予約取得
        POST /user/reservations/{item_id}
    */
    @Override
    public ReservationResponse userReservationResponseHandler(Long itemId) {
        Users user = getUser();

        Reservations reservation = reservationsDao.selectByReservationIdUserId(itemId, user.getId());
        if (reservation == null) {
            throw new IsuconException("Reservation not found", HttpStatus.NOT_FOUND);
        }

        ReservationResponse reservationResponse = makeReservationResponse(reservation);

        if (reservationResponse == null) {
            throw new IsuconException("makeReservationResponse()", HttpStatus.BAD_REQUEST);
        }

        return reservationResponse;
    }

    /*
        予約取消
        POST /user/reservations/{item_id}/cancel
    */
    @Override
    @Transactional
    public MessageResponse userReservationCancelHandler(Long itemId) {
        Users user = getUser();

        Reservations reservation = reservationsDao.selectByReservationIdUserId(itemId, user.getId());
        if (reservation == null) {
            throw new IsuconException("reservations naiyo", HttpStatus.BAD_REQUEST);
        }
        log.info("CANCEL {} {} {}", reservation, itemId, user.getId());

        switch (reservation.getStatus()) {
            case "rejected":
                throw new IsuconException("何らかの理由により予約はRejected状態です", HttpStatus.INTERNAL_SERVER_ERROR);

            case "done":
                // 支払いをキャンセルする
                String payment_api = System.getenv("PAYMENT_API");
                if (!StringUtils.hasLength(payment_api)) {
                    payment_api = "http://payment:5000";
                }

                ResponseEntity<CancelPaymentInformationResponse> resp = null;
                try {
                    String url = payment_api + "/payment/" + reservation.getPaymentId();
                    resp = restTemplate.exchange(url, HttpMethod.DELETE, null, CancelPaymentInformationResponse.class);

                } catch (RestClientException e) {
                    throw new IsuconException("HTTPリクエストの作成に失敗しました", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                // リクエスト失敗
                if (!Objects.equals(resp.getStatusCode(), HttpStatus.OK)) {
                    throw new IsuconException("決済のキャンセルに失敗しました", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                // リクエスト取り出し
                CancelPaymentInformationResponse output = resp.getBody();
                if (output == null) {
                    throw new IsuconException("レスポンスの読み込みに失敗しました", HttpStatus.INTERNAL_SERVER_ERROR);
                }
                log.info("{}", output);

                break;
            default:
                // pass(requesting状態のものはpayment_id無いので叩かない);
        }

        int ret = reservationsDao.delete(itemId, user.getId());
        if (ret <= 0) {
            throw new IsuconException("error", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ret = seatReservationsDao.delete(itemId);
        if (ret <= 0) {
            throw new IsuconException("seat naiyo", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new MessageResponse("cancell complete");
    }
}