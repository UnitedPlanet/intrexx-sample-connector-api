# Intrexx Connector API - Quick start guide

## Introduction

The Intrexx Connector API defines a Java API to implement custom Intrexx connectors that provide external data in Intrexx via an external data group. In principle, the API specifies two Java interfaces, which should be implemented by developers to create, read, update and delete data records and files in the external system (so-called CRUD actions). Furthermore, a metadata model is stored in the connector configuration file. This describes the structure of the data groups (fields, relationships, settings) that are provided by the connector implementation. At runtime, the Connector API provides preconfigured clients for HTTP and OData requests, which automatically perform a user authentication - if it is required by the service - via HTTP Basic Auth or OAuth2. Otherwise, any Java library (e.g. JDBC, REST APIs etc.) can be used in custom code.

To make a quick start into development with the connector, the following provides a description of example implementations that demonstrate the use of the API for integrating external systems in Intrexx.

## Configure the development environment

### Requirements

- Intrexx 19.09, 19.03 and 18.03 OU05
- Java JDK 1.8 for Intrexx 18.03 or OpenJDK 11 for Intrexx 19.03/19.09.
- Optionally, a Java IDE such as IntelliJ IDEA or Eclipse is recommended for developing custom connectors.

### Create project folder

Either clone or download this repostiroy (outside of Intrexx). In the following it is assumed that the project folder is called `intrexx-sample-connector-api`.

### Project dependencies

To be able to compile the source code, some Intrexx and external libraries are required. These are listed in the Gradle project file `build.gradle` and are normally downloaded automatically. If it is not possible to connect to the United Planet Maven Repository, the required JAR files can be copied from the `lib` of the Intrexx installation to the `lib` folder of the project.

### Compile project

