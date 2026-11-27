package handlers

import (
	"fmt"

	"example.com/goshop/internal/config"
	"example.com/goshop/internal/payments"
	"example.com/goshop/internal/store"
	"github.com/gin-gonic/gin"
)

type Handler struct {
	store    store.Store
	payments *payments.Client
	cfg      config.Config
}

func New(s store.Store, p *payments.Client, cfg config.Config) *Handler {
	return &Handler{store: s, payments: p, cfg: cfg}
}

func (h *Handler) Secret() string {
	return h.cfg.JWTSecret
}

func (h *Handler) RegisterPublic(g *gin.RouterGroup) {
	g.GET("/products", h.ListProducts)
	g.GET("/products/search", h.SearchProducts)
	g.GET("/products/:id", h.GetProduct)
	g.GET("/images", h.Image)
	g.GET("/thumbs", h.Thumb)
	g.POST("/auth/login", h.Login)
	g.POST("/auth/reset", h.RequestReset)
	g.GET("/auth/logout", h.Logout)
	g.GET("/auth/return", h.Return)
	g.GET("/search", h.SearchPage)
	g.GET("/search/safe", h.SafeSearchPage)
	g.GET("/recommendations", h.Recommendations)
}

func (h *Handler) RegisterAccount(g *gin.RouterGroup) {
	g.POST("/avatar", h.Avatar)
	g.GET("/profile", h.Profile)
	g.GET("/orders/:id/payment", h.PaymentStatus)
}

func (h *Handler) RegisterAdmin(g *gin.RouterGroup) {
	g.POST("/export", h.Export)
	g.GET("/report", h.Report)
	g.POST("/templates/preview", h.PreviewTemplate)
}

func userID(c *gin.Context) string {
	id, _ := c.Get("userID")
	return fmt.Sprint(id)
}
