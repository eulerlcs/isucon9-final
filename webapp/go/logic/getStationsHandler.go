package logic

import (
	"encoding/json"
	"net/http"
	"zsj-isucon-09-final/dbCache"
)

func GetStationsHandler(w http.ResponseWriter, r *http.Request) {
	/*
		駅一覧
			GET /api/stations

		return []Station{}
	*/
	var stationMasterDao dbCache.StationMasterDao

	stations, _ := stationMasterDao.SelectAllByIDAsc()

	w.Header().Set("Content-Type", "application/json;charset=utf-8")
	json.NewEncoder(w).Encode(stations)
}
