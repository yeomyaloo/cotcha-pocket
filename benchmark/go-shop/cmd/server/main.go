package main

import (
	"database/sql"
	"log"
	"os"

	"example.com/goshop/internal/config"
	"example.com/goshop/internal/handlers"
	"example.com/goshop/internal/payments"
	"example.com/goshop/internal/server"
	"example.com/goshop/internal/store"
	_ "github.com/lib/pq"
)

func main() {
	cfg := config.Load()
	db, err := sql.Open("postgres", cfg.DatabaseURL)
	if err != nil {
		log.Fatal(err)
	}
	defer db.Close()

	h := handlers.New(store.NewPostgres(db), payments.NewClient(cfg.PaymentsURL), cfg)
	router := server.NewRouter(h)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	log.Fatal(router.Run(":" + port))
}
