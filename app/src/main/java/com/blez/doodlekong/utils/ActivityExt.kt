package com.blez.doodlekong.utils

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity

fun Activity.hideKeyboard(root : View){
    val windowToken = root.windowToken
    val imm =   getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE)  as InputMethodManager // input Method Manager
    windowToken?.let {
        imm.hideSoftInputFromWindow(it,0)
    } ?: return
    }



