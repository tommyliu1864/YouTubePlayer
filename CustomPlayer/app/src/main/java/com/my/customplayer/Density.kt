package com.my.customplayer

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Window
import android.view.WindowManager

var statusBarHeight = 0


// 屏幕的宽度
val screenWidth
    get() = MyApplication.context.resources.displayMetrics.widthPixels

fun getScreenHeight(context: Context): Int {
    return if (isAllScreenDevice(context)) { // 全面屏
        getScreenRealHeight(context);
    } else {
        getScreenHeight2(context);
    }
}

// 屏幕的高度
private fun getScreenHeight2(context: Context): Int {
    return context.resources.displayMetrics.heightPixels
}

/**
 * Get screen Real height
 * The second type is to read the defaultDisplay parameter in windowManager
 */
@Volatile
private var sRealSizes = arrayOfNulls<Point>(2)
private fun getScreenRealHeight(context: Context): Int {
    var orientation = context.resources?.configuration?.orientation
    orientation = if (orientation == 1) 0 else 1
    if (sRealSizes[orientation] == null) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getRealSize(point)
        sRealSizes[orientation] = point
    }
    return sRealSizes[orientation]?.y ?: getScreenRealHeight(context)
}

/**
 * 根据手机的分辨率将dp转成为px。
 */
fun dp2px(dp: Float): Int {
    val scale = MyApplication.context.resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

fun getStatusBarHeight(window: Window): Int {
    val localRect = Rect()
    window.decorView.getWindowVisibleDisplayFrame(localRect)
    var mStatusBarHeight = localRect.top
    if (0 == mStatusBarHeight) {
        try {
            val localClass = Class.forName("com.android.internal.R\$dimen")
            val localObject = localClass.newInstance()
            val i5 =
                localClass.getField("status_bar_height")[localObject].toString().toInt()
            mStatusBarHeight = MyApplication.context.resources.getDimensionPixelSize(i5)
        } catch (var6: ClassNotFoundException) {
            var6.printStackTrace()
        } catch (var7: IllegalAccessException) {
            var7.printStackTrace()
        } catch (var8: InstantiationException) {
            var8.printStackTrace()
        } catch (var9: NumberFormatException) {
            var9.printStackTrace()
        } catch (var10: IllegalArgumentException) {
            var10.printStackTrace()
        } catch (var11: SecurityException) {
            var11.printStackTrace()
        } catch (var12: NoSuchFieldException) {
            var12.printStackTrace()
        }
    }
    if (0 == mStatusBarHeight) {
        val resourceId: Int =
            MyApplication.context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            mStatusBarHeight = MyApplication.context.resources.getDimensionPixelSize(resourceId)
        }
    }
    return mStatusBarHeight
}

// 是否是全面屏
fun isAllScreenDevice(context: Context): Boolean {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = windowManager.defaultDisplay
    val point = Point()
    display.getRealSize(point)
    val width: Float
    val height: Float
    if (point.x < point.y) {
        width = point.x.toFloat()
        height = point.y.toFloat()
    } else {
        width = point.y.toFloat()
        height = point.x.toFloat()
    }
    if (height / width >= 1.97f) {
        return true
    }
    return false
}