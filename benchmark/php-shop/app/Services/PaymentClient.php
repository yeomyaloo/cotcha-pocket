<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;

class PaymentClient
{
    public function status(string $paymentId): array
    {
        $response = Http::get(config('services.payments.url') . '/v1/status/' . rawurlencode($paymentId));

        return $response->json() ?? [];
    }
}
