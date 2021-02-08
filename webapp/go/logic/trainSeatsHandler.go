package logic

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"
	"zsj-isucon-09-final/dbCache"
	"zsj-isucon-09-final/dbCacheSiJian"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

func TrainSeatsHandler(w http.ResponseWriter, r *http.Request) {
	/*
		指定した列車の座席列挙
		GET /train/seats?date=2020-03-01&train_class=のぞみ&train_name=96号&car_number=2&from=大阪&to=東京
	*/
	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	var stationMasterDao dbCache.StationMasterDao

	jst := time.FixedZone("Asia/Tokyo", 9*60*60)
	date, err := time.Parse(time.RFC3339, r.URL.Query().Get("date"))
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
	trainName := r.URL.Query().Get("train_name")
	carNumber, _ := strconv.Atoi(r.URL.Query().Get("car_number"))
	fromName := r.URL.Query().Get("from")
	toName := r.URL.Query().Get("to")

	// 対象列車の取得
	var train domain.Train
	//query := "SELECT * FROM train_master WHERE date=? AND train_class=? AND train_name=?"
	//err = dbx.SelectByID(&train, query, date.Format("2006/01/02"), trainClass, trainName)
	//if err == sql.ErrNoRows {
	//	errorResponse(w, http.StatusNotFound, "列車が存在しません")
	//}
	//if err != nil {
	//	errorResponse(w, http.StatusBadRequest, err.Error())
	//	return
	//}
	s := []string{date.Format("2006/01/02"), trainClass, trainName}
	trainList, ok := dbCacheSiJian.CacheTrainMapByDateClassName[strings.Join(s, ",")]
	if !ok {
		errorResponse(w, http.StatusNotFound, "列車が存在しません")
	}
	train = trainList[0]

	var fromStation, toStation domain.Station
	fromStation, _ = stationMasterDao.SelectByName(fromName)
	toStation, _ = stationMasterDao.SelectByName(toName)

	usableTrainClassList := GetUsableTrainClassList(fromStation, toStation)
	usable := false
	for _, v := range usableTrainClassList {
		if v == train.TrainClass {
			usable = true
		}
	}
	if !usable {
		err = fmt.Errorf("invalid train_class")
		log.Print(err)
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}

	seatList := []domain.Seat{}

	//query = "SELECT * FROM seat_master WHERE train_class=? AND car_number=? ORDER BY seat_row, seat_column"
	//err = dbx.Select(&seatList, query, trainClass, carNumber)
	//if err != nil {
	//	errorResponse(w, http.StatusBadRequest, err.Error())
	//	return
	//}
	seatList = dbCacheSiJian.SelectSeatBy(trainClass, carNumber, "", -1, "", "")
	//if len(seatList) == 0 {
	//	errorResponse(w, http.StatusBadRequest, "not found")
	//	return
	//}

	var seatInformationList []domain.SeatInformation

	for _, seat := range seatList {

		s := domain.SeatInformation{seat.SeatRow, seat.SeatColumn, seat.SeatClass, seat.IsSmokingSeat, false}

		seatReservationList := []domain.SeatReservation{}

		query := `SELECT s.* FROM seat_reservations s, reservations r WHERE	r.date=? AND r.train_class=? AND r.train_name=? AND car_number=? AND seat_row=? AND seat_column=?`

		err = dbx.Select(
			&seatReservationList, query,
			date.Format("2006/01/02"),
			seat.TrainClass,
			trainName,
			seat.CarNumber,
			seat.SeatRow,
			seat.SeatColumn,
		)
		if err != nil {
			errorResponse(w, http.StatusBadRequest, err.Error())
			return
		}

		fmt.Println(seatReservationList)

		for _, seatReservation := range seatReservationList {
			reservation := domain.Reservation{}
			query = "SELECT * FROM reservations WHERE reservation_id=?"
			err = dbx.Get(&reservation, query, seatReservation.ReservationId)
			if err != nil {
				panic(err)
			}

			var departureStation, arrivalStation domain.Station
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

		fmt.Println(s.IsOccupied)
		seatInformationList = append(seatInformationList, s)
	}

	// 各号車の情報

	simpleCarInformationList := []domain.SimpleCarInformation{}
	seat := domain.Seat{}
	//query = "SELECT * FROM seat_master WHERE train_class=? AND car_number=? ORDER BY seat_row, seat_column LIMIT 1"
	i := 1
	for {
		seatList := dbCacheSiJian.SelectSeatBy(trainClass, i, "", -1, "", "")
		if len(seatList) == 0 {
			break
		}
		seat = seatList[0]
		//err = dbx.SelectByID(&seat, query, trainClass, i)
		//if err != nil {
		//	break
		//}
		simpleCarInformationList = append(simpleCarInformationList, domain.SimpleCarInformation{i, seat.SeatClass})
		i = i + 1
	}

	c := domain.CarInformation{date.Format("2006/01/02"), trainClass, trainName, carNumber, seatInformationList, simpleCarInformationList}
	resp, err := json.Marshal(c)
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}
	w.Write(resp)
}
