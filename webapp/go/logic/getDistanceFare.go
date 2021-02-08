package logic

import (
	"fmt"
	"zsj-isucon-09-final/dbCacheSiJian"
)

func getDistanceFare(origToDestDistance float64) (int, error) {

	//distanceFareList := []DistanceFare{}
	//
	//query := "SELECT distance,fare FROM distance_fare_master ORDER BY distance"
	//err := dbx.Select(&distanceFareList, query)
	//if err != nil {
	//	return 0, err
	//}

	distanceFareList := dbCacheSiJian.CacheDistanceFareList

	lastDistance := 0.0
	lastFare := 0
	for _, distanceFare := range distanceFareList {

		fmt.Println(origToDestDistance, distanceFare.Distance, distanceFare.Fare)
		if float64(lastDistance) < origToDestDistance && origToDestDistance < float64(distanceFare.Distance) {
			break
		}
		lastDistance = distanceFare.Distance
		lastFare = distanceFare.Fare
	}

	return lastFare, nil
}
