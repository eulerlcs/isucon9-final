SELECT
	tm.train_class ,
	tm.train_name ,
	tm.is_nobori ,
	train_start_station.name train_start_station ,
	train_start_station.id train_start_station_id,
	train_arrival_station.name train_arrival_station ,
	train_arrival_station.id train_arrival_station_id,
	man_start.station man_start_station ,
	man_start_station.id man_start_station_id,
	man_start.departure man_start_departure,
	man_arrival.station man_arrival_station,
	man_arrival_station.id man_arrival_station_id,
	man_arrival.arrival man_arrival_arrival
FROM
	train_master tm
	-- ��Ԃ̎n�_�w
inner join station_master train_start_station on
	tm.start_station = train_start_station.name
	-- ��Ԃ̏I�_�w
inner join station_master train_arrival_station on
	tm.last_station = train_arrival_station.name
	-- ��q�̏�ԉw
inner join train_timetable_master man_start on
	tm.date = man_start.date
	and tm.train_class = man_start.train_class
	and tm.train_name = man_start.train_name
inner join station_master man_start_station on
	man_start.station = man_start_station.name
	-- ��q�̍~�ԉw
inner join train_timetable_master man_arrival on
	tm.date = man_arrival.date
	and tm.train_class = man_arrival.train_class
	and tm.train_name = man_arrival.train_name
inner join station_master man_arrival_station on
	man_arrival.station = man_arrival_station.name
WHERE
	tm.date = '2020-01-01'
	and man_start.departure >= '20:00:00'
	and tm.train_class = '�x�����'
	and man_start.station = '�É�'
	and man_arrival.station = '���'
	and ((
		man_start_station.id < man_arrival_station.id /* �̂ڂ� */
		and man_start_station.id >= train_start_station.id
		and man_arrival_station.id <= train_arrival_station.id
	 ) or (
		man_start_station.id > man_arrival_station.id /* ���� */
		and man_start_station.id <= train_start_station.id
		and man_arrival_station.id >= train_arrival_station.id 
	))
ORDER BY
	man_start.departure
LIMIT 10