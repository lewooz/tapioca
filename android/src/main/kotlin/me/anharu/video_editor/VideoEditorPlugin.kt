package me.anharu.video_editor

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.daasuu.mp4compose.composer.Mp4Composer
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.json.JSONObject
import java.io.File


/** VideoEditorPlugin */
class VideoEditorPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler, PluginRegistry.RequestPermissionsResultListener, ActivityAware {
    private var _context: Context? = null
    var activity: Activity? = null
    private var methodChannel: MethodChannel? = null
    private val myPermissionCode = 34264
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        onAttachedToEngine(flutterPluginBinding.binaryMessenger, flutterPluginBinding.applicationContext)
    }

    private fun onAttachedToEngine(messenger: BinaryMessenger, context: Context) {
        methodChannel = MethodChannel(messenger, "video_editor")
        eventChannel = EventChannel(messenger,"progressStream")
        methodChannel?.setMethodCallHandler(this)
        _context = context
        eventChannel.setStreamHandler(this)

    }



    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        if (call.method == "getPlatformVersion") {
            result.success("Android ${android.os.Build.VERSION.RELEASE}")
        } else if (call.method == "writeVideofile") {

            val getActivity = activity ?: return
            checkPermission(getActivity)

            val srcFilePath: String = call.argument("srcFilePath") ?: run {
                result.error("src_file_path_not_found", "the src file path is not found.", null)
                return
            }
            val destFilePath: String = call.argument("destFilePath") ?: run {
                result.error("dest_file_path_not_found", "the dest file path is not found.", null)
                return
            }
            val processing: HashMap<String, HashMap<String, Any>> = call.argument("processing")
                    ?: run {
                        result.error("processing_data_not_found", "the processing is not found.", null)
                        return
                    }

            //Mp4 composer yaratırken source ve destination burada veriyoruz.
            val generator = VideoGeneratorService(Mp4Composer(srcFilePath, destFilePath))
            generator.writeVideofile(processing, result, getActivity, eventSink)
        } else if (call.method == "getMediaInfo"){
            val srcFilePath: String = call.argument("srcFilePath") ?: run {
                result.error("src_file_path_not_found", "the src file path is not found.", null)
                return
            }
            result.success(getMediaInfoJson(srcFilePath).toString())
        }else{
            result.notImplemented()
        }
    }

    private fun getMediaInfoJson(path: String): JSONObject {
        val file = File(path)
        val retriever = MediaMetadataRetriever()

        retriever.setDataSource(_context, Uri.fromFile(file))

        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
        val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR) ?: ""
        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val aspectRatio = widthStr?.toDouble()?.div(heightStr?.toDouble()!!)
        val duration = java.lang.Long.parseLong(durationStr!!)
        val width = java.lang.Long.parseLong(widthStr!!)
        val height = java.lang.Long.parseLong(heightStr!!)
        val filesize = file.length()

        retriever.release()

        val json = JSONObject()

        json.put("path", path)
        json.put("title", title)
        json.put("author", author)
        json.put("width", width)
        json.put("height", height)
        json.put("duration", duration)
        json.put("filesize", filesize)
        json.put("aspectratio",aspectRatio)

        return json
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onDetachedFromActivity() {
        methodChannel!!.setMethodCallHandler(null)
        methodChannel = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?,
                                            grantResults: IntArray?): Boolean {
        when (requestCode) {
            myPermissionCode -> {
                // Only return true if handling the requestCode
                return true
            }
        }
        return false
    }

    // Invoked either from the permission result callback or permission check
    private fun completeInitialization() {

    }

    private fun checkPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), myPermissionCode)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        _context = null
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }
}
