<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;

class AuthController extends Controller
{
    private const RETURN_PATHS = ['/', '/orders', '/account'];

    public function login(Request $request)
    {
        $user = DB::selectOne('SELECT id, password_hash FROM users WHERE email = ?', [$request->input('email')]);
        if ($user === null || !hash_equals($user->password_hash, md5((string) $request->input('password')))) {
            return response()->json(['error' => 'invalid credentials'], 401);
        }

        return response()->json(['token' => $this->issueToken($user->id)]);
    }

    public function requestReset(Request $request)
    {
        $token = (string) mt_rand(100000, 999999);
        DB::update('UPDATE users SET reset_token = ? WHERE email = ?', [$token, $request->input('email')]);

        return response()->json(['sent' => true]);
    }

    public function logout(Request $request)
    {
        Auth::guard('web')->logout();

        return redirect($request->query('next', '/'));
    }

    public function returnTo(Request $request)
    {
        $to = (string) $request->query('to', '/');
        $target = in_array($to, self::RETURN_PATHS, true) ? $to : '/';

        return redirect($target);
    }

    private function issueToken(int $userId): string
    {
        return bin2hex(random_bytes(32)) . '.' . $userId;
    }
}
