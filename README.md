# Intrexx Connector API Quick Start

## Einführung

Die Intrexx Connector API definiert eine Java API, um eigene Intrexx Connectoren zu implementieren, die externe Daten in Intrexx über eine Fremddatengruppe bereitstellen. Dabei gibt die API im Wesentlichen zwei Java Interfaces vor, die vom Entwickler zu implementieren sind und Datensätze und Dateien im externen System erstellen, lesen, aktualisieren und löschen (sog. CRUD Aktionen). Des Weiteren wird in der Connector Konfigurationsdatei ein Metadaten-Modell hinterlegt, das die Struktur der Datengruppen (Felder, Beziehungen, Settings) beschreibt, die von der Connector Implementierung bereitgestellt werden. Zur Laufzeit bietet die Connector-API vorkonfigurierte Klienten für HTTP und OData Anfragen, welche automatisch eine Authentifizierung des Benutzers - sofern vom Service benötigt - via HTTP Basic Auth oder OAuth2 durchführen. Ansonsten lassen sich beliebige Java Bibliotheken (z.B. JDBC, REST APIs, etc.) im eigenen Code verwenden.

Für einen schnellen Start in die Connector Entwicklung werden im Folgenden Beispiel-Implementierungen beschrieben, die den Einsatz der API zur Anbindung von Fremdsystemen in Intrexx verdeutlichen.

*HINWEIS*: Die API befindet sich derzeit noch in einer Preview-Version. Bitte beachten Sie, dass diese für produktive Einsätze noch nicht freigegeben ist und sich bis zur finalen Freigabe noch Änderungen an der API ergeben können, die in eigenen Projekten nachgezogen werden müssen. Diese Anpassungen sollten aber nur geringfügig ausfallen.

## Einrichtung der Entwicklungsumgebung

### Vorbedingungen

- Intrexx 8.1 Installation mit Online Update 05
- Java JDK 1.8
- Optional wird für die Entwicklung eigener Connectoren eine IDE wie z.B. Eclipse Java IDE oder IntelliJ IDEA empfohlen.

### Projektordner anlegen

Die connector-api-examples.zip Datei kann in einen beliebigen Ordner (außerhalb Intrexx) entpackt werden. Im Folgenden wird davon ausgegangen, dass der Projektornder `connector-api-examples` heißt.

### Projektabhängigkeiten

Um den Quellcode kompilieren zu können, werden einige Intrexx sowie externe Bibliotheken benötigt. Diese sind in der Gradle Projektdatei `build.gradle` aufgeführt und werden normalerweise automatisch heruntergeladen. Sollte der Zugriff auf das United Planet Maven Repository nicht möglich sein, können die benötigten JAR-Dateien aus dem  `lib` Ordner der Intrexx Installation in den `lib` Ordner des Projekts kopiert werden.

*Hinweis*
Die Java Quellcode Dateien benötigen zum Kompilieren mindestens Intrexx 8.1 OU 05, das ab Oktober 2018 verfügbar sein wird. Es ist aber möglich, die Klassen mit Intrexx 8.1.3 zu kompilieren. In dem Fall muss in allen Java Dateien die import Anweisungen für folgende Packages geändert werden:

```java
 de.uplanet.lucy.server.odata.connector.api.v1 -> de.uplanet.lucy.server.odata.connector.api
```

### Projekt kompilieren

