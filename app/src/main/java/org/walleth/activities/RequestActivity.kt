package org.walleth.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import com.github.salomonbrys.kodein.LazyKodein
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import kotlinx.android.synthetic.main.activity_request.*
import org.kethereum.erc67.toERC67String
import org.ligi.compat.HtmlCompat
import org.ligi.kaxt.doAfterEdit
import org.ligi.kaxt.setVisibility
import org.walleth.R
import org.walleth.data.networks.CurrentAddressProvider
import org.walleth.data.tokens.CurrentTokenProvider
import org.walleth.data.tokens.isETH
import org.walleth.functions.setQRCode
import java.math.BigDecimal

class RequestActivity : AppCompatActivity() {

    private lateinit var currentERC67String: String
    private val currentAddressProvider: CurrentAddressProvider by LazyKodein(appKodein).instance()
    private val currentTokenProvider: CurrentTokenProvider by LazyKodein(appKodein).instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_request)

        supportActionBar?.subtitle = getString(R.string.request_transaction_subtitle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        refreshQR()

        request_hint.text = HtmlCompat.fromHtml(getString(R.string.request_hint))
        request_hint.movementMethod = LinkMovementMethod()

        add_value_checkbox.setOnCheckedChangeListener { _, isChecked ->
            value_inputlayout.setVisibility(isChecked)
            refreshQR()
        }

        value_input_edittext.doAfterEdit {
            refreshQR()
        }
    }

    private fun refreshQR() {

        if (currentTokenProvider.currentToken.isETH()) {

            val relevantAddress = currentAddressProvider.getCurrent()
            currentERC67String = relevantAddress.toERC67String()

            if (add_value_checkbox.isChecked) {
                try {
                    currentERC67String = relevantAddress.toERC67String(BigDecimal(value_input_edittext.text.toString()))
                } catch (e: NumberFormatException) {
                }
            }
        } else {
            val relevantAddress = currentTokenProvider.currentToken.address
            currentERC67String = relevantAddress.toERC67String()
            if (add_value_checkbox.isChecked) {
                try {
                    currentERC67String = currentERC67String + "?function=transfer(address " +
                            currentAddressProvider.getCurrent().hex + ", uint " +
                            value_input_edittext.text.toString() + ")"
                } catch (e: NumberFormatException) {
                }
            }

        }

        receive_qrcode.setQRCode(currentERC67String)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_request, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_share -> {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, currentERC67String)
                type = "text/plain"
            }

            startActivity(sendIntent)
            true
        }
        R.id.menu_copy -> {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip = ClipData.newPlainText(getString(R.string.clipboard_copy_name), currentERC67String)
            Snackbar.make(receive_qrcode, R.string.copied_to_clipboard, Snackbar.LENGTH_LONG).show()
            true
        }
        android.R.id.home -> {
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
