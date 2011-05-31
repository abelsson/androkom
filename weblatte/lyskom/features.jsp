<%@ page pageEncoding='iso-8859-1' contentType='text/html;charset=utf-8'%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Weblatte - funktionslista</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>
  <body>
    <pre>

 <b>Lista över funktioner i Weblatte</b>

 * Nyhetslistan uppdateras automatiskt varannan minut. Meddelanden som
   inkommit sparas vid automatiska siduppdateringar.
 * Möjlighet att spara sina inställningar på servern (user-area)
 * Inställning: man kan välja charset för skapade texter.
 * All text visas i UTF-8 och således kan de flesta konstiga
   teckentabeller visas (beroende på webbläsare/OS)
 * Inställning: möjlighet att fetstilsmarkera sitt eget namn
 * Flexibel vilka-lista
 * Context-känslig högerklicks-meny för:
   * Personnamn (brevlådor)
   * Möten
   * Textnummer
   * Övrigt (huvudmeny vid högerklick på en tom yta)
 * "Ramläsning" eller "ramvy" är ett experimentellt sätt att läsa
   ett möte i HTML-frames, där en trädvy visas till vänster med
   ankarlänkar till högerramen där alla inlägg visas. 
 * Effektiv serverkommunikation vid t.ex. "lista ärenden", som innebär
   att hela inlägget inte behöver läsas in från servern.
 * Om man glömmer logga ut så termineras sessionen efter 30 minuter
 * Stöd för stora binära inlägg, t.ex. JPEG-bilder
   (det är upp till webbläsaren att hantera dessa)
 * Asynkrona meddelanden visas och kan skickas
   (en enkel "chatfunktion" finns också, men är ännu inte inlänkad
   från någon meny)
 * "Addera mottagare" och "Addera extrakopiemottagare" finns som
   funktioner om man högerklickar på ett inläggsnummer.
 * "Ändra presentation" finns som alternativ vid högerklick på 
   mötes- eller personnamn. Där finns även "Återse presentation",
   "återse FAQ", "skicka brev" och "skicka meddelande".
 * "Ändra lösenord" finns enbart som textmeny-alternativ.
 * Möjlighet att ha flera LysKOM-sessioner igång samtidigt genom att
   "pausa" den aktiva sessionen och logga in på nytt. Enkla medel för
   att växla mellan alla aktiva sessioner.
 * Senast valda LysKOM-server lagras i en cookie och är automatiskt
   vald nästa gång man skall logga in.
 * "Många möten" är en funktion för mer sparsam inläsning utav
   mötesinformation för de möten man har olästa. Denna gör det möjligt
   att använda Weblatte oavsett hur många olästa möten man har. "Många
   möten" aktiveras automatiskt om man har fler än 50 olästa möten 
   (konfigurerbart).
 * Inlägg med okänd content-type kan laddas ned till webbläsaren i sin
   helhet. Webbläsaren presenteras med inläggets riktiga content-type,
   vilket gör att man till exempel kan titta på en bifogad bild direkt
   i Weblatte. Inlägg med en teckenkodning som inte känns igen
   hanteras på liknande vis, förutom att man får alternativet att
   avkoda det enligt iso-8859-1.
 * Bilagor till importerad e-post läsmarkeras automatiskt när man
   läser inlägget som de är bilaga till, så att man slipper klicka sig
   förbi irrelevanta bilagor var för sig. Vill man se en bilaga måste
   man klicka på bilagans inläggsnummer eller på "visa"-länken på
   samma rad.
    </pre>
  </body>
</html>
