var cordova = require('cordova');

function AutoRecordVideo() {
}

// Override this method (after deviceready) to set the location where you want the iPad popup arrow to appear.
// If not overridden with different values, the popup is not used. Example:
//
//   window.plugins.autorecordvideo.iPadPopupCoordinates = function() {
//     return "100,100,200,300";
//   };
AutoRecordVideo.prototype.iPadPopupCoordinates = function () {
  // left,top,width,height
  return "-1,-1,-1,-1";
};

AutoRecordVideo.prototype.available = function (callback) {
  cordova.exec(function (avail) {
    callback(avail ? true : false);
  }, null, "AutoRecordVideo", "available", []);
};

AutoRecordVideo.prototype.share = function (message, subject, fileOrFileArray, url, successCallback, errorCallback) {
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "share"), "AutoRecordVideo", "share", [message, subject, this._asArray(fileOrFileArray), url]);
};

AutoRecordVideo.prototype.shareViaTwitter = function (message, file /* multiple not allowed by twitter */, url, successCallback, errorCallback) {
  var fileArray = this._asArray(file);
  var ecb = this._getErrorCallback(errorCallback, "shareViaTwitter");
  if (fileArray.length > 1) {
    ecb("shareViaTwitter supports max one file");
  } else {
    cordova.exec(successCallback, ecb, "AutoRecordVideo", "shareViaTwitter", [message, null, fileArray, url]);
  }
};

AutoRecordVideo.prototype.shareViaFacebook = function (message, fileOrFileArray, url, successCallback, errorCallback) {
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "shareViaFacebook"), "AutoRecordVideo", "shareViaFacebook", [message, null, this._asArray(fileOrFileArray), url]);
};

AutoRecordVideo.prototype.shareViaFacebookWithPasteMessageHint = function (message, fileOrFileArray, url, pasteMessageHint, successCallback, errorCallback) {
  pasteMessageHint = pasteMessageHint || "If you like you can paste a message from your clipboard";
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "shareViaFacebookWithPasteMessageHint"), "AutoRecordVideo", "shareViaFacebookWithPasteMessageHint", [message, null, this._asArray(fileOrFileArray), url, pasteMessageHint]);
};

AutoRecordVideo.prototype.shareViaWhatsApp = function (message, fileOrFileArray, url, successCallback, errorCallback) {
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "shareViaWhatsApp"), "AutoRecordVideo", "shareViaWhatsApp", [message, null, this._asArray(fileOrFileArray), url]);
};

AutoRecordVideo.prototype.shareViaSMS = function (options, phonenumbers, successCallback, errorCallback) {
  var opts = options;
  if (typeof options == "string") {
    opts = {"message":options}; // for backward compatibility as the options param used to be the message
  }
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "shareViaSMS"), "AutoRecordVideo", "shareViaSMS", [opts, phonenumbers]);
};

AutoRecordVideo.prototype.shareViaEmail = function (message, subject, toArray, ccArray, bccArray, fileOrFileArray, successCallback, errorCallback) {
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "shareViaEmail"), "AutoRecordVideo", "shareViaEmail", [message, subject, this._asArray(toArray), this._asArray(ccArray), this._asArray(bccArray), this._asArray(fileOrFileArray)]);
};

AutoRecordVideo.prototype.canShareVia = function (via, message, subject, fileOrFileArray, url, successCallback, errorCallback) {
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "canShareVia"), "AutoRecordVideo", "canShareVia", [message, subject, this._asArray(fileOrFileArray), url, via]);
};

AutoRecordVideo.prototype.canShareViaEmail = function (successCallback, errorCallback) {
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "canShareViaEmail"), "AutoRecordVideo", "canShareViaEmail", []);
};

AutoRecordVideo.prototype.shareVia = function (via, message, subject, fileOrFileArray, url, successCallback, errorCallback) {
  cordova.exec(successCallback, this._getErrorCallback(errorCallback, "shareVia"), "AutoRecordVideo", "shareVia", [message, subject, this._asArray(fileOrFileArray), url, via]);
};

AutoRecordVideo.prototype._asArray = function (param) {
  if (param == null) {
    param = [];
  } else if (typeof param === 'string') {
    param = new Array(param);
  }
  return param;
};

AutoRecordVideo.prototype._getErrorCallback = function (ecb, functionName) {
  if (typeof ecb === 'function') {
    return ecb;
  } else {
    return function (result) {
      console.log("The injected error callback of '" + functionName + "' received: " + JSON.stringify(result));
    }
  }
};

AutoRecordVideo.install = function () {
  if (!window.plugins) {
    window.plugins = {};
  }

  window.plugins.autorecordvideo = new AutoRecordVideo();
  return window.plugins.autorecordvideo;
};

cordova.addConstructor(AutoRecordVideo.install);
