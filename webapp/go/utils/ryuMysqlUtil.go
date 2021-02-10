package utils

import (
	"fmt"
	_ "github.com/go-sql-driver/mysql"
	"github.com/jmoiron/sqlx"
	"log"
	"os"
	"strconv"
	"time"
)

type MYSQL struct{}

var Dbx *sqlx.DB

func (myMysql *MYSQL) WaitOK() {
	if Dbx != nil {
		return
	} else {
		Dbx = myMysql.getDBX()
	}
}

func (myMysql *MYSQL) getDBX(timeout ...time.Duration) *sqlx.DB {
	spentTime := 0 * time.Second
	waitTime := WaitTimeMax
	if len(timeout) > 0 {
		waitTime = timeout[0]
	}

	var err error
	var localDbx *sqlx.DB
	for {
		localDbx, err = myMysql.doGetDB()
		if err == nil || spentTime > waitTime {
			log.Println("ZSJ - succeeded to connect to mysql.")
			return localDbx
		} else {
			log.Printf("failed to connect to mysql: %s.\n", err.Error())
			if localDbx != nil {
				localDbx.Close()
			}

			time.Sleep(InitCheckInterval)
			spentTime += InitCheckInterval
		}
	}

	return nil
}

func (myMysql *MYSQL) doGetDB() (*sqlx.DB, error) {
	host := os.Getenv("MYSQL_HOSTNAME")
	if host == "" {
		host = "127.0.0.1"
	}
	port := os.Getenv("MYSQL_PORT")
	if port == "" {
		port = "3306"
	}
	_, err := strconv.Atoi(port)
	if err != nil {
		port = "3306"
	}
	user := os.Getenv("MYSQL_USER")
	if user == "" {
		user = "isutrain"
	}
	dbname := os.Getenv("MYSQL_DATABASE")
	if dbname == "" {
		dbname = "isutrain"
	}
	password := os.Getenv("MYSQL_PASSWORD")
	if password == "" {
		password = "isutrain"
	}

	dsn := fmt.Sprintf(
		"%s:%s@tcp(%s:%s)/%s?charset=utf8mb4&parseTime=true&loc=Local",
		user,
		password,
		host,
		port,
		dbname,
	)

	localDbx, err := sqlx.Open("mysql", dsn)
	if err != nil {
		if localDbx != nil {
			localDbx.Close()
		}
		return nil, err
	}

	err = localDbx.Ping()
	if err != nil {
		if localDbx != nil {
			localDbx.Close()
		}
		return nil, err
	}

	return localDbx, err
}
