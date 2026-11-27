<?php

namespace App\Http\Controllers;

use App\Repositories\ProductRepositoryInterface;
use Illuminate\Http\Request;

class ProductController extends Controller
{
    public function __construct(private ProductRepositoryInterface $products)
    {
    }

    public function index(Request $request)
    {
        $products = $this->products->all((string) $request->query('sort', 'name'));
        $etag = sha1(json_encode($products));

        return response()->json($products)->header('ETag', $etag);
    }

    public function search(Request $request)
    {
        return response()->json($this->products->search((string) $request->query('q', '')));
    }

    public function show(string $id)
    {
        $product = $this->products->find($id);
        if ($product === null) {
            abort(404);
        }

        return response()->json($product);
    }
}
