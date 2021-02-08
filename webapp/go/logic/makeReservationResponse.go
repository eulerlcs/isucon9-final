package logic

import (
	"zsj-isucon-09-final/dbCacheSiJian"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

func makeReservationResponse(reservation domain.Reservation) (domain.ReservationResponse, error) {
	var dbx = (&utils.MYSQL{}).GetDB()
	defer dbx.Close()

	reservationResponse := domain.ReservationResponse{}

	var departure, arrival string
	err := dbx.Get(
		&departure,
		"SELECT departure FROM train_timetable_master WHERE date=? AND train_class=? AND train_name=? AND station=?",
		reservation.Date.Format("2006/01/02"), reservation.TrainClass, reservation.TrainName, reservation.Departure,
	)
	if err != nil {
		return reservationResponse, err
	}
	err = dbx.Get(
		&arrival,
		"SELECT arrival FROM train_timetable_master WHERE date=? AND train_class=? AND train_name=? AND station=?",
		reservation.Date.Format("2006/01/02"), reservation.TrainClass, reservation.TrainName, reservation.Arrival,
	)
	if err != nil {
		return reservationResponse, err
	}

	reservationResponse.ReservationId = reservation.ReservationId
	reservationResponse.Date = reservation.Date.Format("2006/01/02")
	reservationResponse.Amount = reservation.Amount
	reservationResponse.Adult = reservation.Adult
	reservationResponse.Child = reservation.Child
	reservationResponse.Departure = reservation.Departure
	reservationResponse.Arrival = reservation.Arrival
	reservationResponse.TrainClass = reservation.TrainClass
	reservationResponse.TrainName = reservation.TrainName
	reservationResponse.DepartureTime = departure
	reservationResponse.ArrivalTime = arrival

	query := "SELECT * FROM seat_reservations WHERE reservation_id=?"
	err = dbx.Select(&reservationResponse.Seats, query, reservation.ReservationId)

	// 1つの予約内で車両番号は全席同じ
	reservationResponse.CarNumber = reservationResponse.Seats[0].CarNumber

	if reservationResponse.Seats[0].CarNumber == 0 {
		reservationResponse.SeatClass = "non-reserved"
	} else {
		// 座席種別を取得
		//seat := Seat{}
		//query = "SELECT * FROM seat_master WHERE train_class=? AND car_number=? AND seat_column=? AND seat_row=?"
		seat, found := dbCacheSiJian.SelectSeatByPK(reservation.TrainClass, reservationResponse.CarNumber,
			reservationResponse.Seats[0].SeatColumn, reservationResponse.Seats[0].SeatRow)
		if !found {
			return reservationResponse, err
		}
		//err = dbx.Get(
		//	&seat, query,
		//	reservation.TrainClass, reservationResponse.CarNumber,
		//	reservationResponse.Seats[0].SeatColumn, reservationResponse.Seats[0].SeatRow,
		//)
		//if err == sql.ErrNoRows {
		//	return reservationResponse, err
		//}
		//if err != nil {
		//	return reservationResponse, err
		//}
		reservationResponse.SeatClass = seat.SeatClass
	}

	for i, v := range reservationResponse.Seats {
		// omit
		v.ReservationId = 0
		v.CarNumber = 0
		reservationResponse.Seats[i] = v
	}
	return reservationResponse, nil
}
