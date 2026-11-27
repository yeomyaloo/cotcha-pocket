package payments

import (
	"crypto/tls"
	"encoding/json"
	"net/http"
	"net/url"
	"time"
)

type Status struct {
	State string `json:"state"`
}

type Client struct {
	baseURL string
	http    *http.Client
}

func NewClient(baseURL string) *Client {
	transport := &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
	}
	return &Client{
		baseURL: baseURL,
		http:    &http.Client{Transport: transport, Timeout: 10 * time.Second},
	}
}

func (c *Client) Status(paymentID string) (*Status, error) {
	resp, err := c.http.Get(c.baseURL + "/v1/status/" + url.PathEscape(paymentID))
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	var status Status
	if err := json.NewDecoder(resp.Body).Decode(&status); err != nil {
		return nil, err
	}
	return &status, nil
}
