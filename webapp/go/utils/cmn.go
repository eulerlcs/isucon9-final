package utils

import (
	"log"
	"time"
)

var (
	InitCheckInterval = time.Millisecond
	WaitTimeMax       = 20 * time.Minute
)

func IsCached(field string) bool {
	redis0 := (&REDIS{}).GetRedisClient(0)
	pipe := redis0.Pipeline()
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
