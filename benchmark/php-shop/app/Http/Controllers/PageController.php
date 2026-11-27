<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class PageController extends Controller
{
    public function search(Request $request)
    {
        return response('<h1>Results for ' . $request->query('q') . '</h1>');
    }

    public function safeSearch(Request $request)
    {
        return response('<h1>Results for ' . e($request->query('q')) . '</h1>');
    }

    public function recommendations()
    {
        $items = ['coffee', 'tea', 'cocoa', 'juice'];
        shuffle($items);

        return response()->json($items);
    }
}
