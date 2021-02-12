package logic

import (
	"fmt"
	"math"
	"time"
	"zsj-isucon-09-final/dbCache"
	"zsj-isucon-09-final/dbCacheSiJian"
	"zsj-isucon-09-final/domain"
)

func fareCalc(date time.Time, depStation int, destStation int, trainClass, seatClass string) (int, error) {
	//
	// 料金計算メモ
	// 距離運賃(円) * 期間倍率(繁忙期なら2倍等) * 車両クラス倍率(急行・各停等) * 座席クラス倍率(プレミアム・指定席・自由席)
	//
	var err error
	var fromStation, toStation *domain.Station

	var stationMasterDao dbCache.StationMasterDao

	fromStation, _ = stationMasterDao.SelectByID(depStation)
	toStation, _ = stationMasterDao.SelectByID(destStation)

	fmt.Println("distance", math.Abs(toStation.Distance-fromStation.Distance))
	distFare, err := getDistanceFare(math.Abs(toStation.Distance - fromStation.Distance))
	if err != nil {
		return 0, err
	}
	fmt.Println("distFare", distFare)

	// 期間・車両・座席クラス倍率
	//fareList := []Fare{}
	//query := "SELECT * FROM fare_master WHERE train_class=? AND seat_class=? ORDER BY start_date"
	//err = dbx.Select(&fareList, query, trainClass, seatClass)
	//if err != nil {
	//	return 0, err
	//}
	fareList := dbCacheSiJian.SelectFareBy(trainClass, seatClass)
	if len(fareList) == 0 {
		return 0, fmt.Errorf("fare_master does not exists")
	}

	selectedFare := fareList[0]
	date = time.Date(date.Year(), date.Month(), date.Day(), 0, 0, 0, 0, time.UTC)
	for _, fare := range fareList {
		if !date.Before(fare.StartDate) {
			fmt.Println(fare.StartDate, fare.FareMultiplier)
			selectedFare = fare
		}
	}

	fmt.Println("%%%%%%%%%%%%%%%%%%%")

	return int(float64(distFare) * selectedFare.FareMultiplier), nil
}
