using System.Security.Claims;
using CsShop.Data;
using CsShop.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace CsShop.Controllers;

[ApiController]
[Authorize]
[Route("api/orders")]
public class OrdersController(ShopDbContext db, PaymentClient payments) : ControllerBase
{
    [HttpGet("{id:int}/payment")]
    public async Task<IActionResult> PaymentStatus(int id)
    {
        var userId = int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)!);
        var order = await db.Orders.FirstOrDefaultAsync(o => o.Id == id && o.UserId == userId);
        if (order == null)
        {
            return NotFound();
        }
        return Ok(await payments.StatusAsync(order.PaymentId));
    }
}
