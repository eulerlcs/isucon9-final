package logic

import (
	"encoding/json"
	"fmt"
	"github.com/jmoiron/sqlx"
	"net/http"
	"strconv"
	"time"
	"zsj-isucon-09-final/dbCache"
	"zsj-isucon-09-final/dbCacheSiJian"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

func TrainSearchHandler(w http.ResponseWriter, r *http.Request) {
	/*
		列車検索
			GET /train/search?use_at=<ISO8601形式の時刻> & from=東京 & to=大阪

		return
			料金
			空席情報
			発駅と着駅の到着時刻

	*/
	var dbx = utils.Dbx

	var stationMasterDao dbCache.StationMasterDao

	jst := time.FixedZone("JST", 9*60*60)
	date, err := time.Parse(time.RFC3339, r.URL.Query().Get("use_at"))
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}
	date = date.In(jst)

	if !checkAvailableDate(date) {
		errorResponse(w, http.StatusNotFound, "予約可能期間外です")
		return
	}

	trainClass := r.URL.Query().Get("train_class")
	fromName := r.URL.Query().Get("from")
	toName := r.URL.Query().Get("to")

	adult, _ := strconv.Atoi(r.URL.Query().Get("adult"))
	child, _ := strconv.Atoi(r.URL.Query().Get("child"))

	var fromStation, toStation *domain.Station
	fromStation, _ = stationMasterDao.SelectByName(fromName)
	toStation, _ = stationMasterDao.SelectByName(toName)

	isNobori := false
	if fromStation.Distance > toStation.Distance {
		isNobori = true
	}

	//query := "SELECT * FROM station_master ORDER BY distance"
	//if isNobori {
	//	// 上りだったら駅リストを逆にする
	//	query += " DESC"
	//}

	usableTrainClassList := GetUsableTrainClassList(*fromStation, *toStation)

	var inQuery string
	var inArgs []interface{}

	if trainClass == "" {
		query := "SELECT * FROM train_master WHERE date=? AND train_class IN (?) AND is_nobori=? order by departure_at"
		inQuery, inArgs, err = sqlx.In(query, date.Format("2006/01/02"), usableTrainClassList, isNobori)
	} else {
		query := "SELECT * FROM train_master WHERE date=? AND train_class IN (?) AND is_nobori=? AND train_class=? order by departure_at"
		inQuery, inArgs, err = sqlx.In(query, date.Format("2006/01/02"), usableTrainClassList, isNobori, trainClass)
	}
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}

	var trainList []domain.Train
	err = dbx.Select(&trainList, inQuery, inArgs...)
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}

	var stations []domain.Station
	if isNobori {
		// 上りだったら駅リストを逆にする
		stations, _ = stationMasterDao.SelectAllByIDDesc()
	} else {
		stations, _ = stationMasterDao.SelectAllByIDAsc()
	}

	fmt.Println("From", fromStation)
	fmt.Println("To", toStation)

	var trainSearchResponseList []domain.TrainSearchResponse

	for _, train := range trainList {
		isSeekedToFirstStation := false
		isContainsOriginStation := false
		isContainsDestStation := false
		i := 0

		_, err := stationMasterDao.SelectByName(train.StartStation)
		if err == nil {
			isSeekedToFirstStation = true
		}

		fromSt, err := stationMasterDao.SelectByID(fromStation.ID)
		if err == nil {
			isContainsOriginStation = true
		}

		toSt, err := stationMasterDao.SelectByID(toStation.ID)
		if isContainsOriginStation && err == nil {
			// 発駅と着駅を経路中に持つ編成の場合
			isContainsDestStation = true
		}

		if !isNobori && fromSt.Distance <= toSt.Distance {
			break
		}
		if isNobori && fromSt.Distance <= toSt.Distance {
			break
		}

		//if station.ID == toStation.ID {
		//	if isContainsOriginStation {
		//		// 発駅と着駅を経路中に持つ編成の場合
		//		isContainsDestStation = true
		//		break
		//	} else {
		//		// 出発駅より先に終点が見つかったとき
		//		fmt.Println("なんかおかしい")
		//		break
		//	}
		//}

		for _, station := range stations {

			if !isSeekedToFirstStation {
				// 駅リストを列車の発駅まで読み飛ばして頭出しをする
				// 列車の発駅以前は止まらないので無視して良い
				if station.Name == train.StartStation {
					isSeekedToFirstStation = true
				} else {
					continue
				}
			}

			if station.ID == fromStation.ID {
				// 発駅を経路中に持つ編成の場合フラグを立てる
				isContainsOriginStation = true
			}
			if station.ID == toStation.ID {
				if isContainsOriginStation {
					// 発駅と着駅を経路中に持つ編成の場合
					isContainsDestStation = true
					break
				} else {
					// 出発駅より先に終点が見つかったとき
					fmt.Println("なんかおかしい")
					break
				}
			}
			if station.Name == train.LastStation {
				// 駅が見つからないまま当該編成の終点に着いてしまったとき
				break
			}
			i++
		}

		if isContainsOriginStation && isContainsDestStation {
			// 列車情報

			// 所要時間
			var departure, arrival string

			err = dbx.Get(&departure, "SELECT departure FROM train_timetable_master WHERE date=? AND train_class=? AND train_name=? AND station=?", date.Format("2006/01/02"), train.TrainClass, train.TrainName, fromStation.Name)
			if err != nil {
				errorResponse(w, http.StatusInternalServerError, err.Error())
				return
			}

			departureDate, err := time.Parse("2006/01/02 15:04:05 -07:00 MST", fmt.Sprintf("%s %s +09:00 JST", date.Format("2006/01/02"), departure))
			if err != nil {
				errorResponse(w, http.StatusInternalServerError, err.Error())
				return
			}

			if !date.Before(departureDate) {
				// 乗りたい時刻より出発時刻が前なので除外
				continue
			}

			err = dbx.Get(&arrival, "SELECT arrival FROM train_timetable_master WHERE date=? AND train_class=? AND train_name=? AND station=?", date.Format("2006/01/02"), train.TrainClass, train.TrainName, toStation.Name)
			if err != nil {
				errorResponse(w, http.StatusInternalServerError, err.Error())
				return
			}

			premium_avail_seats, err := dbCacheSiJian.GetAvailableSeats(train, *fromStation, *toStation, "premium", false)
			if err != nil {
				errorResponse(w, http.StatusBadRequest, err.Error())
				return
			}
			premium_smoke_avail_seats, err := dbCacheSiJian.GetAvailableSeats(train, *fromStation, *toStation, "premium", true)
			if err != nil {
				errorResponse(w, http.StatusBadRequest, err.Error())
				return
			}

			reserved_avail_seats, err := dbCacheSiJian.GetAvailableSeats(train, *fromStation, *toStation, "reserved", false)
			if err != nil {
				errorResponse(w, http.StatusBadRequest, err.Error())
				return
			}
			reserved_smoke_avail_seats, err := dbCacheSiJian.GetAvailableSeats(train, *fromStation, *toStation, "reserved", true)
			if err != nil {
				errorResponse(w, http.StatusBadRequest, err.Error())
				return
			}

			premium_avail := "○"
			if len(premium_avail_seats) == 0 {
				premium_avail = "×"
			} else if len(premium_avail_seats) < 10 {
				premium_avail = "△"
			}

			premium_smoke_avail := "○"
			if len(premium_smoke_avail_seats) == 0 {
				premium_smoke_avail = "×"
			} else if len(premium_smoke_avail_seats) < 10 {
				premium_smoke_avail = "△"
			}

			reserved_avail := "○"
			if len(reserved_avail_seats) == 0 {
				reserved_avail = "×"
			} else if len(reserved_avail_seats) < 10 {
				reserved_avail = "△"
			}

			reserved_smoke_avail := "○"
			if len(reserved_smoke_avail_seats) == 0 {
				reserved_smoke_avail = "×"
			} else if len(reserved_smoke_avail_seats) < 10 {
				reserved_smoke_avail = "△"
			}

			// 空席情報
			seatAvailability := map[string]string{
				"premium":        premium_avail,
				"premium_smoke":  premium_smoke_avail,
				"reserved":       reserved_avail,
				"reserved_smoke": reserved_smoke_avail,
				"non_reserved":   "○",
			}

			// 料金計算
			premiumFare, err := fareCalc(date, fromStation.ID, toStation.ID, train.TrainClass, "premium")
			if err != nil {
				errorResponse(w, http.StatusBadRequest, err.Error())
				return
			}
			premiumFare = premiumFare*adult + premiumFare/2*child

			reservedFare, err := fareCalc(date, fromStation.ID, toStation.ID, train.TrainClass, "reserved")
			if err != nil {
				errorResponse(w, http.StatusBadRequest, err.Error())
				return
			}
			reservedFare = reservedFare*adult + reservedFare/2*child

			nonReservedFare, err := fareCalc(date, fromStation.ID, toStation.ID, train.TrainClass, "non-reserved")
			if err != nil {
				errorResponse(w, http.StatusBadRequest, err.Error())
				return
			}
			nonReservedFare = nonReservedFare*adult + nonReservedFare/2*child

			fareInformation := map[string]int{
				"premium":        premiumFare,
				"premium_smoke":  premiumFare,
				"reserved":       reservedFare,
				"reserved_smoke": reservedFare,
				"non_reserved":   nonReservedFare,
			}

			trainSearchResponseList = append(trainSearchResponseList, domain.TrainSearchResponse{
				train.TrainClass, train.TrainName, train.StartStation, train.LastStation,
				fromStation.Name, toStation.Name, departure, arrival, seatAvailability, fareInformation,
			})

			if len(trainSearchResponseList) >= 10 {
				break
			}
		}
	}
	resp, err := json.Marshal(trainSearchResponseList)
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}
	w.Write(resp)

}
