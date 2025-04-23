package com.example.civicvoice.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.civicvoice.network.AppUser
import com.google.gson.Gson

object UserSession {
    private const val PREFS_NAME = "CivicVoicePrefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER = "user"
    private var sharedPrefs: SharedPreferences? = null
    private var currentUser: AppUser? = null
    private var token: String? = null

    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        token = sharedPrefs?.getString(KEY_TOKEN, null)
        val userJson = sharedPrefs?.getString(KEY_USER, null)
        currentUser = if (userJson != null) {
            try {
                Gson().fromJson(userJson, AppUser::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    fun setUser(user: AppUser, token: String) {
        currentUser = user
        this.token = token
        sharedPrefs?.edit()?.apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER, Gson().toJson(user))
            apply()
        }
    }

    fun updateUser(user: AppUser) {
        currentUser = user
        sharedPrefs?.edit()?.apply {
            putString(KEY_USER, Gson().toJson(user))
            apply()
        }
    }

    fun getCurrentUser(): AppUser? = currentUser

    fun getToken(): String? = token

    fun isValid(): Boolean = currentUser != null && token != null && !currentUser!!.isAnonymous

    fun logout() {
        clear()
    }

    fun clear() {
        currentUser = null
        token = null
        sharedPrefs?.edit()?.clear()?.apply()
    }
}