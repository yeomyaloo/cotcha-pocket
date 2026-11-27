using System.Security.Cryptography;
using System.Text.Json;
using CsShop.Data;
using Microsoft.AspNetCore.Mvc;

namespace CsShop.Controllers;

[ApiController]
[Route("api/products")]
public class ProductsController(IProductRepository products) : ControllerBase
{
    [HttpGet]
    public async Task<IActionResult> List([FromQuery] string? sort)
    {
        var items = await products.ListAsync(sort);
        var etag = Convert.ToHexString(SHA1.HashData(JsonSerializer.SerializeToUtf8Bytes(items)));
        Response.Headers.ETag = $"\"{etag}\"";
        return Ok(items);
    }

    [HttpGet("search")]
    public async Task<IActionResult> Search([FromQuery] string q)
    {
        return Ok(await products.SearchAsync(q));
    }

    [HttpGet("{id:int}")]
    public async Task<IActionResult> Get(int id)
    {
        var product = await products.FindAsync(id);
        return product == null ? NotFound() : Ok(product);
    }
}
