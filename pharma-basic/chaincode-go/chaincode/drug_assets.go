package chaincode

type Drug struct {
	ID           string `json:"id"`
	Name         string `json:"name"`
	MfgDate      string `json:"manufactureDate"`
	Manufacturer string `json:"manufacturer"`
	Owner        string `json:"owner"`
}

type Crp struct {
	Challenge string
	Response  string
	Assignee  string `json:"assignee"`
	Used      bool   `json:"used"`
}
type DrugCrps struct {
	Crps []Crp `json:"crps"`
}
