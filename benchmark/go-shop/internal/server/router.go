package server

import (
	"net/http"

	"example.com/goshop/internal/handlers"
	"example.com/goshop/internal/middleware"
	"github.com/gin-gonic/gin"
)

func NewRouter(h *handlers.Handler) *gin.Engine {
	r := gin.Default()

	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok"})
	})

	api := r.Group("/api")
	h.RegisterPublic(api)

	account := api.Group("/account", middleware.RequireAuth(h.Secret()))
	h.RegisterAccount(account)

	admin := api.Group("/admin", middleware.RequireAuth(h.Secret()), middleware.RequireAdmin())
	h.RegisterAdmin(admin)

	return r
}
