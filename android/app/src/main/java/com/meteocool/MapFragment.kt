package com.meteocool

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.meteocool.security.Validator
import java.util.*


class MapFragment() : Fragment(){

    interface WebViewClientListener{
        fun receivedWebViewError()
    }

    companion object {
        const val MAP_URL = "https://meteocool.com/?mobile=android2"
        const val DOC_URL = "https://meteocool.com/documentation.html"
    }
    private lateinit var listener : WebViewClientListener
    private lateinit var mWebView : WebView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mWebView = view.findViewById(R.id.webView)

        val webSettings = mWebView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.setGeolocationEnabled(true)

        val locale = when(Locale.getDefault().displayLanguage.compareTo(Locale.GERMAN.displayLanguage)){
            0 -> "&lang=de"
            else -> "&lang=en"
        }
        mWebView.loadUrl(MAP_URL +locale)
        // Force links and redirects to open in the WebView instead of in a browser
        mWebView.webViewClient = MyWebViewClient(listener)
        return view
    }



    override fun onResume() {
        super.onResume()
        if(Validator.isLocationPermissionGranted(activity!!.applicationContext)) {
            val string = "window.manualTileUpdateFn(true);"
            mWebView.post {
                run {
                    mWebView.evaluateJavascript(string) { _ ->
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as WebViewClientListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                " must implement WebViewClientListener"))
        }
    }

    class MyWebViewClient(private val listener : WebViewClientListener) : WebViewClient(){

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            listener.receivedWebViewError()
        }
    }
}
