package com.hyd.lidarcontrol.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    // fixed containing circle
    private var outerCenterX = 0.0f
    private var outerCenterY = 0.0f
    private var outerRadius = 0.0f
    // moving inner circle
    private var innerCenterX = 0.0f
    private var innerCenterY = 0.0f
    private var innerRadius = 0.0f
    // event listener for joystick changes
    lateinit var onChange : OnJoystickChange

    init {
        // define event listener for touches
        val listener = OnTouchListener(function = { view, motionEvent ->
            view.performClick()
            // user drags inner circle
            if (motionEvent.action == MotionEvent.ACTION_MOVE) {
                calculateNewInnerCenter(motionEvent.x, motionEvent.y)
            }
            // put the inner circle back in the middle on drag-leave
            else if (motionEvent.action == MotionEvent.ACTION_UP) {
                innerCenterX = outerCenterX
                innerCenterY = outerCenterY
            }
            // redraw the view
            invalidate()
            // calculate proportional size on ailerone and elivator
            val ailerone =  (innerCenterX - outerCenterX) / outerRadius
            val elivator = -(innerCenterY - outerCenterY) / outerRadius
            // invoke property changed callback
            onChange.invoke(ailerone, elivator)
            true
        })
        setOnTouchListener(listener)
    }

    private fun calculateNewInnerCenter(newX : Float, newY : Float) {
        // calculate the distance between the new point and the original center
        val distance = calculateDistance(newX, newY)
        // if its a valid point within the circle
        if (distance < outerRadius) {
            innerCenterX = newX
            innerCenterY = newY
        }
        else { // use the parallel triangles identity
            // calculate the maximal point of the inner circle in this direction
            val x =  newX - outerCenterX
            val y =  newY - outerCenterY
            // calculate the edge ratio
            val ratio = outerRadius / distance
            // calculate the edge length of the inner triangle
            val a = y * ratio
            val b = x * ratio
            innerCenterX = outerCenterX + b
            innerCenterY = outerCenterY + a
        }
    }

    // calculate euclidean distance between (x,y) to outerCenter
    private fun calculateDistance(x : Float, y : Float) : Float {
        // calculate the distance
        val dx = (x - outerCenterX).pow(2)
        val dy = (y - outerCenterY).pow(2)
        return sqrt(dx + dy)
    }
    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create( "", Typeface.BOLD)
    }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        color = Color.LTGRAY
        typeface = Typeface.create( "", Typeface.BOLD)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        outerCenterX = (width / 2).toFloat()
        outerCenterY = (height / 2).toFloat()
        innerCenterX = outerCenterX
        innerCenterY = outerCenterY
        // Calculate the radius from the smaller of the width and height.
        outerRadius = (min(width, height) / 3.0 * 0.8).toFloat()
        innerRadius = outerRadius / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // draw outer circle
        canvas.drawCircle(outerCenterX, outerCenterY, outerRadius, outerPaint)
        // draw inner circle
        canvas.drawCircle(innerCenterX, innerCenterY, innerRadius, innerPaint)
    }
}