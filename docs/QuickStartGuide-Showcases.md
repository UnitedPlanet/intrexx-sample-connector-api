# Intrexx Connector API - Quick start guide for the Google Drive/Events showcases

## 1. Installation

1. Clone the git repository
2. Build a jar
    * Open a terminal (bash/cmd)
    * CD into the git repository folder connector-api-examples
    * ```gradle clean build jar```
    * The jar will be created in connector-api-examples/build/libs/connector-showcases-8.1.x.jar
3. Add the jar to your Intrexx 18.03 OU4 or 18.09 installation.
    * Copy the jar into this folder: `<INTREXX PORTAL>/lib`
    * Modify the "portal.wcf" file
        * Add the following line under `wrapper.java.classpath.5=/opt/intrexx8100/org/<portalname>/lib/*.jar`
        ```bash
        # Java Classpath (include wrapper.jar)  Add class path elements as
        #  needed starting from 1
        wrapper.java.classpath.1=/opt/intrexx/lib/update
        wrapper.java.classpath.2=/opt/intrexx/lib/*.jar
        wrapper.java.classpath.3=/opt/intrexx/lib/remote/*.jar
        wrapper.java.classpath.4=/opt/intrexx/derby/lib/*.jar
        ```
4. Follow the instructions for "Google Drive data group", "Google Drive file data group", "Google Calendar data group", "Office 365 data group".

## 2. Connector API configuration

Two configuration files are required for a new connector. The first file is a template for your connector instances, the second is the configuration for the connector instance.

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

## 3 Google Drive data group example

This chapter describes how Intrexx can access your Google Drive account. The connector consumes the Google Drive REST API via HTTP.

### 3.1 Create OAuth2 credentials for your Google account

The OAuth2 protocol is used to authenticate with Google. So, we have to register Intrexx as an application in your Google account.

1. Go to:
    ```URL
    https://console.developers.google.com/apis/
    ```
2. Open "Credentials"
3. Click on "Create Credentials"
4. Click on "OAuth client ID"
5. Select "Web application"
    * Redirect-URL: ```https://<yourPortalURL>/oauth2```
6. Click on "Next"
7. Save the client ID and client secret.
8. Create a Google Drive connector configuration

### 3.2 Google Drive connector API configuration

Create a new XML file with the following content.

Replace:

* ```####YOUR_CLIENT_ID####``` with the Google OAuth2 client ID
* ```####YOUR_CLIENT_SECRET####``` with the Google OAuth2 client secret
* ```####ID_OF YOUR_FOLDER####``` with the item ID of the Google Drive folder***

