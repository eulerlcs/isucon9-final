package dbCacheSiJian

import (
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"log"
	"strconv"
	"strings"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
	//"time"
)

//type TrainTimeTable struct {
//	Date         time.Time `json:"date" db:"date"`
//	TrainClass   string    `json:"train_class" db:"train_class"`
//	TrainName    string    `json:"train_name" db:"train_name"`
//	Station      string    `json:"station" db:"station"`
//	Departure    string    `json:"departure" db:"departure"`
//	Arrival      string    `json:"arrival" db:"arrival"`
//}

//var cacheTrainTimeTableArrival = make(map[string]string)
//var cacheTrainTimeTableDepature = make(map[string]string)

//func initCacheTrainTimeTable() () {
//	log.Println("=====initCacheTrainTimeTable====")
//
//	trainTimeTableist := []TrainTimeTable{}
//
//	query := "select * from train_timetable_master"
//	err := dbx.Select(&trainTimeTableist, query)
//
//	if err != nil {
//		log.Print(err.Error())
//		return
//	}
//
//	for _,v := range trainTimeTableist {
//		s:= []string{v.Date.Format("2006/01/02"),v.TrainClass,v.TrainName,v.Station}
//		key := strings.Join(s, ",")
//		cacheTrainTimeTableArrival[key] = v.Arrival
//		cacheTrainTimeTableDepature[key] = v.Departure
//	}
//
//	log.Println("=====initCacheTrainTimeTable====")
//}

var cacheTrainList []domain.Train = nil
var CacheTrainMapByDateClassName = make(map[string][]domain.Train)

func initCacheTrainList() ([]domain.Train, error) {
	log.Println("=====initCacheTrainList====")

	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	cacheTrainList = []domain.Train{}

	query := "SELECT * FROM train_master order by date, departure_at, train_class, train_name"
	err := dbx.Select(&cacheTrainList, query)

	if err != nil {
		log.Print(err.Error())
		return cacheTrainList, err
	}

	for _, v := range cacheTrainList {
		s := []string{v.Date.Format("2006/01/02"), v.TrainClass, v.TrainName}
		key := strings.Join(s, ",")
		trainList := CacheTrainMapByDateClassName[key]
		if trainList == nil {
			trainList = []domain.Train{v}
		} else {
			trainList = append(trainList, v)
		}
		CacheTrainMapByDateClassName[key] = trainList
	}

	log.Println("=====initCacheTrainList====")

	return cacheTrainList, err
}

//func selectTrainListByDateClassName(date time.Time,trainClass string,trainName string) []Train {
//	trainList := []Train{}
//	for _,v := range cacheTrainList {
//		if v.Date == date && v.TrainClass == trainClass && v.TrainName == trainName {
//			trainList = append(trainList, v)
//		}
//	}
//	return trainList
//}

var cacheSeatList []domain.Seat = nil

func initCacheSeatList() ([]domain.Seat, error) {
	log.Println("=====initCacheSeatList====")

	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	cacheSeatList = []domain.Seat{}

	query := "SELECT * FROM seat_master ORDER BY train_class, car_number, seat_row, seat_column"
	err := dbx.Select(&cacheSeatList, query)

	if err != nil {
		log.Print(err.Error())
		return cacheSeatList, err
	}

	//for _,v := range cacheSeatList {
	//	log.Println(v)
	//}

	log.Println("=====initCacheSeatList====")

	return cacheSeatList, err
}

func SelectSeatBy(trainClass string, carNumber int, seatColumn string, seatRow int, seatClass string, isSmokingSeat string) []domain.Seat {
	seatList := []domain.Seat{}
	for _, v := range cacheSeatList {
		if trainClass != "" && v.TrainClass != trainClass {
			continue
		}
		if carNumber != -1 && v.CarNumber != carNumber {
			continue
		}
		if seatColumn != "" && v.SeatColumn != seatColumn {
			continue
		}
		if seatRow != -1 && v.SeatRow != seatRow {
			continue
		}
		if seatClass != "" && v.SeatClass != seatClass {
			continue
		}
		if isSmokingSeat != "" && strconv.FormatBool(v.IsSmokingSeat) != isSmokingSeat {
			continue
		}
		seatList = append(seatList, v)
	}
	return seatList
}

//req.TrainClass, carnum, req.SeatClass, req.IsSmokingSeat
func SelectSeatBy2(trainClass string, carNumber int, seatClass string, isSmokingSeat bool) []domain.Seat {
	log.Println("in selectSeatBy2")
	//log.Println(trainClass,carNumber,seatClass,isSmokingSeat)
	seatList := []domain.Seat{}
	for _, v := range cacheSeatList {
		//log.Println(v)
		if v.TrainClass != trainClass {
			continue
		}
		if v.CarNumber != carNumber {
			continue
		}
		if v.SeatClass != seatClass {
			continue
		}
		if v.IsSmokingSeat != isSmokingSeat {
			continue
		}
		//log.Println(v,"hit")
		seatList = append(seatList, v)
	}
	return seatList
}

