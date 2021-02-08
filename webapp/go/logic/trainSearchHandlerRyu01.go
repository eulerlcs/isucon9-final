package logic

import (
	"encoding/json"
	"github.com/jmoiron/sqlx"
	"log"
	"net/http"
	"strconv"
	"time"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

func trainSearchHandlerRyu01SelectTrainList(date time.Time, trainClass string, manStartStation string, manArrivalStation string) ([]domain.TrainSearchResponseWork, error) {
	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	sql := `
SELECT
	tm.train_class ,
	tm.train_name ,
	tm.is_nobori ,
	train_start_station.name train_start_station ,
	train_start_station.id train_start_station_id,
	train_arrival_station.name train_arrival_station ,
	train_arrival_station.id train_arrival_station_id,
	man_start.station man_start_station ,
	man_start_station.id man_start_station_id,
	man_start.departure man_start_departure,
	man_arrival.station man_arrival_station,
	man_arrival_station.id man_arrival_station_id,
	man_arrival.arrival man_arrival_arrival
FROM
	train_master tm
	-- 列車の始点駅
inner join station_master train_start_station on
	tm.start_station = train_start_station.name
	-- 列車の終点駅
inner join station_master train_arrival_station on
	tm.last_station = train_arrival_station.name
	-- 乗客の乗車駅
inner join train_timetable_master man_start on
	tm.date = man_start.date
	and tm.train_class = man_start.train_class
	and tm.train_name = man_start.train_name
inner join station_master man_start_station on
	man_start.station = man_start_station.name
	-- 乗客の降車駅
inner join train_timetable_master man_arrival on
	tm.date = man_arrival.date
	and tm.train_class = man_arrival.train_class
	and tm.train_name = man_arrival.train_name
inner join station_master man_arrival_station on
	man_arrival.station = man_arrival_station.name
WHERE
	tm.date = ?
	and man_start.departure >= ?
	and tm.train_class in (?)
	and man_start.station = ?
	and man_arrival.station = ?
	and ((
		man_start_station.id < man_arrival_station.id /* のぼり */
		and man_start_station.id >= train_start_station.id
		and man_arrival_station.id <= train_arrival_station.id
	 ) or (
		man_start_station.id > man_arrival_station.id /* 下り */
		and man_start_station.id <= train_start_station.id
		and man_arrival_station.id >= train_arrival_station.id 
	))
ORDER BY
	man_start.departure
LIMIT 10
`
	var usableTrainClassList []string = make([]string, 0, len(TrainClassMap))
	if trainClass == "" {
		for _, value := range TrainClassMap {
			usableTrainClassList = append(usableTrainClassList, value)
		}
	} else {
		usableTrainClassList = append(usableTrainClassList, trainClass)
	}

	inQuery, inArgs, err := sqlx.In(sql, date.Format("2006/01/02"), date.Format("15:04:05"), usableTrainClassList, manStartStation, manArrivalStation)

	if err != nil {
		log.Println(err)
		return nil, err
	}

	trainList := make([]domain.TrainSearchResponseWork, 0, 10)
	err = dbx.Select(&trainList, inQuery, inArgs...)
	if err != nil {
		log.Println(err)
		return nil, err
	}

	return trainList, nil
}

func TrainSearchHandlerRyu01(w http.ResponseWriter, r *http.Request) {
	/*
		列車検索
			GET /train/search?use_at=<ISO8601形式の時刻> & from=東京 & to=大阪

		return
			料金
			空席情報
			発駅と着駅の到着時刻

	*/

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

	var trainList []domain.TrainSearchResponseWork

	trainList, err = trainSearchHandlerRyu01SelectTrainList(date, trainClass, fromName, toName)
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}

	trainSearchResponseList := []domain.TrainSearchResponse{}
	for _, train := range trainList {
		// 列車情報
		premium_avail_seats, err := GetAvailableSeatsRyu01(train, train.DepartureID, train.ArrivalID, "premium", false)
		if err != nil {
			errorResponse(w, http.StatusBadRequest, err.Error())
			return
		}
		premium_smoke_avail_seats, err := GetAvailableSeatsRyu01(train, train.DepartureID, train.ArrivalID, "premium", true)
		if err != nil {
			errorResponse(w, http.StatusBadRequest, err.Error())
			return
		}

		reserved_avail_seats, err := GetAvailableSeatsRyu01(train, train.DepartureID, train.ArrivalID, "reserved", false)
		if err != nil {
			errorResponse(w, http.StatusBadRequest, err.Error())
			return
		}
		reserved_smoke_avail_seats, err := GetAvailableSeatsRyu01(train, train.DepartureID, train.ArrivalID, "reserved", true)
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
		premiumFare, err := fareCalc(date, train.DepartureID, train.ArrivalID, train.Class, "premium")
		if err != nil {
			errorResponse(w, http.StatusBadRequest, err.Error())
			return
		}
		premiumFare = premiumFare*adult + premiumFare/2*child

		reservedFare, err := fareCalc(date, train.DepartureID, train.ArrivalID, train.Class, "reserved")
		if err != nil {
			errorResponse(w, http.StatusBadRequest, err.Error())
			return
		}
		reservedFare = reservedFare*adult + reservedFare/2*child

		nonReservedFare, err := fareCalc(date, train.DepartureID, train.ArrivalID, train.Class, "non-reserved")
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
			train.Class, train.Name, train.Start, train.Last,
			train.Departure, train.Arrival, train.DepartureTime, train.ArrivalTime, seatAvailability, fareInformation,
		})

		if len(trainSearchResponseList) >= 10 {
			break
		}

	}
	resp, err := json.Marshal(trainSearchResponseList)
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}
	w.Write(resp)
}
