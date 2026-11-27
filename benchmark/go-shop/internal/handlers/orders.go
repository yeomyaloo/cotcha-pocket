package handlers

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

func (h *Handler) PaymentStatus(c *gin.Context) {
	order, err := h.store.OrderForUser(c.Param("id"), userID(c))
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "not found"})
		return
	}
	status, err := h.payments.Status(order.PaymentID)
	if err != nil {
		c.JSON(http.StatusBadGateway, gin.H{"error": "payment service unavailable"})
		return
	}
	c.JSON(http.StatusOK, status)
}
