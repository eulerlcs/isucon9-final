package dbCache

import (
	"encoding/json"
	"fmt"
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

	redis0 := (&utils.REDIS{}).GetRedisClient(0)
	defer redis0.Close()

	str, err := redis0.HGet("station:id", strconv.Itoa(id)).Result()
	station := domain.Station{}
	json.Unmarshal([]byte(str), &station)

	return station, err
}

func (slf *StationMasterDao) SelectAllByIDAsc() ([]domain.Station, error) {
	slf.waitUtilCached()

	redis0 := (&utils.REDIS{}).GetRedisClient(0)
	defer redis0.Close()

	size := int(redis0.HLen("station:id").Val())
	stations := make([]domain.Station, size)

	for i := 1; i <= size; i++ {
		str := redis0.HGet("station:id", strconv.Itoa(i)).Val()
		fmt.Println(str)
		json.Unmarshal([]byte(str), &stations[i-1])
	}

	return stations, nil
}

func (slf *StationMasterDao) SelectAllByIDAscForResponse() ([]domain.StationForResponse, error) {
	slf.waitUtilCached()

	redis0 := (&utils.REDIS{}).GetRedisClient(0)
	defer redis0.Close()

	size := int(redis0.HLen("station:id").Val())
	stations := make([]domain.StationForResponse, size)

	for i := 1; i <= size; i++ {
		str := redis0.HGet("station:id", strconv.Itoa(i)).Val()
		fmt.Println(str)
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

	redis0 := (&utils.REDIS{}).GetRedisClient(0)
	defer redis0.Close()

	str, err := redis0.HGet("station:name", name).Result()

	id, err := strconv.Atoi(str)
	station, err := slf.SelectByID(id)

	return station, err
}

func (slf *StationMasterDao) waitUtilCached() {
	for !isOK {
		time.Sleep(utils.InitCheckInterval)
	}

	// アプリが動いているときに、redisがクリアされたら困る
	slf.CacheAll()
}

func (slf *StationMasterDao) CacheAll() error {
	if utils.IsCached("station") {
		isOK = true
		return nil
	} else {
		isOK = false
	}
	dbx := (&utils.MYSQL{}).GetDB()
	defer dbx.Close()
	redis0 := (&utils.REDIS{}).GetRedisClient(0)
	defer redis0.Close()

	var cacheStationList []domain.Station

	query := "SELECT * FROM station_master order by id"
	err := dbx.Select(&cacheStationList, query)

	if err != nil {
		log.Print(err.Error())
		return err
	}

	for _, v := range cacheStationList {
		bytes, _ := json.Marshal(v)
		redis0.HSet("station:id", strconv.Itoa(v.ID), bytes)
		redis0.HSet("station:name", v.Name, strconv.Itoa(v.ID))
	}

	isOK = true
	return err
}
