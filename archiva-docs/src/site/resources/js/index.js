$(document).ready(function(){
  $('#screenshots-carousel').carousel({
    interval: 2500
  })

  $('#carousel-prev' ).on("click", function(){
    $('#screenshots-carousel').carousel('prev');
  })

  $('#carousel-next' ).on("click", function(){
    $('#screenshots-carousel').carousel('next');
  })
  
 $("#openDialogRelease" ).on("click",function(){
   $('#dialogRelease').modal('show');
 })
   
 $("#openDialogPreview" ).on("click",function(){
    $('#dialogPreview').modal('show');
  })

  $("#carousel-main" ).addClass("features-preview");


});

