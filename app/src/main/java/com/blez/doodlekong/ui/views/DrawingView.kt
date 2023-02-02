package com.blez.doodlekong.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.blez.doodlekong.utils.Constants
import java.lang.Math.abs
import java.util.Stack

class DrawingView @JvmOverloads constructor(context: Context,attrs : AttributeSet?=null) :View(context, attrs) {
    private var viewWidth : Int ?= null
    private var viewHeight : Int ?= null
    private var bitmap : Bitmap?=null
    private var canvas : Canvas?=null
    private var curY : Float?=null
    private var curX : Float?= null
    var smoothness = 5
    var isDrawing = false

    private var paint  = Paint(Paint.DITHER_FLAG).apply {
        isDither = true
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = Constants.DEFAULT_PAINT_THICKNESS

    }

    private var path = Path()

    private var paths = Stack<PathData>()
    private var pathDataChangedListener :  ((Stack<PathData>)->Unit) ?= null

    fun setOnPathDataChangedListener(listener : ((Stack<PathData>)-> Unit)){
        pathDataChangedListener = listener

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w
        viewHeight = h
        bitmap = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val initialColor = paint.color
        val initialThickness = paint.strokeWidth
        for (pathData in paths)
        {
            paint.apply {
                color = pathData.color
                strokeWidth = pathData.thickness
            }
            canvas?.drawPath(pathData.path,paint)
        }
        paint.apply {
            color = initialColor
            strokeWidth = initialThickness
        }
        canvas?.drawPath(path,paint)
    }

    private fun startedTouch(x : Float,y: Float)
    {
        path.reset()
        path.moveTo(x, y)
        curX = x
        curY = y
        invalidate()
    }
    private fun movedTouch(toX : Float,toY : Float){
        val dx = abs(toX - (curX?: return))
        val dy = abs(toY -(curY?:return))
        if (dx >= smoothness || dy>= smoothness){


        }
    }

    override fun setOnTouchListener(l: OnTouchListener?) {
        super.setOnTouchListener(l)
    }

    data class PathData(val path : Path, val color : Int, val thickness : Float)




}