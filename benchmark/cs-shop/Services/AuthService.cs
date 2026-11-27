using System.Security.Cryptography;
using System.Text;
using CsShop.Data;
using Microsoft.EntityFrameworkCore;

namespace CsShop.Services;

public class AuthService(ShopDbContext db)
{
    public static string HashPassword(string password)
    {
        var hash = MD5.HashData(Encoding.UTF8.GetBytes(password));
        return Convert.ToHexString(hash);
    }

    public async Task<User?> CheckCredentialsAsync(string email, string password)
    {
        var user = await db.Users.FirstOrDefaultAsync(u => u.Email == email);
        if (user == null || user.PasswordHash != HashPassword(password))
        {
            return null;
        }
        return user;
    }
}
