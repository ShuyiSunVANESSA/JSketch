package com.example.s87sun.jsketch.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.minus
import com.example.s87sun.jsketch.MainModel
import com.example.s87sun.jsketch.Shape
import com.example.s87sun.jsketch.ShapeKind
import com.example.s87sun.jsketch.Tool
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Drawpad(ctx: Context, private val model: MainModel) : View(ctx), Observer {
    var prev: PointF? = null
    val high = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeWidth = 10f
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    init {
        model.addObserver(this)
    }

    private fun drawShape(canvas: Canvas, shape: Shape, highlight: Boolean) {
        val paint = Paint().apply {
            strokeWidth = 15f
            color = model.colors[shape.color].toInt()
        }
        val sb = shape.bound
        val w = measuredWidth
        val h = measuredHeight
        val b = RectF(sb.left * w, sb.top * h, sb.right * w, sb.bottom * h)
        when (shape.kind) {
            ShapeKind.CIRCLE -> {
                val radius = min(abs(b.width()), abs(b.height())) / 2
                canvas.drawCircle(b.centerX(), b.centerY(), radius, paint)
                if (highlight)
                    canvas.drawCircle(b.centerX(), b.centerY(), radius, high)
            }
            ShapeKind.RECT -> {
                canvas.drawRect(b, paint)
                if (highlight)
                    canvas.drawRect(b, high)
            }
            ShapeKind.LINE -> {
                val x = b.left
                val y = b.top
                canvas.drawLine(x, y, b.right, b.bottom, paint)
                if (highlight)
                    canvas.drawLine(x, y, b.right, b.bottom, high)
            }
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas!!
        for ((i, shape) in model.shapes.withIndex()) {
            drawShape(canvas, shape, i == model.selectedShapeIndex)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event!!
        val x = event.getX(event.actionIndex) / measuredWidth
        val y = event.getY(event.actionIndex) / measuredHeight
        val pos = PointF(x, y)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> when (model.tool) {
                Tool.MOVE -> {
                    var index: Int? = null
                    for ((i, shape) in model.shapes.withIndex().reversed()) {
                        if (contains(shape.bound, pos)) {
                            index = i
                            break
                        }
                    }
                    model.selectedShapeIndex = if (index != null) {
                        val shape = model.removeShape(index)
                        model.addShape(shape)
                        model.shapes.size - 1
                    } else {
                        null
                    }
                }
                Tool.DRAW -> {
                    val bound = RectF(x, y, x, y)
                    val shape = Shape(bound, model.shapeKind, model.selectedColorIndex)
                    model.addShape(shape)
                    model.selectedShapeIndex = model.shapes.size - 1
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val index = model.selectedShapeIndex
                if (index != null) {
                    val shape = model.shapes[index]
                    val b = shape.bound
                    val b2 = when (model.tool) {
                        Tool.MOVE -> {
                            val d = pos - prev!!
                            RectF(b.left + d.x, b.top + d.y, b.right + d.x, b.bottom + d.y)
                        }
                        Tool.DRAW -> RectF(b.left, b.top, x, y)
                    }
                    model.updateShapeMut(index, Shape(b2, shape.kind, shape.color))
                }
            }
            MotionEvent.ACTION_UP -> {
                prev = null
                if (model.tool == Tool.DRAW)
                    model.selectedShapeIndex = null
            }
        }
        prev = pos
        invalidate()
        return true
    }

    private fun contains(r: RectF, p: PointF): Boolean {
        return p.x >= min(r.left, r.right) && p.x <= max(r.left, r.right) &&
                p.y >= min(r.top, r.bottom) && p.y <= max(r.top, r.bottom)
    }
}