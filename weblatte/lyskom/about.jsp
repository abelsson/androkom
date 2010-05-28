<%@ page pageEncoding='iso-8859-1' contentType='text/html;charset=utf-8'%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Weblatte - hjälp och information</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>
  <body class="about">
    <h2>Weblatte - en kort introduktion</h2>
    <p>
      Weblatte är en LysKOM-klient för dig som lätt vill kunna läsa
      och skriva i LysKOM-system via din vanliga webbläsare. LysKOM är
      ett konferenssystem, och mer om detta kan du läsa på <a
      href="http://www.lysator.liu.se/lyskom/">LysKOMs
      hemsida</a>.<br/>

      Weblatte är fortfarande i ett tidigt utvecklingsstadium, varför
      det är sannolikt att buggar kan inträffa. Rapporter om fel som
      inträffar emottages mycket tacksamt (se nedan). Man bör dock
      vara medveten om att Weblatte är ett hobbyprojekt, och som
      sådant ges naturligtvis inga garantier för om och när en
      eventuell bugg kan fixas.<br/>

      Det finns andra webb-baserade LysKOM-klienter, till exempel <a
      href="http://webkom.lysator.liu.se/">WebKOM</a> och <a
      href="http://www.lysator.liu.se/jyskom/">JySKOM</a>.
    </p>
<!--
    <p>
      Den officiella Weblatte-URLen är:
      <a href="http://kom.dll.nu/">http://kom.dll.nu</a>.
    </p>
