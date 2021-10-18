import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:tapioca/src/model/media_info.dart';

class VideoEditor {
  static const MethodChannel _channel =
  const MethodChannel('video_editor');

  static const EventChannel _progressStream =
  EventChannel('progressStream');

  static Stream<double>? _onProgressUpdated;

  static Stream<double> get onProgressUpdated {
    _onProgressUpdated ??= _progressStream
        .receiveBroadcastStream()
        .distinct()
        .map<double>((dynamic result) => result != null ? result : 0);
    return _onProgressUpdated!;
  }

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future writeVideofile(String srcFilePath, String destFilePath, Map<String,Map<String, dynamic>> processing) async {
    await _channel.invokeMethod('writeVideofile',<String, dynamic> { 'srcFilePath': srcFilePath, 'destFilePath': destFilePath, 'processing': processing });
  }

  static Future<MediaInfo> getMediaInfo(String srcFilePath)async{
    var jsonString = await _channel.invokeMethod<String>('getMediaInfo',<String, String> { 'srcFilePath': srcFilePath,});
    final jsonMap = json.decode(jsonString!);
    return MediaInfo.fromJson(jsonMap);
  }

}
