package logic

import (
	"encoding/json"
	"log"
	"net/http"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

func UserReservationsHandler(w http.ResponseWriter, r *http.Request) {
	/*
		ログイン
		POST /auth/login
	*/
	var dbx = utils.Dbx

	user, errCode, errMsg := getUser(w, r)
	if errCode != http.StatusOK {
		errorResponse(w, errCode, errMsg)
		return
	}
	reservationList := []domain.Reservation{}

	query := "SELECT * FROM reservations WHERE user_id=?"
	err := dbx.Select(&reservationList, query, user.ID)
	if err != nil {
		errorResponse(w, http.StatusBadRequest, err.Error())
		return
	}

	reservationResponseList := []domain.ReservationResponse{}

	for _, r := range reservationList {
		res, err := makeReservationResponse(r)
		if err != nil {
			errorResponse(w, http.StatusBadRequest, err.Error())
			log.Println("makeReservationResponse()", err)
			return
		}
		reservationResponseList = append(reservationResponseList, res)
	}

	w.Header().Set("Content-Type", "application/json;charset=utf-8")
	json.NewEncoder(w).Encode(reservationResponseList)
}