Das Projekt wird mit Gradle [](http://gradle.io) verwaltet und kompiliert. Folgende Befehle können direkt im Hauptordner des Projekts ausgeführt werden:

Windows:

```bash
gradlew.bat clean
gradlew.bat build
gradlew.bat jar
```

Linux/MacOS

```bash
./gradlew clean
./gradlew build
./gradlew jar
```

#### Eclipse

Eclipse starten und prüfen, dass das Gradle Plugin installiert ist. Anschließend über `File->Import->Gradle` den Projektordner in den Workspace importieren. Das Projekt sollte anschließend im Package Explorer verfügbar sein.

#### IntelliJ

IntelliJ bietet von Haus aus Unterstützung für Gradle Projekte. Das Projekt kann einfach über den Öffnen Dialog importiert werden, in diesem ist das Projektverzeichnis auszuwählen.

### Portalserver im Debug Modus starten

Um eigenen Java-Code zur Laufzeit in einem Intrexx Portal testen und debuggen zu können, lässt sich der Portalserver in Eclise/IntelliJ im Debug-Modus ausführen. Voraussetzung dafür ist, dass Intrexx und das Intrexx Portal auf dem lokalen PC wie die Entwicklungsumgebung installiert sind und der Portalserver Dienst zuvor beendet wurde. Es folgt die Einrichtung der Run Configuration für den Portalserver in Eclipse, in IntelliJ können dieselben Angaben verwendet werden:

- Unter Run->Run Configurations eine neue Java Application erstellen, Name 'Portalserver'.
- Unter 'Main Class' die Klasse `de.uplanet.lucy.server.portalserver.PortalService` eintragen.
- Unter 'Arguments' im Feld 'VM Arguments' folgende Parameter hinzufügen (für Linux ist java.library.path entsprechend anzupassen):

```bash
-ea
-Dfile.encoding=UTF-8
-Djava.library.path=../../bin/windows/amd64
-Djava.security.auth.login.config=file:internal/cfg/LucyAuth.cfg
-Xms256m
-Xmx512m
-Xbootclasspath/p:../../lib/xsltc-hndl-fix.jar
-Dde.uplanet.jdbc.trace=false
-Dde.uplanet.lucy.server.odata.consumer.ssl.allowSelfSignedCerts=true
-Dlog4j.configuration=file:internal/cfg/log4j-console.properties
```

- Unter 'Working directory' den Pfad zum Portalverzeichnis angeben, z.B. `C:\intrexx\org\portal`
- Unter `Classpath -> User Entries -> Advanced -> Add external folder` den Ordner `<INTREXX_HOME>\lib\update` hinzufügen.
- Unter `Classpath -> User Entries -> Add external jars` alle Jar-Dateien aus dem Ordner `<INTREXX_HOME>\lib` hinzufügen.
- Unter `Environment -> New` eine neue Umgebungsvariable mit Namen `INTREXX_HOME` erstellen und als Value den Intrexx Installationsordner eintragen.
- Konfiguration speichern.
- Die Datei `<INTREXX_HOME>\org\<portal>\internal\cfg\log4j.properties` zu `log4j-console.properties` kopieren und dort folgende Zeilen anpassen, um in Eclipse die Logausgaben in der Console zu erhalten:

```bash
# Set root logger level
log4j.rootLogger=WARN, File, Console

# Set United Planet logging level
log4j.logger.de.uplanet=INFO, File, SysFifo, Console
```

Nun kann der Portalserver via Run/Debug in Eclipse/IntelliJ gestartet werden und Breakpoints in eigenem Code gesetzt werden. Wird zur Laufzeit ein Breakpoint erreicht, wird ab dieser Stelle der Debugger aktiviert.

## Connector Beispiel-Implementierungen

Das Projekt enthält drei Beispiele für den Zugriff auf

- Google Kalender und Google Drive
- MS Office365 Termine
- SonarQube

Die Beispiele für Google und Office365 benötigen eine OAuth2 Konfiguration für die Authentifizierung am Service im Intrexx Portal. Wie diese einzurichten ist, wird in der [Intrexx Online-Hilfe](http://up-download.de/up/docs/intrexx-onlinehelp/8100/de/index.html?p=helpfiles/help.2.connectoren-office-365.html) beschrieben.

## Eigenes Connector Projekt erstellen

Eigene Connectoren können direkt innerhalb des Beispiel-Projekts entwickelt werden oder es wird eine Kopie des Projektordners erstellt, der Java Quellcode unter /src/main/java gelöscht und eigene Klassen darunter erstellt. Im Folgenden wird Schritt für Schritt die Erstellung eines eigenen Connectors beschrieben.

*Hinweis*: Sollte die Entwicklung auf Basis eines bereits bestehenden Portals erfolgen, so ist zu prüfen, ob diese beiden Dateien sich bereits im Portalverzeichnis befinden. Ansonsten müssen diese aus dem jeweiligen Ordner unter `<INTREXX_HOME>/orgtempl/...` in alle Portale kopiert werden.

- org\<portal>\internal\cfg\biaconfig\bia-connector.cfg
- org\<portal>\internal\cfg\odata\connector\template\msoffice365.xml

### InMemory Connector Beispiel

Das InMemory Connector Beispiel bietet den Zugriff auf eine interne Datenstruktur über eine Intrexx Fremddatengruppe. Dabei werden alle CRUD Aktionen sowie Sortierung unter Pagination unterstützt.

#### Connector Konfiguration

Neue Connectoren definieren eine Template Datei unter `org\<portal>\internal\cfg\odata\connector\template\`. Als Dateiname sollte ein Connector Bezeichner gewählt werden. Legen Sie in dem Ordner eine neue Datei inmemory.xml an und kopieren Sie diesen Inhalt in die Datei:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<connector>
    <guid></guid>
    <name>InMemory</name>
    <description>Configuration template for the InMemory test connector.</description>
    <connectorId>inmemory</connectorId>
    <properties>
        <property name="serviceTimeout" value="0" description=""/>
        <property name="serviceRootURI" value="" description=""/>
        <property name="useSSL" value="false" description=""/>
    </properties>
    <userMappings>
    </userMappings>
    <metaData>
        <dataGroups>
            <dataGroup name="MyDataGroup">
                <fields>
                    <field name="ID" typeName="long" primaryKey="true" />
                    <field name="Title" typeName="string" primaryKey="false" />
                    <field name="Double" typeName="double" primaryKey="false" />
                    <field name="Long" typeName="long" primaryKey="false" />
                    <field name="Text" typeName="text" primaryKey="false" />
                    <field name="DateTime" typeName="datetime" primaryKey="false" />
                    <field name="Boolean" typeName="boolean" primaryKey="false" />
                </fields>
                <settings>
                    <setting key="connector.cfg.guid" value=""/>
                    <setting key="connector.dataGroup.adapter.class" value="de.uplanet.lucy.connectorapi.examples.simple.InMemoryDataGroupConnector"/>
                </settings>
            </dataGroup>
        </dataGroups>
        <fileFields>
        </fileFields>
    </metaData>
</connector>
```

Der wesentliche Abschnitt im Connector-Template befindet sich unter `<metaData>`. Hier werden die Datengruppen, Felder und Expert Settings definiert, die der Connector für Fremddatengruppen in Intrexx bereitstellt. Es können beliebig viele Datengruppen und Felder definiert werden. Weiterhin können auch Dateifelder für den Datei-Upload/Download definiert werden, worauf hier nicht eingegangen wird, da es den Rahmen dieses Quick Starts überschreiten würde.

In diesem Beispiel wird eine Datengruppe mit 7 Feldern für die jeweils möglichen Intrexx Datentypen erstellt. Zusätzlich ist anzugeben, ob das Feld Teil des Primary Keys ist.

Auf Basis dieses Templates können nun konkrete Connector-Konfigurationen in Intrexx registriert werden. Dadurch ist es möglich, unterschiedliche Services mit denselben Metadaten aber unterschiedlicher Konfiguration anzulegen. Dazu kopieren Sie die Datei nach `org\<portal>\internal\cfg\odata\connector` und benennen Sie um nach `inmemory_test.xml`. Der erste Teil des Namens vor dem Unterstrich bezeichnet die Connector ID (siehe oben `<connectorId>`), der zweite Teil ist der Name dieser konkreten Konfiguration (hier `test`). Öffnen Sie die Datei dann in einem Editor und ergänzen Sie `<guid>` und  `<name>` wie folgt (GUID muss in allen Dateien eindeutig sein:

```xml
<guid>F27302911D5DA4E8FE7A500D3F4E1E699BDF0592</guid>
<name>test</name>
...
<settings>
        <setting key="connector.cfg.guid" value="inmemory_test"/>
...
```

Starten Sie anschließend den Portalserver neu.

#### Intrexx Applikation

Als nächstes kann bereits eine Applikation in Intrexx erstellt werden, die diesen Connector verwendet.

- Legen Sie dazu eine neue 'Leere Applikation' im Anwendungsdesigner an und löschen Sie die Datengruppe 'Datengruppe'.
- Erstellen Sie eine neue Fremddatengruppe mit Namen 'InMemory'.
- Unter 'Datenbankverbindung' wählen Sie den Eintrag `inmemory_test`, unter Datahandler sollte nun 'Connector API - Datenhandler' stehen.
- Klicken Sie neben dem 'Tabellen'-Feld auf das Lupen-Symbol und suchen Sie nach allen Tabellen mit '*'. Es sollte eine Tabelle `CONNECTOR_API.MyDataGroup` erscheinen. Wählen Sie diese aus.
- Klicken Sie nun auf der Registerkarte 'Datenfelder' auf das `+` Symbol und wählen alle Felder aus.
- Wechseln Sie nun auf die Registerkarte Expert und dort auf Settings. Legen Sie folgende zwei neue Settings an:

```properties
connector.cfg.guid = inmemory_test
connector.dataGroup.adapter.class =	de.uplanet.lucy.connectorapi.examples.simple.InMemoryDataGroupConnector
```

- Schließen Sie dann den Datengruppendialog mit 'OK'.
- Nun kann auf der Ansichtsseite der Applikation eine neue Ansichtsstabelle erstellt werden, die alle Felder der Datengruppe beinhaltet.
- Veröffentlichen Sie die Applikation und rufen Sie sie im Portal auf. Es wird ein Fehler erscheinen, da die Adapter Klasse noch nicht gefunden wird.

*Hinweis*: Die App befindet sich auch als Import-Paket im Projektverzeichnis unter `docs/InMemoryConnector.lax` und kann so direkt in Intrexx importiert werden.

#### Projekt in Intrexx einbinden

Damit eigene Connector Klassen zur Laufzeit im Portalserver gefunden werden, muss die Jar-Datei des Projekts in den Intrexx Classpath aufgenommen werden. Am einfachsten geschieht dies, wenn direkt nach Aufruf von `gradlew.bat jar` die Datei unter `build\libs\connector-examples-8.1.3.jar` nach `<INTREXX_HOME>\lib` kopiert wird. Danach muss der Portalserver Dienst neu gestartet werden. Nach dem Neustart und Aufruf der Applikation im Portal sollten drei Datensätze in der Tabelle aufgelistet werden. Diese können geändert, gelöscht oder neu erstellt werden.

#### Connector Klassen Implementierung

Die Logik für den Datenzugriff ist in der Klasse `de.uplanet.lucy.connectorapi.examples.simple.InMemoryDataGroupConnector` implementiert. Darin werden die Anhand der übergebenen Abfrage-Parameter die Daten ermittelt und in die Connector API Datenstrukturen überführt bzw. bei Inserts/Updates die Werte aus den Intrexx Objekten in der Tabelle gespeichert.

## Fortgeschrittene Connector Beispiele

In den Java Packages `de.uplanet.lucy.connectorapi.examples` befinden sich weitere Beispiel Implementierungen, die den Zugriff auf echte REST Webservices via HTTP und OData aufzeigen. Dabei werden auch Intrexx Filter-, Sortierung- und Pagination-Funktionen verwendet. Weiterführende Dokumentationen dazu befinden sich im Unterverzeichnis `docs`.

## Java API Dokumentation

Im `docs/api` Verzeichnis des Projekts befinden sich die Java API Dokumentation der Intrexx Connector API. Darin werden alle wesentlichen Klassen und Interfaces beschrieben. Des Weiteren stehen zur Laufzeit alle Kontextobjekte (Session, Request, etc.) wie in Intrexx Groovy Skripten zur Verfügung.

## Java Interfaces und Klassen der Connector API

Die öffentlichen Schnittstellen und Klassen der Connector API befinden sich im Paket `de.uplanet.lucy.server.odata.connector.api.v1`. Im Folgenden werden die für eigene Connectoren zu implementierenden Methoden und bereitgestellte Hilfsklassen anhand des InMemory Beispiels beschrieben.

### Interface `de.uplanet.lucy.server.odata.connector.api.v1.IConnectorDataGroupAdapter`

Dieses Interface definiert die benötigten Methoden für Create/Read/Update/Delete-Aktionen von Intrexx Fremddatengruppen.

- `IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_criteria)`
    Diese Methode wird von Intrexx aufgerufen, wenn Datensätze für eine Fremddatengruppe geladen werden sollen. Über das `IConnectorQueryCriteria p_criteria` Argument werden zur Auswahl der Datensätze benötigte Informationen wie Filter, Sortierung, Pagination übergeben.
- `IConnectorRecord queryDataRecord(String p_strRecordId, List<IConnectorField> p_fields)`
    Diese Methode wird von Intrexx aufgerufen, wenn ein einzelne Datensatz für eine Fremddatengruppe geladen werden sollen. Über das `String p_strRecId` Argument wird die ID des zu ladenden Datensatzes übergeben, während `List<IConnectorField> p_fields` die zu selektierenden Datensatzfelder beinhaltet.
- `String insert(IConnectorRecord p_record)`
    Diese Methode dient zum Anlegen eines neuen Datensatzes. Das `IConnectorRecord p_record` Argument enthält dabei die Datensatz-ID als auch die Feldwerte.
- `boolean update(IConnectorRecord p_record)`
    Diese Methode dient zum Ändern eines bestehenden Datensatzes. Das `IConnectorRecord p_record` Argument enthält dabei die Datensatz-ID sowie die Feldwerte.
- `void delete(IConnectorRecord p_record)`
    Diese Methode dient zum Löschen eines bestehenden Datensatzes. Das `IConnectorRecord p_record` Argument enthält dabei die Datensatz-ID.

### Basisklasse `de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorDataGroupAdapter`

Von dieser abstrakten Klasse sollten alle konkreten IConnectorDataGroupAdapter-Implementierungen erben. Die Klasse stellt eine Reihe von Hilfsmethoden und Kontextobjekte zur Verfügung.

### Interface `de.uplanet.lucy.server.odata.connector.api.v1.IConnectorFileAdapter`

Dieses Interface definiert die benötigten Methoden für Create/Read/Update/Delete-Aktionen von Intrexx Dateifeldern. Diese können zu einer Intrexx Systemdatengruppe oder einer Fremddatengruppe gehören.

### Basisklasse `de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorFileAdapter`

Von dieser abstrakten Klasse sollten alle konkreten IConnectorFileAdapter-Implementierungen erben. Die Klasse stellt eine Reihe von Hilfsmethoden und Kontextobjekte zur Verfügung.
