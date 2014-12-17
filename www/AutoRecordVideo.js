var cordova = require('cordova');

function AutoRecordVideo() {
}

AutoRecordVideo.prototype.record = function (successCallback, errorCallback, options) {
  cordova.exec(successCallback, errorCallback, "AutoRecordVideo", "record", [options]);
};

AutoRecordVideo.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.autorecordvideo = new AutoRecordVideo();
  return window.plugins.autorecordvideo;
};

cordova.addConstructor(AutoRecordVideo.install);
