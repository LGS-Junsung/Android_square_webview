package io.codegreen.squarewebview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
//import com.google.android.gms.tasks.OnCompleteListener
//import com.google.firebase.messaging.FirebaseMessaging
import java.net.URISyntaxException


import android.webkit.WebView
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var webViewLayout: FrameLayout
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)
        webViewLayout = findViewById(R.id.webview_frame);

//        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
//            val cookieManager = CookieManager.getInstance()
//            cookieManager.setAcceptCookie(true)
//            cookieManager.setAcceptThirdPartyCookies(webView, true)
//        }else{
//            //lollipop 이하 버전에서는 수동으로 쿠키 싱크 var context;
//            CookieSyncManager.createInstance(context)
//
//        }
//
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            CookieSyncManager.getInstance().sync()
//        } else {
//            //쿠키 수동 싱크 (메모리 -> 로컬저장)
//            //그밖에 싱크는 webview 에서 자동
//            CookieManager.getInstance().flush()
//
//        }

        webView.settings.run {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
        }

        // fileChooser로 선택된 파일 처리하는부분
        val getFileResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
                ar: ActivityResult ->
            val intent: Intent? = ar.data
            val result = if (intent == null || ar.resultCode != RESULT_OK) null else arrayOf(Uri.parse(intent.dataString))
            mUploadMessage!!.onReceiveValue(result);
            mUploadMessage = null;
        }

        webView.webChromeClient = object: WebChromeClient() {

            /// ---------- 팝업 열기 ----------
            /// - 카카오 JavaScript SDK의 로그인 기능은 popup을 이용합니다.
            /// - window.open() 호출 시 별도 팝업 webview가 생성되어야 합니다.
            ///
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {

                // 웹뷰 만들기
                var childWebView = WebView(view.context)
                Log.d("TAG", "웹뷰 만들기")
                // 부모 웹뷰와 동일하게 웹뷰 설정
                childWebView.run {
                    settings.run {
                        javaScriptEnabled = true
                        javaScriptCanOpenWindowsAutomatically = true
                        setSupportMultipleWindows(true)
                    }
                    layoutParams = view.layoutParams
                    webViewClient = view.webViewClient
                    webChromeClient = view.webChromeClient
                }

                // 화면에 추가하기
                webViewLayout.addView(childWebView)
                // TODO: 화면 추가 이외에 onBackPressed() 와 같이
                //       사용자의 내비게이션 액션 처리를 위해
                //       별도 웹뷰 관리를 권장함
                //   ex) childWebViewList.add(childWebView)

                // 웹뷰 간 연동
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = childWebView
                resultMsg.sendToTarget()

                return true
            }

            /// ---------- 팝업 닫기 ----------
            /// - window.close()가 호출되면 앞에서 생성한 팝업 webview를 닫아야 합니다.
            ///
            override fun onCloseWindow(window: WebView) {
                super.onCloseWindow(window)

                // 화면에서 제거하기
                webViewLayout.removeView(window)
                // TODO: 화면 제거 이외에 onBackPressed() 와 같이
                //       사용자의 내비게이션 액션 처리를 위해
                //       별도 웹뷰 array 관리를 권장함
                //   ex) childWebViewList.remove(childWebView)
            }


            // input:file 처리
            override fun onShowFileChooser(mWebView:WebView,
                                           filePathCallback:ValueCallback<Array<Uri>>,
                                           fileChooserParams:FileChooserParams):Boolean {
                if (mUploadMessage != null) {
                    mUploadMessage!!.onReceiveValue(null)
                    mUploadMessage = null
                }
                mUploadMessage = filePathCallback
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"
                val intent = Intent(Intent.ACTION_CHOOSER)
                intent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                intent.putExtra(Intent.EXTRA_TITLE, "File Chooser")
                try {
                    getFileResultLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    mUploadMessage = null
                    Toast.makeText(getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show()
                    return false
                }
                return true
            }

        }






        webView.webViewClient = object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                Log.d("TAG url", request.url.toString())
                Log.d("TAG scheme", request.url.scheme.toString())
                if (request.url.scheme == "https") {
                    //webView.loadUrl(request.url.toString())
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().flush()
                } else {
                    CookieSyncManager.getInstance().sync()
                }

                if (request.url.scheme == "intent") {
                    try {
                        Log.d("TAG scheme", intent.getPackage().toString())
                        val intent = Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)
                        // 실행 가능한 앱이 있으면 앱 실행
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                            Log.d("TAG", "ACTIVITY: ${intent.`package`}")
                            return true
                        }

                        // Fallback URL이 있으면 현재 웹뷰에 로딩
                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                        if (fallbackUrl != null) {
                            view.loadUrl(fallbackUrl)
                            Log.d("TAG FALLBACK", "FALLBACK: $fallbackUrl")
                            return true
                        }

                        Log.e("TAG", "Could not parse anythings")

                    } catch (e: URISyntaxException) {
                        Log.e("TAG", "Invalid intent request", e)
                    }
                }

                // 나머지 서비스 로직 구현


                Log.d("TAG", "return false")
                return false
            }
        }

        webView.loadUrl("https://square.codegreen.io")



    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }


}
