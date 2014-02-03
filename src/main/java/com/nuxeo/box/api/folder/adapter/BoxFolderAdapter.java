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
package com.nuxeo.box.api.folder.adapter;

import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxEmail;
import com.box.boxjavalibv2.dao.BoxFolder;
import com.box.boxjavalibv2.dao.BoxItem;
import com.box.boxjavalibv2.dao.BoxTypedObject;
import com.box.boxjavalibv2.exceptions.BoxJSONException;
import com.nuxeo.box.api.BoxAdapter;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.runtime.api.Framework;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Box Folder Adapter
 *
 * @since 5.9.2
 */
public class BoxFolderAdapter extends BoxAdapter {

    protected BoxFolder boxFolder;

    /**
     * Instantiate the adapter and the Box Folder from Nuxeo Document and
     * load its properties into json format
     */
    public BoxFolderAdapter(DocumentModel doc) throws ClientException {
        super(doc);
        CoreSession session = doc.getCoreSession();

        // Email update
        final Map<String, Object> boxEmailProperties = new HashMap<>();
        boxEmailProperties.put(BoxEmail.FIELD_ACCESS, "-1");
        boxEmailProperties.put(BoxEmail.FIELD_EMAIL, "-1");
        final BoxEmail boxEmail = new BoxEmail(Collections.unmodifiableMap
                (boxEmailProperties));
        boxProperties.put(BoxFolder.FIELD_FOLDER_UPLOAD_EMAIL, boxEmail);

        // Children
        boxProperties.put(BoxFolder.FIELD_ITEM_COLLECTION,
                getItemCollection(session, "100", "0", "*"));

        boxFolder = new BoxFolder(Collections.unmodifiableMap(boxProperties));
    }

    public BoxFolder getBoxFolder() {
        return boxFolder;
    }

    public void setBoxFolder(BoxFolder boxFolder) {
        this.boxFolder = boxFolder;
    }

    /**
     * Fill item collection entries box object
     *
     * @return the list of children in item collection
     */
    public BoxCollection getItemCollection(CoreSession session,
            String limit, String offset, String fields) throws
            ClientException {
        final Map<String, Object> boxItemCollectionProperties = new HashMap<>();

        // Fetch items
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM Document WHERE ecm:parentId=");
        query.append("'" + doc.getId() + "'");
        DocumentModelList children = session.query(query.toString(),
                null, Long.parseLong(limit), Long.parseLong(offset), false);
        boxItemCollectionProperties.put(BoxCollection.FIELD_TOTAL_COUNT,
                children.size());
        final List<BoxTypedObject> boxTypedObjects = new ArrayList<>();
        for (DocumentModel child : children) {
            final Map<String, Object> childrenProperties = new HashMap<>();
            childrenProperties.put(BoxTypedObject.FIELD_TYPE, child.getType());
            childrenProperties.put(BoxTypedObject.FIELD_ID, child.getId());
            childrenProperties.put(BoxTypedObject.FIELD_CREATED_AT,
                    ISODateTimeFormat.dateTime().print(
                            new DateTime(child.getPropertyValue("dc:created")
                            )));
            childrenProperties.put(BoxItem.FIELD_MODIFIED_AT,
                    ISODateTimeFormat.dateTime().print(
                            new DateTime(child.getPropertyValue
                                    ("dc:modified"))));
            boxTypedObjects.add(new BoxTypedObject(Collections
                    .unmodifiableMap(childrenProperties)));
        }
        boxItemCollectionProperties.put(BoxCollection.FIELD_ENTRIES,
                boxTypedObjects);
        return new BoxCollection(Collections.unmodifiableMap
                (boxItemCollectionProperties));
    }

    /**
     * Update the document (nx/box sides) thanks to a box folder
     */
    public void save(CoreSession session) throws ClientException,
            ParseException, InvocationTargetException,
            IllegalAccessException, BoxJSONException {

        // Update nx document with new box folder properties
        setTitle(boxFolder.getName());
        setDescription(boxFolder.getDescription());

        // If parent id has been updated -> move the document
        String newParentId = boxFolder.getParent().getId();
        IdRef documentIdRef = new IdRef(doc.getId());
        String oldParentId = session.getParentDocument(documentIdRef).getId();
        if (!oldParentId.equals(newParentId)) {
            session.move(documentIdRef, new IdRef(newParentId),
                    boxFolder.getName());
        }
        setCreator(boxFolder.getOwnedBy().getId());

        TagService tagService = Framework.getLocalService(TagService.class);
        if (tagService != null) {
            tagService.removeTags(session, doc.getId());
            for (String tag : boxFolder.getTags()) {
                tagService.tag(session, doc.getId(), tag,
                        session.getPrincipal().getName());
            }
        }
        session.saveDocument(doc);
        session.save();
    }

}