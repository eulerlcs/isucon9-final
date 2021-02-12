package logic

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"
	"time"
	"zsj-isucon-09-final/dbCache"
	"zsj-isucon-09-final/dbCacheSiJian"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

func TrainReservationHandler(w http.ResponseWriter, r *http.Request) {
	/*
		列車の席予約API　支払いはまだ
		POST /api/train/reserve
			{
				"date": "2020-12-31T07:57:00+09:00",
				"train_name": "183",
				"train_class": "中間",
				"car_number": 7,
				"is_smoking_seat": false,
				"seat_class": "reserved",
				"departure": "東京",
				"arrival": "名古屋",
				"child": 2,
				"adult": 1,
				"column": "A",
				"seats": [
					{
					"row": 3,
					"column": "B"
					},
						{
					"row": 4,
					"column": "C"
					}
				]
		}
		レスポンスで予約IDを返す
		reservationResponse(w http.ResponseWriter, errCode int, id int, ok bool, message string)
	*/
	var dbx = utils.Dbx

	var stationMasterDao dbCache.StationMasterDao

	// json parse
	req := new(domain.TrainReservationRequest)
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		errorResponse(w, http.StatusInternalServerError, "JSON parseに失敗しました")
		log.Println(err.Error())
		return
	}

	// 乗車日の日付表記統一
	jst := time.FixedZone("Asia/Tokyo", 9*60*60)
	date, err := time.Parse(time.RFC3339, req.Date)
	if err != nil {
		errorResponse(w, http.StatusInternalServerError, "時刻のparseに失敗しました")
		log.Println(err.Error())
	}
	date = date.In(jst)

	if !checkAvailableDate(date) {
		errorResponse(w, http.StatusNotFound, "予約可能期間外です")
		return
	}

	tx := dbx.MustBegin()
	// 止まらない駅の予約を取ろうとしていないかチェックする
	// 列車データを取得
	tmas := domain.Train{}
	//query := "SELECT * FROM train_master WHERE date=? AND train_class=? AND train_name=?"
	//err = tx.SelectByID(
	//	&tmas, query,
	//	date.Format("2006/01/02"),
	//	req.TrainClass,
	//	req.TrainName,
	//)
	//if err == sql.ErrNoRows {
	//	tx.Rollback()
	//	errorResponse(w, http.StatusNotFound, "列車データがみつかりません")
	//	log.Println(err.Error())
	//	return
	//}
	//if err != nil {
	//	tx.Rollback()
	//	errorResponse(w, http.StatusInternalServerError, "列車データの取得に失敗しました")
	//	log.Println(err.Error())
	//	return
	//}

	s := []string{date.Format("2006/01/02"), req.TrainClass, req.TrainName}
	trainList, ok := dbCacheSiJian.CacheTrainMapByDateClassName[strings.Join(s, ",")]
	if !ok {
		tx.Rollback()
		errorResponse(w, http.StatusNotFound, "列車データがみつかりません")
		log.Println(err.Error())
		return
	}
	tmas = trainList[0]

	// 列車自体の駅IDを求める
	var departureStation, arrivalStation *domain.Station
	departureStation, _ = stationMasterDao.SelectByName(tmas.StartStation)
	arrivalStation, _ = stationMasterDao.SelectByName(tmas.LastStation)

	// リクエストされた乗車区間の駅IDを求める
	var fromStation, toStation *domain.Station
	fromStation, _ = stationMasterDao.SelectByName(req.Departure)
	toStation, _ = stationMasterDao.SelectByName(req.Arrival)

	switch req.TrainClass {
	case "最速":
		if !fromStation.IsStopExpress || !toStation.IsStopExpress {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, "最速の止まらない駅です")
			return
		}
	case "中間":
		if !fromStation.IsStopSemiExpress || !toStation.IsStopSemiExpress {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, "中間の止まらない駅です")
			return
		}
	case "遅いやつ":
		if !fromStation.IsStopLocal || !toStation.IsStopLocal {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, "遅いやつの止まらない駅です")
			return
		}
	default:
		tx.Rollback()
		errorResponse(w, http.StatusBadRequest, "リクエストされた列車クラスが不明です")
		log.Println(err.Error())
		return
	}

	// 運行していない区間を予約していないかチェックする
	if tmas.IsNobori {
		if fromStation.ID > departureStation.ID || toStation.ID > departureStation.ID {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, "リクエストされた区間に列車が運行していない区間が含まれています")
			return
		}
		if arrivalStation.ID >= fromStation.ID || arrivalStation.ID > toStation.ID {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, "リクエストされた区間に列車が運行していない区間が含まれています")
			return
		}
	} else {
		if fromStation.ID < departureStation.ID || toStation.ID < departureStation.ID {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, "リクエストされた区間に列車が運行していない区間が含まれています")
			return
		}
		if arrivalStation.ID <= fromStation.ID || arrivalStation.ID < toStation.ID {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, "リクエストされた区間に列車が運行していない区間が含まれています")
			return
		}
	}

	/*
		あいまい座席検索
		seatsが空白の時に発動する
	*/
	switch len(req.Seats) {
	case 0:
		if req.SeatClass == "non-reserved" {
			break // non-reservedはそもそもあいまい検索もせずダミーのRow/Columnで予約を確定させる。
		}
		//当該列車・号車中の空き座席検索
		var train domain.Train
		//query := "SELECT * FROM train_master WHERE date=? AND train_class=? AND train_name=?"
		//err = dbx.SelectByID(&train, query, date.Format("2006/01/02"), req.TrainClass, req.TrainName)
		//if err == sql.ErrNoRows {
		//	panic(err)
		//}
		//if err != nil {
		//	tx.Rollback()
		//	errorResponse(w, http.StatusBadRequest, err.Error())
		//	return
		//}
		s := []string{date.Format("2006/01/02"), req.TrainClass, req.TrainName}
		trainList, ok := dbCacheSiJian.CacheTrainMapByDateClassName[strings.Join(s, ",")]
		if !ok {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, err.Error())
			return
		}
		train = trainList[0]

		usableTrainClassList := GetUsableTrainClassList(*fromStation, *toStation)
		usable := false
		for _, v := range usableTrainClassList {
			if v == train.TrainClass {
				usable = true
			}
		}
		if !usable {
			err = fmt.Errorf("invalid train_class")
			log.Print(err)
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, err.Error())
			return
		}

		req.Seats = []domain.RequestSeat{} // 座席リクエスト情報は空に
		for carnum := 1; carnum <= 16; carnum++ {
			//seatList := []Seat{}
			//query = "SELECT * FROM seat_master WHERE train_class=? AND car_number=? AND seat_class=? AND is_smoking_seat=? ORDER BY seat_row, seat_column"
			//log.Println("call selectSeatBy")
			//log.Println("parameter : " ,req.TrainClass, carnum,req.SeatClass, strconv.FormatBool(req.IsSmokingSeat))
			seatList := dbCacheSiJian.SelectSeatBy2(req.TrainClass, carnum, req.SeatClass, req.IsSmokingSeat)
			//if len(seatList) == 0 {
			//	tx.Rollback()
			//	errorResponse(w, http.StatusBadRequest, "not found seat.")
			//	return
			//}
			//err = dbx.Select(&seatList, query, req.TrainClass, carnum, req.SeatClass, req.IsSmokingSeat)
			//if err != nil {
			//	tx.Rollback()
			//	errorResponse(w, http.StatusBadRequest, err.Error())
			//	return
			//}
			var seatInformationList []domain.SeatInformation
			for _, seat := range seatList {
				s := domain.SeatInformation{seat.SeatRow, seat.SeatColumn, seat.SeatClass, seat.IsSmokingSeat, false}
				seatReservationList := []domain.SeatReservation{}
				query := "SELECT s.* FROM seat_reservations s, reservations r WHERE r.date=? AND r.train_class=? AND r.train_name=? AND car_number=? AND seat_row=? AND seat_column=? FOR UPDATE"
				err = dbx.Select(
					&seatReservationList, query,
					date.Format("2006/01/02"),
					seat.TrainClass,
					req.TrainName,
					seat.CarNumber,
					seat.SeatRow,
					seat.SeatColumn,
				)
				if err != nil {
					tx.Rollback()
					errorResponse(w, http.StatusBadRequest, err.Error())
					return
				}

				for _, seatReservation := range seatReservationList {
					reservation := domain.Reservation{}
					query := "SELECT * FROM reservations WHERE reservation_id=? FOR UPDATE"
					err = dbx.Get(&reservation, query, seatReservation.ReservationId)
					if err != nil {
						panic(err)
					}

					var departureStation, arrivalStation *domain.Station
					departureStation, _ = stationMasterDao.SelectByName(reservation.Departure)
					arrivalStation, _ = stationMasterDao.SelectByName(reservation.Arrival)

					if train.IsNobori {
						// 上り
						if toStation.ID < arrivalStation.ID && fromStation.ID <= arrivalStation.ID {
							// pass
						} else if toStation.ID >= departureStation.ID && fromStation.ID > departureStation.ID {
							// pass
						} else {
							s.IsOccupied = true
						}
					} else {
						// 下り
						if fromStation.ID < departureStation.ID && toStation.ID <= departureStation.ID {
							// pass
						} else if fromStation.ID >= arrivalStation.ID && toStation.ID > arrivalStation.ID {
							// pass
						} else {
							s.IsOccupied = true
						}
					}
				}

				seatInformationList = append(seatInformationList, s)
			}

			// 曖昧予約席とその他の候補席を選出
			var seatnum int                  // 予約する座席の合計数
			var reserved bool                // あいまい指定席確保済フラグ
			var vargue bool                  // あいまい検索フラグ
			var VagueSeat domain.RequestSeat // あいまい指定席保存用
			reserved = false
			vargue = true
			seatnum = (req.Adult + req.Child - 1) // 全体の人数からあいまい指定席分を引いておく
			if req.Column == "" {                 // A/B/C/D/Eを指定しなければ、空いている適当な指定席を取るあいまいモード
				seatnum = (req.Adult + req.Child) // あいまい指定せず大人＋小人分の座席を取る
				reserved = true                   // dummy
				vargue = false                    // dummy
			}
			var CandidateSeat domain.RequestSeat
			CandidateSeats := []domain.RequestSeat{}

			// シート分だけ回して予約できる席を検索
			var i int
			for _, seat := range seatInformationList {
				if seat.Column == req.Column && !seat.IsOccupied && !reserved && vargue { // あいまい席があいてる
					VagueSeat.Row = seat.Row
					VagueSeat.Column = seat.Column
					reserved = true
				} else if !seat.IsOccupied && i < seatnum { // 単に席があいてる
					CandidateSeat.Row = seat.Row
					CandidateSeat.Column = seat.Column
					CandidateSeats = append(CandidateSeats, CandidateSeat)
					i++
				}
			}

			if vargue && reserved { // あいまい席が見つかり、予約できそうだった
				req.Seats = append(req.Seats, VagueSeat) // あいまい予約席を追加
			}
			if i > 0 { // 候補席があった
				req.Seats = append(req.Seats, CandidateSeats...) // 予約候補席追加
			}

			if len(req.Seats) < req.Adult+req.Child {
				// リクエストに対して席数が足りてない
				// 次の号車にうつしたい
				fmt.Println("-----------------")
				fmt.Printf("現在検索中の車両: %d号車, リクエスト座席数: %d, 予約できそうな座席数: %d, 不足数: %d\n", carnum, req.Adult+req.Child, len(req.Seats), req.Adult+req.Child-len(req.Seats))
				fmt.Println("リクエストに対して座席数が不足しているため、次の車両を検索します。")
				req.Seats = []domain.RequestSeat{}
				if carnum == 16 {
					fmt.Println("この新幹線にまとめて予約できる席数がなかったから検索をやめるよ")
					req.Seats = []domain.RequestSeat{}
					break
				}
			}
			fmt.Printf("空き実績: %d号車 シート:%v 席数:%d\n", carnum, req.Seats, len(req.Seats))
			if len(req.Seats) >= req.Adult+req.Child {
				fmt.Println("予約情報に追加したよ")
				req.Seats = req.Seats[:req.Adult+req.Child]
				req.CarNumber = carnum
				break
			}
		}
		if len(req.Seats) == 0 {
			errorResponse(w, http.StatusNotFound, "あいまい座席予約ができませんでした。指定した席、もしくは1車両内に希望の席数をご用意できませんでした。")
			tx.Rollback()
			return
		}
	default:
		// 座席情報のValidate
		seatList := []domain.Seat{}
		for _, z := range req.Seats {
			fmt.Println("XXXX", z)
			//query = "SELECT * FROM seat_master WHERE train_class=? AND car_number=? AND seat_column=? AND seat_row=? AND seat_class=?"
			//err = dbx.SelectByID(
			//	&seatList, query,
			//	req.TrainClass,
			//	req.CarNumber,
			//	z.Column,
			//	z.Row,
			//	req.SeatClass,
			//)
			seatList = dbCacheSiJian.SelectSeatBy(req.TrainClass, req.CarNumber, z.Column, z.Row, req.SeatClass, "")
			if len(seatList) == 0 {
				tx.Rollback()
				errorResponse(w, http.StatusNotFound, "リクエストされた座席情報は存在しません。号車・喫煙席・座席クラスなど組み合わせを見直してください")
				//log.Println(err.Error())
				return
			}
			//if err != nil {
			//	tx.Rollback()
			//	errorResponse(w, http.StatusNotFound, "リクエストされた座席情報は存在しません。号車・喫煙席・座席クラスなど組み合わせを見直してください")
			//	log.Println(err.Error())
			//	return
			//}
		}
		break
	}

	// 当該列車・列車名の予約一覧取得
	reservations := []domain.Reservation{}
	query := "SELECT * FROM reservations WHERE date=? AND train_class=? AND train_name=? FOR UPDATE"
	err = tx.Select(
		&reservations, query,
		date.Format("2006/01/02"),
		req.TrainClass,
		req.TrainName,
	)
	if err != nil {
		tx.Rollback()
		errorResponse(w, http.StatusInternalServerError, "列車予約情報の取得に失敗しました")
		log.Println(err.Error())
		return
	}

	for _, reservation := range reservations {
		if req.SeatClass == "non-reserved" {
			break
		}
		// train_masterから列車情報を取得(上り・下りが分かる)
		tmas = domain.Train{}
		//query = "SELECT * FROM train_master WHERE date=? AND train_class=? AND train_name=?"
		//err = tx.SelectByID(
		//	&tmas, query,
		//	date.Format("2006/01/02"),
		//	req.TrainClass,
		//	req.TrainName,
		//)
		//if err == sql.ErrNoRows {
		//	tx.Rollback()
		//	errorResponse(w, http.StatusNotFound, "列車データがみつかりません")
		//	log.Println(err.Error())
		//	return
		//}
		//if err != nil {
		//	tx.Rollback()
		//	errorResponse(w, http.StatusInternalServerError, "列車データの取得に失敗しました")
		//	log.Println(err.Error())
		//	return
		//}

		s := []string{date.Format("2006/01/02"), req.TrainClass, req.TrainName}
		trainList, ok := dbCacheSiJian.CacheTrainMapByDateClassName[strings.Join(s, ",")]
		if !ok {
			tx.Rollback()
			errorResponse(w, http.StatusNotFound, "列車データがみつかりません")
			log.Println(err.Error())
			return
		}
		tmas = trainList[0]

		// 予約情報の乗車区間の駅IDを求める
		var reservedfromStation, reservedtoStation *domain.Station
		reservedfromStation, _ = stationMasterDao.SelectByName(reservation.Departure)
		reservedtoStation, _ = stationMasterDao.SelectByName(reservation.Arrival)

		// 予約の区間重複判定
		secdup := false
		if tmas.IsNobori {
			// 上り
			if toStation.ID < reservedtoStation.ID && fromStation.ID <= reservedtoStation.ID {
				// pass
			} else if toStation.ID >= reservedfromStation.ID && fromStation.ID > reservedfromStation.ID {
				// pass
			} else {
				secdup = true
			}
		} else {
			// 下り
			if fromStation.ID < reservedfromStation.ID && toStation.ID <= reservedfromStation.ID {
				// pass
			} else if fromStation.ID >= reservedtoStation.ID && toStation.ID > reservedtoStation.ID {
				// pass
			} else {
				secdup = true
			}
		}

		if secdup {

			// 区間重複の場合は更に座席の重複をチェックする
			SeatReservations := []domain.SeatReservation{}
			query := "SELECT * FROM seat_reservations WHERE reservation_id=? FOR UPDATE"
			err = tx.Select(
				&SeatReservations, query,
				reservation.ReservationId,
			)
			if err != nil {
				tx.Rollback()
				errorResponse(w, http.StatusInternalServerError, "座席予約情報の取得に失敗しました")
				log.Println(err.Error())
				return
			}

			for _, v := range SeatReservations {
				for _, seat := range req.Seats {
					if v.CarNumber == req.CarNumber && v.SeatRow == seat.Row && v.SeatColumn == seat.Column {
						tx.Rollback()
						fmt.Println("Duplicated ", reservation)
						errorResponse(w, http.StatusBadRequest, "リクエストに既に予約された席が含まれています")
						return
					}
				}
			}
		}
	}
	// 3段階の予約前チェック終わり

	// 自由席は強制的にSeats情報をダミーにする（自由席なのに席指定予約は不可）
	if req.SeatClass == "non-reserved" {
		req.Seats = []domain.RequestSeat{}
		dummySeat := domain.RequestSeat{}
		req.CarNumber = 0
		for num := 0; num < req.Adult+req.Child; num++ {
			dummySeat.Row = 0
			dummySeat.Column = ""
			req.Seats = append(req.Seats, dummySeat)
		}
	}

	// 運賃計算
	var fare int
	switch req.SeatClass {
	case "premium":
		fare, err = fareCalc(date, fromStation.ID, toStation.ID, req.TrainClass, "premium")
		if err != nil {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, err.Error())
			log.Println("fareCalc " + err.Error())
			return
		}
	case "reserved":
		fare, err = fareCalc(date, fromStation.ID, toStation.ID, req.TrainClass, "reserved")
		if err != nil {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, err.Error())
			log.Println("fareCalc " + err.Error())
			return
		}
	case "non-reserved":
		fare, err = fareCalc(date, fromStation.ID, toStation.ID, req.TrainClass, "non-reserved")
		if err != nil {
			tx.Rollback()
			errorResponse(w, http.StatusBadRequest, err.Error())
			log.Println("fareCalc " + err.Error())
			return
		}
	default:
		tx.Rollback()
		errorResponse(w, http.StatusBadRequest, "リクエストされた座席クラスが不明です")
		return
	}
	sumFare := (req.Adult * fare) + (req.Child*fare)/2
	fmt.Println("SUMFARE")

	// userID取得。ログインしてないと怒られる。
	user, errCode, errMsg := getUser(w, r)
	if errCode != http.StatusOK {
		tx.Rollback()
		errorResponse(w, errCode, errMsg)
		log.Printf("%s", errMsg)
		return
	}

	//予約ID発行と予約情報登録
	query = "INSERT INTO `reservations` (`user_id`, `date`, `train_class`, `train_name`, `departure`, `arrival`, `status`, `payment_id`, `adult`, `child`, `amount`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
	result, err := tx.Exec(
		query,
		user.ID,
		date.Format("2006/01/02"),
		req.TrainClass,
		req.TrainName,
		req.Departure,
		req.Arrival,
		"requesting",
		"a",
		req.Adult,
		req.Child,
		sumFare,
	)
	if err != nil {
		tx.Rollback()
		errorResponse(w, http.StatusBadRequest, "予約の保存に失敗しました。"+err.Error())
		log.Println(err.Error())
		return
	}

	id, err := result.LastInsertId() //予約ID
	if err != nil {
		tx.Rollback()
		errorResponse(w, http.StatusInternalServerError, "予約IDの取得に失敗しました")
		log.Println(err.Error())
		return
	}

	//席の予約情報登録
	//reservationsレコード1に対してseat_reservationstが1以上登録される
	query = "INSERT INTO `seat_reservations` (`reservation_id`, `car_number`, `seat_row`, `seat_column`) VALUES (?, ?, ?, ?)"
	for _, v := range req.Seats {
		_, err = tx.Exec(
			query,
			id,
			req.CarNumber,
			v.Row,
			v.Column,
		)
		if err != nil {
			tx.Rollback()
			errorResponse(w, http.StatusInternalServerError, "座席予約の登録に失敗しました")
			log.Println(err.Error())
			return
		}
	}

	rr := domain.TrainReservationResponse{
		ReservationId: id,
		Amount:        sumFare,
		IsOk:          true,
	}
	response, err := json.Marshal(rr)
	if err != nil {
		tx.Rollback()
		errorResponse(w, http.StatusInternalServerError, "レスポンスの生成に失敗しました")
		log.Println(err.Error())
		return
	}
	tx.Commit()
	w.Write(response)
}