The project is managed and compiled with [Gradle](http://gradle.org). The following commands can be performed directly in the main folder of the project:

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

Start Eclipse and check that the Gradle plugin is installed. Afterwards, import the project file to the workspace via `File -> Import -> Gradle`. The project should then be available in the Package Explorer.

#### IntelliJ

IntelliJ inherently provides support for Gradle projects. The project can easily be imported via the Open dialog by selecting the project directory.

### Start the portal server in debug mode

To be able to test and debug custom Java code in an Intrexx portal at runtime, the portal server can be executed in the debug mode in Eclipse/IntelliJ. To do this, Intrexx and the Intrexx portal need to be installed on the same local PC as the development environment, and the portal server service needs to have been stopped. How the run configuration for the portal server can be set up in Eclipse is described below - the same specifications can be made in IntelliJ:

- Create a new Gradle run configurationd 'Portalserver' via `Run -> Run Configurations`.
- Choose the Gradle task `startPortal`.
- Edit the variables for the portal server paths in `settings.gradle` to match your local installation.

The portal server can now be started via Run/Debug in Eclipse/IntelliJ and breakpoints can be set in custom code. If a breakpoint is reached at runtime, the debugger is activated as of this point.

To start the portal server from the command line:

Windows:

```bash
gradlew.bat startPortal
```

Linux/MacOS

```bash
./gradlew startPortal
```

## Example connector implementations

The project contains three examples for accessing

- Google Calendar and Google Drive
- MS Office365 appointments
- SonarQube

The examples for Google and Office365 require an OAuth2 configuration for authenticating to the service in the Intrexx portal. A description of how this can be configured is available in the [Intrexx Online Help](https://onlinehelp.unitedplanet.com/intrexx/9200/de/index.html?p=helpfiles/help.2.connectoren-office-365.html).

## Create a custom connector project

Custom connectors can be developed directly within the example project or you can create a copy of the project folder, delete the Java source code under /src/main/java and create custom classes instead. The following provides a step-by-step description of how to create a custom connector.

*Please note*: If you are developing on the basis of an existing portal, you need to check whether both of these files are already in the portal directory. Otherwise, these need to be copied from the respective folder under `<INTREXX_HOME>/orgtempl/...` to all portals.

- `org\<portal>\internal\cfg\biaconfig\bia-connector.cfg`
- `org\<portal>\internal\cfg\odata\connector\template\msoffice365.xml`

### InMemory connector example

The InMemory connector example provides access to an internal data structure via an Intrexx external data group. In this case, all CRUD actions as well as sorting under pagination are supported.

#### Connector configuration

New connectors define a template file under `org\<portal>\internal\cfg\odata\connector\template\`. Create a new file called 'inmemory.xml' in the folder and copy the content below into the file:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<connector>
    <guid></guid>
    <name>InMemory</name>
    <description>Configuration template for the InMemory test connector.</description>
    <connectorId>inmemory</connectorId>
    <properties>
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
                    <field name="Integer" typeName="integer" primaryKey="false" />
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

The most important section in the connector template is under `<metaData>`. The data groups, fields and expert settings, which the connector provides for external data groups in Intrexx, are defined here. Any number of data groups and fields can be defined here. Additionally, file fields for uploading/downloading files can be defined here, but this won't be looked at here as this would exceed the purpose of this quick start guide.

In this example, a data group is created with 7 fields for each of the possible Intrexx data types. In addition, you can also specify whether a field is part of the primary key.

Based on this template, specific connector configurations can now be registered in Intrexx. This means it is possible to create different services with the same metadata but different configurations. Copy the file to `org\<portal>\internal\cfg\odata\connector` and rename it to `inmemory_test.xml`. The first part of the name before the underscore refers to the connector ID (see above `<connectorId>`), the second part is the name of the specific configuration (`test` in this case). Open the file in an editor of your choice and add the following to `<guid>` and `<name>` as follows (GUID must be unique in all files):

```xml
<guid>F27302911D5DA4E8FE7A500D3F4E1E699BDF0592</guid>
<name>test</name>
...
<settings>
        <setting key="connector.cfg.guid" value="inmemory_test"/>
...
```

Afterwards, restart the portal service.

#### Intrexx application

Next, you can already create an application in Intrexx that uses this connector.

- Start by creating a new 'Empty Application' in the Applications module and delete the 'Data Group' data group.
- Create a new external data group called 'InMemory'.
- Under 'Database connection', select the entry `inmemory_test`. 'Connector API - Datahandler' should now appear under 'Data handler'.
- Click on the magnifying glass symbol next to the 'Table' field and search for all tables with '*'. A table called `CONNECTOR_API.MyDataGroup` should appear. Select this table.
- Now, click on the 'Data fields' tab and click on the `+` symbol. Select every field in the subsequent dialog.
- Next, switch to the 'Expert' tab and then to 'Settings'. Create the following two settings:

```properties
connector.cfg.guid = inmemory_test
connector.dataGroup.adapter.class =	de.uplanet.lucy.connectorapi.examples.simple.InMemoryDataGroupConnector
```

- Close the data group dialog by clicking on 'OK'.
- Now, a view table can be created on the view page of the application. This table should contain all fields of the data group.
- Publish the application and open it in the portal. An error will occur because the adapter class is not yet found.

*Please note*: The app is also available as an import package in the project folder under `docs/InMemoryConnector.lax`, this can be imported directly into Intrexx.

#### Integrate the project in Intrexx

So that custom connector classes are found in the portal server at runtime, the JAR file of the project needs to be included in the Intrexx classpath. The easiest way to achieve this is if the file `build\libs\connector-examples-9.1.5.jar` is copied to `<INTREXX_HOME>\lib` directly after the call of `gradlew.bat jar`. The portal server needs to be restarted afterwards. After the restart, three data records should be listed in the table when you open the application in the browser. These can be modified or deleted, or new records can be created.

#### Implement connector classes

The logic for the data access is implemented in the class `de.uplanet.lucy.connectorapi.examples.simple.InMemoryDataGroupConnector`. Here, the data is determined based on the transferred request parameters and converted into the Connector API data structures or for inserts/updates, the values from the Intrexx objects are saved in the table.

## Advanced connector examples

Additional examples, which demonstrate access to REST web services via HTTP and OData, are available in the Java packages `de.uplanet.lucy.connectorapi.examples`. Intrexx filter, sorting and pagination functions are also used here. More detailed documentation for this can be found in the subdirectory `docs`.

## Packaging and deployment

In order to package and deploy your connector app to an Intrexx portal, follow these steps:

1. There is a Gradle task `deployToPortal`, which when executed, will build and bundle all dependencies and your connector jar file and copies all files to the portal's lib folder. Please check the path to your portal folder in settings.gradle before executing the task.

```shell
./gradlew build
./gradlew :deployToPortal
```

2. In order to include your jar files in the portal's Java classpath, the `internal/cfg/portal.wcf` file must be edited accordingly:

```properties
# Java Classpath (include wrapper.jar)  Add class path elements as
#  needed starting from 1
wrapper.java.classpath.1=C:\intrexx\lib\update
wrapper.java.classpath.2=C:\intrexx\lib\*.jar
wrapper.java.classpath.3=C:\intrexx\lib\remote\*.jar
wrapper.java.classpath.4=C:\intrexx\org\portal\lib\*.jar
```

3. Copy the connector template you created in your development portal to the target portal.

- Template file: `<INTREXX_PORTAL>/cfg/odata/connector/template/<YOUR_TEMPLATE_NAME>`
- Optional concrete connector configuration file: `<INTREXX_PORTAL>/cfg/odata/connector/<YOUR_TEMPLATE_NAME>_<YOUR_CONNECTOR_CFG_NAME>.xml`

4. Restart the target portal service and test/create a connector configuration based on your template.

5. Import your application in the target portal and test it with your connector implementation.

## Java API documentation

The Java API documentation of the Intrexx Connector API can be found in the `docs/api` directory of the project. All of the most important classes and interfaces are described there. Furthermore, all context objects (session, request etc.) are available at runtime just like in Intrexx Groovy scripts.

### Java interfaces and classes of the Connector API

The public interfaces and classes of the Connector API can be found in the package `de.uplanet.lucy.server.odata.connector.api.v1`. The following describes the methods to be implemented for custom connectors and the utility classes provided based on the InMemory example.

#### Interface `de.uplanet.lucy.server.odata.connector.api.v1.IConnectorDataGroupAdapter`

This interface defines the methods required for create/read/update/delete actions in Intrexx external data groups.

- `IConnectorQueryResult queryDataRange(IConnectorQueryCriteria p_criteria)`
    This method is called by Intrexx when data records should be loaded for an external data group. Information required for selecting the data records such as filter, sorting, pagination are transferred via the `IConnectorQueryCriteria p_criteria` argument.
- `IConnectorRecord queryDataRecord(String p_strRecordId, List<IConnectorField> p_fields)`
    This method is called by Intrexx when a single data record should be loaded for an external data group. The ID of the data record to be loaded is transferred via the `String p_strRecId` argument, while `List<IConnectorField> p_fields` contains the data record fields to be selected.
- `String insert(IConnectorRecord p_record)`
    This method serves to create a new data record. The `IConnectorRecord p_record` argument contains the data record ID as well as the field values.
- `boolean update(IConnectorRecord p_record)`
    This method serves to edit an existing data record. `IConnectorRecord p_record` argument contains the data record ID as well as the field values.
- `void delete(IConnectorRecord p_record)`
    This method serves to delete an existing data record. `IConnectorRecord p_record` argument contains the data record ID as well as the field values.

#### Base class `de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorDataGroupAdapter`

All specific IConnectorDataGroupAdapter implementations should inherit from this abstract class. The class provides a range of help methods and context objects.

#### Interface `de.uplanet.lucy.server.odata.connector.api.v1.IConnectorFileAdapter`

This interface defines the actions required for create/read/update/delete actions of Intrexx file fields. These can belong to an Intrexx data group or an external data group.

#### Base class `de.uplanet.lucy.server.odata.connector.api.v1.AbstractConnectorFileAdapter`

All specific IConnectorFileAdapter implementations should inherit from this abstract class. The class provides a range of help methods and context objects.