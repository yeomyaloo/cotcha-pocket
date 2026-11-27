package handlers

import (
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"net/http"
	"os"
	"path/filepath"

	"github.com/gin-gonic/gin"
)

func (h *Handler) ListProducts(c *gin.Context) {
	products, err := h.store.ListProducts(c.Query("sort"))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "query failed"})
		return
	}
	body, _ := json.Marshal(products)
	sum := sha1.Sum(body)
	c.Header("ETag", hex.EncodeToString(sum[:]))
	c.Data(http.StatusOK, "application/json", body)
}

func (h *Handler) SearchProducts(c *gin.Context) {
	products, err := h.store.SearchProducts(c.Query("q"))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "query failed"})
		return
	}
	c.JSON(http.StatusOK, products)
}

func (h *Handler) GetProduct(c *gin.Context) {
	product, err := h.store.GetProduct(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	c.JSON(http.StatusOK, product)
}

func (h *Handler) Image(c *gin.Context) {
	data, err := os.ReadFile(filepath.Join(h.cfg.ImageDir, c.Query("file")))
	if err != nil {
		c.Status(http.StatusNotFound)
		return
	}
	c.Data(http.StatusOK, "image/png", data)
}

func (h *Handler) Thumb(c *gin.Context) {
	name := filepath.Base(c.Query("file"))
	data, err := os.ReadFile(filepath.Join(h.cfg.ThumbDir, name))
	if err != nil {
		c.Status(http.StatusNotFound)
		return
	}
	c.Data(http.StatusOK, "image/png", data)
}
