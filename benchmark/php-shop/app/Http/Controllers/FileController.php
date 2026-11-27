<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class FileController extends Controller
{
    public function image(Request $request)
    {
        $contents = file_get_contents(storage_path('app/images/') . $request->query('file'));
        if ($contents === false) {
            abort(404);
        }

        return response()->make($contents, 200, ['Content-Type' => 'image/png']);
    }

    public function thumb(Request $request)
    {
        $name = basename((string) $request->query('file'));
        $contents = file_get_contents(storage_path('app/thumbs/') . $name);
        if ($contents === false) {
            abort(404);
        }

        return response()->make($contents, 200, ['Content-Type' => 'image/png']);
    }
}
