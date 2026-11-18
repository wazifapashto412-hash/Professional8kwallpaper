package com.infinity.wallpapers.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun friendlyError(e: Throwable): String = when (e) {
    is UnknownHostException, is ConnectException, is SocketTimeoutException ->
        "No internet connection. Check your network and try again."
    is FirebaseFirestoreException ->
        if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) "No internet connection. Check your network and try again."
        else "Could not load wallpapers from the server (${e.code.name.lowercase()})."
    is IOException -> "Network problem. Please try again."
    else -> "Something went wrong. Please try again."
}

fun isOnline(ctx: Context): Boolean {
    val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return false
    val n = cm.activeNetwork ?: return false
    val c = cm.getNetworkCapabilities(n) ?: return false
    return c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun formatCount(n: Long): String = when {
    n >= 1_000_000 -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}
