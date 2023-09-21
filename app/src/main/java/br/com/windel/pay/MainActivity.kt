package br.com.windel.pay

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var editText: EditText
    private lateinit var buttonCancel: Button
    private lateinit var buttonFinish: Button
    private val decimalFormat: DecimalFormat = DecimalFormat("#,##0.00")
    private lateinit var lottie: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        editText = findViewById(R.id.editText)
        buttonFinish = findViewById(R.id.btnFinish);
        buttonCancel = findViewById(R.id.btnCancel)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && text != "-") {
                    editText.removeTextChangedListener(this)
                    val cleanString = text.replace("[^0-9]".toRegex(), "")
                    val parsed = cleanString.toDouble() / 100
                    val formatted = decimalFormat.format(parsed)
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                    editText.addTextChangedListener(this)
                }
            }
        })

        lottie = findViewById(R.id.lottieAnimationView)

        buttonFinish.setOnClickListener {
            lottie.visibility = View.VISIBLE
            lottie.playAnimation()
        }

        buttonCancel.setOnClickListener {
            lottie.visibility = View.INVISIBLE
            lottie.cancelAnimation()
        }
    }
}