<%@ page pageEncoding='iso-8859-1' contentType='text/html;charset=utf-8'%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Weblatte - hjälp och information</title>
    <link rel="stylesheet" href="lattekom.css">
  </head>
  <body>
    <h2>Weblatte</h2>
    <p>
      Weblatte är en LysKOM-klient för dig som lätt vill kunna läsa
      och skriva i LysKOM-system via din vanliga webbläsare. LysKOM är
      ett konferenssystem, och mer om detta kan du läsa på <a
      href="http://www.lysator.liu.se/lyskom/">LysKOMs
      hemsida</a>.<br/> Weblatte är fortfarande i ett tidigt
      utvecklingsstadium, varför det är sannolikt att buggar kan
      inträffa. Rapporter om fel som inträffar emottages mycket
      tacksamt (se nedan). Man bör dock vara medveten om att
      Weblatte är ett hobbyprojekt, och som sådant ges naturligtvis
      inga garantier för om och när en eventuell bugg kan fixas.<br/>
      Det finns andra webb-baserade LysKOM-klienter, till exempel <a
	href="http://webkom.lysator.liu.se/">WebKOM</a> och <a
	href="http://www.lysator.liu.se/jyskom/">JySKOM</a>.
    </p>
    <p>
      Den officiella Weblatte-URLen är:
      <a href="http://dll.nu/lyskom/">http://dll.nu/lyskom/</a>.
    </p>
    <p>Om du vill köra en egen Weblatte-server så krävs en JSP-motor
      (Resin rekommenderas) och lite CVS-färdigheter för att hämta hem
      koden från SourceForge. Fråga i KOM för mer information (se
      nedan).
    </p>
    <h2>Att använda Weblatte</h2>
    <p>
      När du har loggat in i Weblatte skall en lista med alla möten
      där du har olästa texter listas. För att börja läsa i ett möte
      är det bara att klicka på mötesnamnet i listan. Weblatte kommer
      då att visa det första (tidigaste) olästa inlägget i det mötet.
      När du läst en text så finns de två vanligaste alternativen som
      länkar under texten:<br/>

      "<b>Läsmarkera denna text (och läs nästa)</b>" säger till
      LysKOM-servern att du har läst texten, varpå du får läsa nästa
      inlägg i mötet.<br/>

      "<b>Kommentera (och läsmarkera) denna text</b>" läsmarkerar även
      den texten, men istället för att visa nästa olästa så visas
      inlägget samt en textruta där du kan skriva en kommentar till
      texten. Texten kommenteras enligt gängse LysKOM-konventioner,
      d.v.s. den nya kommentaren får samma mottagarlista som den
      kommenterade texten, förutom extra kopiemottagare. Vill du ändra
      mottagare, klicka på knappen "<b>ändra mottagarlista</b>" för
      att komma till en sida där du kan fylla i vilka mottagare din
      text ska ha.<br/>

      När du läst ut ett möte återförs du till listan över olästa
      möten (undantaget då du kommenterat den sista texten i ett möte
      - du måste då välja "<b>lista nyheter</b>" manuellt, detta för
      att du ska kunna få en bekräftelse på att din text skapats och
      kunna se dess textnummer).<br/>
    </p>
    <p>
      Med dessa enkla medel klarar du av den mest grundläggande
      LysKOM-användningen, nämligen att läsa och skriva inlägg.  Mer
      avancerade kommandon får du genom att högerklicka på en tom yta
      i webbläsaren. Du får då upp en meny på alla
      Weblatte-kommandon. Du kan även högerklicka på t.ex. personnamn,
      mötesnamn och textnummer för att få valmöjligheter specifika för
      dessa. Högerklicks-menyn fungerar inte i alla webbläsare -
      därför kan du även få de flesta menyalternativ som
      textlänkar. För att aktivera textmenyer temporärt (under den
      innevarande inloggningen), klicka på "<b>visa menyer</b>" längst
      ned på sidan.  Om du alltid vill ha synliga textmenyer så
      väljer du "<b>inställningar</b>" och ställer alternativet
      "<b>visa menyer</b>" till "På" och klickar sedan på "<b>spara
      inställningar</b>" (i samma ruta). Inställningarna sparas på den
      server du är inloggad på.
    </p>
    <h2>Buggrapportering och förslag</h2>
    <p>
      Om man har konkreta felanmälningar eller förbättringsförslag så
      uppskattas det mycket om dessa kan lämnas via de officiella
      "trackers" som finns för ändamåler på websiten <a
	href="http://sourceforge.net/">SourceForge</a>. Genom att
      använda dessa så kan SourceForge hjälpa oss att hålla reda på
      buggar som måste fixas och förslag som skall övervägas. Följande
      två länkar kan användas för att rapportera fel och lämna förslag
      på funktionalitet, respektive:
      <ul>
        <li><a href="http://sourceforge.net/tracker/?func=add&group_id=10071&atid=110071">Buggrapportering</a></li>
      <li><a href="http://sourceforge.net/tracker/?func=add&group_id=10071&atid=360071">Förslagsrapportering</a></li>
      </ul>
      <i>När du lämnar buggrapporter, välj <b>weblatte</b> i menyn
      benämnd "category".</i> Det går bra att skriva på svenska i
      rapporterna.
    </p>
    <p>
      För mer informell kontakt och diskussion om Weblatte så får du
      gärna posta i något utav de "officiella" LatteKOM-möten som
      finns på följande KOM-system:
      <ul>
      <li>SnoppKOM: <tt>LatteKOM (nu.dll.lyskom.*, T2,
	  SwingKOM, ...)</tt> (möte 19)</li>
      <li>LysLysKOM: <tt>LysKOM; Javaklienter baserade på
	  LatteKOM</tt> (möte 11056)</li>
      </ul>
    </p>
    <h2>Upphov, copyright, et.c.</h2>
    <p>
      Weblatte är baserat på <a
      href="http://lattekom.sf.net/">LatteKOM</a>, som är ett
      klientklassbibliotek för LysKOM. LatteKOM och Weblatte är båda
      utvecklade utav <a
      href="http://dll.nu/lyskom/?lookup=Rasmus%20Sten">Rasmus
      Sten</a>. All källkod är fritt tillgänglig via <a
      href="http://lattekom.sourceforge.net/">hemsidan på
      SourceForge</a> och vem som helst får kopiera och modifiera den
      i enlighet med <a
      href="http://opensource.org/licenses/mit-license.php">MIT-licensen</a>
      som den distribueras under. Tack för visat intresse!
    </p>
    <p class="footer">
      $Revision: 1.6 $
    </p>
  </body>
</html>
