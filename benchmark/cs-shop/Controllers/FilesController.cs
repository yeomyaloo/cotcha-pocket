using Microsoft.AspNetCore.Mvc;

namespace CsShop.Controllers;

[ApiController]
public class FilesController(IWebHostEnvironment env) : ControllerBase
{
    [HttpGet("api/images")]
    public IActionResult Image([FromQuery] string file)
    {
        var imageDir = Path.Combine(env.ContentRootPath, "storage", "images");
        return PhysicalFile(Path.Combine(imageDir, file), "image/png");
    }

    [HttpGet("api/thumbs")]
    public IActionResult Thumb([FromQuery] string file)
    {
        var thumbDir = Path.Combine(env.ContentRootPath, "storage", "thumbs");
        return PhysicalFile(Path.Combine(thumbDir, Path.GetFileName(file)), "image/png");
    }
}