func SelectSeatByPK(trainClass string, carNumber int, seatColumn string, seatRow int) (domain.Seat, bool) {
	for _, v := range cacheSeatList {
		if trainClass != "" && v.TrainClass != trainClass {
			continue
		}
		if carNumber != -1 && v.CarNumber != carNumber {
			continue
		}
		if seatColumn != "" && v.SeatColumn != seatColumn {
			continue
		}
		if seatRow != -1 && v.SeatRow != seatRow {
			continue
		}
		return v, true
	}
	return domain.Seat{}, false
}

var CacheDistanceFareList []domain.DistanceFare = nil

func initCacheDistanceFareList() ([]domain.DistanceFare, error) {
	log.Println("=====initCacheDistanceFareList====")

	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	CacheDistanceFareList = []domain.DistanceFare{}

	query := "SELECT * FROM distance_fare_master ORDER BY distance"
	err := dbx.Select(&CacheDistanceFareList, query)

	if err != nil {
		log.Print(err.Error())
		return CacheDistanceFareList, err
	}

	//for _,v := range cacheDistanceFareList {
	//	log.Println(v)
	//}

	log.Println("=====initCacheDistanceFareList====")

	return CacheDistanceFareList, err
}

var cacheFareList []domain.Fare = nil

func initCacheFareList() ([]domain.Fare, error) {
	log.Println("=====initCacheFareList====")

	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	cacheFareList = []domain.Fare{}

	query := "SELECT * FROM fare_master ORDER BY train_class,seat_class,start_date"
	err := dbx.Select(&cacheFareList, query)

	if err != nil {
		log.Print(err.Error())
		return cacheFareList, err
	}

	//for _,v := range cacheFareList {
	//	log.Println(v)
	//}

	log.Println("=====initCacheFareList====")

	return cacheFareList, err
}

func SelectFareBy(trainClass string, seatClass string) []domain.Fare {
	fareList := []domain.Fare{}
	for _, v := range cacheFareList {
		if trainClass != "" && v.TrainClass != trainClass {
			continue
		}
		if seatClass != "" && v.SeatClass != seatClass {
			continue
		}
		fareList = append(fareList, v)
	}
	return fareList
}

func GetAvailableSeats(train domain.Train, fromStation domain.Station, toStation domain.Station, seatClass string, isSmokingSeat bool) ([]domain.Seat, error) {
	// 指定種別の空き座席を返す

	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	var err error

	// 全ての座席を取得する
	//query := "SELECT * FROM seat_master WHERE train_class=? AND seat_class=? AND is_smoking_seat=?"

	//seatList := []Seat{}
	//err = dbx.Select(&seatList, query, train.TrainClass, seatClass, isSmokingSeat)
	//if err != nil {
	//	return nil, err
	//}
	seatList := SelectSeatBy(train.TrainClass, -1, "", -1, seatClass, strconv.FormatBool(isSmokingSeat))

	availableSeatMap := map[string]domain.Seat{}
	for _, seat := range seatList {
		availableSeatMap[fmt.Sprintf("%d_%d_%s", seat.CarNumber, seat.SeatRow, seat.SeatColumn)] = seat
	}

	// すでに取られている予約を取得する
	query := `
	SELECT sr.reservation_id, sr.car_number, sr.seat_row, sr.seat_column
	FROM seat_reservations sr, reservations r, seat_master s, station_master std, station_master sta
	WHERE
		r.reservation_id=sr.reservation_id AND
		s.train_class=r.train_class AND
		s.car_number=sr.car_number AND
		s.seat_column=sr.seat_column AND
		s.seat_row=sr.seat_row AND
		std.name=r.departure AND
		sta.name=r.arrival
	`

	if train.IsNobori {
		query += "AND ((sta.id < ? AND ? <= std.id) OR (sta.id < ? AND ? <= std.id) OR (? < sta.id AND std.id < ?))"
	} else {
		query += "AND ((std.id <= ? AND ? < sta.id) OR (std.id <= ? AND ? < sta.id) OR (sta.id < ? AND ? < std.id))"
	}

	seatReservationList := []domain.SeatReservation{}
	err = dbx.Select(&seatReservationList, query, fromStation.ID, fromStation.ID, toStation.ID, toStation.ID, fromStation.ID, toStation.ID)
	if err != nil {
		return nil, err
	}

	for _, seatReservation := range seatReservationList {
		key := fmt.Sprintf("%d_%d_%s", seatReservation.CarNumber, seatReservation.SeatRow, seatReservation.SeatColumn)
		delete(availableSeatMap, key)
	}

	ret := []domain.Seat{}
	for _, seat := range availableSeatMap {
		ret = append(ret, seat)
	}
	return ret, nil
}

func InitCache() {
	initCacheSeatList()
	initCacheDistanceFareList()
	initCacheFareList()
	initCacheTrainList()
	//initCacheTrainTimeTable()
}
