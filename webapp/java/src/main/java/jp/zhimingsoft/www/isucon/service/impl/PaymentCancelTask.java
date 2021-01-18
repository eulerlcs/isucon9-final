package jp.zhimingsoft.www.isucon.service.impl;

import jp.zhimingsoft.www.isucon.dao.ReservationsDao;
import jp.zhimingsoft.www.isucon.dao.SeatReservationsDao;
import jp.zhimingsoft.www.isucon.domain.CancelPaymentInformationResponse;
import jp.zhimingsoft.www.isucon.domain.Reservations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
@Component
public class PaymentCancelTask {
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    private SeatReservationsDao seatReservationsDao;
    @Autowired
    private ReservationsDao reservationsDao;

    @Transactional
    public void cancel(Long reservationId, Long userId) {
        Reservations reservation = reservationsDao.selectByReservationIdUserId(reservationId, userId);
        if (reservation == null) {
            log.error("reservations naiyo");
        }
        log.info("CANCEL {} {} {}", reservation, reservationId, userId);

        switch (reservation.getStatus()) {
            case "rejected":
                log.error("何らかの理由により予約はRejected状態です");

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
                    log.error("HTTPリクエストの作成に失敗しました");
                }

                // リクエスト失敗
                if (!Objects.equals(resp.getStatusCode(), HttpStatus.OK)) {
                    log.error("決済のキャンセルに失敗しました");
                }

                // リクエスト取り出し
                CancelPaymentInformationResponse output = resp.getBody();
                if (output == null) {
                    log.error("レスポンスの読み込みに失敗しました");
                }
                log.info("{}", output);

                break;
            default:
                // pass(requesting状態のものはpayment_id無いので叩かない);
        }

        int ret = reservationsDao.delete(reservationId, userId);
        if (ret <= 0) {
            log.error("error", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ret = seatReservationsDao.delete(reservationId);
        if (ret <= 0) {
            log.error("seat naiyo", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}