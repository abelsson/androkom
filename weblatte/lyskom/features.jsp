<%@ page pageEncoding='iso-8859-1' contentType='text/html;charset=utf-8'%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>Weblatte - funktionslista</title>
    <link rel="stylesheet" href="lattekom.css" />
  </head>
  <body>
    <pre>

 <b>Lista �ver funktioner i Weblatte</b>

 * Nyhetslistan uppdateras automatiskt varannan minut. Meddelanden som
   inkommit sparas vid automatiska siduppdateringar.
 * M�jlighet att spara sina inst�llningar p� servern (user-area)
 * Inst�llning: man kan v�lja charset f�r skapade texter.
 * All text visas i UTF-8 och s�ledes kan de flesta konstiga
   teckentabeller visas (beroende p� webbl�sare/OS)
 * Inst�llning: m�jlighet att fetstilsmarkera sitt eget namn
 * Flexibel vilka-lista
 * Context-k�nslig h�gerklicks-meny f�r:
   * Personnamn (brevl�dor)
   * M�ten
   * Textnummer
   * �vrigt (huvudmeny vid h�gerklick p� en tom yta)
 * "Raml�sning" eller "ramvy" �r ett experimentellt s�tt att l�sa
   ett m�te i HTML-frames, d�r en tr�dvy visas till v�nster med
   ankarl�nkar till h�gerramen d�r alla inl�gg visas. 
 * Effektiv serverkommunikation vid t.ex. "lista �renden", som inneb�r
   att hela inl�gget inte beh�ver l�sas in fr�n servern.
 * Om man gl�mmer logga ut s� termineras sessionen efter 30 minuter
 * St�d f�r stora bin�ra inl�gg, t.ex. JPEG-bilder
   (det �r upp till webbl�saren att hantera dessa)
 * Asynkrona meddelanden visas och kan skickas
   (en enkel "chatfunktion" finns ocks�, men �r �nnu inte inl�nkad
   fr�n n�gon meny)
 * "Addera mottagare" och "Addera extrakopiemottagare" finns som
   funktioner om man h�gerklickar p� ett inl�ggsnummer.
 * M�jlighet att ha flera LysKOM-sessioner ig�ng samtidigt genom att
   "pausa" den aktiva sessionen och logga in p� nytt. Enkla medel f�r
   att v�xla mellan alla aktiva sessioner.
 * Senast valda LysKOM-server lagras i en cookie och �r automatiskt
   vald n�sta g�ng man skall logga in.
 * "M�nga m�ten" �r en funktion f�r mer sparsam inl�sning utav
   m�tesinformation f�r de m�ten man har ol�sta. Denna g�r det m�jligt
   att anv�nda Weblatte oavsett hur m�nga ol�sta m�ten man har. "M�nga
   m�ten" aktiveras automatiskt om man har fler �n 200 ol�sta m�ten.
 * Inl�gg med ok�nd content-type kan laddas ned till webbl�saren i sin
   helhet. Webbl�saren presenteras med inl�ggets riktiga content-type,
   vilket g�r att man till exempel kan titta p� en bifogad bild direkt
   i Weblatte. Inl�gg med en teckenkodning som inte k�nns igen
   hanteras p� liknande vis, f�rutom att man f�r alternativet att
   avkoda det enligt iso-8859-1.
 * Bilagor till importerad e-post l�smarkeras automatiskt n�r man
   l�ser inl�gget som de �r bilaga till, s� att man slipper klicka sig
   f�rbi irrelevanta bilagor var f�r sig. Vill man se en bilaga m�ste
   man klicka p� bilagans inl�ggsnummer eller p� "visa"-l�nken p�
   samma rad.
    </pre>
  </body>
</html>
