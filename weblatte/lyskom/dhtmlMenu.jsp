<% if (lyskom != null && lyskom.getLoggedIn()) { %>
<!-- Context menu Script- � Dynamic Drive (www.dynamicdrive.com) Last updated: 01/08/22
     For full source code and Terms Of Use, visit http://www.dynamicdrive.com -->
<div id="ie5menu" class="skin0" onMouseover="highlightie5(event)" onMouseout="lowlightie5(event)" onClick="jumptoie5(event)" display:none>
<div class="menuitems" url="<%= basePath %>?listnews">Lista nyheter</div>
<div class="menuitems" url="<%= basePath %>composer.jsp" style="font-weight: bold">Skriv inl�gg</div>
<div class="menuitems" target="_top" url="<%= basePath %>frames.jsp">Ramvy</div>
<%  if (lyskom.getServer().equals("sno.pp.se")) { %>
<div class="menuitems" url="<%= basePath %>?uploadForm">Ladda upp bild</div>
<%  } %>
<div class="menuitems" url="<%= basePath %>?reviewMarked">Lista markerade texter</div>
<div class="menuitems" script="change_name_interactive(window, '<%= dqescJS(lookupName(lyskom, lyskom.getMyPerson().getNo())) %>');">Byt namn...</div>
<div class="menuitems" script="search_interactive(window);">S�k m�te/person...</div>
<div class="menuitems" url="<%= basePath %>prefs.jsp">Inst�llningar</div>
<hr>
<div class="menuitems" url="<%= basePath %>vilka/">Vilka �r inloggade?</div>
<%  if (lyskom.getServer().equals("sno.pp.se")) { %>
<div class="menuitems" url="<%= basePath %>bilder/">Vem �r vem i SnoppKOM?</div>
<%  } %>
<hr>
<div class="menuitems" url="<%= basePath %>?refresh" target="_top">Starta om</div>
<div class="menuitems" url="<%= basePath %>?suspend" target="_top">Pausa</div>
<div class="menuitems" url="<%= basePath %>?purgeOtherSessions">Logga ut mina andra sessioner</div>
<div class="menuitems" url="<%= basePath %>?logout" target="_top" style="font-weight: bold">Logga ut</div>
</div>

<div id="ctxmenu_conference" class="skin0" onMouseover="highlightie5(event)" onMouseout="lowlightie5(event)" onClick="jumptoie5(event)" display:none>
<div class="menuitems" url="<%= basePath %>?conference=%NO%">G� till</div>
<div class="menuitems" url="<%= basePath %>?reviewPresentation=%NO%">�terse presentation</div>
<div class="menuitems" url="<%= basePath %>?reviewFaq=%NO%">�terse FAQ</div>
<div class="menuitems" url="<%= basePath %>?changePresentation=%NO%">�ndra presentation</div>
<div class="menuitems" url="<%= basePath %>?sendTo=%NO%#sendMessage">Skicka meddelande</div>
<div class="menuitems" url="<%= basePath %>?joinNo=%NO%">Bli medlem</div>
<div class="menuitems" url="<%= basePath %>?leaveNo=%NO%">Uttr�da</div>
<div class="menuitems" script="endast_interactive(window, %NO%, '%NAME%');">Endast...</div>
	<div class="menuitems" url="<%= basePath %>frames.jsp?conference=%NO%" target="_top">L�s i ramvy</div>
</div>

<div id="ctxmenu_letterbox" class="skin0" onMouseover="highlightie5(event)" onMouseout="lowlightie5(event)" onClick="jumptoie5(event)" display:none>
<div class="menuitems" url="<%= basePath %>?reviewPresentation=%NO%">�terse presentation</div>
<div class="menuitems" url="<%= basePath %>?reviewFaq=%NO%">�terse FAQ</div>
<div class="menuitems" url="<%= basePath %>?changePresentation=%NO%">�ndra presentation</div>
<div class="menuitems" url="<%= basePath %>composer.jsp?recipientNo=%NO%&recipientNo=<%=lyskom.getMyPerson().getNo()%>">Skicka brev</div>
<div class="menuitems" url="<%= basePath %>?sendTo=%NO%#sendMessage">Skicka meddelande</div>
</div>

<div id="ctxmenu_text" class="skin0" onMouseover="highlightie5(event)" onMouseout="lowlightie5(event)" onClick="jumptoie5(event)" display:none>
<div class="menuitems" url="<%= basePath %>?delete=%NO%">Radera inl�gg</div>
<div class="menuitems" url="<%= basePath %>?mark=%NO%&text=%NO%">Markera</div>
<div class="menuitems" url="<%= basePath %>?unmark=%NO%&text=%NO%">Avmarkera</div>
<div class="menuitems" url="<%= basePath %>?privateReply=%NO%&text=%NO%">Personligt svar</div>
<div class="menuitems" script="add_recipient_interactive(window, %NO%, <%= TextStat.miscRecpt %>)">Addera mottagare</div>
<div class="menuitems" script="add_recipient_interactive(window, %NO%, <%= TextStat.miscCcRecpt %>)">Addera extra kopiemottagare</div>
<div class="menuitems" url="<%= basePath %>frames.jsp?reviewTree=%NO%" target="_top">L�s kommentarstr�d</div>
</div>
<script language="JavaScript1.2">
//set this variable to 1 if you wish the URLs of the highlighted menu to be displayed in the status bar
var display_url=0;
 
var ie5=document.all&&document.getElementById;
var ns6=document.getElementById&&!document.all;

var menuobj;
getMenuObj();
if (ie5||ns6){
  menuobj.style.display='';
  document.oncontextmenu=showmenuie5;
  document.onclick=hidemenuie5;
  //document.onclick=showhidemenu
}
</script>
<% } %>
