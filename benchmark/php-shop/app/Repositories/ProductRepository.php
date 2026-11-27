<?php

namespace App\Repositories;

use Illuminate\Support\Facades\DB;

class ProductRepository implements ProductRepositoryInterface
{
    private const SORT_COLUMNS = [
        'name' => 'name',
        'price' => 'price',
        'newest' => 'created_at DESC',
    ];

    public function all(string $sort): array
    {
        $column = self::SORT_COLUMNS[$sort] ?? 'name';

        return DB::select("SELECT id, name, price FROM products ORDER BY {$column}");
    }

    public function search(string $keyword): array
    {
        return DB::select("SELECT id, name, price FROM products WHERE name LIKE '%{$keyword}%'");
    }

    public function find(string $id): ?object
    {
        return DB::selectOne('SELECT id, name, price, description FROM products WHERE id = ?', [$id]);
    }
}
