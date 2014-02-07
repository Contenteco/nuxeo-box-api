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

import com.nuxeo.box.api.BoxAdapter;
import com.nuxeo.box.api.dao.BoxCollection;
import com.nuxeo.box.api.dao.BoxEmail;
import com.nuxeo.box.api.dao.BoxFile;
import com.nuxeo.box.api.dao.BoxFolder;
import com.nuxeo.box.api.dao.BoxItem;
import com.nuxeo.box.api.dao.BoxTypedObject;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

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

        boxItem = new BoxFolder(Collections.unmodifiableMap(boxProperties));
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
        final List<BoxTypedObject> boxChildren = new ArrayList<>();
        for (DocumentModel child : children) {
            final Map<String, Object> childrenProperties = new HashMap<>();
            childrenProperties.put(BoxTypedObject.FIELD_ID, child.getId());
            childrenProperties.put(BoxTypedObject.FIELD_CREATED_AT,
                    ISODateTimeFormat.dateTime().print(
                            new DateTime(child.getPropertyValue("dc:created")
                            )));
            childrenProperties.put(BoxItem.FIELD_MODIFIED_AT,
                    ISODateTimeFormat.dateTime().print(
                            new DateTime(child.getPropertyValue
                                    ("dc:modified"))));
            childrenProperties.put(BoxItem.FIELD_NAME, child.getName());
            BoxTypedObject boxChild;
            // This different instantiation is related to the param type
            // which is automatically added in json payload by Box marshaller
            // following the box object type
            if (child.isFolder()) {
                boxChild = new BoxFolder(Collections
                        .unmodifiableMap(childrenProperties));
            } else {
                boxChild = new BoxFile(Collections
                        .unmodifiableMap(childrenProperties));
            }
            boxChildren.add(boxChild);
        }
        boxItemCollectionProperties.put(BoxCollection.FIELD_ENTRIES,
                boxChildren);
        return new BoxCollection(Collections.unmodifiableMap
                (boxItemCollectionProperties));
    }

}