*Folder ID*
The following website helps you get the item ID of a folder: []("https://developers.google.com/drive/api/v3/reference/files/list)

There, you can execute REST requests using your Google account. The JSON response contains all files and folders as items. Search for your folder and add the "ID" to your configuration.

### 3.3 Configuration file `<Intrexx Portal>/intrenal/cfg/odata/connector`

The file must begin with the template name and end with the ID of the connector.

**Pattern:** `<TemplateName>_<ConnectorId>.xml`

**Example:** `testconnector_googledrive.xml`

**Description of the configuration file:**

* guid: A random GUID as an identifier for your connector.
* connectorId: Unique identifier for your connector (as defined in the template).
* properties: Expert settings the implementation can access.
    ** oauth2.*: Information needed for authentication with the Google Rest API
    ** guid: A GUID for the connector configuration as an identifier
* dataGroups: Defines which data groups are available
    ** dataGroup: Define the specific data group(s) for the connector
        *** fields: Defines which fields of the data group are available
            **** field: Defines the specific data fields of the data group

    ** settings: Defines settings for the data group
    *** connector.dataGroup.adapter.class: Defines the class, implementing the "IConnectorDataGroupAdapter" interface.

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
        <property name="oauth2.redirectUri" value="http://localhost:8082/office365/oauth2" description=""/>
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

### 3.4 Create Google Drive app - External data group

Before the Connector API configuration is available, the portal server must be restarted.

1. Create an empty application.
2. Delete the existing data group.
3. Add an external data group.
4. Select "testconnector_googledrive" as the database connection.
5. The "Datahandler" should be "Connector Handler - Datahandler", if not please see "Troubleshooting - Missing connector BIA-CFG".
6. Search for a table with "*".
7. Choose "CONNECTOR_API.GoogleDrive".
8. Go to the "Data fields" tab.
9. Choose the data field(s) you need.
    * The ID is a primary key field and required.
10. Add a view table to your page.
11. Add the field(s) to your table.
    * Thumbnail link: change control type to "Image URL".
    * Webview link: change control type to "URL".
    * Don't add the "fileupload" field. It is only required for the upload page.
12. Save the application.
13. Now the application can display your files from Google Drive.

### 3.5 Create Google Drive App - File upload

1. Open your Google Drive app.
2. Add an edit page (you can use the page wizard) under your Google Drive external data group.
   * Only add the following fields: "id", "name", "description" and "fileupload".
3. Save the application. After saving, move the "id" field to the "Hidden area".
4. Open the properties of the Google Drive external data group and copy the GUID of the "name" data field.
5. Open the properties of the Google Drive external data group and open the expert settings of the "fileupload" data field.
    * Add "Name GUID" expert setting:
        * Key = `connector.google.drive.name.guid`
        * Value = GUID from "name" data field.
    * Add "Item ID GUID" expert setting:
        * Key = `google.drive.item.id`
        * Value = GUID from "id" data field.
    * Add "ParentId" expert flag:
        * Key: `connector.google.drive.parentId`
        * Value = ID of your Google Drive Folder.
6. Open the properties of the "Save" button and edit the "Link destination" on the "Actions" tab.
    * Select the page with the view table as the destination.
7. Add the edit page to your application menu.
    1. Open the application properties
    2. Go to the "Application menu" tab
    3. Move the "Edit page" from available pages to selected pages.
8. Save your application.

### 3.6 Create Google Drive app - Edit/delete page

Use the upload page for editing or deleting records.

1. Open the properties of the view table.
2. Add a button.
3. Go to the "Actions" tab and select your upload page as the destination.
4. Save your application.

## 4. Google Drive Calendar example

In this chapter, we will explain how Intrexx can access your Google Calendar account.
The Connector API consumes the Google Calendar API.

### 4.1 Create OAuth2 credentials in your Google account

See chapter 3.1.

### 4.2 Google Calendar connector API configuration

Create a new XML file with the following content.
Replace:

* ```####YOUR_CLIENT_ID####``` with the Google OAuth2 client ID
* ```####YOUR_CLIENT_SECRET####``` with the Google OAuth2 client secret

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
        <property name="oauth2.redirectUri" value="http://localhost:8082/office365/oauth2" description=""/>
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

Before the application can use the Google Calendar connector, you need to restart the portal server.

1. Create an empty application.
2. Delete the existing data group.
3. Add an external data group.
4. Select "testconnector_googlecalendar" as the database connection.
5. The "Datahandler" should be "Connector Handler - Datahandler", if not please see "Troubleshooting - Missing connector BIA-CFG".
6. Search for tables with "*".
7. Choose "CONNECTOR_API.CONNECTOR_API.GoogleEvents".
8. Go to the "Data fields" tab.
9. Choose all the data fields.
10. Go to the "Expert" tab.
    * Add the following setting:
        * Key: `connector.google.calendar.id`
        * Value: `primary` (or add your calendar id)
11. Add a calendar control to your page.
12. Open the calendar properties.
13. Add a new plugin.
    * Select your calendar data group.
    * Add an additional field for the "id" field.
14. Save the application.
15. Now the app should show your Google events in the browser.

### 4.4 Create Google Calendar app - Create/modify events

1. Create a new edit page under your external data group.
    * Use the page wizard.
    * Select all the data fields excluding "WebUrl".
    * Move "ID" to the hidden area.
2. Open the properties of the "Save" button and change the link destination to your calendar page.
3. Open the calendar properties -> Open the plugin properties.
    * Go to the "Link destination" tab.
    * Select the edit page you just created for "Link on click".
4. Go to the "Actions" tab of the calendar.
    * Edit the "Highlight action".
    * Jump: To a page of an application.
    * Application: Current application.
    * Select your edit page.
5. Save the application.

Now you can create and edit events in your Google calendar.

## 5. Troubleshooting

### 5.1 Missing connector BIA-CFG

If the "Datahandler" field in the external data group configuration dialog is blank then the `bia-config.cfg` file is missing.

1. Check that your Portal Manager version is at least 18.03 OU04.
2. Go to `<INTREXX-INSTALL>/orgtempl/blank/internal/cfg/biaconfig` and copy the `bia_connector.cfg` file.
3. Paste the `bia_connector.cfg` into your portal's `<INTREXX PORTAL>/internal/cfg/biaconfig` folder.
4. Restart the portal server.
