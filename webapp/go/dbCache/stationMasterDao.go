package dbCache

import (
	"encoding/json"
	_ "github.com/go-sql-driver/mysql"
	"log"
	"sort"
	"strconv"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

type StationMasterDao struct {
	isOK bool
}

func (slf *StationMasterDao) SelectByID(id int) (*domain.Station, error) {
	slf.DoCacheNX()

	strID := strconv.Itoa(id)

	indexStr, err := utils.Rdb.HGet("station:id:index", strID).Result()
	if err != nil {
		log.Printf("ZSJ - station:id:index[%s] is not exists.\n", strID)
		return nil, err
	}

	index, _ := strconv.ParseInt(indexStr, 10, 64)
	str, err := utils.Rdb.LRange("station:list", index, index).Result()
	if err != nil {
		log.Printf("ZSJ - station:list[%d] is not exists.\n", index)
		return nil, err
	}

	station := domain.Station{}
	json.Unmarshal([]byte(str[0]), &station)

	return &station, err
}

func (slf *StationMasterDao) SelectByName(name string) (*domain.Station, error) {
	slf.DoCacheNX()

	indexStr, err := utils.Rdb.HGet("station:name:index", name).Result()
	if err != nil {
		log.Printf("ZSJ - station:name:index[%s] is not exists.\n", name)
		return nil, err
	}

	index, _ := strconv.ParseInt(indexStr, 10, 64)
	str, err := utils.Rdb.LRange("station:list", index, index).Result()
	if err != nil {
		log.Printf("ZSJ - station:list[%d] is not exists.\n", index)
		return nil, err
	}

	station := domain.Station{}
	json.Unmarshal([]byte(str[0]), &station)

	return &station, err
}

func (slf *StationMasterDao) SelectAllByIDAscForResponse() ([]domain.StationForResponse, error) {
	slf.DoCacheNX()

	strList := utils.Rdb.LRange("station:list", 0, -1).Val()
	size := len(strList)
	stationList := make([]domain.StationForResponse, size)

	for i, str := range strList {
		json.Unmarshal([]byte(str), &stationList[i])
	}

	return stationList, nil
}

func (slf *StationMasterDao) SelectAllByIDAsc() ([]domain.Station, error) {
	slf.DoCacheNX()

	strList := utils.Rdb.LRange("station:list", 0, -1).Val()
	size := len(strList)
	stationList := make([]domain.Station, size)

	for i, str := range strList {
		json.Unmarshal([]byte(str), &stationList[i])
	}

	return stationList, nil
}

func (slf *StationMasterDao) SelectAllByIDDesc() ([]domain.Station, error) {
	stationList, _ := slf.SelectAllByIDAsc()

	sort.SliceStable(stationList, func(i, j int) bool {
		return stationList[i].ID < stationList[j].ID
	})

	return stationList, nil
}

func (slf *StationMasterDao) DoCacheNX() {
	if slf.isOK {
		return
	}

	if utils.IsCached("station") {
		slf.isOK = true
		return
	}

	slf.doCache()
}

func (slf *StationMasterDao) doCache() error {
	log.Printf("ZSJ - cache %s start.\n", "StationMasterDao")

	stationList, err := slf.selectStationListByIDAscFromDB()
	if err != nil {
		return err
	}

	var indexStr string
	for i, v := range stationList {
		indexStr = strconv.Itoa(i)
		bytes, _ := json.Marshal(v)

		utils.Rdb.HSet("station:id:index", strconv.Itoa(v.ID), indexStr)
		utils.Rdb.HSet("station:name:index", v.Name, indexStr)
		utils.Rdb.RPush("station:list", bytes)
	}

	slf.isOK = true
	log.Printf("ZSJ - cache %s end.\n", "StationMasterDao")

	return err
}

// **************************************
//    以下、DB操作
func (slf *StationMasterDao) selectStationListByIDAscFromDB() ([]domain.Station, error) {
	var cacheStationList []domain.Station

	query := "SELECT * FROM station_master order by id"
	err := utils.Dbx.Select(&cacheStationList, query)

	if err != nil {
		log.Print(err.Error())
		return nil, err
	}
	return cacheStationList, err
}
