package logic

import (
	"encoding/json"
	"fmt"
	"net/http"
	"zsj-isucon-09-final/dbCacheSiJian"
)

func DistanceFareHandler(w http.ResponseWriter, r *http.Request) {

	//distanceFareList := []DistanceFare{}

	//query := "SELECT * FROM distance_fare_master"
	//err := dbx.Select(&distanceFareList, query)
	//if err != nil {
	//	errorResponse(w, http.StatusBadRequest, err.Error())
	//	return
	//}

	distanceFareList := dbCacheSiJian.CacheDistanceFareList

	for _, distanceFare := range distanceFareList {
		fmt.Fprintf(w, "%#v, %#v\n", distanceFare.Distance, distanceFare.Fare)
	}

	w.Header().Set("Content-Type", "application/json;charset=utf-8")
	json.NewEncoder(w).Encode(distanceFareList)
}
