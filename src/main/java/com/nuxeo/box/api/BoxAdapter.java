/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package com.nuxeo.box.api;

import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxObject;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.dao.BoxUser;
import com.box.boxjavalibv2.exceptions.BoxJSONException;
import com.box.boxjavalibv2.jsonparsing.BoxJSONParser;
import com.box.boxjavalibv2.jsonparsing.BoxResourceHub;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.quota.size.QuotaAware;
import org.nuxeo.ecm.quota.size.QuotaAwareDocument;
import org.nuxeo.runtime.api.Framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract Box Adapter
 *
 * @since 5.9.2
 */
public abstract class BoxAdapter {

    protected final DocumentModel doc;

    protected final Map<String, Object> boxProperties = new HashMap<>();

    public BoxAdapter(DocumentModel doc) throws ClientException {
        this.doc = doc;
        CoreSession session = doc.getCoreSession();
        boxProperties.put(BoxItem.FIELD_TYPE, doc.getType());
        boxProperties.put(BoxItem.FIELD_ID, doc.getId());

        // Etag / Sequence ID
        boxProperties.put(BoxItem.FIELD_SEQUENCE_ID, doc.getCacheKey());
        boxProperties.put(BoxItem.FIELD_ETAG, doc.getCacheKey() + doc
                .getVersionLabel());

        boxProperties.put(BoxItem.FIELD_NAME, doc.getName());
        boxProperties.put(BoxItem.FIELD_CREATED_AT,
                ISODateTimeFormat.dateTime().print(
                        new DateTime(doc.getPropertyValue("dc:created"))));
        boxProperties.put(BoxItem.FIELD_MODIFIED_AT,
                ISODateTimeFormat.dateTime().print(
                        new DateTime(doc.getPropertyValue("dc:modified"))));
        boxProperties.put(BoxItem.FIELD_DESCRIPTION,
                doc.getPropertyValue("dc:description"));

        // size
        final QuotaAwareDocument quotaAwareDocument = (QuotaAwareDocument)
                doc.getAdapter(QuotaAware.class);
        boxProperties.put(BoxItem.FIELD_SIZE, quotaAwareDocument != null ?
                quotaAwareDocument.getInnerSize() : -1.0);

        // path_collection
        final DocumentModel parentDoc = session.getDocument(doc.getParentRef());
        final Map<String, Object> pathCollection = new HashMap<>();
        pathCollection.put(BoxCollection.FIELD_TOTAL_COUNT,
                doc.getPathAsString().split("\\\\").length);
        pathCollection.put(BoxCollection.FIELD_ENTRIES,
                getParentsHierarchy(session, parentDoc));
        BoxCollection boxPathCollection = new BoxCollection(Collections
                .unmodifiableMap(pathCollection));
        boxProperties.put(BoxItem.FIELD_PATH_COLLECTION, boxPathCollection);

        // parent
        final Map<String, Object> parentProperties = new HashMap<>();
        parentProperties.put(BoxItem.FIELD_ID, parentDoc.getId());
        parentProperties.put(BoxItem.FIELD_TYPE, parentDoc.getType());
        parentProperties.put(BoxItem.FIELD_NAME, parentDoc.getName());
        parentProperties.put(BoxItem.FIELD_SEQUENCE_ID, "-1");
        parentProperties.put(BoxItem.FIELD_ETAG, "-1");
        BoxFolder parentFolder = new BoxFolder(Collections.unmodifiableMap
                (parentProperties));
        boxProperties.put(BoxItem.FIELD_PARENT, parentFolder);

        // Users
        // Creator
        final UserManager userManager = Framework.getLocalService(UserManager
                .class);
        final NuxeoPrincipal creator = userManager.getPrincipal((String) doc
                .getPropertyValue("dc:creator"));
        final BoxUser boxCreator = fillUser(creator);
        boxProperties.put(BoxItem.FIELD_CREATED_BY, boxCreator);

        //Last Contributor
        final NuxeoPrincipal lastContributor = userManager.getPrincipal(
                (String) doc.getPropertyValue("dc:lastContributor"));
        final BoxUser boxContributor = fillUser(lastContributor);
        boxProperties.put(BoxItem.FIELD_MODIFIED_BY, boxContributor);

        // Owner
        boxProperties.put(BoxItem.FIELD_OWNED_BY, boxCreator);

        // Shared Link
        boxProperties.put(BoxItem.FIELD_SHARED_LINK, null);

        // Status
        boxProperties.put(BoxItem.FIELD_ITEM_STATUS,
                doc.getCurrentLifeCycleState());

        // Tags
        boxProperties.put(BoxItem.FIELD_TAGS, getTags(session));

    }

    protected List<BoxTypedObject> getParentsHierarchy(CoreSession session,
            DocumentModel parentDoc) throws ClientException {
        final List<BoxTypedObject> pathCollection = new ArrayList<>();
        while (parentDoc != null) {
            final Map<String, Object> parentCollectionProperties = new
                    HashMap<>();
            parentCollectionProperties.put(BoxItem.FIELD_TYPE,
                    parentDoc.getType());
            parentCollectionProperties.put(BoxItem.FIELD_ID, parentDoc.getId());
            parentCollectionProperties.put(BoxItem.FIELD_SEQUENCE_ID, "-1");
            parentCollectionProperties.put(BoxItem.FIELD_ETAG, "-1");
            parentCollectionProperties.put(BoxItem.FIELD_NAME,
                    parentDoc.getName());
            pathCollection.add(new BoxTypedObject(Collections.unmodifiableMap
                    (parentCollectionProperties)));
            parentDoc = session.getParentDocument(parentDoc.getParentRef());
        }
        return pathCollection;
    }

    protected String[] getTags(CoreSession session) throws ClientException {
        final TagService tagService = Framework.getLocalService(TagService
                .class);
        final List<Tag> tags = tagService.getDocumentTags(session,
                doc.getId(), session.getPrincipal().getName());
        final String[] tagNames = new String[tags.size()];
        int index = 0;
        for (Tag tag : tags) {
            tagNames[index] = tag.getLabel();
            index++;
        }
        return tagNames;
    }

    /**
     * Fill box object user
     *
     * @param creator
     * @return a box User
     */
    protected BoxUser fillUser(NuxeoPrincipal creator) {
        final Map<String, Object> mapUser = new HashMap<>();
        mapUser.put("type", "user");
        mapUser.put("id", creator.getPrincipalId());
        mapUser.put("name", creator.getFirstName() + " " + creator
                .getLastName());
        mapUser.put("login", creator.getName());
        return new BoxUser(Collections.unmodifiableMap(mapUser));
    }

    public void setTitle(String value) throws ClientException {
        doc.setPropertyValue("dc:title", value);
    }

    public void setDescription(String value) throws ClientException {
        doc.setPropertyValue("dc:description", value);
    }

    public void setCreator(String value) throws ClientException {
        doc.setPropertyValue("dc:creator", value);
    }

    public String toJSONString(BoxObject boxObject) throws BoxJSONException {
        BoxJSONParser boxJSONParser = new BoxJSONParser(new
                BoxResourceHub());
        return boxObject.toJSONString(boxJSONParser);
    }
}