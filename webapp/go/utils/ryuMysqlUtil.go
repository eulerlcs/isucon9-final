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

func (slf *MYSQL) WaitOK() {
	if Dbx != nil {
		return
	} else {
		Dbx = slf.getDBX()
	}
}

func (slf *MYSQL) getDBX(timeout ...time.Duration) *sqlx.DB {
	spentTime := 0 * time.Second
	waitTime := WaitTimeMax
	if len(timeout) > 0 {
		waitTime = timeout[0]
	}

	var err error
	var localDbx *sqlx.DB
	for {
		localDbx, err = slf.doGetDB()
		if err == nil || spentTime > waitTime {
			log.Println("ZSJ - succeeded to connect to mysql.")
			return localDbx
		} else {
			log.Printf("ZSJ - failed to connect to mysql: %s.\n", err.Error())
			if localDbx != nil {
				localDbx.Close()
			}

			time.Sleep(InitCheckInterval)
			spentTime += InitCheckInterval
		}
	}

	return nil
}

func (slf *MYSQL) doGetDB() (*sqlx.DB, error) {

	dsn := slf.getDsn()

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

	localDbx.SetConnMaxLifetime(10 * time.Second)
	localDbx.SetMaxIdleConns(200)
	localDbx.SetMaxOpenConns(200)

	return localDbx, err
}

func (slf *MYSQL) getDsn() string {
	//		Host:     getEnv("MYSQL_HOST", "127.0.0.1"),
	//		Port:     getEnv("MYSQL_PORT", "3306"),
	//		User:     getEnv("MYSQL_USER", "isucon"),
	//		DBName:   getEnv("MYSQL_DBNAME", "isuumo"),
	//		Password: getEnv("MYSQL_PASS", "isucon"),

	host := os.Getenv("MYSQL_HOST")
	if host == "" {
		host = "127.0.0.1"
	}
	port := os.Getenv("MYSQL_PORT")
	if port == "" {
		port = "3306"
	}
	_, err := strconv.Atoi(port)
	if err != nil {
		log.Fatalf("failed to read DB port number from an environment variable MYSQL_PORT.\nError: %s", err.Error())
	}
	user := os.Getenv("MYSQL_USER")
	if user == "" {
		user = "isutrain"
	}
	dbname := os.Getenv("MYSQL_DBNAME")
	if dbname == "" {
		dbname = "isutrain"
	}
	password := os.Getenv("MYSQL_PASS")
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

	return dsn
}
