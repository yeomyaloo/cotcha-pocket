package handlers

import (
	"html/template"
	"net/http"
	"os/exec"

	"github.com/gin-gonic/gin"
)

var reportTables = map[string]bool{"orders": true, "products": true}

func (h *Handler) Export(c *gin.Context) {
	dir := c.DefaultQuery("dir", "daily")
	cmd := exec.Command("sh", "-c", "tar czf /tmp/export.tgz /var/shop/exports/"+dir)
	if err := cmd.Run(); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "export failed"})
		return
	}
	c.File("/tmp/export.tgz")
}

func (h *Handler) Report(c *gin.Context) {
	table := c.Query("table")
	if !reportTables[table] {
		c.JSON(http.StatusBadRequest, gin.H{"error": "unknown table"})
		return
	}
	out, err := exec.Command("pg_dump", "--data-only", "--table", table, "shop").Output()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "report failed"})
		return
	}
	c.Data(http.StatusOK, "text/plain", out)
}

func (h *Handler) PreviewTemplate(c *gin.Context) {
	tmpl, err := template.New("preview").Parse(c.PostForm("template"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid template"})
		return
	}
	c.Header("Content-Type", "text/html; charset=utf-8")
	tmpl.Execute(c.Writer, gin.H{"shop": "Go Shop", "config": h.cfg})
}
