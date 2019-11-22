package com.example.s87sun.jsketch

import android.graphics.Rect
import android.graphics.RectF
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

enum class ShapeKind {
    CIRCLE, LINE, RECT;
}

fun rect(json: JSONObject): RectF =
    RectF(json.getDouble("left").toFloat(), json.getDouble("top").toFloat(),
        json.getDouble("right").toFloat(), json.getDouble("bottom").toFloat())

fun <T> fromJsonArray(arr: JSONArray, fn: (JSONObject) -> T): MutableList<T> {
    return (0 until arr.length()).map { i ->
        fn(arr.getJSONObject(i))
    }.toMutableList()
}

data class Shape(val bound: RectF, val kind: ShapeKind, val color: Int) {
    constructor(json: JSONObject) : this(
        rect(json.getJSONObject("bound")),
        ShapeKind.valueOf(json.getString("kind")),
        json.getInt("color")
    )
}

enum class Tool {
    DRAW,
    MOVE;
}

data class State(val shapes: MutableList<Shape>)

class MainModel() : Observable() {
    val colors = arrayListOf(0xff000000, 0xfffc7b03, 0xff45ba16, 0xff1b69a6, 0xffb5bd22, 0xff6b050a)
    private var states: MutableList<State> = arrayListOf(State(arrayListOf()))
    private var currentStateIndex: Int = 0

    var tool: Tool = Tool.MOVE
        private set
    var shapeKind: ShapeKind = ShapeKind.CIRCLE
        private set
    var selectedShapeIndex: Int? = null
        set(value) {
            field = value
            if (value != null && value < shapes.size) {
                selectedColorIndex = shapes[value].color
            }
            setChanged()
            notifyObservers()
        }
    var selectedColorIndex: Int = 0
        set(value) {
            field = value
            setChanged()
            notifyObservers()
        }

    val shapes: List<Shape>
        get() = states[currentStateIndex].shapes
    val hasUndo: Boolean
        get() = currentStateIndex > 0
    val hasRedo: Boolean
        get() = currentStateIndex < states.size - 1

    fun undo() {
        assert(hasUndo)
        currentStateIndex -= 1
        updateSelection()
        setChanged()
        notifyObservers()
    }

    fun redo() {
        assert(hasRedo)
        currentStateIndex += 1
        updateSelection()
        setChanged()
        notifyObservers()
    }

    fun addShape(shape: Shape) {
        newState().shapes.add(shape)
        setChanged()
        notifyObservers()
    }

    fun removeShape(index: Int): Shape {
        val shape = newState().shapes.removeAt(index)
        setChanged()
        notifyObservers()
        return shape
    }

    fun updateShape(index: Int, shape: Shape) {
        newState().shapes[index] = shape
        setChanged()
        notifyObservers()
    }

    fun updateShapeMut(index: Int, shape: Shape) {
        states[currentStateIndex].shapes[index] = shape
        setChanged()
        notifyObservers()
    }

    fun moveTool() {
        tool = Tool.MOVE
        setChanged()
        notifyObservers()
    }

    fun drawTool(kind: ShapeKind) {
        tool = Tool.DRAW
        shapeKind = kind
        setChanged()
        notifyObservers()
    }

    private fun updateSelection() {
        if (selectedShapeIndex ?: 0 >= shapes.size)
            selectedShapeIndex = null
    }

    private fun newState(): State {
        while (states.size > currentStateIndex + 1)
            states.removeAt(states.size - 1)
        val newState = State(shapes.toMutableList())
        states.add(newState)
        currentStateIndex += 1
        println(serialize())
        return newState
    }

    fun serialize(): JSONObject = JSONObject(mapOf(
        "states" to JSONArray(states.map { state -> JSONObject(mapOf(
            "shapes" to JSONArray(state.shapes.map { shape -> JSONObject(mapOf(
                "bound" to JSONObject(mapOf(
                    "top" to shape.bound.top,
                    "left" to shape.bound.left,
                    "bottom" to shape.bound.bottom,
                    "right" to shape.bound.right
                )),
                "kind" to shape.kind.name,
                "color" to shape.color
            ))})
        ))}),
        "currentStateIndex" to currentStateIndex,
        "tool" to tool.name,
        "shapeKind" to shapeKind.name,
        "selectedShapeIndex" to selectedShapeIndex,
        "selectedColorIndex" to selectedColorIndex
    ))

    constructor(json: JSONObject) : this() {
        states = fromJsonArray(json.getJSONArray("states")) { state ->
            State(fromJsonArray(state.getJSONArray("shapes")) { shape ->
                Shape(shape)
            })
        }
        currentStateIndex = json.getInt("currentStateIndex")
        tool = Tool.valueOf(json.getString("tool"))
        shapeKind = ShapeKind.valueOf(json.getString("shapeKind"))
        val si = json.optInt("selectedShapeIndex", -1)
        selectedShapeIndex = if (si < 0) null else si
        selectedColorIndex = json.getInt("selectedColorIndex")
    }
}
