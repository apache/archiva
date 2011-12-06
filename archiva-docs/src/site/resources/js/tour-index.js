$(document).ready(function() {
  $("a#single_image").fancybox({
    'transitionIn'	:	'elastic',
    'transitionOut'	:	'elastic',
    'speedIn'		:	600,
    'speedOut'		:	200,
    'overlayShow'	:	true
  });
    $("a.gallery_image").fancybox({
      'transitionIn'	:	'elastic',
      'transitionOut'	:	'elastic',
      'speedIn'		:	600,
      'speedOut'		:	200,
      'overlayShow'	:	true,
      'titlePosition': 'inside'
    });
});