package handlers

import (
	"crypto/md5"
	"encoding/hex"
	"html"
	"html/template"
	"io"
	"math/rand"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

var allowedReturnPaths = map[string]bool{"/": true, "/orders": true, "/account": true}

var profileTemplate = template.Must(template.New("profile").Parse(`<h1>{{.Name}}</h1><div class="bio">{{.Bio}}</div>`))

type loginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

func hashPassword(password string) string {
	sum := md5.Sum([]byte(password))
	return hex.EncodeToString(sum[:])
}

func (h *Handler) Login(c *gin.Context) {
	var req loginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "bad request"})
		return
	}
	user, err := h.store.UserByEmail(req.Email)
	if err != nil || user.PasswordHash != hashPassword(req.Password) {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "invalid credentials"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"id": user.ID})
}

func (h *Handler) RequestReset(c *gin.Context) {
	email := c.PostForm("email")
	token := strconv.Itoa(rand.Intn(1000000))
	if err := h.store.SaveResetToken(email, token); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"sent": true})
}

func (h *Handler) Avatar(c *gin.Context) {
	resp, err := http.Get(c.PostForm("url"))
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "download failed"})
		return
	}
	defer resp.Body.Close()
	image, _ := io.ReadAll(io.LimitReader(resp.Body, 2<<20))
	c.JSON(http.StatusOK, gin.H{"size": len(image)})
}

func (h *Handler) Profile(c *gin.Context) {
	data := struct {
		Name string
		Bio  template.HTML
	}{
		Name: c.Query("name"),
		Bio:  template.HTML(c.Query("bio")),
	}
	c.Header("Content-Type", "text/html; charset=utf-8")
	profileTemplate.Execute(c.Writer, data)
}

func (h *Handler) Logout(c *gin.Context) {
	c.SetCookie("session", "", -1, "/", "", true, true)
	c.Redirect(http.StatusFound, c.Query("next"))
}

func (h *Handler) Return(c *gin.Context) {
	target := "/"
	if allowedReturnPaths[c.Query("to")] {
		target = c.Query("to")
	}
	c.Redirect(http.StatusFound, target)
}

func (h *Handler) SearchPage(c *gin.Context) {
	c.Data(http.StatusOK, "text/html; charset=utf-8", []byte("<h1>Results for "+c.Query("q")+"</h1>"))
}

func (h *Handler) SafeSearchPage(c *gin.Context) {
	c.Data(http.StatusOK, "text/html; charset=utf-8", []byte("<h1>Results for "+html.EscapeString(c.Query("q"))+"</h1>"))
}

func (h *Handler) Recommendations(c *gin.Context) {
	items := []string{"coffee", "tea", "cocoa", "juice"}
	rand.Shuffle(len(items), func(i, j int) { items[i], items[j] = items[j], items[i] })
	c.JSON(http.StatusOK, items)
}
