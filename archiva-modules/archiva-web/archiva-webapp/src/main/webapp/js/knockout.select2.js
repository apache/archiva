(function(factory) {
  if (typeof define === "function" && define.amd) {
    // AMD anonymous module
    define("knockout.select2",["jquery","knockout","utils","select2"], factory);
  } else {
    // No module loader (plain <script> tag) - put directly in global namespace
    factory(window.ko, jQuery);
  }
})(function ($,ko,utils,select2) {


    ko.bindingHandlers.select2 = {
      init: function(element, valueAccessor) {
        $.log("select2 binding#init");
        $(element).select2(valueAccessor());

        ko.utils.domNodeDisposal.addDisposeCallback(element, function() {
          $(element).select2('destroy');
        });
      },
      update: function(element) {
        $.log("select2 binding#update");
        $(element).trigger('change');
      }
    };



})