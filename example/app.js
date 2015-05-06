// This is a test harness for your module
// You should do something interesting in this harness
// to test out the module and to provide instructions
// to users on how to use it by example.


// open a single window
var win = Ti.UI.createWindow({
	backgroundColor:'white'
});
var label = Ti.UI.createLabel();
win.add(label);
win.open();

var sensors = require('com.sws.sensors.bt.ble');
sensors.registerListeners({
   statusCallback: function(e) {
      console.log('Status = " + e);
   },
   scanningCallback: function(e) {
      console.log('Scanning = " + e);
   },
   dataCallback: function(e) {
      console.log('Data = " + e);
   }
});

sensors.startScanning();

