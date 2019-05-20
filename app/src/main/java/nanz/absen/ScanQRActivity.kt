package nanz.absen

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.jetbrains.anko.alert
import org.jetbrains.anko.intentFor

class ScanQRActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {
    private var mScannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)
    }

    override fun onResume() {
        super.onResume()
        mScannerView?.setResultHandler(this)
        mScannerView?.startCamera()
    }

    override fun onPause() {
        super.onPause()
        mScannerView?.stopCamera()
    }

    override fun handleResult(rawResult: Result?) {
//        alert(rawResult.toString(), "Hasil", {
//            positiveButton("Ok", {
//            })
//        }).show()

        setResult(Activity.RESULT_OK, intentFor<MainActivity>("id" to rawResult.toString()))
        finish()

        mScannerView?.resumeCameraPreview(this)
    }

}
