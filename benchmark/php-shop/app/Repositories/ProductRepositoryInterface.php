<?php

namespace App\Repositories;

interface ProductRepositoryInterface
{
    public function all(string $sort): array;

    public function search(string $keyword): array;

    public function find(string $id): ?object;
}
