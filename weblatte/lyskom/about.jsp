<%@ page pageEncoding='iso-8859-1' contentType='text/html;charset=utf-8'%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Weblatte - hj�lp och information</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>
  <body class="about">
    <h2>Weblatte - en kort introduktion</h2>
    <p>
      Weblatte �r en LysKOM-klient f�r dig som l�tt vill kunna l�sa
      och skriva i LysKOM-system via din vanliga webbl�sare. LysKOM �r
      ett konferenssystem, och mer om detta kan du l�sa p� <a
      href="http://www.lysator.liu.se/lyskom/">LysKOMs
      hemsida</a>.<br/>

      Weblatte �r fortfarande i ett tidigt utvecklingsstadium, varf�r
      det �r sannolikt att buggar kan intr�ffa. Rapporter om fel som
      intr�ffar emottages mycket tacksamt (se nedan). Man b�r dock
      vara medveten om att Weblatte �r ett hobbyprojekt, och som
      s�dant ges naturligtvis inga garantier f�r om och n�r en
      eventuell bugg kan fixas.<br/>

      Det finns andra webb-baserade LysKOM-klienter, till exempel <a
      href="http://webkom.lysator.liu.se/">WebKOM</a> och <a
      href="http://www.lysator.liu.se/jyskom/">JySKOM</a>.
    </p>
    <p>
      Den officiella Weblatte-URLen �r:
      <a href="http://dll.nu/lyskom/">http://dll.nu/lyskom/</a>.
    </p>
    <p>Om du vill k�ra en egen Weblatte-server s� kr�vs en JSP-motor
      (Resin rekommenderas) och lite CVS-f�rdigheter f�r att h�mta hem
      koden fr�n SourceForge. Fr�ga i KOM f�r mer information (se
      nedan).
    </p>
    <h2>Att anv�nda Weblatte</h2>
    <p>
      N�r du har loggat in i Weblatte skall en lista med alla m�ten
      d�r du har ol�sta texter listas. F�r att b�rja l�sa i ett m�te
      �r det bara att klicka p� m�tesnamnet i listan. Weblatte kommer
      d� att visa det f�rsta (tidigaste) ol�sta inl�gget i det m�tet.
      N�r du l�st en text s� finns de tv� vanligaste alternativen som
      l�nkar under texten:<br/>

      "<b>L�smarkera denna text (och l�s n�sta)</b>" s�ger till
      LysKOM-servern att du har l�st texten, varp� du f�r l�sa n�sta
      inl�gg i m�tet.<br/>

      "<b>Kommentera (och l�smarkera) denna text</b>" l�smarkerar �ven
      den texten, men ist�llet f�r att visa n�sta ol�sta s� visas
      inl�gget samt en textruta d�r du kan skriva en kommentar till
      texten. Texten kommenteras enligt g�ngse LysKOM-konventioner,
      d.v.s. den nya kommentaren f�r samma mottagarlista som den
      kommenterade texten, f�rutom extra kopiemottagare. Vill du �ndra
      mottagare, klicka p� knappen "<b>�ndra mottagarlista</b>" f�r
      att komma till en sida d�r du kan fylla i vilka mottagare din
      text ska ha.
    </p>
    <p>
      N�r du l�st ut ett m�te �terf�rs du till listan �ver ol�sta
      m�ten.
    </p>
    <p>
      Med dessa enkla medel klarar du av den mest grundl�ggande
      LysKOM-anv�ndningen, n�mligen att l�sa och skriva inl�gg.  Mer
      avancerade kommandon f�r du genom att h�gerklicka p� en tom yta
      i webbl�saren. Du f�r d� upp en meny p� alla
      Weblatte-kommandon. Du kan �ven h�gerklicka p� t.ex. personnamn,
      m�tesnamn och textnummer f�r att f� valm�jligheter specifika f�r
      dessa. H�gerklicks-menyn fungerar inte i alla webbl�sare -
      d�rf�r kan du �ven f� de flesta menyalternativ som
      textl�nkar. F�r att aktivera textmenyer tempor�rt (under den
      innevarande inloggningen), klicka p� "<b>visa menyer</b>" l�ngst
      ned p� sidan.  Om du alltid vill ha synliga textmenyer s�
      v�ljer du "<b>inst�llningar</b>" och st�ller alternativet
      "<b>visa menyer</b>" till "P�" och klickar sedan p� "<b>spara
      inst�llningar</b>" (i samma ruta). Inst�llningarna sparas p� den
      server du �r inloggad p�.
    </p>
    <h2>Avancerade funktioner</h2>
    <p>
      Det finns en mer eller mindre <a href="features.jsp">fullst�ndig
      lista �ver funktioner i Weblatte</a> tillg�nglig. Detta stycker
      beskriver n�gra av dem.
    </p>
    <h3>Att anv�nda flera LysKOM-servrar samtidigt</h3>
    <p>
      Weblatte tillhandah�ller funktioner f�r att v�xla mellan olika
      LysKOM-servrar utan att beh�va logga ut och in varje g�ng. Detta
      g�r man genom att "pausa" en LysKOM-session. Genom att v�lja
      menyvalet "pausa" s� l�ggs sessionen i bakgrunden, s� att du kan
      logga in p� en annan LysKOM-server (eller som en annan anv�ndare
      p� samma server). Om du har en eller fler pausade
      LysKOM-sessioner s� visas det l�ngst ned p� huvudsidan som en
      l�nk med texten "<b>OBS! Du har N ol�st(a)
      LysKOM-sessioner</b>". Genom att klicka p� denna l�nk kommer du
      till en lista �ver alla sessioner som �r ig�ng f�r
      tillf�llet. Om du v�ljer en av sessionerna i listan s� kommer
      automatiskt den aktiva sessionen att pausas och ist�llet
      ers�ttas med den du valt, varp� du �terf�rs till huvudsidan och
      kan b�rja arbeta med den valda sessionen. Till h�ger om
      sessionsl�nken p� huvudsidan visas �ven en l�nk, "<b>>></b>",
      som tar dig direkt till den f�rst pausade sessionen i listan. Om
      du har fler �n en pausad session visas �ven en l�nk �t andra
      h�llet, "<b>&lt;&lt;</b>", tar dig till den senast pausade
      sessionen. P� s� vis kan du stega mellan alla sessioner utan
      att g� via sessions-listan.<br/> Om du har en eller flera
      pausade sessioner n�r du loggar ut ur en session, s� tas du till
      listan �ver sessioner. D�r kan du �ven v�lja att "<b>terminera
      alla sessioner</b>", vilket loggar ut alla LysKOM-sessioner du
      har startat (b�de pausade och den aktiva).  <br/> En pausad
      session kommer att spara alla meddelanden du f�r, vilka visas
      n�r du �terg�r till den sessionen.<br/> Om du gl�mmer att logga
      ut s� termineras automatiskt alla sessioner efter 30
      minuter. Vill du f�rhindra att bli utloggad s� r�cker det med
      att l�ta den aktiva sessionen vara i "<b>lista nyheter</b>"-l�ge
      med automatisk uppdatering p�slagen.  <br/>
    </p>
    <h3>M�nga ol�sta m�ten</h3>
    <p>
      Om du �r medlem i m�nga m�ten d�r du har ol�sta inl�gg (fler �n
      50), s� kommer Weblatte automatiskt aktivera en funktion som
      heter "m�nga m�ten". Den inneb�r i korthet att Weblatte inte
      fr�gar LysKOM-servern om information om alla ol�sta m�ten vid
      inloggning, utan ist�llet g�r det f�rst n�r du ska l�sa i ett
      m�te, samt att nyhetslistan begr�nsas till att visa fem m�ten �t
      g�ngen. N�r du har mindre �n 50 ol�sta m�ten s� kan du
      deaktivera funktionen under "Inst�llningar". Du kan givetvis
      aktivera "m�nga m�ten" �ven om du inte har fler �n 50 ol�sta
      m�ten, om du f�redrar den snabbare inloggningen som funktionen
      ger. Du kan justera gr�nsen f�r n�r "m�nga m�ten" skall
      aktiveras genom inst�llningen "Max antal ol�sta m�ten innan
      'm�nga m�ten' aktiveras automatiskt".
    </p>
    <h3>Om internationell teckenkodning</h3>
    <p>
      Som standard anv�nder Weblatte alltid "iso-8859-1" som
      teckenkodning f�r de inl�gg du skapar. Detta �r den kodning som
      alla LysKOM-klienter klaras av, och den kan representera de
      flesta tecken som beh�vs till vardags. Om du d�remot vill skriva
      inl�gg som inneh�ller icke-latinska tecken, till exempel
      hebreiska, s� m�ste du instruera Weblatte att anv�nda en
      teckenkodning som kan representera denna. Med hj�lp av
      inst�llningen "<b>Teckenkodning att anv�nda vid skapande av
      texter</b>" kan du styra hur dina inl�gg skall kodas. "utf-8" �r
      en Unicode-teckenkodning som klarar n�stan allt. Var dock beredd
      p� att vissa klienter inte kommer att kunna l�sa dina inl�gg
      korrekt om du anv�nder n�got annat �n "iso-8859-1".<br/>
      N�r det g�ller l�sning utav inl�gg s� �vers�tter Weblatte alla
      inl�gg till UTF-8 n�r de skickas till din webbl�sare. Detta g�r 
      att de flesta inl�gg kan visas korrekt. Det f�ruts�tter dock att
      din webbl�sare och operativsystem har tillg�ng till teckensnitt
      som inneh�ller de tecken som skall visas.
    </p>
    <h3>Om e-post, bin�rdata och mail-attachments</h3>
    <p>
      Flera KOM-system har s� kallad "mail-import", vilket �r ett s�tt
      f�r vanlig e-post att leta sig in i en LysKOM-server. Ett e-brev
      kan som bekant inneh�lla bilagor, till exempel filer och
      bilder. En bilaga visas ungef�r som en kommentar, men med texten
      "Bilaga av typen image/jpeg i NNNNNN (<b>visa</b>)". Bilagor
      l�smarkeras automatiskt n�r du l�ser brevet som de �r bilagor
      till. Om du klickar p� "visa" s� kommer bilagans inneh�ll att
      skickas till din webbl�sare i sin helhet. F�r en bild inneb�r
      detta i allm�nhet att du f�r se bilden direkt i din
      webbl�sare och f�r andra andra sorters bilagor s� kommer din
      webbl�sare bete sig precis som om du skulle ha laddat ned ett
      dokument av samma typ fr�n en vanlig webbplats.
    </p>
    <h2>Buggrapportering och f�rslag</h2>
    <p>
      Om man har konkreta felanm�lningar eller f�rb�ttringsf�rslag s�
      uppskattas det mycket om dessa kan l�mnas via de officiella
      "trackers" som finns f�r �ndam�ler p� websiten <a
	href="http://sourceforge.net/">SourceForge</a>. Genom att
      anv�nda dessa s� kan SourceForge hj�lpa oss att h�lla reda p�
      buggar som m�ste fixas och f�rslag som skall �verv�gas. F�ljande
      tv� l�nkar kan anv�ndas f�r att rapportera fel och l�mna f�rslag
      p� funktionalitet, respektive:
      <ul>
        <li><a href="http://sourceforge.net/tracker/?func=add&group_id=10071&atid=110071">Buggrapportering</a></li>
        <li><a href="http://sourceforge.net/tracker/?func=add&group_id=10071&atid=360071">F�rslagsrapportering</a></li>
      </ul>
      <i>N�r du l�mnar buggrapporter, v�lj <b>weblatte</b> i menyn
      ben�mnd "category".</i> Det g�r bra att skriva p� svenska i
      rapporterna.
    </p>
    <p>
      F�r mer informell kontakt och diskussion om Weblatte s� f�r du
      g�rna posta i n�got utav de "officiella" LatteKOM-m�ten som
      finns p� f�ljande KOM-system:
      <ul>
      <li>SnoppKOM: <tt>LatteKOM (nu.dll.lyskom.*, T2,
	  SwingKOM, ...)</tt> (m�te 19)</li>
      <li>LysLysKOM: <tt>LysKOM; Javaklienter baserade p�
	  LatteKOM</tt> (m�te 11056)</li>
      </ul>
    </p>
    <h2>Upphov, copyright, et.c.</h2>
    <p>
      Weblatte �r baserat p� <a
      href="http://lattekom.sf.net/">LatteKOM</a>, som �r ett
      klientklassbibliotek f�r LysKOM. LatteKOM och Weblatte �r b�da
      utvecklade utav <a
      href="http://dll.nu/lyskom/?lookup=Rasmus%20Sten">Rasmus
      Sten</a>. All k�llkod �r fritt tillg�nglig via <a
      href="http://lattekom.sourceforge.net/">hemsidan p�
      SourceForge</a> och vem som helst f�r kopiera och modifiera den
      i enlighet med <a
      href="http://opensource.org/licenses/mit-license.php">MIT-licensen</a>
      som den distribueras under. Tack f�r visat intresse!
    </p>
    <p class="footer">
      $Revision: 1.14 $
    </p>
  </body>
</html>
