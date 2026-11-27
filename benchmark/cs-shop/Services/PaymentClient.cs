namespace CsShop.Services;

public record PaymentStatus(string State);

public class PaymentClient(HttpClient http, IConfiguration configuration)
{
    private readonly string _baseUrl = configuration["Payments:BaseUrl"] ?? "https://payments.example.com";

    public Task<PaymentStatus?> StatusAsync(string paymentId)
    {
        return http.GetFromJsonAsync<PaymentStatus>($"{_baseUrl}/v1/status/{Uri.EscapeDataString(paymentId)}");
    }
}
