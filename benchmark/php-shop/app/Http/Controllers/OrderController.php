<?php

namespace App\Http\Controllers;

use App\Services\PaymentClient;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;

class OrderController extends Controller
{
    public function __construct(private PaymentClient $payments)
    {
    }

    public function paymentStatus(Request $request, string $id)
    {
        $order = DB::selectOne('SELECT payment_id FROM orders WHERE id = ? AND user_id = ?', [$id, $request->user()->id]);
        if ($order === null) {
            abort(404);
        }

        return response()->json($this->payments->status($order->payment_id));
    }
}
