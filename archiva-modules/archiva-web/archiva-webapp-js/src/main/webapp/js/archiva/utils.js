$.log = (function(message) {
  if (typeof window.console != 'undefined' && typeof window.console.log != 'undefined') {
    console.log(message);
  } else {
    // do nothing no console
  }
});

$.urlParam = function(name){
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    if (results) {
      return results[1] || 0;
    }
    return null;
}