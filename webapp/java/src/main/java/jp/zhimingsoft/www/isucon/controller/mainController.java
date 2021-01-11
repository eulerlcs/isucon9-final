package jp.zhimingsoft.www.isucon.controller;

import jp.zhimingsoft.www.isucon.domain.*;
import jp.zhimingsoft.www.isucon.service.MainService;
import jp.zhimingsoft.www.isucon.utils.MessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import javax.servlet.http.HttpServletRequest;
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
    public InitializeResponse initializeHandler(HttpServletRequest request) {
        // TODO  exception回避：java.lang.IllegalStateException:
        //  Cannot create a session after the response has been committed
        request.getSession();
        return mainService.initializeHandler();
    }

    // 予約関係

    @GetMapping("/api/settings")
    public Settings settingsHandler(HttpServletRequest request) {
        request.getSession();
        return mainService.settingsHandler();
    }

    @GetMapping("/api/stations")
    public List<StationMaster> getStationsHandler(HttpServletRequest request) {
        request.getSession();
        return mainService.getStationsHandler();
    }

    /*
       指定した列車の座席列挙;
       GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京;
    */
    @GetMapping("/api/train/search")
    public List<TrainSearchResponse> trainSearchHandler(
            HttpServletRequest request,
            @RequestParam("use_at") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime useAt,
            @RequestParam(name = "train_class", required = false) String trainClass,
            String from,
            String to,
            Integer adult,
            Integer child) {
        request.getSession();
        return mainService.trainSearchHandler(useAt, trainClass, from, to, adult, child);
    }

    /*
       指定した列車の座席列挙;
       GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京;
    */
    @GetMapping("/api/train/seats")
    public CarInformation trainSeatsHandler(
            HttpServletRequest request,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime date,
            @RequestParam(name = "train_class") String trainClass,
            @RequestParam(name = "train_name") String trainName,
            @RequestParam(name = "car_number") Integer carNumber,
            String from,
            String to
    ) {
        request.getSession();
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
    public TrainReservationResponse trainReservationHandler(
            HttpServletRequest request,
            @RequestBody TrainReservationRequest req) {
        request.getSession();
        return mainService.trainReservationHandler(req);
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
    @PostMapping("/api/train/reservation/commit")
    public ReservationPaymentResponse reservationPaymentHandler(
            HttpServletRequest request,
            @RequestBody ReservationPaymentRequest req) {
        request.getSession();
        return mainService.reservationPaymentHandler(req);
    }

    // 認証関連
    /*
       ユーザー登録
       POST /auth/signup
   */
    @PostMapping("/api/auth/signup")
    public MessageResponse signUpHandler(HttpServletRequest request, @RequestBody Users user) {
        request.getSession();
        return mainService.signUpHandler(user);
    }

    /*
        ログイン
        POST /auth/login
    */
    @PostMapping("/api/auth/login")
    public MessageResponse loginHandler(HttpServletRequest request, @RequestBody Users postUser) {
        request.getSession();
        return mainService.loginHandler(postUser);
    }

    /*
       認証情報取得
       GET /auth
    */
    @GetMapping("/api/auth")
    public AuthResponse getAuthHandler(HttpServletRequest request) {
        request.getSession();
        return mainService.getAuthHandler();
    }

    /*
        ログアウト
        POST /auth/logout
    */
    @PostMapping("/api/auth/logout")
    public MessageResponse logoutHandler(HttpServletRequest request, SessionStatus sessionStatus) {
        request.getSession();
        MessageResponse messageResponse = mainService.logoutHandler();
        sessionStatus.setComplete();

        return messageResponse;
    }

    /*
        予約取得
        GET /user/reservations
    */
    @GetMapping("/api/user/reservations")
    public List<ReservationResponse> userReservationsHandler(HttpServletRequest request) {
        request.getSession();
        return mainService.userReservationsHandler();
    }

    /*
       個別予約取得
       POST /user/reservations/{item_id}
   */
    @GetMapping("/api/user/reservations/{item_id}")
    public ReservationResponse userReservationResponseHandler(
            HttpServletRequest request,
            @PathVariable("item_id") Long itemId) {
        request.getSession();
        return mainService.userReservationResponseHandler(itemId);
    }

    /*
        予約取消
        POST /user/reservations/{item_id}/cancel
    */
    @PostMapping("/api/user/reservations/{item_id}/cancel")
    public MessageResponse userReservationCancelHandler(
            HttpServletRequest request,
            @PathVariable("item_id") Long itemId) {
        request.getSession();
        return mainService.userReservationCancelHandler(itemId);
    }
}
