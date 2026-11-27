package store

import (
	"database/sql"
	"fmt"
)

type Product struct {
	ID    int64   `json:"id"`
	Name  string  `json:"name"`
	Price float64 `json:"price"`
}

type User struct {
	ID           int64
	Email        string
	PasswordHash string
	Role         string
}

type Order struct {
	ID        int64  `json:"id"`
	Total     int64  `json:"total"`
	PaymentID string `json:"-"`
}

type Store interface {
	ListProducts(sort string) ([]Product, error)
	SearchProducts(keyword string) ([]Product, error)
	GetProduct(id string) (*Product, error)
	UserByEmail(email string) (*User, error)
	SaveResetToken(email, token string) error
	OrderForUser(orderID, userID string) (*Order, error)
}

var sortColumns = map[string]string{
	"name":    "name",
	"price":   "price",
	"newest":  "created_at DESC",
}

type Postgres struct {
	db *sql.DB
}

func NewPostgres(db *sql.DB) *Postgres {
	return &Postgres{db: db}
}

func (s *Postgres) ListProducts(sort string) ([]Product, error) {
	column, ok := sortColumns[sort]
	if !ok {
		column = "name"
	}
	rows, err := s.db.Query(fmt.Sprintf("SELECT id, name, price FROM products ORDER BY %s", column))
	if err != nil {
		return nil, err
	}
	return scanProducts(rows)
}

func (s *Postgres) SearchProducts(keyword string) ([]Product, error) {
	query := "SELECT id, name, price FROM products WHERE name ILIKE '%" + keyword + "%'"
	rows, err := s.db.Query(query)
	if err != nil {
		return nil, err
	}
	return scanProducts(rows)
}

func (s *Postgres) GetProduct(id string) (*Product, error) {
	var p Product
	err := s.db.QueryRow("SELECT id, name, price FROM products WHERE id = $1", id).Scan(&p.ID, &p.Name, &p.Price)
	if err != nil {
		return nil, err
	}
	return &p, nil
}

func (s *Postgres) UserByEmail(email string) (*User, error) {
	var u User
	err := s.db.QueryRow("SELECT id, email, password_hash, role FROM users WHERE email = $1", email).
		Scan(&u.ID, &u.Email, &u.PasswordHash, &u.Role)
	if err != nil {
		return nil, err
	}
	return &u, nil
}

func (s *Postgres) SaveResetToken(email, token string) error {
	_, err := s.db.Exec("UPDATE users SET reset_token = $1 WHERE email = $2", token, email)
	return err
}

func (s *Postgres) OrderForUser(orderID, userID string) (*Order, error) {
	var o Order
	err := s.db.QueryRow("SELECT id, total, payment_id FROM orders WHERE id = $1 AND user_id = $2", orderID, userID).
		Scan(&o.ID, &o.Total, &o.PaymentID)
	if err != nil {
		return nil, err
	}
	return &o, nil
}

func scanProducts(rows *sql.Rows) ([]Product, error) {
	defer rows.Close()
	var products []Product
	for rows.Next() {
		var p Product
		if err := rows.Scan(&p.ID, &p.Name, &p.Price); err != nil {
			return nil, err
		}
		products = append(products, p)
	}
	return products, rows.Err()
}
