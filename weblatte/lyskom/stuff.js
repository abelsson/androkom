// -*- Mode: c -*-
// $Id: stuff.js,v 1.4 2005/01/27 22:52:20 pajp Exp $
alert("hej poop.");
function context_in(no, isLetterBox, isText, name) {
  document.ctxNo = no;
  document.ctxIsLetterBox = isLetterBox;
  document.ctxIsText = isText;
  document.ctxName = name;
  document.inContext = true;
  getMenuObj();
}

function context_out() {
  document.inContext = false;
  getMenuObj();
}

function endast_interactive(win, confNo, name, url) {
  name = unescape(name);
  var count = win.prompt("Hur många olästa vill du ha i \"" + name + "\"?");
  if (count == null) return;
  document.location.href=url+"?endast=" + escape(count) + "&endastConferenceName=" +
    escape(name);
}

function search_interactive(win, url) {
  var str = win.prompt("Ange sökvillkor (lämna blankt för att lista alla möten och personer):", "");
  if (str == null) return;
  document.location.href=url+"?lookup=" + escape(str);
}

function review_pres_interactive(win, def, url) {
  var name = win.prompt("Vilket möte eller vilken person vill du se presentationen för?");
  if (name == null) return;
  document.location.href=url+"?reviewPresentation=" + escape(name);
}

function change_name_interactive(win, def, url) {
  var name = win.prompt("Vilket namn vill du byta?", def);
  if (name == null) {
    return;
  }
  var newName = win.prompt("Vilket namn vill du byta till?", name);
  if (newName == null) {
    alert("Avbruten.");
    return;
  }
  document.location.href = url + "?changeName=" + escape(name) + "&newName=" + 
    escape(newName);
}

function _test_context(confNo, isLetterbox) {
   //alert(confNo + " is a " + (isLetterbox ? "person" : "conference"));
   window.open("context.jsp?confNo=" + confNo, "ctx", "chrome");
}

function getMenuObj() {
  if (ie5||ns6) {
    if (typeof(document.inContext)=="undefined" || document.inContext == false) {
      menuobj = document.getElementById("ie5menu");
    } else if (document.ctxIsLetterBox == true) {
      menuobj = document.getElementById("ctxmenu_letterbox");
    } else if (document.ctxIsText == true) {
      menuobj = document.getElementById("ctxmenu_text");
    } else {
      menuobj = document.getElementById("ctxmenu_conference");
    }
  }
}

function showhidemenu(e) {
    getMenuObj();
    if (menuobj.style.visibility = "hidden") {
	showmenuie5(e);
    } else {
	hidemenuie5(e);
    }
}


function showmenuie5(e){
  getMenuObj();
  //Find out how close the mouse is to the corner of the window
  var rightedge=ie5? document.body.clientWidth-event.clientX : window.innerWidth-e.clientX;
  var bottomedge=ie5? document.body.clientHeight-event.clientY : window.innerHeight-e.clientY;
  
  //if the horizontal distance isn't enough to accomodate the width of the context menu
  if (rightedge<menuobj.offsetWidth) {
    //move the horizontal position of the menu to the left by it's width
    menuobj.style.left=ie5? document.body.scrollLeft+event.clientX-menuobj.offsetWidth : window.pageXOffset+e.clientX-menuobj.offsetWidth;
  } else {
    //position the horizontal position of the menu where the mouse was clicked
    menuobj.style.left=ie5? document.body.scrollLeft+event.clientX : window.pageXOffset+e.clientX;
  }

  //same concept with the vertical position
  if (bottomedge<menuobj.offsetHeight)
    menuobj.style.top=ie5? document.body.scrollTop+event.clientY-menuobj.offsetHeight : window.pageYOffset+e.clientY-menuobj.offsetHeight;
  else
    menuobj.style.top=ie5? document.body.scrollTop+event.clientY : window.pageYOffset+e.clientY;

  menuobj.style.visibility="visible";
  return false
}

function hidemenuie5(e){
  /*
  var menus = [ "ie5menu", "ctxmenu_conference", "ctxmenu_text", "ctxmenu_letterbox" ];
  for (var menu in menus) {
    document.getElementById(menu).style.visibility="hidden";
  }
  */
  menuobj.style.visibility="hidden";
}

function highlightie5(e){
  var firingobj=ie5? event.srcElement : e.target;
  if (firingobj.className=="menuitems"||ns6&&firingobj.parentNode.className=="menuitems"){
    if (ns6&&firingobj.parentNode.className=="menuitems") firingobj=firingobj.parentNode; //up one node
    firingobj.style.backgroundColor="highlight";
    firingobj.style.color="white";
    if (display_url==1)
      window.status=event.srcElement.url;
  }
}

function lowlightie5(e){
  var firingobj=ie5? event.srcElement : e.target;
  if (firingobj.className=="menuitems"||ns6&&firingobj.parentNode.className=="menuitems"){
    if (ns6&&firingobj.parentNode.className=="menuitems") firingobj=firingobj.parentNode; //up one node
    firingobj.style.backgroundColor="";
    firingobj.style.color="black";
    window.status='';
  }
}

function ctxVars(str) {
  var s = str;
  s = s.replace("%NO%", document.ctxNo);
  s = s.replace("%NAME%", escape(document.ctxName));
  return s;
}

function jumptoie5(e){
  var firingobj=ie5? event.srcElement : e.target;
  if (firingobj.className=="menuitems"||ns6&&firingobj.parentNode.className=="menuitems"){
    if (ns6&&firingobj.parentNode.className=="menuitems") firingobj=firingobj.parentNode;
    
    var script = firingobj.getAttribute("script");
    if (typeof(script) != "undefined" && 
	script != null && script != "undefined") {
      eval(ctxVars(script));
    }
    var urlAttr = firingobj.getAttribute("url");
    var _url = ctxVars(urlAttr);
    
    if (firingobj.getAttribute("target"))
      window.open(_url,firingobj.getAttribute("target"));
    else
      window.location=_url;
  }
}


