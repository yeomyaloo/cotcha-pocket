using System.Diagnostics;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.CodeAnalysis.CSharp.Scripting;

namespace CsShop.Controllers;

[ApiController]
[Authorize(Roles = "Admin")]
[Route("api/admin")]
public class AdminController : ControllerBase
{
    private static readonly string[] ReportTables = ["orders", "products"];

    [HttpPost("export")]
    public async Task<IActionResult> Export([FromQuery] string dir = "daily")
    {
        using var process = Process.Start("/bin/sh", $"-c \"tar czf /tmp/export.tgz /var/shop/exports/{dir}\"");
        await process.WaitForExitAsync();
        return process.ExitCode == 0 ? PhysicalFile("/tmp/export.tgz", "application/gzip") : StatusCode(500);
    }

    [HttpGet("report")]
    public async Task<IActionResult> Report([FromQuery] string table)
    {
        if (!ReportTables.Contains(table))
        {
            return BadRequest(new { error = "unknown table" });
        }
        var startInfo = new ProcessStartInfo("pg_dump", $"--data-only --table={table} shop") { RedirectStandardOutput = true };
        using var process = Process.Start(startInfo)!;
        var output = await process.StandardOutput.ReadToEndAsync();
        return Content(output, "text/plain");
    }

    [HttpPost("pricing/preview")]
    public async Task<IActionResult> PreviewPrice([FromForm] string formula)
    {
        var result = await CSharpScript.EvaluateAsync<decimal>(formula);
        return Ok(new { result });
    }
}
