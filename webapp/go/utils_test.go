package main

import (
	"testing"
	"zsj-isucon-09-final/domain"
	"zsj-isucon-09-final/logic"
)

func TestgetUsableTrainClassList(t *testing.T) {

	fromStation := domain.Station{1, "全部止まる", 10.0, true, true, true}
	ret := logic.GetUsableTrainClassList(fromStation, fromStation)

	if len(ret) != 3 {
		t.Fatalf("failed test %#v", ret)
	}

	fromStation = domain.Station{1, "ちょっと止まる", 10.0, false, true, true}
	ret = logic.GetUsableTrainClassList(fromStation, fromStation)

	if len(ret) != 2 {
		t.Fatalf("failed test %#v", ret)
	}

	fromStation = domain.Station{1, "各駅", 10.0, false, false, true}
	ret = logic.GetUsableTrainClassList(fromStation, fromStation)

	if len(ret) != 1 {
		t.Fatalf("failed test %#v", ret)
	}
}
