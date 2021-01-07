package jp.zhimingsoft.www.isucon.controller;

import jp.zhimingsoft.www.isucon.domain.*;
import jp.zhimingsoft.www.isucon.service.MainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@Slf4j
public class mainController {

    @Autowired
    private MainService mainService;


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

    @PostMapping("/api/train/reserve")
    public String trainReservationHandler() {
        return "/api/train/reserve";
    }

    @PostMapping("/api/train/reservation/commit")
    public String reservationPaymentHandler() {
        return "/api/train/reservation/commit";
    }

    // 認証関連

    @GetMapping("/api/auth")
    public String getAuthHandler() {
        return "/api/auth";
    }

    @PostMapping("/api/auth/signup")
    public String signUpHandler() {
        return "/api/auth/signup";
    }

    @PostMapping("/api/auth/login")
    public String loginHandler() {
        return "/api/auth/login";
    }

    @PostMapping("/api/auth/logout")
    public String logoutHandler() {
        return "/api/auth/logout";
    }

    @GetMapping("/api/user/reservations")
    public String userReservationsHandler() {
        return "/api/user/reservations";
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
