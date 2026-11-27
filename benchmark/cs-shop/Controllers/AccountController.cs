using System.Xml;
using CsShop.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Newtonsoft.Json;

namespace CsShop.Controllers;

public record CartItem(int ProductId, int Quantity);

[ApiController]
[Authorize]
[Route("api/account")]
public class AccountController(IHttpClientFactory httpClientFactory, ShopDbContext db) : ControllerBase
{
    [HttpPost("avatar")]
    public async Task<IActionResult> Avatar([FromForm] string url)
    {
        var http = httpClientFactory.CreateClient();
        var bytes = await http.GetByteArrayAsync(url);
        return Ok(new { size = bytes.Length });
    }

    [HttpPost("cart")]
    public IActionResult RestoreCart([FromForm] string state)
    {
        var settings = new JsonSerializerSettings { TypeNameHandling = TypeNameHandling.Auto };
        var cart = JsonConvert.DeserializeObject<List<object>>(state, settings);
        return Ok(new { items = cart?.Count ?? 0 });
    }

    [HttpPost("import")]
    public async Task<IActionResult> ImportWishlist()
    {
        using var reader = new StreamReader(Request.Body);
        var xml = await reader.ReadToEndAsync();
        var doc = new XmlDocument { XmlResolver = new XmlUrlResolver() };
        doc.LoadXml(xml);
        var count = doc.SelectNodes("//item")?.Count ?? 0;
        await db.SaveChangesAsync();
        return Ok(new { imported = count });
    }
}