-->
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
      text ska ha.
    </p>
    <p>
      När du läst ut ett möte återförs du till listan över olästa
      möten.
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
    <h2>Avancerade funktioner</h2>
    <p>
      Det finns en mer eller mindre <a href="features.jsp">fullständig
      lista över funktioner i Weblatte</a> tillgänglig. Följande
      stycken beskriver några av dem.
    </p>
    <h3>Att använda flera LysKOM-servrar samtidigt</h3>
    <p>
      Weblatte tillhandahåller funktioner för att växla mellan olika
      LysKOM-servrar utan att behöva logga ut och in varje gång. Detta
      gör man genom att "pausa" en LysKOM-session. Genom att välja
      menyvalet "starta ny session" så läggs sessionen i bakgrunden,
      så att du kan
      logga in på en annan LysKOM-server (eller som en annan användare
      på samma server). Om du har en eller fler pausade
      LysKOM-sessioner så visas det längst ned på huvudsidan som en
      länk med texten "<b>OBS! Du har N oläst(a)
      LysKOM-sessioner</b>". Genom att klicka på denna länk kommer du
      till en lista över alla sessioner som är igång för
      tillfället. Om du väljer en av sessionerna i listan så kommer
      automatiskt den aktiva sessionen att pausas och istället
      ersättas med den du valt, varpå du återförs till huvudsidan och
      kan börja arbeta med den valda sessionen. Till höger om
      sessionslänken på huvudsidan visas även en länk, "<b>>></b>",
      som tar dig direkt till den först pausade sessionen i listan.
      På så vis kan du stega mellan alla sessioner utan
      att gå via sessions-listan.<br/> Om du har en eller flera
      pausade sessioner när du loggar ut ur en session, så tas du till
      listan över sessioner. Där kan du även välja att "<b>terminera
      alla sessioner</b>", vilket loggar ut alla LysKOM-sessioner du
      har startat (både pausade och den aktiva).  <br/> En pausad
      session kommer att spara alla meddelanden du får, vilka visas
      när du återgår till den sessionen.<br/> Om du glömmer att logga
      ut så termineras automatiskt alla sessioner efter 30
      minuter. Vill du förhindra att bli utloggad så räcker det med
      att låta den aktiva sessionen vara i "<b>lista nyheter</b>"-läge
      med automatisk uppdatering påslagen.  <br/>
    </p>
    <h3>Många olästa möten</h3>
    <p>
      Om du är medlem i många möten där du har olästa inlägg (fler än
      50), så kommer Weblatte automatiskt aktivera en funktion som
      heter "många möten". Den innebär i korthet att Weblatte inte
      frågar LysKOM-servern om information om alla olästa möten vid
      inloggning, utan istället gör det först när du ska läsa i ett
      möte, samt att nyhetslistan begränsas till att visa fem möten åt
      gången. När du har mindre än 50 olästa möten så kan du
      deaktivera funktionen under "Inställningar". Du kan givetvis
      aktivera "många möten" även om du inte har fler än 50 olästa
      möten, om du föredrar den snabbare inloggningen som funktionen
      ger. Du kan justera gränsen för när "många möten" skall
      aktiveras genom inställningen "Max antal olästa möten innan
      'många möten' aktiveras automatiskt".
    </p>
    <h3>Om internationell teckenkodning</h3>
    <p>
      Som standard använder Weblatte alltid "iso-8859-1" som
      teckenkodning för de inlägg du skapar. Om du använder tecken som
      inte finns med i iso-8859-1 (även kallat "Latin-1", eller
      "ISO-Latin-1"), så kommer Weblatte att försöka koda inlägget med
      UTF-8 istället.
      Med hjälp av
      inställningen "<b>Teckenkodning att använda vid skapande av
      texter</b>" kan du styra hur dina inlägg skall kodas. "utf-8" är
      en Unicode-teckenkodning som klarar nästan allt. Var dock beredd
      på att vissa klienter inte kommer att kunna läsa dina inlägg
      korrekt om du använder något annat än "iso-8859-1".<br/>
      När det gäller läsning utav inlägg så översätter Weblatte alla
      inlägg till UTF-8 när de skickas till din webbläsare. Detta gör 
      att de flesta inlägg kan visas korrekt. Det förutsätter dock att
      din webbläsare och operativsystem har tillgång till teckensnitt
      som innehåller de tecken som skall visas.
    </p>
    <h3>Om e-post, binärdata och mail-attachments</h3>
    <p>
      Flera KOM-system har så kallad "mail-import", vilket är ett sätt
      för vanlig e-post att leta sig in i en LysKOM-server. Ett e-brev
      kan som bekant innehålla bilagor, till exempel filer och
      bilder. En bilaga visas ungefär som en kommentar, men med texten
      "Bilaga av typen image/jpeg i NNNNNN (<b>visa</b>)". Bilagor
      läsmarkeras automatiskt när du läser brevet som de är bilagor
      till. Om du klickar på "visa" så kommer bilagans innehåll att
      skickas till din webbläsare i sin helhet. För en bild innebär
      detta i allmänhet att du får se bilden direkt i din
      webbläsare och för andra andra sorters bilagor så kommer din
      webbläsare bete sig precis som om du skulle ha laddat ned ett
      dokument av samma typ från en vanlig webbplats.
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
      gärna posta i något utav de officiella LatteKOM-möten som
      finns på följande KOM-system:
      <ul>
      <li><a href="/?server=kom.lysator.liu.se&conference=11056">LysLysKOM: LysKOM; Weblatte (och andra) LatteKOM-klienter (möte 11056)</a>
      </ul>
    </p>
    <h2>Upphov, copyright, et.c.</h2>
    <p>
      Weblatte är baserat på <a
      href="http://lattekom.sf.net/">LatteKOM</a>, som är ett
      klientklassbibliotek för LysKOM. LatteKOM och Weblatte är båda
      utvecklade utav <a
      href="http://kom.dll.nu/?lookup=Rasmus%20Sten">Rasmus
      Sten</a>. All källkod är fritt tillgänglig via <a
      href="http://lattekom.sourceforge.net/">hemsidan på
      SourceForge</a> och vem som helst får kopiera och modifiera den
      i enlighet med <a
      href="http://opensource.org/licenses/mit-license.php">MIT-licensen</a>
      som den distribueras under. Tack för visat intresse!
    </p>
    <p class="footer">
      $Revision: 1.18 $
    </p>
  </body>
</html>
