<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class AdminController extends Controller
{
    private const REPORT_TABLES = ['orders', 'products'];

    public function export(Request $request)
    {
        $dir = $request->input('dir', 'daily');
        exec("tar czf /tmp/export.tgz /var/shop/exports/{$dir}", $output, $status);
        if ($status !== 0) {
            return response()->json(['error' => 'export failed'], 500);
        }

        return response()->download('/tmp/export.tgz');
    }

    public function report(Request $request)
    {
        $table = (string) $request->query('table');
        if (!in_array($table, self::REPORT_TABLES, true)) {
            return response()->json(['error' => 'unknown table'], 400);
        }
        exec('pg_dump --data-only --table=' . escapeshellarg($table) . ' shop', $lines);

        return response()->make(implode("\n", $lines), 200, ['Content-Type' => 'text/plain']);
    }

    public function previewPrice(Request $request)
    {
        $price = (float) $request->input('price');
        $result = eval('return ' . $request->input('formula') . ';');

        return response()->json(['price' => $price, 'result' => $result]);
    }
}
