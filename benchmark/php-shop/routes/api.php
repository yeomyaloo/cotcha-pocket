<?php

use App\Http\Controllers\AccountController;
use App\Http\Controllers\AdminController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\FileController;
use App\Http\Controllers\OrderController;
use App\Http\Controllers\PageController;
use App\Http\Controllers\ProductController;
use Illuminate\Support\Facades\Route;

Route::get('/products', [ProductController::class, 'index']);
Route::get('/products/search', [ProductController::class, 'search']);
Route::get('/products/{id}', [ProductController::class, 'show']);

Route::get('/images', [FileController::class, 'image']);
Route::get('/thumbs', [FileController::class, 'thumb']);

Route::post('/auth/login', [AuthController::class, 'login']);
Route::post('/auth/reset', [AuthController::class, 'requestReset']);
Route::get('/auth/logout', [AuthController::class, 'logout']);
Route::get('/auth/return', [AuthController::class, 'returnTo']);

Route::get('/search', [PageController::class, 'search']);
Route::get('/search/safe', [PageController::class, 'safeSearch']);
Route::get('/recommendations', [PageController::class, 'recommendations']);

Route::middleware('auth:sanctum')->group(function () {
    Route::post('/account/avatar', [AccountController::class, 'avatar']);
    Route::post('/account/cart', [AccountController::class, 'restoreCart']);
    Route::post('/account/import', [AccountController::class, 'importWishlist']);
    Route::get('/account/orders/{id}/payment', [OrderController::class, 'paymentStatus']);
});

Route::prefix('admin')->middleware(['auth:sanctum', 'can:admin'])->group(function () {
    Route::post('/export', [AdminController::class, 'export']);
    Route::get('/report', [AdminController::class, 'report']);
    Route::post('/pricing/preview', [AdminController::class, 'previewPrice']);
});
