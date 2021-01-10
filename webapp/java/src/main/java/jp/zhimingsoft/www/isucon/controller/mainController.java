package jp.zhimingsoft.www.isucon.controller;

import jp.zhimingsoft.www.isucon.domain.*;
import jp.zhimingsoft.www.isucon.service.MainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
@Slf4j
@SessionAttributes("user")
public class mainController {

    @Autowired
    HttpSession session;

    @Autowired
    private MainService mainService;

    @ModelAttribute("user")
    public Users getUser() {
        return new Users();
    }

    @PostMapping("/initialize")
    public InitializeResponse initializeHandler() {
        return mainService.initializeHandler();
    }

    // 予約関係

    @GetMapping("/api/settings")
    public Settings settingsHandler() {
        return mainService.settingsHandler();
    }

    @GetMapping("/api/stations")
    public List<StationMaster> getStationsHandler() {
        return mainService.getStationsHandler();
    }

    /*
       指定した列車の座席列挙;
       GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京;
    */
    @GetMapping("/api/train/search")
    public List<TrainSearchResponse> trainSearchHandler(
            @RequestParam("use_at") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime useAt,
            @RequestParam(name = "train_class", required = false) String trainClass,
            String from,
            String to,
            Integer adult,
            Integer child) {
        return mainService.trainSearchHandler(useAt, trainClass, from, to, adult, child);
    }

    /*
       指定した列車の座席列挙;
       GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京;
    */
    @GetMapping("/api/train/seats")
    public CarInformation trainSeatsHandler(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime date,
            @RequestParam(name = "train_class") String trainClass,
            @RequestParam(name = "train_name") String trainName,
            @RequestParam(name = "car_number") Integer carNumber,
            String from,
            String to
    ) {
        return mainService.trainSeatsHandler(date, trainClass, trainName, carNumber, from, to);
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
    */
    @PostMapping("/api/train/reserve")
    public TrainReservationResponse trainReservationHandler(@RequestBody TrainReservationRequest req) {
        return mainService.trainReservationHandler(req);
    }

    @PostMapping("/api/train/reservation/commit")
    public String reservationPaymentHandler() {
        return "/api/train/reservation/commit";
    }

    // 認証関連
    /*
       ユーザー登録
       POST /auth/signup
   */
    @PostMapping("/api/auth/signup")
    public String signUpHandler(@RequestBody Users user) {
        mainService.signUpHandler(user);
        return "/api/auth/signup";
    }

    /*
        ログイン
        POST /auth/login
    */
    @PostMapping("/api/auth/login")
    public void loginHandler(@RequestBody Users postUser) {
        mainService.loginHandler(postUser);
    }

    /*
       認証情報取得
       GET /auth/login
    */
    @GetMapping("/api/auth")
    public AuthResponse getAuthHandler() {
        return mainService.getAuthHandler();
    }

    /*
        ログアウト
        POST /auth/logout
    */
    @PostMapping("/api/auth/logout")
    public void logoutHandler() {
        mainService.logoutHandler();
    }

    /*
        予約取得
        GET /user/reservations
    */
    @GetMapping("/api/user/reservations")
    public List<ReservationResponse> userReservationsHandler() {
        return   mainService.userReservationsHandler();
    }

    @GetMapping("/api/user/reservations/:itemId")
    public String userReservationResponseHandler(String itemId) {
        return "/api/user/reservations";
    }

    @PostMapping("/api/user/reservations/:itemId/cancel")
    public String userReservationCancelHandler(String itemId) {
        return "/api/user/reservations";
    }
}
