using System.Text.Encodings.Web;
using Microsoft.AspNetCore.Mvc;

namespace CsShop.Controllers;

[ApiController]
[Route("api")]
public class PagesController : ControllerBase
{
    private static readonly string[] Recommendations = ["coffee", "tea", "cocoa", "juice"];

    [HttpGet("search")]
    public IActionResult Search([FromQuery] string q)
    {
        return Content($"<h1>Results for {q}</h1>", "text/html");
    }

    [HttpGet("search/safe")]
    public IActionResult SafeSearch([FromQuery] string q)
    {
        return Content($"<h1>Results for {HtmlEncoder.Default.Encode(q)}</h1>", "text/html");
    }

    [HttpGet("recommendations")]
    public IActionResult Recommendation()
    {
        return Ok(new { pick = Recommendations[Random.Shared.Next(Recommendations.Length)] });
    }
}
