package com.example.s87sun.jsketch

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.s87sun.jsketch.ui.Drawpad
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity(), Observer {
    private lateinit var model: MainModel

    private lateinit var undo: ImageButton
    private lateinit var redo: ImageButton
    private lateinit var erase: ImageButton
    private lateinit var circle: ImageButton
    private lateinit var rect: ImageButton
    private lateinit var line: ImageButton
    private lateinit var colorLayout: LinearLayout
    private val colorButtons = arrayListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pref = getPreferences(Context.MODE_PRIVATE)
        val data = pref.getString("data", null)
        model = if (data != null) MainModel(JSONObject(data)) else MainModel()

        model.addObserver(this)

        setContentView(R.layout.activity_main)
        findViewById<LinearLayout>(R.id.container).addView(Drawpad(this, model))
        val toolbar: LinearLayout = findViewById(R.id.toolbar)
        // select shape
        toolbar.findViewById<ImageButton>(R.id.selection_tool).setOnClickListener { model.moveTool() }
        undo = toolbar.findViewById(R.id.undo_tool)
        undo.setOnClickListener { model.undo() }
        redo = toolbar.findViewById(R.id.redo_tool)
        redo.setOnClickListener { model.redo() }
        erase = toolbar.findViewById(R.id.erase_tool)
        erase.setOnClickListener {
            model.removeShape(model.selectedShapeIndex!!)
            model.selectedShapeIndex = null
        }
        circle = toolbar.findViewById(R.id.draw_circle_tool)
        circle.setOnClickListener { model.drawTool(ShapeKind.CIRCLE) }
        rect = toolbar.findViewById(R.id.draw_square_tool)
        rect.setOnClickListener { model.drawTool(ShapeKind.RECT) }
        line = toolbar.findViewById(R.id.draw_line_tool)
        line.setOnClickListener { model.drawTool(ShapeKind.LINE) }

        colorLayout = findViewById(R.id.colors)
        for ((i_color, color) in model.colors.withIndex()) {
            val size = resources.getDimensionPixelSize(R.dimen.color_button_size)
            val layout = View.inflate(this, R.layout.color_button, null)
            layout.findViewById<Button>(R.id.color_button).apply {
                setBackgroundColor(color.toInt())
                setOnClickListener {
                    model.selectedColorIndex = i_color
                    val index = model.selectedShapeIndex
                    if (index != null) {
                        val s = model.shapes[index]
                        model.updateShape(index, Shape(s.bound, s.kind, i_color))
                    }
                }
            }
            colorLayout.addView(layout, size, size)
            colorButtons.add(layout)
        }

        update(null, null)
    }

    override fun update(o: Observable?, arg: Any?) {
        val selected = model.selectedShapeIndex != null
        undo.isEnabled = model.hasUndo
        redo.isEnabled = model.hasRedo
        erase.isEnabled = selected
        circle.isEnabled = !selected
        rect.isEnabled = !selected
        line.isEnabled = !selected
        for ((i, button) in colorButtons.withIndex()) {
            val color = if (i == model.selectedColorIndex) Color.GRAY else Color.TRANSPARENT
            button.setBackgroundColor(color)
        }
        colorLayout.invalidate()
    }

    override fun onPause() {
        super.onPause()
        val pref = getPreferences(Context.MODE_PRIVATE)
        pref.edit {
            putString("data", model.serialize().toString())
            commit()
        }
    }
}

