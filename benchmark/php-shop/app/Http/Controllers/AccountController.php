<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Http;

class AccountController extends Controller
{
    public function avatar(Request $request)
    {
        $response = Http::timeout(10)->get($request->input('url'));
        if ($response->failed()) {
            return response()->json(['error' => 'download failed'], 400);
        }
        DB::update('UPDATE users SET avatar = ? WHERE id = ?', [base64_encode($response->body()), $request->user()->id]);

        return response()->json(['saved' => true]);
    }

    public function restoreCart(Request $request)
    {
        $cart = unserialize(base64_decode((string) $request->input('cart_state')));

        return response()->json(['items' => is_array($cart) ? count($cart) : 0]);
    }

    public function importWishlist(Request $request)
    {
        $xml = simplexml_load_string($request->getContent(), 'SimpleXMLElement', LIBXML_NOENT | LIBXML_DTDLOAD);
        if ($xml === false) {
            return response()->json(['error' => 'invalid xml'], 400);
        }
        foreach ($xml->item as $item) {
            DB::insert('INSERT INTO wishlist (user_id, product_id) VALUES (?, ?)', [$request->user()->id, (string) $item['id']]);
        }

        return response()->json(['imported' => count($xml->item)]);
    }
}
