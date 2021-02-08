package logic

import (
	"database/sql"
	"encoding/json"
	"log"
	"net/http"
	"strconv"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"

	"goji.io/pat"
)

func UserReservationResponseHandler(w http.ResponseWriter, r *http.Request) {
	/*
		ログイン
		POST /auth/login
	*/
	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	user, errCode, errMsg := getUser(w, r)
	if errCode != http.StatusOK {
		errorResponse(w, errCode, errMsg)
		return
	}
	itemIDStr := pat.Param(r, "item_id")
	itemID, err := strconv.ParseInt(itemIDStr, 10, 64)
	if err != nil || itemID <= 0 {
		errorResponse(w, http.StatusBadRequest, "incorrect item id")
		return
	}

	reservation := domain.Reservation{}
	query := "SELECT * FROM reservations WHERE reservation_id=? AND user_id=?"
	err = dbx.Get(&reservation, query, itemID, user.ID)
	if err == sql.ErrNoRows {
		errorResponse(w, http.StatusNotFound, "Reservation not found")
		return
	}
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}

	reservationResponse, err := makeReservationResponse(reservation)

	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		log.Println("makeReservationResponse() ", err)
		return
	}

	w.Header().Set("Content-Type", "application/json;charset=utf-8")
	json.NewEncoder(w).Encode(reservationResponse)
}
