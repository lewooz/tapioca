import Flutter
import UIKit
import AVFoundation

public class SwiftVideoEditorPlugin: NSObject, FlutterPlugin, FlutterStreamHandler {
    
    private let avController = AvController()
    private var eventSink : FlutterEventSink? = nil
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "video_editor", binaryMessenger: registrar.messenger())
        let instance = SwiftVideoEditorPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let eventChannel = FlutterEventChannel(name: "progressStream", binaryMessenger: registrar.messenger())
        eventChannel.setStreamHandler(instance.self)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
        guard let args = call.arguments as? [String: Any] else {
            result(FlutterError(code: "arguments_not_found",
                                message: "the arguments is not found.",
                                details: nil))
            return
        }
        
        switch call.method {
        case "writeVideofile":
            let video = VideoGeneratorService(eventSink: eventSink)
            
            guard let srcName = args["srcFilePath"] as? String else {
                result(FlutterError(code: "src_file_path_not_found",
                                    message: "the src file path sr is not found.",
                                    details: nil))
                return
            }
            guard let destName = args["destFilePath"] as? String else {
                result(FlutterError(code: "dest_file_path_not_found",
                                    message: "the dest file path is not found.",
                                    details: nil))
                return
            }
            guard let processing = args["processing"] as?  [String: [String: Any]] else {
                result(FlutterError(code: "processing_data_not_found",
                                    message: "the processing is not found.",
                                    details: nil))
                return
            }
            video.writeVideofile(srcPath: srcName, destPath: destName, processing: processing,eventSink: eventSink, result: result)
        case "getMediaInfo":
            guard let srcName = args["srcFilePath"] as? String else {
                result(FlutterError(code: "src_file_path_not_found",
                                    message: "the src file path sr is not found.",
                                    details: nil))
                return
            }
            let json = getMediaInfo(path: srcName)
            let string = Utility.keyValueToJson(json)
            
            result(string)
            
        default:
            result("iOS d" + UIDevice.current.systemVersion)
        }
    }
    
    private func getMediaInfo(path:String) -> [String : Any?]{
        let url = Utility.getPathUrl(path)
        let asset = avController.getVideoAsset(url)
        guard let track = avController.getTrack(asset) else { return [:] }
        
        let playerItem = AVPlayerItem(url: url)
        let metadataAsset = playerItem.asset
        
        
        let title = avController.getMetaDataByTag(metadataAsset,key: "title")
        let author = avController.getMetaDataByTag(metadataAsset,key: "author")
        
        let duration = asset.duration.seconds * 1000
        let filesize = track.totalSampleDataLength
        
        let size = track.naturalSize.applying(track.preferredTransform)
        
        let aspectRatio = size.width / size.height
        
        let width = abs(size.width)
        let height = abs(size.height)
        
        let dictionary = [
            "path":Utility.excludeFileProtocol(path),
            "title":title,
            "author":author,
            "width":width,
            "height":height,
            "duration":duration,
            "filesize":filesize,
            "aspectratio":aspectRatio,
        ] as [String : Any?]
        
        return dictionary
        
    }
    
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = events
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        eventSink = nil
        return nil
    }
}
