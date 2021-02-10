package dbCache

import (
	"encoding/json"
	_ "github.com/go-sql-driver/mysql"
	"log"
	"sort"
	"strconv"
	"time"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

type StationMasterDao struct{}

var (
	isOK = false
)

func (slf *StationMasterDao) SelectByID(id int) (domain.Station, error) {
	slf.waitUtilCached()

	redisClient := utils.RedisClient1

	str, err := redisClient.HGet("station:id", strconv.Itoa(id)).Result()
	station := domain.Station{}
	json.Unmarshal([]byte(str), &station)

	return station, err
}

func (slf *StationMasterDao) SelectAllByIDAsc() ([]domain.Station, error) {
	slf.waitUtilCached()

	redisClient := utils.RedisClient1

	size := int(redisClient.HLen("station:id").Val())
	stations := make([]domain.Station, size)

	for i := 1; i <= size; i++ {
		str := redisClient.HGet("station:id", strconv.Itoa(i)).Val()
		json.Unmarshal([]byte(str), &stations[i-1])
	}

	return stations, nil
}

func (slf *StationMasterDao) SelectAllByIDAscForResponse() ([]domain.StationForResponse, error) {
	slf.waitUtilCached()

	redisClient := utils.RedisClient1

	size := int(redisClient.HLen("station:id").Val())
	stations := make([]domain.StationForResponse, size)

	for i := 1; i <= size; i++ {
		str := redisClient.HGet("station:id", strconv.Itoa(i)).Val()
		json.Unmarshal([]byte(str), &stations[i-1])
	}

	return stations, nil
}

func (slf *StationMasterDao) SelectAllByIDDesc() ([]domain.Station, error) {
	stations, _ := slf.SelectAllByIDAsc()
	sort.SliceStable(stations, func(i, j int) bool {
		return stations[i].ID < stations[j].ID
	})

	return stations, nil
}

func (slf *StationMasterDao) SelectByName(name string) (domain.Station, error) {
	slf.waitUtilCached()

	redisClient := utils.RedisClient1

	str, err := redisClient.HGet("station:name", name).Result()

	id, err := strconv.Atoi(str)
	station, err := slf.SelectByID(id)

	return station, err
}

func (slf *StationMasterDao) CacheAll() error {
	if utils.IsCached("station") {
		isOK = true
		return nil
	} else {
		isOK = false
	}

	log.Printf("ZSJ - cache %s start.\n", "StationMasterDao")

	var dbx = utils.Dbx

	redisClient := utils.RedisClient1

	var cacheStationList []domain.Station

	query := "SELECT * FROM station_master order by id"
	err := dbx.Select(&cacheStationList, query)

	if err != nil {
		log.Print(err.Error())
		return err
	}

	for _, v := range cacheStationList {
		bytes, _ := json.Marshal(v)
		redisClient.HSet("station:id", strconv.Itoa(v.ID), bytes)
		redisClient.HSet("station:name", v.Name, strconv.Itoa(v.ID))
	}

	isOK = true
	log.Printf("ZSJ - cache %s end.\n", "StationMasterDao")

	return err
}

func (slf *StationMasterDao) waitUtilCached() {
	for !isOK {
		time.Sleep(utils.InitCheckInterval)
	}

	// アプリが動いているときに、redisがクリアされたら困る
	slf.CacheAll()
}
