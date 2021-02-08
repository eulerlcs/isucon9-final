package logic

import (
	"fmt"
	"strconv"
	"zsj-isucon-09-final/dbCacheSiJian"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/utils"
)

func GetAvailableSeatsRyu01(train domain.TrainSearchResponseWork, fromStationID int, toStationID int, seatClass string, isSmokingSeat bool) ([]domain.Seat, error) {
	// 指定種別の空き座席を返す
	var dbx = utils.Dbx

	var err error

	// 全ての座席を取得する
	//query := "SELECT * FROM seat_master WHERE train_class=? AND seat_class=? AND is_smoking_seat=?"

	//seatList := []Seat{}
	//err = dbx.Select(&seatList, query, train.TrainClass, seatClass, isSmokingSeat)
	//if err != nil {
	//	return nil, err
	//}
	seatList := dbCacheSiJian.SelectSeatBy(train.Class, -1, "", -1, seatClass, strconv.FormatBool(isSmokingSeat))

	availableSeatMap := map[string]domain.Seat{}
	for _, seat := range seatList {
		availableSeatMap[fmt.Sprintf("%d_%d_%s", seat.CarNumber, seat.SeatRow, seat.SeatColumn)] = seat
	}

	// すでに取られている予約を取得する
	query := `
	SELECT sr.reservation_id, sr.car_number, sr.seat_row, sr.seat_column
	FROM seat_reservations sr, reservations r, seat_master s, station_master std, station_master sta
	WHERE
		r.reservation_id=sr.reservation_id AND
		s.train_class=r.train_class AND
		s.car_number=sr.car_number AND
		s.seat_column=sr.seat_column AND
		s.seat_row=sr.seat_row AND
		std.name=r.departure AND
		sta.name=r.arrival
	`

	if train.IsNobori {
		query += "AND ((sta.id < ? AND ? <= std.id) OR (sta.id < ? AND ? <= std.id) OR (? < sta.id AND std.id < ?))"
	} else {
		query += "AND ((std.id <= ? AND ? < sta.id) OR (std.id <= ? AND ? < sta.id) OR (sta.id < ? AND ? < std.id))"
	}

	seatReservationList := []domain.SeatReservation{}
	err = dbx.Select(&seatReservationList, query, fromStationID, fromStationID, toStationID, toStationID, fromStationID, toStationID)
	if err != nil {
		return nil, err
	}

	for _, seatReservation := range seatReservationList {
		key := fmt.Sprintf("%d_%d_%s", seatReservation.CarNumber, seatReservation.SeatRow, seatReservation.SeatColumn)
		delete(availableSeatMap, key)
	}

	ret := []domain.Seat{}
	for _, seat := range availableSeatMap {
		ret = append(ret, seat)
	}
	return ret, nil
}
