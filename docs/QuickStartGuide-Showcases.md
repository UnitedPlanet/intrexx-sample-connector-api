# Quick start guide for the Google Drive/Events Intrexx Connector API showcases

## 1. Installation

1. clone the git repository
2. build a jar
    * Open a terminal (bash/cmd)
    * CD into the git repository folder connector-api-examples
    * ```gradle clean build jar```
    * The jar will be created in connector-api-examples/build/libs/connector-showcases-8.1.x.jar
3. Add the jar to your Intrexx 18.03 OU4 or 18.09 installation.
    * Copy the jar into this folder: `<INTREXX-PORTAL>/lib`
    * Modify the portal.wcf
        * Add the following line under `wrapper.java.classpath.5=/opt/intrexx8100/org/<portalname>/lib/*.jar`
        ```bash
        # Java Classpath (include wrapper.jar)  Add class path elements as
        #  needed starting from 1
        wrapper.java.classpath.1=/opt/intrexx/lib/update
        wrapper.java.classpath.2=/opt/intrexx/lib/*.jar
        wrapper.java.classpath.3=/opt/intrexx/lib/remote/*.jar
        wrapper.java.classpath.4=/opt/intrexx/derby/lib/*.jar
        ```
4. Follow the instructions for "Google Drive Datagroup", "Google Drive Filedatagroup", "Google Calendar Datagroup", "Office 365 Datagroup".

## 2. The Connector API Configuration

For a new Connector two configuration files are required. The first one is a template for your connector instances. The second file is the configuration for the connector instance.

Folder path for the configuration template file:

```bash
<INTREXX-PORTAL>/internal/cfg/odata/connector/template
```

Folder path for the configuration file:

```bash
<INTREXX-PORTAL>/internal/cfg/odata/connector
```

### 2.1 Connector API configuration template

```XML
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<connector>
    <guid>18695048E7C7606440B997FD6BCCFF4305E09554</guid>
    <name>testconnector</name>
    <description>Configuration template for connector showcases.</description>
    <connectorId>testconnector</connectorId>
    <properties>
        <property name="serviceTimeout" value="0" description=""/>
    </properties>
    <userMappings></userMappings>
    <metaData>
        <dataGroups>
        </dataGroups>
        <fileFields></fileFields>
    </metaData>
</connector>
```

## 3 Google Drive Datagroup Example

This chapter describes how Intrexx can access your Google Drive Account. The connector consumes the Google Drive REST API via HTTP.

### 3.1 Create OAuth2 credentials for your Google account

For authentication with Google the OAuth2 protocol is used. So we have to register Intrexx with your Google Account as an application.

1. Go to:
    ```URL
    https://console.developers.google.com/apis/
    ```
2. Open Credentials
3. Click "Add Credentials"
4. Click "OAuth-Client-ID"
5. Select "Web Application"
    * Redirect-URL: ```https://<yourPortalURL>/oauth2```
6. Click Next
7. Save client-ID and client secret.
8. Create a Google Drive Connector configuration

### 3.2 Google Drive Connector API configuration

Create an new XML file with the following xml content.
Replace:

* ```####YOUR_CLIENT_ID####``` with the Google OAuth2 client ID
* ```####YOUR_CLIENT_SECRET####``` with the Google OAuth2 client secret
* ```####ID_OF YOUR_FOLDER####``` with the item id of the Google Drive Folder***

