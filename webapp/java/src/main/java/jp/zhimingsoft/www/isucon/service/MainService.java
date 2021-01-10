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

    /*
       指定した列車の座席列挙;
       GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京;
    */
    CarInformation trainSeatsHandler(
            ZonedDateTime date,
            String trainClass,
            String trainName,
            int carNumber,
            String from,
            String to);

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
    TrainReservationResponse trainReservationHandler(TrainReservationRequest req);

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
    public ReservationPaymentResponse reservationPaymentHandler(ReservationPaymentRequest req);

    /*
       ユーザー登録
       POST /auth/signup
   */
    void signUpHandler(Users user);

    /*
        ログイン
        POST /auth/login
    */
    void loginHandler(Users postUser);

    /*
       認証情報取得
       GET /auth/login
    */
    AuthResponse getAuthHandler();

    /*
        ログアウト
        POST /auth/logout
    */
    void logoutHandler();

    /*
        予約取得
        GET /user/reservations
    */
    List<ReservationResponse> userReservationsHandler();

    /*
		個別予約取得
		POST /user/reservations/{item_id}
	*/
    ReservationResponse userReservationResponseHandler(Long itemId);

    /*
        予約取消
        POST /user/reservations/{item_id}/cancel
    */
    public void userReservationCancelHandler(Long itemId);
}
