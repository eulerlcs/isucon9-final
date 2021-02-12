package logic

import (
	"encoding/json"
	"net/http"
	"os"
	"strconv"
	"sync"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

var wg sync.WaitGroup

const (
	// sessionName   = "session_isutrain"
	availableDays = 90
)

func InitializeHandler(w http.ResponseWriter, r *http.Request) {
	/*
		initialize
	*/
	var dbx = utils.Dbx

	dbx.Exec("TRUNCATE seat_reservations")
	dbx.Exec("TRUNCATE reservations")
	dbx.Exec("TRUNCATE users")

	setDays := availableDays
	days := os.Getenv("AVAILABLE_DAYS")
	if days != "" {
		setDays, _ = strconv.Atoi(days)
	}

	resp := domain.InitializeResponse{
		AvailableDays: setDays,
		Language:      "golang",
	}
	w.Header().Set("Content-Type", "application/json;charset=utf-8")
	json.NewEncoder(w).Encode(resp)
}
