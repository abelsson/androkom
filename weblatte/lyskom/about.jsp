<%@ page pageEncoding='iso-8859-1' contentType='text/html;charset=utf-8'%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Weblatte - hj�lp och information</title>
    <link rel="stylesheet" href="lattekom.css">
  </head>
  <body>
    <h2>Weblatte</h2>
    <p>
      Weblatte �r en LysKOM-klient f�r dig som l�tt vill kunna l�sa
      och skriva i LysKOM-system via din vanliga webbl�sare. LysKOM �r
      ett konferenssystem, och mer om detta kan du l�sa p� <a
      href="http://www.lysator.liu.se/lyskom/">LysKOMs
      hemsida</a>.<br/> Weblatte �r fortfarande i ett tidigt
      utvecklingsstadium, varf�r det �r sannolikt att buggar kan
      intr�ffa. Rapporter om fel som intr�ffar emottages mycket
      tacksamt (se nedan). Man b�r dock vara medveten om att
      Weblatte �r ett hobbyprojekt, och som s�dant ges naturligtvis
      inga garantier f�r om och n�r en eventuell bugg kan fixas.<br/>
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
      text ska ha.<br/>

      N�r du l�st ut ett m�te �terf�rs du till listan �ver ol�sta
      m�ten (undantaget d� du kommenterat den sista texten i ett m�te
      - du m�ste d� v�lja "<b>lista nyheter</b>" manuellt, detta f�r
      att du ska kunna f� en bekr�ftelse p� att din text skapats och
      kunna se dess textnummer).<br/>
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
      $Revision: 1.6 $
    </p>
  </body>
</html>
