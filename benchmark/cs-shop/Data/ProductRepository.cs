using Microsoft.EntityFrameworkCore;

namespace CsShop.Data;

public interface IProductRepository
{
    Task<List<Product>> ListAsync(string? sort);
    Task<List<Product>> SearchAsync(string keyword);
    Task<Product?> FindAsync(int id);
}

public class ProductRepository : IProductRepository
{
    private static readonly Dictionary<string, string> SortColumns = new()
    {
        ["name"] = "Name",
        ["price"] = "Price",
        ["newest"] = "Id DESC",
    };

    private readonly ShopDbContext _db;

    public ProductRepository(ShopDbContext db)
    {
        _db = db;
    }

    public Task<List<Product>> ListAsync(string? sort)
    {
        var column = sort != null && SortColumns.TryGetValue(sort, out var value) ? value : "Name";
        return _db.Products.FromSqlRaw($"SELECT * FROM Products ORDER BY {column}").ToListAsync();
    }

    public Task<List<Product>> SearchAsync(string keyword)
    {
        return _db.Products.FromSqlRaw($"SELECT * FROM Products WHERE Name LIKE '%{keyword}%'").ToListAsync();
    }

    public Task<Product?> FindAsync(int id)
    {
        return _db.Products.FromSqlInterpolated($"SELECT * FROM Products WHERE Id = {id}").FirstOrDefaultAsync();
    }
}
