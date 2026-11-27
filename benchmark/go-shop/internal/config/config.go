package config

import "os"

type Config struct {
	DatabaseURL string
	JWTSecret   string
	PaymentsURL string
	ImageDir    string
	ThumbDir    string
}

func Load() Config {
	return Config{
		DatabaseURL: os.Getenv("DATABASE_URL"),
		JWTSecret:   os.Getenv("JWT_SECRET"),
		PaymentsURL: getenv("PAYMENTS_URL", "https://payments.example.com"),
		ImageDir:    getenv("IMAGE_DIR", "/srv/shop/images"),
		ThumbDir:    getenv("THUMB_DIR", "/srv/shop/thumbs"),
	}
}

func getenv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
