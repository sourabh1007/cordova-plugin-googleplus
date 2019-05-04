#!/usr/bin/env node
'use strict';

var fs = require('fs');

var getPreferenceValue = function(config, name) {
    var value = config.match(new RegExp('name="' + name + '" value="(.*?)"', "i"));
    if(value && value[1]) {
        return value[1]
    } else {
        return null
    }
};

var WEB_APPLICATION_CLIENT_ID = '';
if(process.argv.join("|").indexOf("WEB_APPLICATION_CLIENT_ID=") > -1) {
    WEB_APPLICATION_CLIENT_ID = process.argv.join("|").match(/WEB_APPLICATION_CLIENT_ID=(.*?)(\||$)/)[1];
} else {
    var config = fs.readFileSync("config.xml").toString();
    WEB_APPLICATION_CLIENT_ID = getPreferenceValue(config, "WEB_APPLICATION_CLIENT_ID");
}

var GOOGLE_SCOPES = '';
if(process.argv.join("|").indexOf("GOOGLE_SCOPES=") > -1) {
    GOOGLE_SCOPES = process.argv.join("|").match(/GOOGLE_SCOPES=(.*?)(\||$)/)[1];
} else {
    var config = fs.readFileSync("config.xml").toString();
    GOOGLE_SCOPES = getPreferenceValue(config, "GOOGLE_SCOPES");
}

var GOOGLE_API_KEY = '';
if(process.argv.join("|").indexOf("GOOGLE_API_KEY=") > -1) {
    GOOGLE_API_KEY = process.argv.join("|").match(/GOOGLE_API_KEY=(.*?)(\||$)/)[1];
} else {
    var config = fs.readFileSync("config.xml").toString();
    GOOGLE_API_KEY = getPreferenceValue(config, "GOOGLE_API_KEY");
}

var files = [
    "platforms/browser/www/plugins/cordova-plugin-googleplus/src/browser/GooglePlusProxy.js",
    "platforms/browser/platform_www/plugins/cordova-plugin-googleplus/src/browser/GooglePlusProxy.js"
];

for(var i=0; i<files.length; i++) {
    try {
        var contents = fs.readFileSync(files[i]).toString();
        fs.writeFileSync(files[i], contents.replace(/WEB_APPLICATION_CLIENT_ID/g, WEB_APPLICATION_CLIENT_ID));
        contents = fs.readFileSync(files[i]).toString();
        fs.writeFileSync(files[i], contents.replace(/GOOGLE_SCOPES/g, GOOGLE_SCOPES));
        contents = fs.readFileSync(files[i]).toString();
        fs.writeFileSync(files[i], contents.replace(/GOOGLE_API_KEY/g, GOOGLE_API_KEY));
    } catch(err) {}
}
