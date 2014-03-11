#Nuxeo Box API
---
##General Information

The **Nuxeo** addon _nuxeo-box-api_ provides REST API endpoints [Box](http://www.box.com) compliant.

### Getting Started

- [Download a Nuxeo server](http://www.nuxeo.com/en/downloads)

- Unzip

- Issue the following commands from root:
  - `chmod +x bin/*ctl bin/*.sh`
  - `./bin/nuxeoctl mp-init`
  - `./bin/nuxeoctl mp-install nuxeo-box-api`
  - `./bin/nuxeoctl start`


###REST API Compatibility Matrix

Box Features | Nuxeo Compatibility
------------ | -------------
[**CRUD Folders**](https://developers.box.com/docs/#folders)| Yes
[**CRUD Files**](https://developers.box.com/docs/#files)| Yes
[**Comments**](https://developers.box.com/docs/#comments)| Yes
[**Collaborations**](https://developers.box.com/docs/#collaborations)| Yes
[**Search**](https://developers.box.com/docs/#search)| Yes
[**Events**](https://developers.box.com/docs/#events)| Not yet
[**Shared Items**](https://developers.box.com/docs/#shared-items)| Not yet
[**Users**](https://developers.box.com/docs/#users)| Not yet
[**Groups**](https://developers.box.com/docs/#groups)| Not yet
[**Tasks**](https://developers.box.com/docs/#tasks)| Not yet

###Report & Contribute

- Ask your questions or report issues on [Nuxeo Answers](http://answers.nuxeo.com)
- [Nuxeo JIRA Issue Management](https://jira.nuxeo.com/browse/NXP)
- See [Contributing to Nuxeo](http://doc.nuxeo.com/x/VIZH)

##Sample usage

###Java Box SDK usage with Nuxeo Box API

The [Box Java SDK](https://github.com/box/box-java-sdk-v2) can be used to browse Nuxeo repositories.

Initialize the client with the following lines to make it point to your Nuxeo server:

        import com.box.boxjavalibv2.BoxClient;
        import com.box.boxjavalibv2.BoxConfig;

        static final String PROTOCOL = "http";
        static final String SERVER_URL = "localhost:8080";

        BoxConfig boxConfig = BoxConfig.getInstance();

        boxConfig.setAuthUrlScheme(PROTOCOL); // Default value is "https"
        boxConfig.setOAuthUrlAuthority(SERVER_URL);
        boxConfig.setOAuthApiUrlPath("/nuxeo");

        boxConfig.setApiUrlScheme(PROTOCOL); // Default value is "https"
        boxConfig.setApiUrlAuthority(SERVER_URL);
        boxConfig.setApiUrlPath("/nuxeo/box/2.0");

        boxConfig.setUploadUrlScheme(PROTOCOL); // Default value is "https"
        boxConfig.setUploadUrlAuthority("/nuxeo");
        boxConfig.setUploadUrlPath("/nuxeo/site/box/2.0");



###Box/Nuxeo hybrid client application sample

[This sample application](https://github.com/nuxeo/nuxeo-box-angular-sample) browses Nuxeo and Box repositories with the same [REST API](https://developers.box.com/docs/). Only the base URL should be updated.

Examples:

- Same endpoints (to fetch a folderish document definition) such as:
  - *GET http://server/nuxeo/box/**2.0/folders/{folder_id}***
  - *GET https://api.box.com/**2.0/folders/{folder id}***
- Same JSON payload response such as:
  - <http://developers.box.com/docs/#folders-folder-object>

##About
###Nuxeo

[Nuxeo](http://www.nuxeo.com) provides a modular, extensible Java-based [open source software platform for enterprise content management](http://www.nuxeo.com/en/products/content-management-platform), and packaged applications for [document management](http://www.nuxeo.com/en/products/document-management), [digital asset management](http://www.nuxeo.com/en/products/digital-asset-management), [social collaboration](http://www.nuxeo.com/en/products/social-collaboration) and [case management](http://www.nuxeo.com/en/products/case-management).

Designed by developers for developers, the Nuxeo platform offers a modern architecture, a powerful plug-in model and extensive packaging capabilities for building content applications.

More information on: <http://www.nuxeo.com/>

###Box
Box documentation and account:

- <https://app.box.com/signup/personal/> -> Box Signup
- <https://developers.box.com/docs/> -> API Documentation
