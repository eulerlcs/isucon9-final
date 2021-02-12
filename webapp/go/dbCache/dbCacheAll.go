package dbCache

import (
	"log"
	"zsj-isucon-09-final/dbCacheSiJian"
)

func DoCacheAll() {
	log.Println("ZSJ - init cache begin...")

	(&StationMasterDao{}).DoCacheNX()

	dbCacheSiJian.InitCache()

	log.Println("ZSJ - init cache end.")
}