*folder id*
To get the item ID of a folder you can use this website: []("https://developers.google.com/drive/api/v3/reference/files/list)

There you can execute REST requests using your Google account. The JSON response contains all files and folders as items. Search for your folder and add the "id" to your configuration.

### 3.3 The configuration File `<IntrexxPortal>/intrenal/cfg/odata/connector`

The file must begin with the template name and ends with the id of the connector.

**Pattern:** `<TemplateName>_<ConnectorId>.xml`

**Example:** `testconnector_googledrive.xml`

**Description of the configuration file:**

* guid: A random GUID as identifier for your connector.
* connectorId: Unique identifier for your connector (as defined in the template).
* properties: Expert settings the implementation can access.
    ** oauth2.*: Information needed for authentication with the Google Rest API
    ** guid: A GUID for the connector configuration as identifier
* datagroups: Defines the datagroups the connector will offer
    ** datagroup: Define the datagroup for the connector
        *** fields: Define the fields the datagroup will offer
            **** field: Define the data fields of the datagroup

    ** settings: define settings for the datagroup
    *** connector.dataGroup.adapter.class: Define the class implementing the IConnectorDataGroupAdapter interface.

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<connector>
    <guid>58695048E747656140B917FD6BCCFF4305E09999</guid>
    <name>googledrive</name>
    <description>Configuration for the Google Drive connector.</description>
    <connectorId>googledrive</connectorId>
    <properties>
        <property name="serviceTimeout" value="0" description=""/>
        <property name="serviceRootURI" value="https://www.googleapis.com" description=""/>
        <property name="useSSL" value="true" description=""/>
        <property name="oauth2.grantType" value="authorization_code" description=""/>
        <property name="oauth2.accessTokenUri" value="https://accounts.google.com/o/oauth2/token" description=""/>
        <property name="oauth2.userAuthorizationUri" value="https://accounts.google.com/o/oauth2/auth" description=""/>
        <property name="oauth2.clientId.source" value="STATIC" description=""/>
        <property name="oauth2.clientId" value="####YOUR_CLIENT_ID####" description=""/>
        <property name="oauth2.clientSecret.source" value="STATIC" description=""/>
        <property name="oauth2.clientSecret" value="####YOUR_CLIENT_SECRET####" description=""/>
        <property name="oauth2.scope.source" value="STATIC" description=""/>
        <property name="oauth2.scope" value="https://www.googleapis.com/auth/drive" description=""/>
        <property name="oauth2.redirectUri" value="http://localhost:1337/service/oauth2/authorize" description=""/>
        <property name="oauth2.clientAuthenticationScheme" value="form" description=""/>
        <property name="oauth2.authenticationScheme" value="header" description=""/>

        <property name="guid" value="testconnector_googledrive" description=""/>
    </properties>
    <metaData>
        <dataGroups>
            <dataGroup name="GoogleDrive">
                <fields>
                    <field name="id" typeName="string" primaryKey="true">
                        <settings/>
                    </field>
                    <field name="name" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="thumbnailLink" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="kind" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="mimeType" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="size" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="description" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="webViewLink" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="createdTime" typeName="datetime" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="modifiedTime" typeName="datetime" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="fileupload" typeName="file" primaryKey="false">
                        <settings/>
                    </field>
                </fields>
                <settings>
                    <setting key="connector.dataGroup.adapter.class" value="de.uplanet.lucy.connectorapi.examples.google.drive.datagroup.GoogleDriveDataGroupAdapter"/>
                    <setting key="connector.google.drive.parentId" value="####ID_OF YOUR_FOLDER####"/>
                </settings>
            </dataGroup>
        </dataGroups>
        <fileFields>
            <fileField>
                <name>fileupload</name>
                <settings>
                    <setting key="connector.file.adapter.class" value="de.uplanet.lucy.connectorapi.examples.google.drive.file.GoogleDriveFileAdapter"/>
                    <setting key="connector.google.drive.parentId" value="####ID_OF YOUR_FOLDER####"/>
                </settings>
            </fileField>
        </fileFields>
    </metaData>
    <userMappings/>
</connector>
```

### 3.4 Create Google Drive app - foreign datagoup

Before the Connector API configuration is available, the portal server must be restarted.

1. Create an empty application.
2. Delete the datagroup.
3. Add an external data group.
4. Choose testconnector_googledrive as database connection.
5. Datahandler should be "Connector Handler - Datahandler", if not please check the "Troubleshooting - Missing Connector BIA-CFG" chapter.
6. Search for a table with "*".
7. Choose CONNECTOR_API.GoogleDrive.
8. Switch to data field tab.
9. Choose the data field you need.
    * The id is a primary key field and required
10. Add a viewtable to your page.
11. Add the fields to your table.
    * ThumbnailLink: change control type to image url.
    * WebviewLink: change to URL.
    * Don't add the fileupload field. It is only requierd for the upload page.
12. Save the application.
13. Now the application can display your files from Google Drive.

### 3.5 Create Google Drive App - Fileupload

1. Open your Google Drive App.
2. Add under your Google Drive external data group an edit page (you can use the page wizard).
   * Add only the fields "id", "name", "description" and "fileupload".
3. Save the application, after save move the field "id" into the "hidden area".
4. Go into Google Drive external data group and copy the GUID from name data field.
5. Go into Google Drive external data group and open the expert settings of the file upload data field.
    * Add "Name GUID" expert setting:
        * Key = `connector.google.drive.name.guid`
        * Value = GUID from "name" data field.
    * Add "Item ID GUID" expert setting:
        * Key = `google.drive.item.id`
        * Value = GUID from "id" data field.
    * Add "ParentId" expert flag:
        * Key: `connector.google.drive.parentId`
        * Value = ID of your Google Drive Folder.
6. Open the settings of the "Save Button" and edit the "Link destination" in the action tab.
    * Select as destination the page with the view table.
7. Add the edit page to your application menu.
    1. Open application -> properties
    2. Application menu tab
    3. Switch "Edit page" from available pages to selected pages.
8. Save your application.

### 3.6 Create Google Drive App - Edit/Delete Page

For editing or deleting records, use the upload page.

1. Open properties of view table.
2. Add a button.
3. Open action tab and select your upload page as destination.
4. Save your application.

## 4. Example Google Drive Calendar

In this Chapter we will explain how Intrexx can access your Google Calendar account.
The Connector API consumes the Google Calendar API.

### 4.1 Create OAuth2 credentials in your Google Account

See chapter 3.1.

### 4.2 Google Calendar Connector API configuration

Create an new XML file with the following xml content.
Replace:

* ```####YOUR_CLIENT_ID####``` with the Google OAuth2 Client Id
* ```####YOUR_CLIENT_SECRET####``` with the Google OAuth2 Client Secret

```XML
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<connector>
    <guid>58695048E7C7656440B997FD6BCCFF4305E09999</guid>
    <name>googlecalendar</name>
    <description>Configuration for google calendar.</description>
    <connectorId>testconnector</connectorId>
    <properties>
        <property name="serviceTimeout" value="0" description=""/>
        <property name="serviceRootURI" value="https://www.googleapis.com" description=""/>
        <property name="useSSL" value="true" description=""/>
        <property name="oauth2.grantType" value="authorization_code" description=""/>
        <property name="oauth2.accessTokenUri" value="https://accounts.google.com/o/oauth2/token" description=""/>
        <property name="oauth2.userAuthorizationUri" value="https://accounts.google.com/o/oauth2/auth" description=""/>
        <property name="oauth2.clientId.source" value="STATIC" description=""/>
        <property name="oauth2.clientId" value="###YOUR CLIENT ID###" description=""/>
        <property name="oauth2.clientSecret.source" value="STATIC" description=""/>
        <property name="oauth2.clientSecret" value="###YOUR CLIENT SECRET###" description=""/>
        <property name="oauth2.scope.source" value="STATIC" description=""/>
        <property name="oauth2.scope" value="https://www.googleapis.com/auth/calendar" description=""/>
        <property name="oauth2.redirectUri" value="http://localhost:1337/service/oauth2/authorize" description=""/>
        <property name="oauth2.clientAuthenticationScheme" value="form" description=""/>
        <property name="oauth2.authenticationScheme" value="header" description=""/>
        <property name="guid" value="testconnector_googlecalendar" description=""/>
    </properties>
    <metaData>
        <dataGroups>
            <dataGroup name="GoogleEvents">
                <fields>
                    <field name="ID" typeName="string" primaryKey="true">
                        <settings/>
                    </field>
                    <field name="Subject" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="WebUrl" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="body" typeName="string" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="startDate" typeName="datetime" primaryKey="false">
                        <settings/>
                    </field>
                    <field name="endDate" typeName="datetime" primaryKey="false">
                        <settings/>
                    </field>
                </fields>
                <settings>
                    <setting key="connector.dataGroup.adapter.class" value="de.uplanet.lucy.connectorapi.examples.google.calendar.GoogleCalendarEventDataGroupAdapter"/>
                </settings>
            </dataGroup>
        </dataGroups>
        <fileFields>
        </fileFields>
    </metaData>
    <userMappings/>
</connector>
```

### 4.3 Create Google Calendar app - View your events

Before the application can use the Google Calendar connector, we need to restart the portal server.

1. Create an empty application.
2. Delete datagroup.
3. Create external data group.
4. Select testconnector_googlecalendar as connection.
5. Datahandler should be "Connector Handler - Datahandler", if not please check the "Troubleshooting - Missing Connector BIA-CFG" chapter.
6. Search for Table with "*".
7. Choose CONNECTOR_API.CONNECTOR_API.GoogleEvents.
8. Switch to datafield tab.
9. Choose all data fields.
10. Open expert tab.
    * Add setting:
        * Key: `connector.google.calendar.id`
        * Value: `primary` (or add your calendar id)
11. Add a calendar control on your page.
12. Open the calendar.
13. Add new plugin.
    * Select your calendar data group.
    * Add an additional field for the id field.
14. Save the application.
15. Now the app should show your google events in the browser.

### 4.4 Create Google Calendar app - Create/modify events

1. Create under your external data group a new edit page.
    * Use the page wizard.
    * Select all data fields excluding WebUrl.
    * Move ID to the hidden area.
2. Go to save button and change link destination to your calendar page.
3. Go to your calender view -> open your plugin.
    * Open tab "Link destination".
    * Select in "Link on click": edit page.
4. Open "Actions" tab of calendar view.
    * Edit highlight action.
    * Jump: To a page of an application.
    * Application: Current application.
    * Select your edit page.
5. Save the application.

Now you can create and edit events in your Google calendar.

## 5. Troubleshooting

### 5.1 Missing Connector BIA-CFG

If the datahander field in the foreign datagroup configuration dialog is empty then the `bia-config.cfg` file is missing.

1. Proove that the Portal version is at least 18.03 OU04.
2. Go to `<INTREXX-INSTALL>/orgtempl/blank/internal/cfg/biaconfig` and copy the `bia_connector.cfg` file.
3. Paste the `bia_connector.cfg` into your portal's `<INTREXX-PORTAL>/internal/cfg/biaconfig` folder.
4. Restart the portal server.

### 5.2 New Redirect URL since version 18.09

Change the redirect URL from ```http://<portal>/oauth2``` to ```http://<portal>/service/oauth2/authorize```.

Example:<br>
v18.03: ```http://localhost:8082/test/oauth2```<br>
v18.09: ```http://localhost:1337/service/oauth2/authorize```