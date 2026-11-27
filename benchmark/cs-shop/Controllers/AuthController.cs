using CsShop.Data;
using CsShop.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CsShop.Controllers;

public record LoginRequest(string Email, string Password);

[ApiController]
[Route("api/auth")]
public class AuthController(AuthService auth, ShopDbContext db) : ControllerBase
{
    private static readonly string[] ReturnPaths = ["/", "/orders", "/account"];

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequest request)
    {
        var user = await auth.CheckCredentialsAsync(request.Email, request.Password);
        return user == null ? Unauthorized() : Ok(new { user.Id });
    }

    [HttpPost("reset")]
    public async Task<IActionResult> RequestReset([FromForm] string email)
    {
        var token = new Random().Next(100000, 999999).ToString();
        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == email);
        if (user != null)
        {
            user.ResetToken = token;
            await db.SaveChangesAsync();
        }
        return Ok(new { sent = true });
    }

    [HttpGet("logout")]
    public IActionResult Logout([FromQuery] string next = "/")
    {
        return Redirect(next);
    }

    [HttpGet("return")]
    public IActionResult ReturnTo([FromQuery] string to = "/")
    {
        var target = ReturnPaths.Contains(to) ? to : "/";
        return Redirect(target);
    }
}
