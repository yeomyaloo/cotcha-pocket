using Microsoft.EntityFrameworkCore;

namespace CsShop.Data;

public class Product
{
    public int Id { get; set; }
    public string Name { get; set; } = "";
    public decimal Price { get; set; }
}

public class User
{
    public int Id { get; set; }
    public string Email { get; set; } = "";
    public string PasswordHash { get; set; } = "";
    public string? ResetToken { get; set; }
    public string Role { get; set; } = "customer";
}

public class Order
{
    public int Id { get; set; }
    public int UserId { get; set; }
    public string PaymentId { get; set; } = "";
}

public class ShopDbContext(DbContextOptions<ShopDbContext> options) : DbContext(options)
{
    public DbSet<Product> Products => Set<Product>();
    public DbSet<User> Users => Set<User>();
    public DbSet<Order> Orders => Set<Order>();
}
