package utils

import (
	"log"
	"time"
)

var (
	InitCheckInterval = 500 * time.Millisecond
	WaitTimeMax       = 120 * time.Minute
)

func IsCached(field string) bool {
	redis1 := RedisClient1
	pipe := redis1.Pipeline()
	result := pipe.HSetNX("init:status", field, "1")
	pipe.Exec()

	ret, err := result.Result()
	if err == nil {
		return !ret
	} else {
		log.Printf("failed to connect to DB: %s.\n", err.Error())
		return false
	}
}
