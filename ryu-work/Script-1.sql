SELECT
	tm.train_class ,
	tm.train_name ,
	tm.is_nobori ,
	train_start.station train_start_station ,
	train_start.departure train_start_departure,
	train_arrival.station train_arrival_station ,
	train_arrival.arrival train_arrival_arrival,
	man_start.station man_start_station ,
	man_start.departure man_start_departure,
	man_arrival.station man_arrival_station,
	man_arrival.arrival man_arrival_arrival
FROM
	train_master tm
	-- —ñÔ‚Ìn“_‰w
inner join train_timetable_master train_start on
	tm.date = train_start.date
	and tm.train_class = train_start.train_class
	and tm.train_name = train_start.train_name
	and tm.start_station = train_start.station
	-- —ñÔ‚ÌI“_‰w
inner join train_timetable_master train_arrival on
	tm.date = train_arrival.date
	and tm.train_class = train_arrival.train_class
	and tm.train_name = train_arrival.train_name
	and tm.last_station = train_arrival.station
	-- æ‹q‚ÌæÔ‰w
inner join train_timetable_master man_start on
	tm.date = man_start.date
	and tm.train_class = man_start.train_class
	and tm.train_name = man_start.train_name
	-- æ‹q‚Ì~Ô‰w
inner join train_timetable_master man_arrival on
	tm.date = man_arrival.date
	and tm.train_class = man_arrival.train_class
	and tm.train_name = man_arrival.train_name
WHERE
	( (tm.is_nobori = 1
	and train_start.departure >= man_start.departure
	and train_arrival.arrival <= man_arrival.arrival)
	or (tm.is_nobori = 0
	and train_start.departure <= man_start.departure
	and train_arrival.arrival >= man_arrival.arrival) )
	and tm.is_nobori = 1
	and train_start.date = '2020-01-01'
	and man_start.departure >= '13:00:00'
	and train_start.train_class = '’x‚¢‚â‚Â'
	and man_start.station = '‘åã'
	and man_arrival.station = '“Œ‹'
	and man_start.departure >= train_start.departure --
	and man_arrival.arrival <= train_arrival.arrival
ORDER BY
	train_start.departure
LIMIT 100